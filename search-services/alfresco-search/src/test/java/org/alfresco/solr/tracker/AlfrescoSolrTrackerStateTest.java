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

import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
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
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.junit.Assert.assertEquals;

public class AlfrescoSolrTrackerStateTest extends AbstractAlfrescoSolrTests
{
    private static long MAX_WAIT_TIME = 80000;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
    }

    private Acl acl;

    @Before
    public void prepare() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);
        acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, singletonList("jim"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                asList(acl, acl2),
                asList(aclReaders, aclReaders2));

        // Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }

    @Test
    public void shardStateMustBeConsistentWithCoreSummaryStats() throws Exception
    {
        Transaction txn = getTransaction(0, 4);

        Node node1 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node2 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node3 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node4 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        NodeMetaData nodeMetaData1 = getNodeMetaData(node1, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData2 = getNodeMetaData(node2, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData3 = getNodeMetaData(node3, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData4 = getNodeMetaData(node4, txn, acl, "mike", null, false);

        indexTransaction(txn,
                asList(node1, node2, node3, node4),
                asList(nodeMetaData1, nodeMetaData2, nodeMetaData3, nodeMetaData4));

        makeSureTransactionHasBeenIndexed(txn.getId());
        makeSureNodesHaveBeenIndexed("mike", 4);

        SolrCore core = getCore();
        AlfrescoCoreAdminHandler adminHandler =
                of(core).map(SolrCore::getCoreContainer)
                    .map(CoreContainer::getMultiCoreHandler)
                    .map(AlfrescoCoreAdminHandler.class::cast)
                    .orElseThrow(() -> new IllegalStateException("Cannot retrieve the Core Admin Handler on this test core."));

        MetadataTracker tracker =
                of(adminHandler)
                    .map(AlfrescoCoreAdminHandler::getTrackerRegistry)
                    .map(registry -> registry.getTrackerForCore(core.getName(), MetadataTracker.class))
                    .orElseThrow(() -> new IllegalStateException("Cannot retrieve the Metadata tracker on this test core."));


        ShardState state = tracker.getShardState();

        SolrParams params =
                new ModifiableSolrParams()
                    .add(CoreAdminParams.CORE, core.getName())
                    .add(CoreAdminParams.ACTION, "SUMMARY");

        SolrQueryRequest request = new LocalSolrQueryRequest(core, params);
        SolrQueryResponse response = new SolrQueryResponse();
        adminHandler.handleRequest(request, response);

        NamedList<?> summary =
                ofNullable(response.getValues())
                    .map(values -> values.get("Summary"))
                    .map(NamedList.class::cast)
                    .map(values -> values.get(core.getName()))
                    .map(NamedList.class::cast)
                    .orElseGet(NamedList::new);

        assertEquals(state.getLastIndexedChangeSetId(), summary.get("Id for last Change Set in index"));
        assertEquals(state.getLastIndexedChangeSetCommitTime(), summary.get("Last Index Change Set Commit Time"));
        assertEquals(state.getLastIndexedTxCommitTime(), summary.get("Last Index TX Commit Time"));
        assertEquals(state.getLastIndexedTxId(), summary.get("Id for last TX in index"));
    }

    private void makeSureTransactionHasBeenIndexed(long transactionId) throws Exception
    {
        //Check for the TXN state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, transactionId, transactionId + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }

    /**
     * Queries the index using a token from the (dummy) text produced by the test framework.
     * Once the query returns a positive result we are sure the ContentTracker
     *
     * <ol>
     *     <li>
     *         Fetched the text content associated with the test nodes, from Alfresco
     *     </li>
     *     <li>
     *         Computed a fingerprint (using the retrieved text) for each node
     *     </li>
     *     <li>
     *         Updated the nodes definitions in the (Solr)ContentStore and in Solr
     *     </li>
     * </ol>
     *
     * Last but not least, we are also making sure that CommitTracker executed its cycle as well (otherwise documents
     * wouldn't be searchable).
     *
     * @param owner the #FIELD_OWNER which will be used as an additional required query clause.
     * @throws Exception in case the MAX_WAIT_TIME is reached and the node is not in results.
     */
    private void makeSureNodesHaveBeenIndexed(final String owner, final int expectedCount) throws Exception
    {
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), expectedCount, MAX_WAIT_TIME);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, owner)), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), expectedCount, MAX_WAIT_TIME);
    }
}