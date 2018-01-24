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

import org.apache.solr.common.util.Hash;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Acl;


/*
 * @author Joel
 */

public class DBIDRangeRouter implements DocRouter
{

    private long startRange;
    private long endRange;

    public DBIDRangeRouter(long startRange, long endRange) {
        this.startRange = startRange;
        this.endRange = endRange;
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //When routing by DBID range, all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node, long dbidCap) {
        long dbid = node.getId();
        if(dbid >= startRange && dbid < endRange) {
            return true;
        } else {
            return false;
        }
    }
}