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
 * Nodes are evenly distributed over the shards at random based on the murmur hash of the ACL ID.
 * To use this method, when creating a shard add a new configuration property:
 *
 * <ul>
 *     <li>shard.method=ACL_ID</li>
 *     <li>shard.instance=&lt;shard.instance></li>
 *     <li>shard.count=&lt;shard.count></li>
 * </ul>
 *
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
public class ACLIDMurmurRouter implements DocRouter
{
    @Override
    public Boolean routeAcl(int numShards, int shardInstance, Acl acl)
    {
        return (numShards <= 1) || route(acl.getId(), numShards, shardInstance);
    }

    @Override
    public Boolean routeNode(int numShards, int shardInstance, Node node)
    {
        return (numShards <= 1) || route(node.getAclId(), numShards, shardInstance);
    }

    private boolean route(long id, int numShards, int shardInstance)
    {
        String value = Long.toString(id);
        return (Math.abs(Hash.murmurhash3_x86_32(value, 0, value.length(), 77)) % numShards) == shardInstance;
    }
}