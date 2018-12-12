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

import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.client.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class CascadeTrackerTest extends AbstractAlfrescoSolrTests
{
    private static long MAX_WAIT_TIME = 80000;

    private Node folderNode;
    private NodeMetaData childFolderMetaData;

    private Node childFolderNode;
    private NodeMetaData folderMetaData;

    private Node fileNode;
    private NodeMetaData fileMetaData;

    @BeforeClass
    public static void beforeClass() throws Exception 
    {
        initAlfrescoCore("schema.xml");
    }

    /**
     * Setup initial data, which consists of the following hierarchy:
     *
     * |- a top level folder
     * |---- a child folder
     * |------ a file
     */
    @Before
    public void indexTestData() throws Exception
    {
        Acl acl = getTestAcl();

        Transaction txn = getTransaction(0, 3);

        folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        childFolderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        folderMetaData = getNodeMetaData(folderNode, txn, acl, "mike", null, false);
        folderMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("folder1"));

        childFolderMetaData = getNodeMetaData(childFolderNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        childFolderMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("folder2"));

        fileMetaData = getNodeMetaData(fileNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef(),childFolderMetaData.getNodeRef()),false);
        fileMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("file1"));

        upsertData(txn, asList(folderNode,childFolderNode, fileNode), asList(folderMetaData, childFolderMetaData, fileMetaData));
    }

    /**
     * After updating the test data hierarchy (folders and file), the test checks that the cascade tracker properly
     * reflects the changes in the index.
     */
    @Test 
    public void solrTracking_folderUpdate_shouldReIndexFolderAndChildren() throws Exception
    {
        // Update the folder
        Transaction txn = getTransaction(0, 1);

        folderMetaData.getProperties().put(ContentModel.PROP_CASCADE_TX, new StringPropertyValue(Long.toString(txn.getId())));
        folderMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("folder2"));
        folderNode.setTxnId(txn.getId());
        folderMetaData.setTxnId(txn.getId());

        // Change the ancestor on the file just to see if it's been updated
        NodeRef nodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        childFolderMetaData.setAncestors(ancestors(nodeRef));
        fileMetaData.setAncestors(ancestors(nodeRef));

        upsertData(txn, singletonList(folderNode), singletonList(folderMetaData));

        // Check that the ancestor has been changed and indexed
        TermQuery query = new TermQuery(new Term(QueryConstants.FIELD_ANCESTOR, nodeRef.toString()));
        waitForDocCount(query, 2, MAX_WAIT_TIME);

        // Child folder and grandchild document must be updated
        // This is the same query as before but instead of using a Lucene query, it uses the /afts endpoint (request handler)
        ModifiableSolrParams params =
                new ModifiableSolrParams()
                        .add(CommonParams.Q, QueryConstants.FIELD_ANCESTOR + ":\"" + nodeRef.toString() + "\"")
                        .add(CommonParams.QT, "/afts")
                        .add(CommonParams.START, "0")
                        .add(CommonParams.ROWS, "6")
                        .add(CommonParams.SORT, "id asc")
                        .add(CommonParams.FQ, "{!afts}AUTHORITY_FILTER_FROM_JSON");

        SolrServletRequest req =
                areq(params,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"mike\"], \"tenants\": [ \"\" ]}");

        assertQ(req, "*[count(//doc)=2]", "//result/doc[1]/long[@name='DBID'][.='" + childFolderNode.getId() + "']", "//result/doc[2]/long[@name='DBID'][.='" + fileNode.getId() + "']");
    }

    @After
    public void clearQueue()
    {
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
    }

    /**
     * Setup, indexes and returns the ACL used within the tests.
     *
     * @return the ACL used within the test.
     */
    private Acl getTestAcl() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);
        Acl acl = getAcl(aclChangeSet);
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet, singletonList(acl), singletonList(aclReaders));

        //Check for the ACL state stamp.
        BooleanQuery.Builder builder =
                new BooleanQuery.Builder()
                        .add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST))
                        .add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(
                                QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));

        Query waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
        return acl;
    }

    /**
     * Inserts or updates the given data (nodes and corresponding metadata) using the given transaction.
     *
     * @param txn the transaction.
     * @param nodes the nodes.
     * @param metadata the nodes metadata.
     * @throws Exception hopefully never, otherwise the test fails (i.e. data hasn't been properly indexed).
     */
    private void upsertData(Transaction txn, List<Node> nodes, List<NodeMetaData> metadata) throws Exception
    {
        indexTransaction(txn, nodes, metadata);

        BooleanQuery.Builder builder =
                new BooleanQuery.Builder()
                        .add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST))
                        .add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false),BooleanClause.Occur.MUST));

        Query waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }
}