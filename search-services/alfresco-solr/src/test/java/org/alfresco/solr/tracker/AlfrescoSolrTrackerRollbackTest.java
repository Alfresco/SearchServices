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

import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

import java.util.Collection;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;

import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.AlfrescoCoreAdminHandler;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoSolrTrackerRollbackTest extends AbstractAlfrescoSolrTests
{
    private static Log logger = LogFactory.getLog(AlfrescoSolrTrackerTest.class);
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
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
    }


    @Test
    public void testTrackers() throws Exception
    {

        AlfrescoCoreAdminHandler alfrescoCoreAdminHandler = (AlfrescoCoreAdminHandler)h.getCore().getCoreDescriptor().getCoreContainer().getMultiCoreHandler();


        /*
        * Create and index an AclChangeSet.
        */


        AclChangeSet aclChangeSet = getAclChangeSet(1, 1);

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

        TrackerRegistry trackerRegistry = alfrescoCoreAdminHandler.getTrackerRegistry();
        Collection<Tracker> trackers = trackerRegistry.getTrackersForCore(h.getCore().getName());
        MetadataTracker metadataTracker = null;
        CommitTracker commitTracker = null;

        for(Tracker tracker : trackers) {
            if(tracker instanceof MetadataTracker) {
                metadataTracker = (MetadataTracker)tracker;
            } else if(tracker instanceof CommitTracker) {
                commitTracker = (CommitTracker)tracker;
            }
        }

        /*
        * Create and index a Transaction
        */

        //First create a transaction.
        Transaction txn = getTransaction(0, 3, 1);

        //Next create two nodes to update for the transaction
        Node folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node errorNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

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
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();

        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(fileNode.getId()))), 1, MAX_WAIT_TIME);

        //Stop the commit tracker
        //This will allow the metadata tracker to index the next transaction and leave it uncommitted in the index.

        commitTracker.getRunLock().acquire();

        Transaction rollbackTxn = getTransaction(0, 1, 2);

        Node rollbackNode = getNode(rollbackTxn, acl, Node.SolrApiNodeStatus.UPDATED);

        NodeMetaData rollbackMetaData = getNodeMetaData(rollbackNode, rollbackTxn, acl, "mike", null, false);

        indexTransaction(rollbackTxn,
                         list(rollbackNode),
                         list(rollbackMetaData));

        long cycles = metadataTracker.getTrackerState().getTrackerCycles();
        //Wait three tracker cycles
        while (metadataTracker.getTrackerState().getTrackerCycles() < cycles+3) {
            Thread.sleep(1000);
        }

        //Take the rollback transaction out of the queue so it doesn't get re-indexed following the rollback.
        //This will prove the rollback transaction was rolled back

        SOLRAPIQueueClient.transactionQueue.remove(rollbackTxn);
        metadataTracker.setRollback(true);

        commitTracker.getRunLock().release();

        while(commitTracker.getRollbackCount() == 0) {
            Thread.sleep(1000);
        }

        //The rollback occurred
        //Let's add another node and acl

        AclChangeSet afterRollbackAclChangeSet = getAclChangeSet(1, 10);

        Acl afterRollbackAcl = getAcl(aclChangeSet);
        AclReaders afterRollbackAclReaders = getAclReaders(afterRollbackAclChangeSet, afterRollbackAcl, list("joel"), list("phil"), null);
        indexAclChangeSet(afterRollbackAclChangeSet,
                          list(afterRollbackAcl),
                          list(afterRollbackAclReaders));


        Transaction afterRollbackTxn = getTransaction(0, 1, 3);

        Node afterRollbackNode = getNode(afterRollbackTxn, acl, Node.SolrApiNodeStatus.UPDATED);

        //Next create the NodeMetaData for each node. TODO: Add more metadata
        NodeMetaData afterRollbackMetaData = getNodeMetaData(afterRollbackNode, afterRollbackTxn, acl, "mike", null, false);

        //Index the transaction, nodes, and nodeMetaDatas.
        //Note that the content is automatically created by the test framework.
        indexTransaction(afterRollbackTxn,
                         list(afterRollbackNode),
                         list(afterRollbackMetaData));

        //Wait for the node to appear
        //Assert the rolled back transaction is not in the index.

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 3, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(afterRollbackNode.getId()))), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(rollbackNode.getId()))), 0, MAX_WAIT_TIME);

        //Check for the ACL state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, afterRollbackAclChangeSet.getId(), afterRollbackAclChangeSet.getId()+1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }
}
