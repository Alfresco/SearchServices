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
import java.util.Set;

/*
 * @author Joel
 */

public class DocRouterFactory
{
    protected final static Logger log = LoggerFactory.getLogger(DocRouterFactory.class);

    public static DocRouter getRouter(Properties properties, ShardMethodEnum method) {

        if (properties != null)
        {
            String shardDotId = properties.getProperty("shard.id");
            if (shardDotId != null  && !shardDotId.isEmpty())
            {
                try
                {
                    int shardid = Integer.parseInt(shardDotId);
                    log.info("Sharding via an ExplicitRouter for shard "+shardid);
                    return new ExplicitRouter(shardid);
                } catch (NumberFormatException e)
                {
                    log.error("Failed to parse a shard.id of "+shardDotId);
                }
            }
        }

        switch(method) {
            case DB_ID:
                log.info("Sharding via DB_ID");
                return new DBIDRouter();
            case DB_ID_RANGE:
                String range = properties.getProperty("shard.range");
                String[] rangeParts = range.split("-");
                long startRange = Long.parseLong(rangeParts[0].trim());
                long endRange = Long.parseLong(rangeParts[1].trim());
                log.info("Sharding via DB_ID_RANGE");
                return new DBIDRangeRouter(startRange, endRange);
            case ACL_ID:
                log.info("Sharding via ACL_ID");
                return new ACLIDMurmurRouter();
            case MOD_ACL_ID:
                log.info("Sharding via MOD_ACL_ID");
                return new ACLIDModRouter();
            case DATE:
                log.info("Sharding via DATE");
                return new DateMonthRouter(properties.getProperty("shard.date.grouping", "1"));
            case PROPERTY:
                log.info("Sharding via PROPERTY");
                return new PropertyRouter(properties.getProperty("shard.regex", ""));
            default:
                log.info("Sharding via DB_ID (default)");
                return new DBIDRouter();
        }
    }

}