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


import org.alfresco.solr.client.Node;

import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;

/*
 * This tracks two things: transactions and metadata nodes
 * @author Joel
 */
public interface DocRouter
{
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl);
    public boolean routeNode(int shardCount, int shardInstance, Node node);
    
    /**
     * Get additional properties to "shardProperty" depending on the Shard Method
     * @param shardProperty custom property used to configure the Router
     * @return pair of key, value 
     */
    default public Map<String, String> getProperties(QName shardProperty) {
        return null;
    }
    
}