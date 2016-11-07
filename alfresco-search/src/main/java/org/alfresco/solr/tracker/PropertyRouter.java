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

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.apache.solr.common.util.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Routes based on a text property field.
 *
 * @author Gethin James
 */
public class PropertyRouter implements DocRouter
{
    protected final static Logger log = LoggerFactory.getLogger(PropertyRouter.class);

    Pattern pattern = null;

    //Fallback to DB_ID routing
    private DocRouter fallback = DocRouterFactory.getRouter(null, ShardMethodEnum.DB_ID);

    public PropertyRouter(String propertyRegEx)
    {
        if (propertyRegEx != null && !propertyRegEx.isEmpty())
        {
            pattern = Pattern.compile(propertyRegEx);
        }
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node)
    {
        if(shardCount <= 1)
        {
            return true;
        }
        String shardBy = node.getShardPropertyValue();

        if (shardBy !=null && pattern != null)
        {
            try
            {
                Matcher matcher = pattern.matcher(shardBy);
                if (matcher.find() && !matcher.group(1).isEmpty())
                {
                    shardBy = matcher.group(1);
                }
                else
                {
                    //If a reqex is specified but it doesn't match, then use the fallback
                    shardBy = null;
                }
            }
            catch (IndexOutOfBoundsException | NullPointerException exc)
            {
                log.debug("Regex matched, but group 1 not found, so falling back to DBID sharding.");
                shardBy = null;
            }
        }

        if (shardBy == null || shardBy.isEmpty())
        {
            log.debug("Property not found or regex not matched, so falling back to DBID sharding.");
            return fallback.routeNode(shardCount,shardInstance,node);
        }

        return (Math.abs(Hash.murmurhash3_x86_32(shardBy, 0, shardBy.length(), 66)) % shardCount) == shardInstance;
    }
}
