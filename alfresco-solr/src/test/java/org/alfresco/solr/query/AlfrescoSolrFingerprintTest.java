/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.solr.query;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.alfresco.solr.AlfrescoSolrUtils.*;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoSolrFingerprintTest extends AbstractAlfrescoSolrTests
{
    private static Log logger = LogFactory.getLog(AlfrescoSolrFingerprintTest.class);
    private static long MAX_WAIT_TIME = 80000;
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema-fingerprint.xml");
    }

    @Before
    public void setUp() throws Exception {
        // if you override setUp or tearDown, you better callf
        // the super classes version
        //clearIndex();
        //assertU(commit());
    }

    @After
    public void clearQueue() throws Exception {
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
        SOLRAPIQueueClient.nodeContentMap.clear();
    }


    @Test
    public void testBasciFingerPrint() throws Exception
    {
        /*
        * Create and index an AclChangeSet.
        */

        logger.info("######### Starting fingerprint test ###########");
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);


        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, list("jim"), list("phil"), null);


        indexAclChangeSet(aclChangeSet,
                list(acl, acl2),
                list(aclReaders, aclReaders2));


        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);

        logger.info("#################### Passed First Test ##############################");

        /*
        * Create and index a Transaction
        */

        //First create a transaction.
        Transaction txn = getTransaction(0, 4);

        //Next create two nodes to update for the transaction
        Node node1 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node2 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node3 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node4 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);


        //Next create the NodeMetaData for each node. TODO: Add more metadata
        NodeMetaData nodeMetaData1 = getNodeMetaData(node1, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData2 = getNodeMetaData(node2, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData3 = getNodeMetaData(node3, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData4 = getNodeMetaData(node4, txn, acl, "mike", null, false);

        List<String> content = new ArrayList();
        int[] sizes = {2000, 1000, 1500, 750};

        Random r = new Random(1);
        String token1 = Integer.toString(Math.abs(r.nextInt()));

        for(int i=0; i<4; i++) {
            Random rand = new Random(1);
            StringBuilder buf = new StringBuilder();
            int size = sizes[i];
            for(int s=0; s<size; s++) {
                if(s>0) {
                    buf.append(" ");
                }
                buf.append(Integer.toString(Math.abs(rand.nextInt())));
            }
            content.add(buf.toString());
        }

        //Index the transaction, nodes, and nodeMetaDatas.
        //Note that the content is automatically created by the test framework.
        indexTransaction(txn,
                list(node1, node2, node3, node4),
                list(nodeMetaData1, nodeMetaData2, nodeMetaData3, nodeMetaData4),
                content);

        //Check for the TXN state stamp.
        logger.info("#################### Started Second Test ##############################");
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();

        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
        logger.info("#################### Passed Second Test ##############################");

        /*
        * Query the index for the content
        */

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", token1)), 4, MAX_WAIT_TIME);

        logger.info("#################### Passed Third Test ##############################");

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "FINGERPRINT:" + node1.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("fl", "DBID,score");
        params.add("start", "0");
        params.add("rows", "6");
        SolrServletRequest req = areq(params, null);
        assertQ(req, "*[count(//doc)=4]",
                "//result/doc[1]/long[@name='DBID'][.='" + node1.getId() + "']",
                "//result/doc[2]/long[@name='DBID'][.='" + node3.getId() + "']",
                "//result/doc[3]/long[@name='DBID'][.='" + node2.getId() + "']",
                "//result/doc[4]/long[@name='DBID'][.='" + node4.getId() + "']");

        params = new ModifiableSolrParams();
        params.add("q", "FINGERPRINT:" + node1.getId() + "_70");  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("fl","DBID,score");
        params.add("start", "0");
        params.add("rows", "6");
        req = areq(params, null);
        assertQ(req, "*[count(//doc)= 2]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node3.getId()+"']");

        params = new ModifiableSolrParams();
        params.add("q", "FINGERPRINT:" + node1.getId()+"_45");  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("fl","DBID,score");
        params.add("start", "0");
        params.add("rows", "6");
        req = areq(params, null);
        assertQ(req, "*[count(//doc)= 3]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node2.getId()+"']");

        params = new ModifiableSolrParams();
        params.add("q", "FINGERPRINT:" + node1.getId()+"_30");
        params.add("qt", "/afts");
        params.add("fl","DBID,score");
        params.add("start", "0");
        params.add("rows", "6");
        req = areq(params, null);
        assertQ(req, "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node4.getId()+"']");

        
        params = new ModifiableSolrParams();
        params.add("q", "FINGERPRINT:" + node4.getId());
        params.add("qt", "/afts");
        params.add("fl","DBID,score");
        params.add("start", "0");
        params.add("rows", "6");
        req = areq(params, null);
        assertQ(req, "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node4.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node1.getId()+"']");
    }
}
