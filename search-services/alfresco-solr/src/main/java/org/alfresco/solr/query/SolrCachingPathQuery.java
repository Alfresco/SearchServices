/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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

import java.io.IOException;

import org.alfresco.solr.cache.CacheConstants;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.WrappedQuery;

/**
 * Decorator that executes a SolrPathQuery and returns cached results where possible.
 * 
 * @author Andy
 */
public class SolrCachingPathQuery extends Query
{
    final SolrPathQuery pathQuery;

    public SolrCachingPathQuery(SolrPathQuery pathQuery)
    {
        if (pathQuery == null) throw new IllegalStateException("pathQuery cannot be null");
        this.pathQuery = pathQuery;
    }
    
    /*
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher)
     */
    public Weight createWeight(IndexSearcher indexSearcher, boolean requiresScore) throws IOException
    {
        SolrIndexSearcher searcher = null;
        if(!(indexSearcher instanceof SolrIndexSearcher))
        {
            throw new IllegalStateException("Must have a SolrIndexSearcher");
        }
        else
        {
            searcher = (SolrIndexSearcher)indexSearcher;
        }

        DocSet results = (DocSet) searcher.cacheLookup(CacheConstants.ALFRESCO_PATH_CACHE, pathQuery);
        if (results == null)
        {
            // Cache miss: get path query results and cache them
            WrappedQuery wrapped = new WrappedQuery(pathQuery);
            wrapped.setCache(false);
            results = searcher.getDocSet(wrapped);
            searcher.cacheInsert(CacheConstants.ALFRESCO_PATH_CACHE, pathQuery, results);
        }

        return new ConstantScoreQuery(results.getTopFilter()).createWeight(searcher, false);
    }

    /*
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    public String toString(String field)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CACHED -> :");
        stringBuilder.append(pathQuery.toString());
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolrCachingPathQuery)) return false;

        SolrCachingPathQuery that = (SolrCachingPathQuery) o;
        return pathQuery.equals(that.pathQuery);

    }

    @Override
    public int hashCode() {
        return pathQuery.hashCode();
    }
}
