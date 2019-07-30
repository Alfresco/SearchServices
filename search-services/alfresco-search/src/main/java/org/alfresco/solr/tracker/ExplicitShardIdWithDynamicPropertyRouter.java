/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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

import java.util.Collections;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;

/**
 * Routes a document only if the shardInstance matches the provided shardId.
 * The access control information is duplicated in each shard.
 * The target shard identifier is provided using a (configurable) property of the incoming document.
 *
 * @author agazzarini
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
public class ExplicitShardIdWithDynamicPropertyRouter extends ComposableDocRouter
{
    public ExplicitShardIdWithDynamicPropertyRouter()
    {
        super();
    }

    public ExplicitShardIdWithDynamicPropertyRouter(boolean isInStandaloneMode)
    {
        super(isInStandaloneMode);
    }

    @Override
    public Boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        return true;
    }

    @Override
    public Boolean routeNode(int shardCount, int shardInstance, Node node)
    {
        String shardBy = node.getShardPropertyValue();
        if (shardBy == null || shardBy.trim().length() == 0)
        {
            debug("Shard {}: EXPLICIT_ID routing specified but no shard id property found for node {}", shardInstance, node.getNodeRef());
            return negativeReturnValue();
        }

        try
        {
            int shardid = Integer.parseInt(shardBy.trim());
            return shardid == shardInstance;

        }
        catch (NumberFormatException exception)
        {
            debug("Shard {} EXPLICIT_ID routing specified but failed to parse a shard property value ({}) for node {}",
                    shardInstance,
                    shardBy,
                    node.getNodeRef());
            return negativeReturnValue();
        }
    }
    
    @Override
    public Map<String, String> getProperties(QName shardProperty)
    {
        return (shardProperty == null ? 
                Collections.emptyMap() : 
                Map.of(DocRouterFactory.SHARD_KEY_KEY, shardProperty.getPrefixString()));
    }

}
