/*-
 * #%L
 * Alfresco Solr Search
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.solr.tracker;

import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Acl;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This {@link DocRouter} has been deprecated because it is a special case of {@link DateMonthRouter} with a grouping
 * parameter equal to 3.
 *
 * @see DateMonthRouter
 * @see <a href="https://docs.alfresco.com/search-enterprise/concepts/solr-shard-approaches.html">Search Services sharding methods</a>
 */
@Deprecated
public class DateQuarterRouter implements DocRouter
{
    @Override
    public Boolean routeAcl(int numShards, int shardInstance, Acl acl)
    {
        return true;
    }

    public Boolean routeNode(int numShards, int shardInstance, Node node)
    {
        if(numShards <= 1)
        {
            return true;
        }

        String ISO8601Date = node.getShardPropertyValue();
        Date date = ISO8601DateFormat.parse(ISO8601Date);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        int year  = calendar.get(Calendar.YEAR);

        // Avoid using Math.ceil with Integer
        int countMonths = ((year * 12) + (month+1));
        int grouping = 3;
        int ceilGroupInstance = (countMonths + grouping - 1) / grouping;
        
        return ceilGroupInstance % numShards == shardInstance;
        
    }
}
