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

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoSolrUtils.randomPositiveInteger;
import static org.alfresco.solr.AlfrescoSolrUtils.randomShardCountGreaterThanOne;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExplicitShardIdWithDynamicPropertyRouterTest
{
    private ExplicitShardIdWithDynamicPropertyRouter router;

    @Mock
    private Acl acl;

    @Mock
    private Node node;

    @Before
    public void setUp()
    {
        router = new ExplicitShardIdWithDynamicPropertyRouter();
    }

    @Test
    public void aclsAreReplicatedAcrossShards()
    {
        range(0, 100).forEach(index ->
                assertTrue(router.routeAcl(randomPositiveInteger(), randomPositiveInteger(), acl)));
    }

    @Test
    public void standaloneModeShardPropertyNaN_shouldntAcceptNode()
    {
        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn("This is not a Number");
        assertFalse(router.routeNode(shardCount, shardInstance, node));
    }

    @Test
    public void composableModeShardPropertyNaN_shouldntAcceptNode()
    {
        router = new ExplicitShardIdWithDynamicPropertyRouter(false);

        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn("This is not a Number");
        assertNull(router.routeNode(shardCount, shardInstance, node));
    }

    @Test
    public void standaloneModeShardPropertyValueIsNull_shouldntAcceptTheNode()
    {
        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn(null);

        assertFalse(router.routeNode(shardCount, shardInstance, node));
    }

    @Test
    public void composableModeShardPropertyValueIsNull_shouldRejectTheRequest()
    {
        router = new ExplicitShardIdWithDynamicPropertyRouter(false);

        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn(null);

        assertNull(router.routeNode(shardCount, shardInstance, node));
    }

    @Test
    public void standaloneModeShardPropertyValueIsEmpty_shouldntAcceptNode()
    {
        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn("    \t\t\t \n\n");
        assertFalse(router.routeNode(shardCount, shardInstance, node));
    }

    @Test
    public void composableModeShardPropertyValueIsEmpty_shouldRejectTheRequest()
    {
        router = new ExplicitShardIdWithDynamicPropertyRouter(false);

        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn("    \t\t\t \n\n");
        assertNull(router.routeNode(shardCount, shardInstance, node));
    }

    @Test
    public void explicitShardMatchesShardInstance()
    {
        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn(String.valueOf(shardInstance));

        assertTrue(router.routeNode(shardCount, shardInstance, node));
    }

    @Test
    public void explicitShardDoesntMatchShardInstance()
    {
        int shardCount = randomShardCountGreaterThanOne();
        int shardInstance = randomPositiveInteger();

        when(node.getShardPropertyValue()).thenReturn(String.valueOf(shardInstance));

        assertFalse(router.routeNode(shardCount, shardInstance + 1, node));
    }
}