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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.alfresco.solr.AlfrescoSolrUtils.randomShardCountGreaterThanOne;
import static org.alfresco.solr.AlfrescoSolrUtils.randomPositiveInteger;

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

@RunWith(MockitoJUnitRunner.class)
public class DateMonthRouterTest
{
    private Random randomizer = new Random();

    private DocRouter router;

    @Mock
    private Acl acl;

    @Mock
    private Node node;

    @Before
    public void setUp()
    {
        router = DocRouterFactory.getRouter(new Properties(), ShardMethodEnum.DATE);
    }

    @Test
    public void aclsAreReplicatedAcrossShards()
    {
        range(0, 100).forEach(index -> assertTrue(router.routeAcl(randomizer.nextInt(), randomizer.nextInt(), acl)));
    }

    @Test
    public void invalidDate_shouldFallBackToDBIDRouting()
    {
        DBIDRouter fallbackRouting = mock(DBIDRouter.class);
        ((DateMonthRouter)router).dbidRouter = fallbackRouting;

        when(node.getShardPropertyValue()).thenReturn("Something which is not an ISO Date");

        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        router.routeNode(shardCount, shardInstance, node);

        verify(fallbackRouting).routeNode(shardCount, shardInstance, node);
    }

    @Test
    public void nullDate_shouldFallBackToDBIDRouting()
    {
        DBIDRouter fallbackRouting = mock(DBIDRouter.class);
        ((DateMonthRouter)router).dbidRouter = fallbackRouting;

        when(node.getShardPropertyValue()).thenReturn(null);

        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        router.routeNode(shardCount, shardInstance, node);

        verify(fallbackRouting).routeNode(shardCount, shardInstance, node);
    }

    @Test
    public void twelveShardsInTheCluster_shouldBalanceNodes()
    {
        int [] shardIdentifiers = range(0,12).toArray();
        int shardCount = shardIdentifiers.length;

        router.routeNode(shardCount, 0, node);
        int howManyDocuments = shardCount * 10000;

        Map<Integer, Integer> nodeDistributionMap = new HashMap<>();

        range(0, howManyDocuments)
                .mapToLong(Long::valueOf)
                .forEach(id -> {
                    String date = ISO8601DateFormat.format(new Date(System.currentTimeMillis() + id * (1000L * 60 * 60 * 24 * 30)));
                    Node node = new Node();
                    node.setShardPropertyValue(date);

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

        assertEquals(shardIdentifiers.length, nodeDistributionMap.size());

        // Asserts the standard deviation of the distribution map is in percentage lesser than 30%
        assertTrue(
                nodeDistributionMap.values().toString() + ", SD = " + deviation,
                deviation/(howManyDocuments/shardCount) * 100 < 30);

    }
}