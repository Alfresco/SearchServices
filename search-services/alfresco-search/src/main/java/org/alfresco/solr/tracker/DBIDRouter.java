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

import org.apache.solr.common.util.Hash;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Acl;

/**
 * DBID murmur hash based document router.
 * This method is available in Alfresco Search Services 1.0 and later versions and is the default sharding option in Solr 6.
 * Nodes are evenly distributed over the shards at random based on the murmur hash of the DBID.
 * The access control information is duplicated in each shard.
 * The distribution of nodes over each shard is very even and shards grow at the same rate.
 * Also, this is the fall back method if any other sharding information is unavailable.
 *
 * To use this method, when creating a shard add a new configuration property:
 *
 * <ul>
 *     <li>shard.method=DB_ID</li>
 *     <li>shard.instance=&lt;shard.instance></li>
 *     <li>shard.count=&lt;shard.count></li>
 * </ul>
 *
 * @author Joel
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
public class DBIDRouter implements DocRouter
{
    @Override
    public Boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        return true;
    }

    @Override
    public Boolean routeNode(int shardCount, int shardInstance, Node node)
    {
        if(shardCount <= 1)
        {
            return true;
        }

        String dbid = Long.toString(node.getId());
        return (Math.abs(Hash.murmurhash3_x86_32(dbid, 0, dbid.length(), 77)) % shardCount) == shardInstance;
    }
}