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
import org.apache.solr.common.util.Hash;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.GregorianCalendar;

/*
* @author Joel
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
    public DateMonthRouter(String groupparam) {
        try {
            this.grouping = Integer.parseInt(groupparam);
        } catch (NumberFormatException e) {
            log.error("shard.date.grouping needs to be a valid integer.", e);
            throw e;
        }
    }

    @Override
    public boolean routeAcl(int numShards, int shardInstance, Acl acl) {
        return true;
    }

    @Override
    public boolean routeNode(int numShards, int shardInstance, Node node, long dbidCap) {
        if(numShards <= 1) {
            return true;
        }

        String ISO8601Date = node.getShardPropertyValue();

        if(ISO8601Date == null) {
            return dbidRouter.routeNode(numShards, shardInstance, node, dbidCap);
        }

        Date date = ISO8601DateFormat.parse(ISO8601Date);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int month = cal.get(cal.MONTH);
        int year  = cal.get(cal.YEAR);
        return ((((year * 12) + month)/grouping) % numShards) == shardInstance;

    }
}