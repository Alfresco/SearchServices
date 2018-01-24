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

import java.util.Properties;

/*
 * @author Joel
 */

public class DocRouterFactory
{
    public static DocRouter getRouter(Properties properties, ShardMethodEnum method) {

        switch(method) {
            case DB_ID:
                return new DBIDRouter();
            case DB_ID_RANGE:
                String range = properties.getProperty("shard.range");
                String[] rangeParts = range.split("-");
                long startRange = Long.parseLong(rangeParts[0].trim());
                long endRange = Long.parseLong(rangeParts[1].trim());
                return new DBIDRangeRouter(startRange, endRange);
            case ACL_ID:
                return new ACLIDMurmurRouter();
            case MOD_ACL_ID:
                return new ACLIDModRouter();
            case DATE:
                return new DateMonthRouter(properties.getProperty("shard.date.grouping", "1"));
            case PROPERTY:
                return new PropertyRouter(properties.getProperty("shard.regex", ""));
            default:
                return new DBIDRouter();
        }
    }

}