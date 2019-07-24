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

import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoSolrUtils.randomPositiveInteger;
import static org.alfresco.solr.AlfrescoSolrUtils.randomShardCountGreaterThanOne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExplicitIDWithLRISRouterTest
{
    private DocRouter router;

    @Mock
    private Acl acl;

    @Mock
    private Node node;

    @Before
    public void setUp()
    {
        router = DocRouterFactory.getRouter(new Properties(), ShardMethodEnum.EXPLICIT_ID);
    }

    @Test
    public void aclsAreReplicatedAcrossShards()
    {
        range(0, 100).forEach(index ->
                assertTrue(router.routeAcl(randomPositiveInteger(), randomPositiveInteger(), acl)));
    }

    @Test
    public void shardPropertyEmpty_shouldFallBackToDBID()
    {
        // Don't set the DBID on the node, as the test is doing that in a loop.
        // That allows to use this Node instance as a prototype
        Node node = new Node();
        node.setShardPropertyValue("\n\n\n\n    \t\t");

        assertDataIsDistributedAccordingWithDBIDRouting(node);
    }

    @Test
    public void shardPropertyNull_shouldFallBackToDBID()
    {
        // Don't set the DBID on the node, as the test is doing that in a loop.
        // That allows to use this Node instance as a prototype
        Node node = new Node();
        node.setShardPropertyValue(null);

        assertDataIsDistributedAccordingWithDBIDRouting(node);
    }

    @Test
    public void shardPropertyNaN_shouldFallBackToDBID()
    {
        // Don't set the DBID on the node, as the test is doing that in a loop.
        // That allows to use this Node instance as a prototype
        Node node = new Node();
        node.setShardPropertyValue("This is not a valid Number that can be used as shard ID.");

        assertDataIsDistributedAccordingWithDBIDRouting(node);
    }

    @Test
    public void explicitShardMatchesShardInstance()
    {
        int shardCount = 2;
        int firstShardInstance = 0;
        int secondShardInstance = 1;

        Node prototypeNodeOnFirstShard = new Node();
        prototypeNodeOnFirstShard.setShardPropertyValue(String.valueOf(firstShardInstance));

        Node prototypeNodeOnSecondShard = new Node();
        prototypeNodeOnSecondShard.setShardPropertyValue(String.valueOf(secondShardInstance));

        int howManyDocumentsPerShard = 1000;
        Map<Integer, Integer> nodeDistributionMap = new HashMap<>();

        range(0,2).forEach(shardId -> {
            range(0, howManyDocumentsPerShard)
                    .forEach(index -> {
                        if (router.routeNode(shardCount, shardId, prototypeNodeOnFirstShard))
                        {
                            nodeDistributionMap.merge(shardId, 1, Integer::sum);
                        }

                        if (router.routeNode(shardCount, shardId, prototypeNodeOnSecondShard))
                        {
                            nodeDistributionMap.merge(shardId, 1, Integer::sum);
                        }
                    });
        });

        assertEquals(shardCount, nodeDistributionMap.size());
        assertEquals(howManyDocumentsPerShard, nodeDistributionMap.get(firstShardInstance).intValue());
        assertEquals(howManyDocumentsPerShard, nodeDistributionMap.get(secondShardInstance).intValue());
    }

    @Test
    public void explicitShardDoesntMatchShardInstance()
    {
        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn(String.valueOf(shardInstance));

        assertFalse(router.routeNode(shardCount, shardInstance + 1, node));
    }

    private void assertDataIsDistributedAccordingWithDBIDRouting(Node node)
    {
        int [] shardIdentifiers = range(0, 15).toArray();
        int shardCount = shardIdentifiers.length;
        int howManyDocumentsPerShard = 10000;

        Map<Integer, Integer> nodeDistributionMap = new HashMap<>();

        range(0, shardCount * howManyDocumentsPerShard)
                .mapToLong(Long::valueOf)
                .forEach(id -> {
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