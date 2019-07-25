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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Properties;
import java.util.Random;

import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link DBIDRangeRouter} test case.
 *
 * @author agazzarini
 */
@RunWith(MockitoJUnitRunner.class)
public class DBIDRangeRouterTest
{
    private final Random randomizer = new Random();
    private DocRouter router;

    @Mock
    private Acl acl;

    @Mock
    private Node node;

    @Before
    public void setUp()
    {
        Properties properties = new Properties();
        properties.put("shard.range", "200-20000");
        router = DocRouterFactory.getRouter(properties, ShardMethodEnum.DB_ID_RANGE);
    }

    @Test
    public void aclsAreReplicatedAcrossShards()
    {
        range(0, 100).forEach(index -> assertTrue(router.routeAcl(randomizer.nextInt(), randomizer.nextInt(), acl)));
    }

    @Test
    public void outOfBoundsShouldRejectTheNode()
    {
        when(node.getId()).thenReturn(199L);
        assertFalse(router.routeNode(randomizer.nextInt(), randomizer.nextInt(), node));

        when(node.getId()).thenReturn(20000L);
        assertFalse(router.routeNode(randomizer.nextInt(), randomizer.nextInt(), node));
    }

    @Test
    public void inRange_shouldAcceptTheNode()
    {
        when(node.getId()).thenReturn(200L);
        assertTrue(router.routeNode(randomizer.nextInt(), randomizer.nextInt(), node));

        when(node.getId()).thenReturn(543L);
        assertTrue(router.routeNode(randomizer.nextInt(), randomizer.nextInt(), node));
    }
}