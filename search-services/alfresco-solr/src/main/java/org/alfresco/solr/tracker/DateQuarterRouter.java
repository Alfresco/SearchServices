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

import org.alfresco.util.ISO8601DateFormat;
import org.apache.solr.common.util.Hash;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Acl;
import java.util.Date;
import java.util.GregorianCalendar;

/*
* @author Joel
*/

public class DateQuarterRouter implements DocRouter
{
    public boolean routeAcl(int numShards, int shardInstance, Acl acl) {
        return true;
    }

    public boolean routeNode(int numShards, int shardInstance, Node node) {
        if(numShards <= 1) {
            return true;
        }

        String ISO8601Date = node.getShardPropertyValue();
        //TODO: we can parse the string to make this more efficient rather then creating a calendar.
        Date date = ISO8601DateFormat.parse(ISO8601Date);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int month = cal.get(cal.MONTH);
        int year  = cal.get(cal.YEAR);
        return Math.ceil(((year * 12) + (month+1)) / 3) % numShards == shardInstance;
    }
}