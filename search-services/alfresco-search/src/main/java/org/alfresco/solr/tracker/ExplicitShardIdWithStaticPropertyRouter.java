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

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;

/**
 * Routes the incoming nodes (not ACLs!) on the last registered indexing shard (LRIS).
 * The access control information is duplicated in each shard.
 *
 * @author Elia
 * @author agazzarini
 */
public class ExplicitShardIdWithStaticPropertyRouter extends ComposableDocRouter
{

    public ExplicitShardIdWithStaticPropertyRouter()
    {
        super();
    }

    public ExplicitShardIdWithStaticPropertyRouter(boolean isInStandaloneMode)
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
        Integer explicitShardId = node.getExplicitShardId();

        if (explicitShardId == null)
        {
            debug("ExplicitShardId property is not set for node {} ", node.getNodeRef());
            return negativeReturnValue();
        }

        return explicitShardId.equals(shardInstance);
    }
}