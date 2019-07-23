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

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class AclModCountRouterTest
{
    private DocRouter router;

    @Mock
    private Acl acl;

    @Mock
    private Node node;

    @Before
    public void setUp()
    {
        router = DocRouterFactory.getRouter(new Properties(), ShardMethodEnum.MOD_ACL_ID);
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
    public void sevenShardsInTheCluster_shouldBalanceNodesAndAcls()
    {
        int [] shardIdentifiers = range(0,7).toArray();
        int shardCount = shardIdentifiers.length;
        int howManyDocumentsPerShard = 100;

        // Maps used for validating the data distribution
        Map<Integer, Integer> aclDistributionMap = new HashMap<>();
        Map<Integer, Integer> nodeDistributionMap = new HashMap<>();

        range(0, shardCount * howManyDocumentsPerShard)
                .mapToLong(Long::valueOf)
                .forEach(id -> {
                    when(acl.getId()).thenReturn(id);
                    when(node.getAclId()).thenReturn(id);
                    stream(shardIdentifiers)
                            .forEach(shardId -> {
                                if (router.routeAcl(shardCount, shardId, acl))
                                {
                                    aclDistributionMap.merge(shardId, 1, Integer::sum);
                                }

                                if (router.routeNode(shardCount, shardId, node))
                                {
                                    nodeDistributionMap.merge(shardId, 1, Integer::sum);
                                }
                            });
                    reset(acl, node);
                });

        assertEquals(shardIdentifiers.length, aclDistributionMap.size());
        aclDistributionMap.forEach((k, v) -> assertEquals(howManyDocumentsPerShard, v.intValue()));

        assertEquals(shardIdentifiers.length, nodeDistributionMap.size());
        nodeDistributionMap.forEach((k, v) -> assertEquals(howManyDocumentsPerShard, v.intValue()));
    }
}