/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;

import java.util.Objects;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;

/**
 * A composable {@link DocRouter} which consists of
 *
 * <ul>
 *     <li>A primary routing strategy</li>
 *     <li>A fallback strategy used in case of failure of the strategy above</li>
 * </ul>
 *
 * @author agazzarini
 */
public class DocRouterWithFallback implements DocRouter
{

    private final DocRouter primaryStrategy;
    private final DocRouter fallbackStrategy;

    public DocRouterWithFallback(DocRouter primaryStrategy, DocRouter fallbackStrategy)
    {
        this.primaryStrategy = Objects.requireNonNull(primaryStrategy);
        this.fallbackStrategy = Objects.requireNonNull(fallbackStrategy);
    }

    @Override
    public Boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        return primaryStrategy.routeAcl(shardCount, shardInstance, acl);
    }

    @Override
    public Boolean routeNode(int shardCount, int shardInstance, Node node)
    {
        return ofNullable(primaryStrategy.routeNode(shardCount, shardInstance, node))
                .orElseGet(() -> ofNullable(fallbackStrategy.routeNode(shardCount, shardInstance, node))
                                    .orElse(false));
    }
    
    @Override
    public Map<String, String> getProperties(Optional<QName> shardProperty)
    {
        return primaryStrategy.getProperties(shardProperty);
    }
}
