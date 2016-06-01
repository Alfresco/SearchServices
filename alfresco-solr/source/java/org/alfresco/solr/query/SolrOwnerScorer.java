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

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.solr.cache.CacheConstants;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.FixedBitSet;
import org.apache.solr.search.BitDocSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Find the set of documents for which the specified authority is the owner.
 * If the authority is not a user then an empty set is returned.
 * 
 * @author Matt Ward
 */
public class SolrOwnerScorer extends AbstractSolrCachingScorer
{
    SolrOwnerScorer(Weight weight, DocSet in, LeafReaderContext context, SolrIndexSearcher searcher)
    {
        super(weight, in, context, searcher);
    }

    public static SolrOwnerScorer createOwnerScorer(Weight weight, LeafReaderContext context, SolrIndexSearcher searcher, String authority) throws IOException
    {
        if (AuthorityType.getAuthorityType(authority) == AuthorityType.USER)
        {
            DocSet ownedDocs = (DocSet) searcher.cacheLookup(CacheConstants.ALFRESCO_OWNERLOOKUP_CACHE, authority);

            if (ownedDocs == null)
            {
                // Cache miss: query the index for docs where the owner matches the authority. 
                ownedDocs = searcher.getDocSet(new TermQuery(new Term(QueryConstants.FIELD_OWNER, authority)));
                searcher.cacheInsert(CacheConstants.ALFRESCO_OWNERLOOKUP_CACHE, authority, ownedDocs);
            }
            return new SolrOwnerScorer(weight, ownedDocs, context, searcher);
        }
        
        // Return an empty doc set, as the authority isn't a user.
        return new SolrOwnerScorer(weight, new BitDocSet(new FixedBitSet(0)), context, searcher);
    }

}
