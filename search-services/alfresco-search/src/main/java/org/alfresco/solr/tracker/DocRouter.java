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

import org.alfresco.solr.client.Node;

import java.util.Collections;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;

/**
 * Defines the logic used for distributing data across the shards.
 * A {@link DocRouter} implementor instance is properly configured on each shard.
 * Each time an incoming document D arrives to the shard S, the DocRouter (on the S instance)
 * will be used for deciding if D needs to be managed (i.e. indexed) by S.
 *
 * The {@link DocRouter} contract requires a concrete implementor to provide the logic for
 * understanding:
 *
 * <li>
 *     <ul>If an incoming ACL belongs to the receiving shard or not</ul>
 *     <ul>If an incoming Node belongs to the receiving shard or not</ul>
 * </li>
 *
 * @author Joel
 */
public interface DocRouter
{
    /**
     * Checks if the incoming ACL document must be indexed on this shard.
     *
     * @param shardCount the total shard count.
     * @param shardInstance the owning shard instance (i.e. instance number).
     * @param acl the ACL.
     * @return true if the ACL must be indexed in the shard which owns this {@link DocRouter} instance, false otherwise.
     */
    Boolean routeAcl(int shardCount, int shardInstance, Acl acl);

    /**
     * Checks if the incoming Node must be indexed on this shard.
     *
     * @param shardCount the total shard count.
     * @param shardInstance the owning shard instance (i.e. instance number).
     * @param node the {@link Node} instance.
     * @return true if the {@link Node} instance must be indexed in the shard which owns this {@link DocRouter} instance, false otherwise.
     */
    Boolean routeNode(int shardCount, int shardInstance, Node node);
    
    /**
     * Get additional properties to "shardProperty" depending on the Shard Method
     * @param shardProperty custom property used to configure the Router
     * @return pair of key, value
     */
    default public Map<String, String> getProperties(QName shardProperty) {
        return Collections.emptyMap();
    }
    
}

