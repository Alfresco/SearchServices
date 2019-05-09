/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoSolrUtils.MAX_WAIT_TIME;
import static org.alfresco.solr.AlfrescoSolrUtils.assertShardAndCoreSummaryConsistency;
import static org.alfresco.solr.AlfrescoSolrUtils.coreAdminHandler;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.alfresco.repo.index.shard.ShardState;
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
import org.alfresco.solr.dataload.TestDataProvider;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.core.SolrCore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A partial state of {@link org.alfresco.solr.TrackerState} is exposed through two interfaces: AdminHandler.SUMMARY and
 * {@link MetadataTracker#getShardState}.
 *
 * This test makes sure that state is consistent across the two mentioned approaches. That is, properties returned by the
 * Core SUMMARY must have the same value of the same properties in the ShardState.
 *
 * @author agazzarini
 */
public class AlfrescoSolrTrackerStateTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
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

    @Before
    public void indexTestAclTransactionAndNodes() throws Exception
    {
        Acl acl = createAndIndexSomeAclData();
        createAndIndexTransactionWithSomeNodes(4, acl, "first");
    }

    @Test
    @SuppressWarnings("deprecated")
    public void shardStateMustBeConsistentWithCoreSummaryStats() throws Exception {
        SolrCore core = getCore();

        MetadataTracker tracker =
                of(coreAdminHandler(core))
                        .map(AlfrescoCoreAdminHandler::getTrackerRegistry)
                        .map(registry -> registry.getTrackerForCore(core.getName(), MetadataTracker.class))
                        .orElseThrow(() -> new IllegalStateException("Cannot retrieve the Metadata tracker on this test core."));

        // 1. First consistency check: ShardState must have the same values of CoreAdmin.SUMMARY report
        ShardState shardStateAfterFirstIndexingRound = tracker.getShardState();
        assertShardAndCoreSummaryConsistency(shardStateAfterFirstIndexingRound, core);

        // 2. Index additional ACLs
        Acl acl = createAndIndexSomeAclData();

        ShardState shardStateAfterIndexingSomeAdditionalAcl= tracker.getShardState();

        // 3. We indexed only ACLs, so ACL data must be different while Transaction data must be the same
        assertEquals(shardStateAfterFirstIndexingRound.getLastIndexedTxId(), shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedTxId());
        assertEquals(shardStateAfterFirstIndexingRound.getLastIndexedTxCommitTime(), shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedTxCommitTime());
        assertNotEquals(shardStateAfterFirstIndexingRound.getLastIndexedChangeSetId(), shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedChangeSetId());
        assertNotEquals(shardStateAfterFirstIndexingRound.getLastIndexedChangeSetCommitTime(), shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedChangeSetCommitTime());

        // Second consistency check: ShardState must have the same values of CoreAdmin.SUMMARY report
        assertShardAndCoreSummaryConsistency(shardStateAfterIndexingSomeAdditionalAcl, core);

        // 4. Index a transaction with 5 other nodes
        createAndIndexTransactionWithSomeNodes(5, acl, "second");

        ShardState shardStateAfterIndexingAnAdditionalTransaction = tracker.getShardState();
        assertNotEquals(shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedTxId(), shardStateAfterIndexingAnAdditionalTransaction.getLastIndexedTxId());
        assertNotEquals(shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedTxCommitTime(), shardStateAfterIndexingAnAdditionalTransaction.getLastIndexedTxCommitTime());
        assertEquals(shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedChangeSetId(), shardStateAfterIndexingAnAdditionalTransaction.getLastIndexedChangeSetId());
        assertEquals(shardStateAfterIndexingSomeAdditionalAcl.getLastIndexedChangeSetCommitTime(), shardStateAfterIndexingAnAdditionalTransaction.getLastIndexedChangeSetCommitTime());

        // 5. Third consistency check: ShardState must have the same values of CoreAdmin.SUMMARY report
        assertShardAndCoreSummaryConsistency(tracker.getShardState(), core);
    }

    private void makeSureTransactionHasBeenIndexed(long transactionId) throws Exception
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, transactionId, transactionId + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }

    private void makeSureNodesHaveBeenIndexed(int expectedCount, String searchText) throws Exception
    {
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", searchText)), expectedCount, MAX_WAIT_TIME);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", searchText)), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "mike")), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), expectedCount, MAX_WAIT_TIME);
    }

    private Acl createAndIndexSomeAclData() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, singletonList("jim"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet, asList(acl, acl2), asList(aclReaders, aclReaders2));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);

        return acl;
    }

    /**
     * Creates and indexes a transaction with a certain number of nodes.
     *
     * @param howManyTestNodes how many nodes we want to index.
     * @param acl the related ACL.
     * @param sampleTextContent a sample text content that will be used to assert nodes have been actually indexed.
     */
    private void createAndIndexTransactionWithSomeNodes(int howManyTestNodes, Acl acl, String sampleTextContent) throws Exception
    {
        Transaction txn = getTransaction(0, howManyTestNodes);
        Map.Entry<List<Node>, List<NodeMetaData>> data = TestDataProvider.nSampleNodesWithSampleContent(acl, txn, howManyTestNodes);

        indexTransaction(txn, data.getKey(), data.getValue(), range(0, howManyTestNodes).mapToObj(index -> sampleTextContent).collect(Collectors.toList()));

        makeSureTransactionHasBeenIndexed(txn.getId());
        makeSureNodesHaveBeenIndexed(data.getKey().size(), sampleTextContent);
    }
}