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
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.apache.solr.common.util.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Routes based on a text property field.
 * In this method, the value of some property is hashed and this hash is used to assign the node to a random shard.
 * All nodes with the same property value will be assigned to the same shard.
 * Each shard will duplicate all the ACL information.
 *
 * To use this method, when creating a shard add the new configuration properties:
 *
 * <ul>
 *     <li>shard.key=cm:creator</li>
 *     <li>shard.method=PROPERTY</li>
 *     <li>shard.instance=&lt;shard.instance></li>
 *     <li>shard.count=&lt;shard.count></li>
 * </ul>
 *
 * It is possible to extract a part of the property value to use for sharding using a regular expression,
 * for example, a year at the start of a string:
 *
 * <ul>
 *     <li>shard.regex=^\d{4}</li>
 * </ul>
 *
 * @author Gethin James
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
public class PropertyRouter implements DocRouter
{
    private final static Logger LOGGER = LoggerFactory.getLogger(PropertyRouter.class);

    Pattern pattern;
    String propertyRegEx;

    //Fallback to DB_ID routing
    DocRouter fallback = DocRouterFactory.getRouter(null, ShardMethodEnum.DB_ID);

    public PropertyRouter(String propertyRegEx)
    {
        if (propertyRegEx != null && propertyRegEx.trim().length() > 0)
        {
            this.propertyRegEx = propertyRegEx;
            this.pattern = Pattern.compile(propertyRegEx.trim());
        }
        else
        {
            this.propertyRegEx = "";
        }
    }

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
                LOGGER.debug("Regex matched, but group 1 not found, so falling back to DBID sharding.");
                shardBy = null;
            }
        }

        if (shardBy == null || shardBy.isEmpty())
        {
            LOGGER.debug("Property not found or regex not matched, so falling back to DBID sharding.");
            return fallback.routeNode(shardCount,shardInstance,node);
        }

        return (Math.abs(Hash.murmurhash3_x86_32(shardBy, 0, shardBy.length(), 66)) % shardCount) == shardInstance;
    }
    
    @Override
    public Map<String, String> getProperties(QName shardProperty)
    {
        return (shardProperty == null ? 
                Collections.emptyMap() : 
                Map.of(DocRouterFactory.SHARD_KEY_KEY, shardProperty.getPrefixString(),
                       DocRouterFactory.SHARD_REGEX_KEY, propertyRegEx));
    }
    
}
