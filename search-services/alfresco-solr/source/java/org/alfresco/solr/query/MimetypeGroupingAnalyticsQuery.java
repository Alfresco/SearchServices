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
package org.alfresco.solr.query;

import java.util.HashMap;

import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.AnalyticsQuery;
import org.apache.solr.search.DelegatingCollector;

/**
 * @author Andy
 *
 */
public class MimetypeGroupingAnalyticsQuery extends AnalyticsQuery
{
    private HashMap<String, String> mappings;
    private boolean group;

    /**
     * @param mappings
     * @param group 
     */
    public MimetypeGroupingAnalyticsQuery(HashMap<String, String> mappings, boolean group)
    {
        this.mappings = mappings;
        this.group = group;
        
    }

    /* (non-Javadoc)
     * @see org.apache.solr.search.AnalyticsQuery#getAnalyticsCollector(org.apache.solr.handler.component.ResponseBuilder, org.apache.lucene.search.IndexSearcher)
     */
    @Override
    public DelegatingCollector getAnalyticsCollector(ResponseBuilder rb, IndexSearcher searcher)
    {
        return new MimetypeGroupingCollector(rb, mappings, group);
    }

}
