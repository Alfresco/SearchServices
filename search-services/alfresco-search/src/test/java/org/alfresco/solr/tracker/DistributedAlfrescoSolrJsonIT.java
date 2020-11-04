/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

import static org.alfresco.solr.AlfrescoSolrUtils.TestActChanges;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;

import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
public class DistributedAlfrescoSolrJsonIT extends AbstractAlfrescoDistributedIT
{
    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(2, getSimpleClassName(), null);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    @Test
    public void testTracker() throws Exception
    {
        TestActChanges testActChanges = new TestActChanges().createBasicTestData();
        AclChangeSet aclChangeSet = testActChanges.getChangeSet();
        Acl acl1 = testActChanges.getFirstAcl();
        Acl acl2 = testActChanges.getSecondAcl();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);
        
        putHandleDefaults();

        //Load 1000 nodes

        int numNodes = 1000;
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaDatas = new ArrayList<>();

        Transaction bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<500; i++)
        {
            Node node = getNode(bigTxn, acl1, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, acl1, "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        for(int i=0; i<500; i++)
        {
            Node node = getNode(bigTxn, acl2, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, acl2, "steve", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);

        QueryResponse queryResponse = query(getDefaultTestClient(),
                                            true,
                                            "{\"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ], \"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                                            params("q", "t1:world", "qt", "/afts", "fq", "{!afts}AUTHORITY_FILTER_FROM_JSON", "shards.qt", "/afts", "start", "0", "rows", "1000", "sort", "id asc"));

        assertEquals(queryResponse.getResults().getNumFound(), 1000);

        queryResponse = query(getDefaultTestClient(),
                              true,
                              null, //Send in null JSON string and pass in the ALFRESCO_JSON parameter instead
                              params("q", "t1:world",
                                     "ALFRESCO_JSON",  "{\"authorities\": [ \"jim\" ], \"tenants\": [ \"\" ], \"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                                      "qt", "/afts",
                                      "fq", "{!afts}AUTHORITY_FILTER_FROM_JSON",
                                      "shards.qt", "/afts",
                                      "start", "0",
                                      "rows", "100",
                                      "sort", "id asc"));

        assertEquals(queryResponse.getResults().getNumFound(), 500);
    }
}

