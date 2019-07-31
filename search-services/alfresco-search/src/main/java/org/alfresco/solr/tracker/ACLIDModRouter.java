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

/**
 * Nodes and access control lists are grouped by their ACL ID.
 * This places the nodes together with all the access control information required to determine the access to a node in the same shard.
 * Both the nodes and access control information are sharded. The overall index size will be smaller than other methods as the ACL index information is not duplicated in every shard.
 * Also, the ACL count is usually much smaller than the node count.
 *
 * This method is beneficial if you have lots of ACLs and the documents are evenly distributed over those ACLs.
 * For example, if you have many Share sites, nodes and ACLs are assigned to shards randomly based on the ACL and the documents to which it applies.
 *
 * The node distribution may be uneven as it depends how many nodes share ACLs.
 * To use this method, when creating a shard add a new configuration property:
 *
 * <ul>
 *     <li>shard.method=MOD_ACL_ID</li>
 *     <li>shard.instance=&lt;shard.instance></li>
 *     <li>shard.count=&lt;shard.count></li>
 * </ul>
 *
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
public class ACLIDModRouter implements DocRouter
{
    @Override
    public Boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        return shardCount <= 1 || route(acl.getId(), shardCount, shardInstance);
    }

    @Override
    public Boolean routeNode(int shardCount, int shardInstance, Node node)
    {
        return (shardCount <= 1) || route(node.getAclId() , shardCount, shardInstance);
    }

    private boolean route(long id, int shardCount, int shardInstance)
    {
        return id % shardCount == shardInstance;
    }
}