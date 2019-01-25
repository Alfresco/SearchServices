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

import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoDistributedTestStatic;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedAlfrescoSolrFingerPrintTest extends AbstractAlfrescoDistributedTestStatic
{
    private static Node[] nodes = new Node[4];
    private static NodeMetaData[] nodesMetada = new NodeMetaData[4];
    
    @BeforeClass
    private static void initData() throws Throwable
    {
        initSolrServers(2,"DistributedAlfrescoSolrFingerPrintTest",null);
        /*
         * Create and index an AclChangeSet.
         */

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
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        /*
         * Create and index a Transaction
         */

        //First create a transaction.
        Transaction txn = getTransaction(0, 4);

        //Next create two nodes to update for the transaction
        nodes[0] = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        nodes[1] = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        nodes[2] = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        nodes[3] = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);


        //Next create the NodeMetaData for each node. TODO: Add more metadata
        nodesMetada[0] = getNodeMetaData(nodes[0], txn, acl, "mike", null, false);
        nodesMetada[1] = getNodeMetaData(nodes[1], txn, acl, "mike", null, false);
        nodesMetada[2] = getNodeMetaData(nodes[2], txn, acl, "mike", null, false);
        nodesMetada[3] = getNodeMetaData(nodes[3], txn, acl, "mike", null, false);

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
            list(nodes[0], nodes[1], nodes[2], nodes[3]),
            list(nodesMetada[0], nodesMetada[1], nodesMetada[2], nodesMetada[3]),
            content);

        //Check for the TXN state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();

        waitForDocCountAllCores(waitForQuery, 1, 80000);

        /*
         * Query the index for the content
         */

        waitForDocCountAllCores(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, 80000);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", token1)), 4, 80000);
    }

    @AfterClass
    private static void destroyData() throws Throwable
    {
        dismissSolrServers();
    }
    
    @Test
    public void testFingerPrint() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
                                       "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
                                        params("q", "FINGERPRINT:"+nodes[0].getId(),
                                                "qt", "/afts",
                                                "shards.qt", "/afts",
                                                "start", "0",
                                                "fl","DBID,score",
                                                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertTrue(docs.getNumFound() == 4);
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertTrue(dbid0 == nodes[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertTrue(dbid1 == nodes[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertTrue(dbid2 == nodes[1].getId());

        SolrDocument doc3 = docs.get(3);
        long dbid3 = (long)doc3.getFieldValue("DBID");
        assertTrue(dbid3 == nodes[3].getId());
    }

    @Test
    public void testFingerPrint2() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + nodes[0].getId()+"_70",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertTrue(docs.getNumFound() == 2);
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertTrue(dbid0 == nodes[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertTrue(dbid1 == nodes[2].getId());
    }

    @Test
    public void testFingerPrint3() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + nodes[0].getId()+"_45",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertTrue(docs.getNumFound() == 3);
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertTrue(dbid0 == nodes[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertTrue(dbid1 == nodes[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertTrue(dbid2 == nodes[1].getId());
    }

    @Test
    public void testFingerPrint4() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:"+nodesMetada[0].getNodeRef().getId(),
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl","DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertTrue(docs.getNumFound() == 4);
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertTrue(dbid0 == nodes[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertTrue(dbid1 == nodes[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertTrue(dbid2 == nodes[1].getId());

        SolrDocument doc3 = docs.get(3);
        long dbid3 = (long)doc3.getFieldValue("DBID");
        assertTrue(dbid3 == nodes[3].getId());
    }

    @Test
    public void testFingerPrint5() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + nodesMetada[0].getNodeRef().getId() +"_70",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertTrue(docs.getNumFound() == 2);
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertTrue(dbid0 == nodes[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertTrue(dbid1 == nodes[2].getId());
    }

    @Test
    public void testFingerPrint6() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + nodesMetada[0].getNodeRef().getId() +"_45",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertTrue(docs.getNumFound() == 3);
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertTrue(dbid0 == nodes[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertTrue(dbid1 == nodes[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertTrue(dbid2 == nodes[1].getId());
    }
    
    
}

