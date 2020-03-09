/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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

import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.solr.client.Node;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

/**
 * The date-based sharding assigns dates sequentially through shards based on the month.
 * For example: If there are 12 shards, each month would be assigned sequentially to each shard, wrapping round and
 * starting again for each year.
 * The non-random assignment facilitates easier shard management - dropping shards or scaling out replication for some
 * date range.
 * Typical ageing strategies could be based on the created date or destruction date.
 *
 * Each shard contains copies of all the ACL information, so this information is replicated in each shard.
 * However, if the property is not present on a node, sharding falls back to the {@link DBIDRouter} to randomly distribute
 * these nodes.
 *
 * To use this method, when creating a shard add the new configuration properties:
 *
 * <ul>
 *     <li>shard.key=exif:dateTimeOriginal</li>
 *     <li>shard.method=DATE</li>
 *     <li>shard.instance=&lt;shard.instance></li>
 *     <li>shard.count=&lt;shard.count></li>
 * </ul>
 *
 * Months can be grouped together, for example, by quarter. Each quarter of data would be assigned sequentially through the available shards.
 *
 * <ul>
 *     <li>shard.date.grouping=3</li>
 * </ul>
 *
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
public class DateMonthRouter implements DocRouter
{
    protected final static Logger log = LoggerFactory.getLogger(DateMonthRouter.class);

    DBIDRouter dbidRouter = new DBIDRouter();
    private final int grouping;

    /**
     * Creates a date month router
     * @param groupparam - the number of months that should be grouped together on a shard before moving to use the next shard in sequence
     */
    public DateMonthRouter(String groupparam)
    {
        try
        {
            this.grouping = Integer.parseInt(groupparam);
        }
        catch (NumberFormatException e)
        {
            log.error("shard.date.grouping needs to be a valid integer.", e);
            throw e;
        }
    }

    @Override
    public Boolean routeAcl(int numShards, int shardInstance, Acl acl)
    {
        return true;
    }

    @Override
    public Boolean routeNode(int numShards, int shardInstance, Node node)
    {
        if(numShards <= 1)
        {
            return true;
        }

        String ISO8601Date = node.getShardPropertyValue();

        if(ISO8601Date == null)
        {
            return dbidRouter.routeNode(numShards, shardInstance, node);
        }

        try
        {
            Date date = ISO8601DateFormat.parse(ISO8601Date);
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            int month = cal.get(Calendar.MONTH);
            int year = cal.get(Calendar.YEAR);
            return ((((year * 12) + month) / grouping) % numShards) == shardInstance;
        }
        catch (Exception exception)
        {
            return dbidRouter.routeNode(numShards, shardInstance, node);
        }
    }
    
    @Override
    public Map<String, String> getProperties(Optional<QName> shardProperty)
    {
        return shardProperty
                .map(QName::getPrefixString)
                .map(prefix -> Map.of(
                            DocRouterFactory.SHARD_KEY_KEY, prefix,
                            DocRouterFactory.SHARD_DATE_GROUPING_KEY, String.valueOf(grouping)))
                .orElse(emptyMap());
    }
}
