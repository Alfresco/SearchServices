/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
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

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.solr.cache.CacheConstants;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.WrappedQuery;

/**
 * Find the set of documents owned by the specified set of authorities,
 * for those authorities that are users (e.g. we're not interested in groups etc.)
 * 
 * @author Matt Ward
 */
public class SolrOwnerSetScorer extends AbstractSolrCachingScorer
{
    /**
     * Package private constructor.
     * @param acceptDocs 
     */
    SolrOwnerSetScorer(Weight weight, DocSet in, LeafReaderContext context, SolrIndexSearcher searcher)
    {
        super(weight, in, context, searcher);
    }

    public static SolrOwnerSetScorer createOwnerSetScorer(Weight weight, LeafReaderContext context, SolrIndexSearcher searcher, String authorities) throws IOException
    {
        
        DocSet authorityOwnedDocs = (DocSet) searcher.cacheLookup(CacheConstants.ALFRESCO_OWNERLOOKUP_CACHE, authorities);
        
        if(authorityOwnedDocs == null)
        {
            // Split the authorities. The first character in the authorities String
            // specifies the separator, e.g. ",jbloggs,abeecher"
            String[] auths = authorities.substring(1).split(authorities.substring(0, 1));

            BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
            for(String current : auths)
            {
                if (AuthorityType.getAuthorityType(current) == AuthorityType.USER)
                {
                    bQuery.add(new TermQuery(new Term(QueryConstants.FIELD_OWNER, current)), Occur.SHOULD);
                }
            }
            
            WrappedQuery wrapped = new WrappedQuery(bQuery.build());
            wrapped.setCache(false);
            authorityOwnedDocs = searcher.getDocSet(wrapped);
        
            searcher.cacheInsert(CacheConstants.ALFRESCO_OWNERLOOKUP_CACHE, authorities, authorityOwnedDocs);
        }
        
        // TODO: Cache the final set? e.g. searcher.cacheInsert(authorities, authorityOwnedDocs)
        return new SolrOwnerSetScorer(weight, authorityOwnedDocs, context, searcher);
       
    }
}
