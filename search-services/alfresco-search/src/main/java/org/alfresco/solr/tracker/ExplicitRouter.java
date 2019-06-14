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
 * Routes a document only if the shardInstance matches the provided shardId
 */
public class ExplicitRouter implements DocRouter {

    protected final static Logger log = LoggerFactory.getLogger(ExplicitRouter.class);
    private final DocRouter fallbackRouter;

    public ExplicitRouter(DocRouter fallbackRouter) {
        this.fallbackRouter = fallbackRouter;
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node) {

        String shardBy = node.getShardPropertyValue();

        if (shardBy != null && !shardBy.isEmpty())
        {
            try
            {
                int shardid = Integer.parseInt(shardBy);
                return shardid == shardInstance;
            }
            catch (NumberFormatException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Shard "+shardInstance+" EXPLICIT_ID routing specified but failed to parse a shard property value ("+shardBy+") for node "+node.getNodeRef());
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Shard "+shardInstance+" EXPLICIT_ID routing specified but no shard id property found for node "+node.getNodeRef());
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("Shard "+shardInstance+" falling back to DBID routing for node "+node.getNodeRef());
        }
        return fallbackRouter.routeNode(shardCount, shardInstance, node);
    }
}
