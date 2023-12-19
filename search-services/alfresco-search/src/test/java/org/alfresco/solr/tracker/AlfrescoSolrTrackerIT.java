/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.solr.tracker;

import static java.util.Arrays.asList;

import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;
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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.AbstractAlfrescoSolrIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Node.SolrApiNodeStatus;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class AlfrescoSolrTrackerIT extends AbstractAlfrescoSolrIT
{
    private static Log logger = LogFactory.getLog(AlfrescoSolrTrackerIT.class);
    private static long MAX_WAIT_TIME = 80000;
    @BeforeClass
    public static void beforeClass() throws Exception 
    {
        initAlfrescoCore("schema.xml");
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
        SOLRAPIQueueClient.NODE_META_DATA_MAP.clear();
        SOLRAPIQueueClient.TRANSACTION_QUEUE.clear();
        SOLRAPIQueueClient.ACL_CHANGE_SET_QUEUE.clear();
        SOLRAPIQueueClient.ACL_READERS_MAP.clear();
        SOLRAPIQueueClient.ACL_MAP.clear();
        SOLRAPIQueueClient.NODE_MAP.clear();
        SOLRAPIQueueClient.NODE_FAILED_TRANSFORM_LIST.clear();
    }


    @Test
    public void testTrackers() throws Exception
    {
        /*
        * Create and index an AclChangeSet.
        */

        logger.info("######### Starting tracker test ###########");
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet, Long.MAX_VALUE-10); // Test with long value


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
        Transaction txn = getTransaction(0, 2);

        //Next create two nodes to update for the transaction
        Node folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node transformFailedNode = getNode(txn, acl, SolrApiNodeStatus.UPDATED);
        Node errorNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        logger.info("######### error node:"+errorNode.getId());

        //Next create the NodeMetaData for each node. TODO: Add more metadata
        NodeMetaData folderMetaData = getNodeMetaData(folderNode, txn, acl, "mike", null, false);
        NodeMetaData fileMetaData = getNodeMetaData(fileNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        NodeMetaData transformFailedMetaData = getNodeMetaData(transformFailedNode, txn, acl, "lisa", ancestors(folderMetaData.getNodeRef()), false);
        //The errorNodeMetaData will cause an exception.
        NodeMetaData errorMetaData = getNodeMetaData(errorNode, txn, acl, "lisa", ancestors(folderMetaData.getNodeRef()), true);
        SOLRAPIQueueClient.NODE_FAILED_TRANSFORM_LIST.add(transformFailedNode.getId());

        //Index the transaction, nodes, and nodeMetaDatas.
        //Note that the content is automatically created by the test framework.
        indexTransaction(txn,
                         list(errorNode, folderNode, fileNode, transformFailedNode),
                         list(errorMetaData, folderMetaData, fileMetaData, transformFailedMetaData));

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
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(fileNode.getId()))), 1, MAX_WAIT_TIME);

        logger.info("#################### Passed Third Test ##############################");



        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "t1:" + fileNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        SolrServletRequest req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"joel\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]","//result/doc[1]/long[@name='DBID'][.='"+fileNode.getId()+"']");

        logger.info("#################### Passed Fourth Test ##############################");


        //Check for the error doc

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_DOC_TYPE, "ErrorNode")), 1, MAX_WAIT_TIME);

        logger.info("#################### Passed Fifth Test ##############################");

        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s__tr_status@{http://www.alfresco.org/model/content/1.0}content", "transform_failed")), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, MAX_WAIT_TIME);

        SOLRAPIQueueClient.NODE_FAILED_TRANSFORM_LIST.clear();

        //Check if the node with the transform_failed status has been reindexed
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s__tr_status@{http://www.alfresco.org/model/content/1.0}content", "transform_failed")), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 0, MAX_WAIT_TIME);

        logger.info("#################### Passed Sixth Test ##############################");

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
        logger.info("################### ADDING CASCADE TRANSACTION #################");
        indexTransaction(txn1, list(folderNode), list(folderMetaData));

        //Check for the TXN state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn1.getId(), txn1.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, MAX_WAIT_TIME);

        logger.info("#################### Passed Seventh Test ##############################");


        TermQuery termQuery1 = new TermQuery(new Term(QueryConstants.FIELD_ANCESTOR, nodeRef.toString()));

        waitForDocCount(termQuery1, 1, MAX_WAIT_TIME);

        params = new ModifiableSolrParams();
        params.add("q", QueryConstants.FIELD_ANCESTOR+":\"" + nodeRef.toString()+"\"");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"mike\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]","//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");


        logger.info("#################### Passed Eighth Test ##############################");


        //Check that both documents have been indexed and have content.
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 3, MAX_WAIT_TIME);

        logger.info("#################### Passed Ninth Test ##############################");

        testIsContentIndexed(acl);

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

        logger.info("############################ Bulk Nodes:" + nodes.size());
        indexTransaction(txn2, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 553, MAX_WAIT_TIME);

        logger.info("#################### Passed Tenth Test ##############################");

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

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 1553, MAX_WAIT_TIME);

        logger.info("#################### Passed Eleventh Test ##############################");


        //Test the maintenance methods

        fileMetaData.setOwner("amy");
        reindexTransactionId(txn.getId());

        folderMetaData.setOwner("jill");
        reindexNodeId(folderNode.getId());

        // Wait for a document that has the new owner and the content populated.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "amy")), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, MAX_WAIT_TIME);

        logger.info("#################### Passed Twelfth Test ##############################");


        // Wait for a document that has the new owner and the content populated.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "jill")), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, MAX_WAIT_TIME);

        logger.info("#################### Passed Thirteenth Test ##############################");



        params = new ModifiableSolrParams();
        params.add("q", "t1:" + fileNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"amy\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]","//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");

        logger.info("#################### Passed Fourteenth Test ##############################");



        params = new ModifiableSolrParams();
        params.add("q", "t1:" + folderNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"jill\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=1]", "//result/doc[1]/long[@name='DBID'][.='" + folderNode.getId() + "']");

        logger.info("#################### Passed Fifteenth Test ##############################");

        // Change the aclReaders
        aclReaders.setReaders(asList("andy"));
        indexAclId(acl.getId());

        aclReaders2.setReaders(asList("ice"));
        reindexAclId(acl2.getId());


        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "andy")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "ice")), 1, MAX_WAIT_TIME); //Ice should have replaced jim in acl2.
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 0, MAX_WAIT_TIME);

        logger.info("#################### Passed Sixteenth Test ##############################");


        params = new ModifiableSolrParams();
        params.add("q", "t1:" + fileNode.getId());  //Query for an id in the content field. The node id is automatically populated into the content field by test framework
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"andy\"], \"tenants\": [ \"\" ]}");
        //FIX ME assertQ(req, "*[count(//doc)=1]","//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");

        logger.info("#################### Passed Seventeenth Test ##############################");

        // Change the aclReaders
        aclReaders.setReaders(asList("alan"));
        aclReaders2.setReaders(asList("paul"));

        reindexAclChangeSetId(aclChangeSet.getId()); //This should replace "andy" and "ice" with "alan" and "paul"

        //Test that "alan" and "paul" are in the index
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "alan")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "paul")), 1, MAX_WAIT_TIME);

        //Test that "andy" and "ice" are removed
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "andy")), 0, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "ice")), 0, MAX_WAIT_TIME);


        //Test Maintenance acl purge
        purgeAclId(acl2.getId());

        //Test Maintenance node purge
        purgeNodeId(fileNode.getId());

        purgeTransactionId(txn2.getId());

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "paul")), 0, MAX_WAIT_TIME); //paul should be purged
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(fileNode.getId()))), 0, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 1002, MAX_WAIT_TIME); // Refects the purged node and transaction

        logger.info("#################### Passed Eighteenth Test ##############################");


        purgeAclChangeSetId(aclChangeSet.getId());
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "alan")), 0, MAX_WAIT_TIME); //alan should be purged

        //Fix the error node
        errorMetaData.setNodeRef(new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID()));
        //Reload the error node.

        logger.info("Retry the error node");
        retry();
        //The error in the index should disappear.
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_DOC_TYPE, "ErrorNode")), 0, MAX_WAIT_TIME);
        //And the error node should be present
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(errorNode.getId()))), 1, MAX_WAIT_TIME);
        logger.info("#################### Passed Nineteenth Test ##############################");
    }

    // test cm:isContentIndexed property
    private void testIsContentIndexed(Acl acl) throws Exception
    {
        // number of existing documents before running this test
        final int previousNumberOfDocuments = 3;

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), previousNumberOfDocuments, MAX_WAIT_TIME);

        // adds a document with cm:indexControl aspect with "cm:isIndexed" as true and "cm:isContentIndexed" is false
        Transaction txnWithAspect = getTransaction(0, 1);
        Node aspectNode = getNode(txnWithAspect, acl, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData aspectNodeMetaData = getNodeMetaData(aspectNode, txnWithAspect, acl, "mike", null, false);
        aspectNodeMetaData.getAspects().add(ContentModel.ASPECT_INDEX_CONTROL);
        aspectNodeMetaData.getProperties().put(ContentModel.PROP_IS_INDEXED, new StringPropertyValue("true"));
        aspectNodeMetaData.getProperties().put(ContentModel.PROP_IS_CONTENT_INDEXED, new StringPropertyValue("false"));
        indexTransaction(txnWithAspect, list(aspectNode), list(aspectNodeMetaData));

        // the new document is not supposed to be indexed
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), previousNumberOfDocuments, MAX_WAIT_TIME);

        // switch "cm:isContentIndexed" from false to true
        Transaction txnContentIndexedFromTrueToFalse = getTransaction(0, 1);
        aspectNodeMetaData.getProperties().put(ContentModel.PROP_IS_CONTENT_INDEXED, new StringPropertyValue("true"));
        indexTransaction(txnContentIndexedFromTrueToFalse, list(aspectNode), list(aspectNodeMetaData));

        // it's supposed the new document to be indexed
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), previousNumberOfDocuments+1, MAX_WAIT_TIME);

        // switch "cm:isContentIndexed" from true to false
        Transaction txnContentIndexedFromFalseToTrue = getTransaction(0, 1);
        aspectNodeMetaData.getProperties().put(ContentModel.PROP_IS_CONTENT_INDEXED, new StringPropertyValue("false"));
        indexTransaction(txnContentIndexedFromFalseToTrue, list(aspectNode), list(aspectNodeMetaData));

        // it's supposed the new document not to be indexed again
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), previousNumberOfDocuments, MAX_WAIT_TIME);

        // delete document
        Transaction txnDeleteContentIndexed = getTransaction(1, 0);
        indexTransaction(txnDeleteContentIndexed, list(aspectNode), list(aspectNodeMetaData));

        // it's supposed to the new document disappear
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), previousNumberOfDocuments, MAX_WAIT_TIME);
    }
}
