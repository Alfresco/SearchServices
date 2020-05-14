/*-
 * #%L
 * Alfresco Solr Search
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

import static java.util.stream.IntStream.range;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.Random;

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * {@link DBIDRangeRouter} test case.
 *
 * @author agazzarini
 */
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
        MockitoAnnotations.initMocks(this);
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
