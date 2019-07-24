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
import static org.alfresco.solr.AlfrescoSolrUtils.randomPositiveInteger;

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
public class ACLIDMurmurRouterTest
{
    private DocRouter router;

    @Mock
    private Acl acl;

    @Mock
    private Node node;

    @Before
    public void setUp()
    {
        router = DocRouterFactory.getRouter(new Properties(), ShardMethodEnum.ACL_ID);
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
        int howManyDocumentsPerShard = 1000;

        // Maps used for validating the data distribution
        Map<Integer, Integer> aclDistributionMap = new HashMap<>();
        Map<Integer, Integer> nodeDistributionMap = new HashMap<>();

        range(0, shardCount * howManyDocumentsPerShard)
                .mapToLong(Long::valueOf)
                .forEach(id -> {
                    Acl acl = new Acl(randomPositiveInteger(), id);
                    Node node = new Node();
                    node.setAclId(acl.getId());

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
                });

        StandardDeviation sd = new StandardDeviation();
        double aclsDeviation = sd.evaluate(aclDistributionMap.values().stream().mapToDouble(Number::doubleValue).toArray());
        double nodesDeviation = sd.evaluate(nodeDistributionMap.values().stream().mapToDouble(Number::doubleValue).toArray());

        assertEquals(shardIdentifiers.length, nodeDistributionMap.size());
        assertEquals(shardIdentifiers.length, aclDistributionMap.size());

        // Asserts the standard deviation of the distribution map is in percentage lesser than 30%
        assertTrue(aclDistributionMap.values().toString() + ", SD = " + aclsDeviation, aclsDeviation/(howManyDocumentsPerShard) * 100 < 30);
        assertTrue(nodeDistributionMap.values().toString() + ", SD = " + nodesDeviation,nodesDeviation/(howManyDocumentsPerShard) * 100 < 30);
    }
}