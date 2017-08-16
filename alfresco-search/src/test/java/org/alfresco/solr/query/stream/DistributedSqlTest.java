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
package org.alfresco.solr.query.stream;

import java.util.List;

import org.alfresco.solr.stream.AlfrescoSolrStream;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.common.params.SolrParams;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedSqlTest extends AbstractStreamTest
{
	@Rule
    public JettyServerRule jetty = new JettyServerRule(2, this);
    @Test
    public void testSearch() throws Exception
    {
        List<SolrClient> clusterClients = getClusterClients();

        String alfrescoJson = "{ \"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ] }";

        String sql = "select DBID from alfresco where `cm:content` = 'world' order by DBID limit 10 ";

        String shards = getShardsString(clusterClients);
        SolrParams params = params("stmt", sql, "qt", "/sql", "alfresco.shards", shards);
        System.out.println("!!!!!!!!!!!!!!!!!!Shard: " + shards);

        AlfrescoSolrStream tupleStream = new AlfrescoSolrStream(((HttpSolrClient) clusterClients.get(0)).getBaseURL(), params);

        tupleStream.setJson(alfrescoJson);
        List<Tuple> tuples = getTuples(tupleStream);

        assertTrue(tuples.size() == 4);
        assertNodes(tuples, node1, node2, node3, node4);

        String alfrescoJson2 = "{ \"authorities\": [ \"joel\" ], \"tenants\": [ \"\" ] }";
        //Test that the access control is being applied.
        tupleStream = new AlfrescoSolrStream(((HttpSolrClient) clusterClients.get(0)).getBaseURL(), params);
        tupleStream.setJson(alfrescoJson2);
        tuples = getTuples(tupleStream);
        assertTrue(tuples.size() == 2);
        assertNodes(tuples, node1, node2);
    }

}

