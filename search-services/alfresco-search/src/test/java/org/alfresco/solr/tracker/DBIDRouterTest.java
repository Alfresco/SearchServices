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

import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.alfresco.solr.AlfrescoSolrUtils.randomPositiveInteger;
import static org.alfresco.solr.AlfrescoSolrUtils.randomShardCountGreaterThanOne;

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class DBIDRouterTest
{
    private DocRouter router;

    @Mock
    private Acl acl;

    @Mock
    private Node node;

    @Before
    public void setUp()
    {
        router = DocRouterFactory.getRouter(new Properties(), ShardMethodEnum.DB_ID);
    }

    @Test
    public void negativeShardCount_shouldAlwaysReturnTrue()
    {
        // Should never happen
        int negativeShardCount = -14;

        assertTrue(router.routeAcl(negativeShardCount, 1, acl));
        assertTrue(router.routeNode(negativeShardCount, 1, node));
    }

    @Test
    public void zeroShardCount_shouldAlwaysReturnTrue()
    {
        // Should never happen
        int zeroShardCount = 0;

        assertTrue(router.routeAcl(zeroShardCount, 1, acl));
        assertTrue(router.routeNode(zeroShardCount, 1, node));
    }

    @Test
    public void oneShardInTheCluster_shouldAlwaysReturnTrue()
    {
        // Should never happen
        int zeroShardCount = 0;

        assertTrue(router.routeAcl(zeroShardCount, 1, acl));
        assertTrue(router.routeNode(zeroShardCount, 1, node));
    }

    @Test
    public void aclsAreReplicatedAcrossShards()
    {
        range(0, 100).forEach(index -> assertTrue(router.routeAcl(randomShardCountGreaterThanOne(), randomPositiveInteger(), acl)));
    }

    @Test
    public void multipleShardsInTheCluster_shouldBalanceNodes()
    {
        int [] shardIdentifiers = range(0,15).toArray();
        int shardCount = shardIdentifiers.length;
        int howManyDocumentsPerShard = 10000;

        Map<Integer, Integer> nodeDistributionMap = new HashMap<>();

        range(0, shardCount * howManyDocumentsPerShard)
                .mapToLong(Long::valueOf)
                .forEach(id -> {
                    Node node = new Node();
                    node.setId(id);
                    stream(shardIdentifiers)
                            .forEach(shardId -> {
                                if (router.routeNode(shardCount, shardId, node))
                                {
                                    nodeDistributionMap.merge(shardId, 1, Integer::sum);
                                }
                            });
                });

        StandardDeviation sd = new StandardDeviation();
        double deviation = sd.evaluate(nodeDistributionMap.values().stream().mapToDouble(Number::doubleValue).toArray());
        double norm = deviation/(howManyDocumentsPerShard) * 100;

        assertEquals(shardIdentifiers.length, nodeDistributionMap.size());

        // Asserts the standard deviation of the distribution map is in percentage lesser than 30%
        assertTrue(
                nodeDistributionMap.values().toString() + ", SD = " + deviation + ", SD_NORM = " + norm + "%",
                norm < 30);
    }
}