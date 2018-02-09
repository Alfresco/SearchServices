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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/*
 * @author Joel
 */

public class DBIDRangeRouter implements DocRouter
{
    private long startRange;
    private AtomicLong expandableRange;
    private AtomicBoolean expanded = new AtomicBoolean(false);
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public DBIDRangeRouter(long startRange, long endRange) {
        this.startRange = startRange;
        this.expandableRange = new AtomicLong(endRange);
    }

    public void setEndRange(long endRange) {
        expandableRange.set(endRange);
    }

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public void setInitialized(boolean initialized) {
        this.initialized.set(initialized);
    }

    public boolean getInitialized() {
        return this.initialized.get();
    }

    public long getEndRange() {
        return expandableRange.longValue();
    }

    public long getStartRange() {
        return this.startRange;
    }

    public boolean getExpanded() {
        return this.expanded.get();
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //When routing by DBID range, all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node) {
        long dbid = node.getId();
        if(dbid >= startRange && dbid < expandableRange.longValue()) {
            return true;
        } else {
            return false;
        }
    }
}