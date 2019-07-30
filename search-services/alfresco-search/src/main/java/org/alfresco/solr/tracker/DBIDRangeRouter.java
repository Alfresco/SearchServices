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

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;

/**
 * This routes documents within specific DBID ranges to specific shards.
 * It adds new shards to the cluster without requiring a reindex.
 * The access control information is duplicated in each shard.
 * DBID range sharding is the only option to offer auto-scaling as opposed to defining your exact shard count at the start.
 * All the other sharding methods require repartitioning in some way.
 *
 * For each shard, you specify the range of DBIDs to be included. As your repository grows you can add shards.
 *
 * To use this method, when creating a shard add a new configuration property:
 *
 * <ul>
 *     <li>shard.method=DB_ID_RANGE</li>
 *     <li>shard.range=0-20000000</li>
 *     <li>shard.instance=&lt;shard.instance></li>
 * </ul>
 *
 * @author joel
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
public class DBIDRangeRouter implements DocRouter
{
    private long startRange;
    private AtomicLong expandableRange;
    private AtomicBoolean expanded = new AtomicBoolean(false);
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public DBIDRangeRouter(long startRange, long endRange)
    {
        this.startRange = startRange;
        this.expandableRange = new AtomicLong(endRange);
    }

    public void setEndRange(long endRange)
    {
        expandableRange.set(endRange);
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded.set(expanded);
    }

    public void setInitialized(boolean initialized)
    {
        this.initialized.set(initialized);
    }

    public boolean getInitialized()
    {
        return this.initialized.get();
    }

    public long getEndRange()
    {
        return expandableRange.longValue();
    }

    public long getStartRange()
    {
        return this.startRange;
    }

    public boolean getExpanded()
    {
        return this.expanded.get();
    }

    @Override
    public Boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        return true;
    }

    @Override
    public Boolean routeNode(int shardCount, int shardInstance, Node node)
    {
        long dbid = node.getId();
        return dbid >= startRange && dbid < expandableRange.longValue();
    }

    @Override
    public Map<String, String> getProperties(QName shardProperty)
    {
        return Map.of(DocRouterFactory.SHARD_RANGE_KEY, startRange + "-" + expandableRange);
    }
    
}
