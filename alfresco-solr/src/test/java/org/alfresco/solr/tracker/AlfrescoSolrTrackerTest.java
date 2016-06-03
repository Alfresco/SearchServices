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
package org.alfresco.solr.tracker;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.AlfrescoSolrTestCaseJ4;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoSolrTrackerTest extends AlfrescoSolrTestCaseJ4
{
    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        // if you override setUp or tearDown, you better callf
        // the super classes version
        super.setUp();
        clearIndex();
        assertU(commit());
    }

    @Test
    public void testTrackers() throws Exception
    {
        /*
        * Create and index an AclChangeSet.
        */

        System.out.println("######### Starting tracker test ###########");
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
        waitForDocCount(waitForQuery, 1, 80000);

        System.out.println("#################### Passed First Test ##############################");


        /*
        * Create and index a Transaction
        */

        //First create a transaction.
        Transaction txn = getTransaction(0, 2);

        //Next create two nodes to update for the transaction
        Node folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node errorNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        System.out.println("######### error node:"+errorNode.getId());

        //Next create the NodeMetaData for each node. TODO: Add more metadata
        NodeMetaData folderMetaData = getNodeMetaData(folderNode, txn, acl, "mike", null, false);
        NodeMetaData fileMetaData   = getNodeMetaData(fileNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        //The errorNodeMetaData will cause an exception.
        NodeMetaData errorMetaData   = getNodeMetaData(errorNode, txn, acl, "lisa", ancestors(folderMetaData.getNodeRef()), true);

        //Index the transaction, nodes, and nodeMetaDatas.
        //Note that the content is automatically created by the test framework.
        indexTransaction(txn,
                         list(errorNode, folderNode, fileNode),
                         list(errorMetaData, folderMetaData, fileMetaData));

        //Check for the TXN state stamp.
        System.out.println("#################### Started Second Test ##############################");
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();

        waitForDocCount(waitForQuery, 1, 80000);
        System.out.println("#################### Passed Second Test ##############################");



        /*
        * Query the index for the content
        */
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, 80000);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2, 80000);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(fileNode.getId()))), 1, 80000);

        System.out.println("#################### Passed Third Test ##############################");



        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "t1:" + fileNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        SolrServletRequest req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"joel\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]",
                "//result/doc[1]/long[@name='DBID'][.='"+fileNode.getId()+"']");

        System.out.println("#################### Passed Fourth Test ##############################");




        //Check for the error doc

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_DOC_TYPE, "ErrorNode")), 1, 80000);

        System.out.println("#################### Passed Fifth Test ##############################");




        //Mark the folder as needing cascade
        Transaction txn1 = getTransaction(0, 1);
        //Update the properties on the Node and NodeMetaData to simulate an update to the Node.
        folderMetaData.getProperties().put(ContentModel.PROP_CASCADE_TX, new StringPropertyValue(Long.toString(txn1.getId())));
        folderNode.setTxnId(txn1.getId()); // Update the txnId
        folderMetaData.setTxnId(txn1.getId());


        //Change the ancestor on the file just to see if it's been updated
        NodeRef nodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        fileMetaData.setAncestors(ancestors(nodeRef));

        //This will add the PROP_CASCADE_TX property to the folder.
        System.out.println("################### ADDING CASCADE TRANSACTION #################");
        indexTransaction(txn1, list(folderNode), list(folderMetaData));

        //Check for the TXN state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn1.getId(), txn1.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, 80000);

        System.out.println("#################### Passed Sixth Test ##############################");




        TermQuery termQuery1 = new TermQuery(new Term(QueryConstants.FIELD_ANCESTOR, nodeRef.toString()));

        waitForDocCount(termQuery1, 1, 80000);

        params = new ModifiableSolrParams();
        params.add("q", QueryConstants.FIELD_ANCESTOR+":\"" + nodeRef.toString()+"\"");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"mike\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]",
                "//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");


        System.out.println("#################### Passed Seventh Test ##############################");


        //Check that both documents have been indexed and have content.
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2, 80000);

        System.out.println("#################### Passed Eighth Test ##############################");


        //Try bulk loading

        Transaction txn2 = getTransaction(0, 550);

        List<Node> nodes = new ArrayList();
        List<NodeMetaData> nodeMetaDatas = new ArrayList();

        for(int i=0; i<550; i++)
        {
            Node n = getNode(txn2, acl, Node.SolrApiNodeStatus.UPDATED);
            NodeMetaData nm = getNodeMetaData(n, txn2, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
            nodes.add(n);
            nodeMetaDatas.add(nm);
        }

        System.out.println("############################ Bulk Nodes:" + nodes.size());
        indexTransaction(txn2, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 552, 80000);

        System.out.println("#################### Passed Ninth Test ##############################");

        for(int i=0; i<1000; i++)
        {
            Transaction txnX = getTransaction(0, 1);
            List<Node> nodesX = new ArrayList();
            List<NodeMetaData> nodeMetaDatasX = new ArrayList();
            Node n = getNode(txnX, acl, Node.SolrApiNodeStatus.UPDATED);
            NodeMetaData nm = getNodeMetaData(n, txnX, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
            nodesX.add(n);
            nodeMetaDatasX.add(nm);
            indexTransaction(txnX, nodesX, nodeMetaDatasX);
        }

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 1552, 80000);

        System.out.println("#################### Passed Tenth Test ##############################");


        //Test the maintenance methods

        fileMetaData.setOwner("amy");
        reindexTransactionId(txn.getId());

        folderMetaData.setOwner("jill");
        reindexNodeId(folderNode.getId());

        // Wait for a document that has the new owner and the content populated.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "amy")), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, 80000);

        System.out.println("#################### Passed Eleventh Test ##############################");


        // Wait for a document that has the new owner and the content populated.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "jill")), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, 80000);

        System.out.println("#################### Passed Twelth Test ##############################");



        params = new ModifiableSolrParams();
        params.add("q", "t1:" + fileNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"amy\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]",
                "//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");

        System.out.println("#################### Passed Fourteenth Test ##############################");



        params = new ModifiableSolrParams();
        params.add("q", "t1:" + folderNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"jill\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]",
                "//result/doc[1]/long[@name='DBID'][.='" + folderNode.getId() + "']");

        System.out.println("#################### Passed Fifteenth Test ##############################");


        List<String> readers = aclReaders.getReaders();
        readers.set(0, "andy"); // Change the aclReader
        indexAclId(acl.getId());

        List<String> readers2 = aclReaders2.getReaders();
        readers2.set(0, "ice"); // Change the aclReader
        reindexAclId(acl2.getId());


        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "andy")), 1, 80000);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "ice")), 1, 80000); //Ice should have replaced jim in acl2.
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 0, 80000);

        System.out.println("#################### Passed Sixteenth Test ##############################");


        params = new ModifiableSolrParams();
        params.add("q", "t1:" + fileNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"andy\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]",
                "//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");

        System.out.println("#################### Passed Seventeenth Test ##############################");

        readers.set(0, "alan"); // Change the aclReader
        readers2.set(0, "paul"); // Change the aclReader

        reindexAclChangeSetId(aclChangeSet.getId()); //This should replace "andy" and "ice" with "alan" and "paul"

        //Test that "alan" and "paul" are in the index
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "alan")), 1, 80000);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "paul")), 1, 80000);

        //Test that "andy" and "ice" are removed
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "andy")), 0, 80000);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "ice")), 0, 80000);


        //Test Maintenance acl purge
        purgeAclId(acl2.getId());

        //Test Maintenance node purge
        purgeNodeId(fileNode.getId());

        purgeTransactionId(txn2.getId());

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "paul")), 0, 80000); //paul should be purged
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(fileNode.getId()))), 0, 80000);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 1001, 80000); // Refects the purged node and transaction

        System.out.println("#################### Passed Eighteenth Test ##############################");


        purgeAclChangeSetId(aclChangeSet.getId());
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "alan")), 0, 80000); //alan should be purged

        //Fix the error node
        errorMetaData.setNodeRef(new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID()));
        //Reload the error node.

        System.out.println("Retry the error node");
        retry();
        //The error in the index should disappear.
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_DOC_TYPE, "ErrorNode")), 0, 80000);
        //And the error node should be present
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(errorNode.getId()))), 1, 80000);
        System.out.println("#################### Passed Nineteenth Test ##############################");
        //assert(false);
    }
}
