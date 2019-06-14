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
 * Routes a document only if the explicitShardId matches the provided shardId
 *
 * @author Elia
 */
public class LastRegisteredShardRouter implements DocRouter
{

    protected final static Logger log = LoggerFactory.getLogger(ExplicitRouter.class);

    public LastRegisteredShardRouter()
    {
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        //all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node)
    {

        Integer explicitShardId = node.getExplicitShardId();

        if (explicitShardId == null)
        {
            log.error("explicitShardId is not set for node " + node.getNodeRef());
            return false;
        }

        return explicitShardId.equals(shardInstance);

    }
}
