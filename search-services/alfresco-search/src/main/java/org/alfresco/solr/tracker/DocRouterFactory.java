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

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/*
 * @author Joel
 */

public class DocRouterFactory
{
	private final static Logger LOGGER = LoggerFactory.getLogger(DocRouterFactory.class);
    
    public static final String SHARD_KEY_KEY = "shard.key";
    public static final String SHARD_RANGE_KEY = "shard.range";
    public static final String SHARD_DATE_GROUPING_KEY = "shard.date.grouping";
    public static final String SHARD_REGEX_KEY = "shard.regex";

    public static DocRouter getRouter(Properties properties, ShardMethodEnum method) {

        switch(method) {
            case DB_ID:
                LOGGER.info("Sharding via DB_ID");
                return new DBIDRouter();
            case DB_ID_RANGE:
                if(properties.containsKey(SHARD_RANGE_KEY))
                {
                    LOGGER.info("Sharding via DB_ID_RANGE");
                    String[] pair =properties.getProperty(SHARD_RANGE_KEY).split("-");
                    long start = Long.parseLong(pair[0]);
                    long end = Long.parseLong(pair[1]);
                    return new DBIDRangeRouter(start, end);
                }
            case ACL_ID:
                LOGGER.info("Sharding via ACL_ID");
                return new ACLIDMurmurRouter();
            case MOD_ACL_ID:
                LOGGER.info("Sharding via MOD_ACL_ID");
                return new ACLIDModRouter();
            case DATE:
                LOGGER.info("Sharding via DATE");
                return new DateMonthRouter(properties.getProperty(SHARD_DATE_GROUPING_KEY, "1"));
            case PROPERTY:
                LOGGER.info("Sharding via PROPERTY");
                return new PropertyRouter(properties.getProperty(SHARD_REGEX_KEY, ""));
            case LAST_REGISTERED_INDEXING_SHARD:
                LOGGER.warn("Sharding via LAST_REGISTERED_INDEXING_SHARD: Note this is available at the moment as an Early Access/preview feature!");
                return new ExplicitShardIdWithStaticPropertyRouter();
            case EXPLICIT_ID_FALLBACK_LRIS:
                LOGGER.warn("Sharding via EXPLICIT_ID_FALLBACK_LRIS: Note the LRIS Router (which is part of this composite router) is available at the moment as an Early Access/preview feature!");
                return new DocRouterWithFallback(
                        new ExplicitShardIdWithDynamicPropertyRouter(false),
                        new ExplicitShardIdWithStaticPropertyRouter());
            case EXPLICIT_ID:
                LOGGER.info("Sharding via EXPLICIT_ID");
                return new DocRouterWithFallback(
                        new ExplicitShardIdWithDynamicPropertyRouter(false),
                        new DBIDRouter());
            default:
                LOGGER.warn("WARNING! Unknown/unsupported sharding method ({}). System will fallback to DB_ID", method);
                return new DBIDRouter();
        }
    }

}
