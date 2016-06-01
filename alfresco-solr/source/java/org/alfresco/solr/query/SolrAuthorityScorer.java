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

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.solr.cache.CacheConstants;
import org.alfresco.solr.data.GlobalReaders;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Find the set of docs that the supplied authority can read.
 * <p>
 * Note that anyDenyDenies is not directly supported by this class, see {@link AbstractQParser}.
 * 
 * @see AbstractQParser
 * @author Matt Ward
 */
public class SolrAuthorityScorer extends AbstractSolrCachingScorer
{
    SolrAuthorityScorer(Weight weight, DocSet in, LeafReaderContext context, SolrIndexSearcher searcher)
    {
        super(weight, in, context, searcher);
    }

    public static SolrAuthorityScorer createAuthorityScorer(Weight weight, LeafReaderContext context, SolrIndexSearcher searcher, String authority) throws IOException
    {
        Properties p = searcher.getSchema().getResourceLoader().getCoreProperties();
        boolean doPermissionChecks = Boolean.parseBoolean(p.getProperty("alfresco.doPermissionChecks", "true"));
        
        Query key = new SolrAuthorityQuery(authority);
        
        DocSet answer = (DocSet)searcher.cacheLookup(CacheConstants.ALFRESCO_AUTHORITY_CACHE, key);
        if(answer != null)
        {
            // Answer was in the cache, so return it.
            return new SolrAuthorityScorer(weight, answer, context, searcher);
        }
        
        // Answer was not in cache, so build the results, cache and return.        
        final HashSet<String> globalReaders = GlobalReaders.getReaders();

        if (globalReaders.contains(authority) || (doPermissionChecks == false))
        {
            // can read all
            DocSet allDocs = searcher.getDocSet(new MatchAllDocsQuery());
            return new SolrAuthorityScorer(weight, allDocs, context, searcher);
        }

        // Docs for which the authority has explicit read access.
        DocSet readableDocSet = searcher.getDocSet(new SolrReaderQuery(authority));

        // Are all doc owners granted read permissions at a global level?
        if (globalReaders.contains(PermissionService.OWNER_AUTHORITY))
        {
            // Get the set of docs owned by the authority (which they can therefore read).
            DocSet authorityOwnedDocs = searcher.getDocSet(new SolrOwnerQuery(authority));
            // Final set of docs that the authority can read.
            DocSet toCache = readableDocSet.union(authorityOwnedDocs);
            searcher.cacheInsert(CacheConstants.ALFRESCO_AUTHORITY_CACHE, key, toCache);
            return new SolrAuthorityScorer(weight, toCache, context, searcher);
        }
        else
        {
            // for that docs I own that have owner Read rights
            DocSet ownerReadableDocSet = searcher.getDocSet(new SolrReaderQuery(PermissionService.OWNER_AUTHORITY));
            DocSet authorityOwnedDocs = searcher.getDocSet(new SolrOwnerQuery(authority));
           
            // Docs where the authority is an owner and where owners have read rights.
            DocSet docsAuthorityOwnsAndCanRead = ownerReadableDocSet.intersection(authorityOwnedDocs);
            // Final set of docs that the authority can read.
            DocSet toCache = readableDocSet.union(docsAuthorityOwnsAndCanRead);
            searcher.cacheInsert(CacheConstants.ALFRESCO_AUTHORITY_CACHE, key, toCache);
            return new SolrAuthorityScorer(weight, toCache, context, searcher);
        }
    }
}
