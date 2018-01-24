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

/*
 * @author Joel
 */

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;

public class ACLIDModRouter implements DocRouter
{
    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        if(shardCount <= 1) {
            return true;
        }

        return acl.getId() % shardCount == shardInstance;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node, long dbidCap) {
        if(shardCount <= 1) {
            return true;
        }

        //Route the node based on the mod of the aclId
        return node.getAclId() % shardCount == shardInstance;
    }
}