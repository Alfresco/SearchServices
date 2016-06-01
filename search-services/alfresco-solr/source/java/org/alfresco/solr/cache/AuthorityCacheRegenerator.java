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
package org.alfresco.solr.cache;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.SolrCache;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Cache regeneration for AUTHORITY and AUTHSET queries.
 * 
 * @author Matt Ward
 */
public class AuthorityCacheRegenerator implements CacheRegenerator
{
    @SuppressWarnings({ "rawtypes" })
    @Override
    public boolean regenerateItem(SolrIndexSearcher newSearcher, SolrCache newCache,
                SolrCache oldCache, Object oldKey, Object oldVal) throws IOException
    {
        if (oldKey instanceof Query)
        {
            // The authority cache contains results keyed by SolrAuthorityQuery
            // and SolrAuthoritySetQuery.
            Query authQuery = (Query) oldKey;
            // Execute the query on the new searcher - resulting in cache population as a side-effect.
            newSearcher.getDocSet(authQuery);
        }
        return true;
    }
}
