/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2025 Alfresco Software Limited
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

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.client.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.alfresco.solr.AlfrescoSolrUtils.*;

@SolrTestCaseJ4.SuppressSSL
public class DistributedAlfrescoSolrMetadataTrackerReindexingByTransactionIdUsingDbIdShardingMethodIT extends AbstractAlfrescoDistributedIT
{
    private static final int NUMBER_OF_SHARDS = 3;

    @BeforeClass
    public static void initData() throws Throwable
    {
        Properties properties = new Properties();
        properties.put("shard.method", ShardMethodEnum.DB_ID.toString());
        initSolrServers(NUMBER_OF_SHARDS, getSimpleClassName(), properties);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    @Test
    public void shouldReindexNodeOnlyOnShardWhereNodeBelongsTo() throws Exception
    {
        //GIVEN
        putHandleDefaults();
        Node node = indexNewNode();
        long nodeId = node.getId();
        long txnId = node.getTxnId();
        assertThatNodeIsIndexedOnExactlyOneShard(nodeId);

        //WHEN
        triggerTransactionReindexing(txnId);

        //THEN
        for (int assertCount = 1; assertCount <= 10; assertCount++)
        {
            assertThatNodeIsIndexedOnExactlyOneShard(nodeId);
            Thread.sleep(Duration.ofSeconds(1).toMillis());
        }
    }

    private void triggerTransactionReindexing(long txnId) throws Exception
    {
        final Map<String, SolrCore> cores = getCores(solrShards)
                .stream()
                .collect(Collectors.toMap(SolrCore::getName, Function.identity()));

        final List<AlfrescoCoreAdminHandler> adminHandlers = getAdminHandlers(solrShards);
        assertEquals(NUMBER_OF_SHARDS, adminHandlers.size());

        for (int i = 0; i < NUMBER_OF_SHARDS; i++)
        {
            final SolrCore core = cores.get("shard" + i);
            assertNotNull(core);

            final AlfrescoCoreAdminHandler admin = adminHandlers.get(i);
            assertNotNull(admin);

            final SolrQueryRequest reindexRequest = new LocalSolrQueryRequest(core,
                params(
                        CoreAdminParams.ACTION, "REINDEX",
                        CoreAdminParams.CORE, core.getName(),
                        "txid", Long.toString(txnId)
                      )
            );
            final SolrQueryResponse reindexResponse = new SolrQueryResponse();

            admin.handleRequestBody(reindexRequest, reindexResponse);
            assertNull(reindexResponse.getException());
        }
    }

    private void assertThatNodeIsIndexedOnExactlyOneShard(long nodeId)
    {
        final List<SolrClient> allClients = getShardedClients();
        assertEquals(NUMBER_OF_SHARDS, allClients.size());

        final ModifiableSolrParams queryParams = params("qt", "/afts", "q", "DBID:" + nodeId);

        final long sumForAllShards = allClients.stream().map(c -> {
            try
            {
                return c.query(queryParams);
            } catch (SolrServerException | IOException e)
            {
                throw new RuntimeException(e);
            }
        }).map(QueryResponse::getResults).mapToLong(SolrDocumentList::getNumFound).sum();

        assertEquals(1, sumForAllShards);
    }

    private Node indexNewNode() throws Exception
    {
        final Transaction tx = getTransaction(0, 1);
        final Acl acl = getAcl();

        final Node node = getNode(tx, acl, Node.SolrApiNodeStatus.UPDATED);
        final NodeMetaData nodeMetaData = getNodeMetaData(node, tx, acl, "testOwner", null, false);

        indexTransaction(tx, List.of(node), List.of(nodeMetaData));
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 1, 100000);

        return node;
    }

    private Acl getAcl() throws Exception
    {
        TestActChanges testActChanges = new TestActChanges().createBasicTestData();
        AclChangeSet aclChangeSet = testActChanges.getChangeSet();

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        return testActChanges.getFirstAcl();
    }
}
