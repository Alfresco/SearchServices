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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes the incoming nodes (not ACLs!) on the shard explicitly indicated in {@link Node#getExplicitShardId()} method.
 * The access control information is duplicated in each shard.
 *
 * <p><br/><b>
 *  WARNING: This {@link DocRouter} is part of an early access/preview routing feature called
 *  Last-Registered-Indexing-Shard (LRIS), where the {@link Node#getExplicitShardId()} is filled with the identifier of
 *  the last Master Shard which subscribed the cluster.
 *
 * </b><br/><br/>
 * </p>
 *
 * Specifically, until the whole feature will be officially released, the LRIS document routing feature is not compatible
 * with the "Purge" action on the Alfresco Admin Console.
 * Note that at time of writing, the "Purge on startup" option in the Admin Console is enabled by default so prior to
 * build your search cluster, you have to make sure that option is unchecked.
 *
 * @author Elia
 * @author agazzarini
 * @since 1.4
 */
public class ExplicitShardIdWithStaticPropertyRouter extends ComposableDocRouter
{
    private final static Logger log = LoggerFactory.getLogger(ExplicitShardIdWithStaticPropertyRouter.class);

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