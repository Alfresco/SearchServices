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
import java.util.HashSet;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.cache.CacheConstants;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.FixedBitSet;
import org.apache.solr.search.BitDocSet;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.WrappedQuery;

public class SolrReaderSetScorer2 extends AbstractSolrCachingScorer
{
	SolrReaderSetScorer2(Weight weight, DocSet in, LeafReaderContext context, SolrIndexSearcher searcher)
    {
        super(weight, in, context, searcher);
    }

    public static AbstractSolrCachingScorer createReaderSetScorer(Weight weight, LeafReaderContext context, SolrIndexSearcher searcher, String authorities, LeafReader reader) throws IOException
    {
        
        DocSet readableDocSet = (DocSet) searcher.cacheLookup(CacheConstants.ALFRESCO_READER_CACHE, authorities);

        if (readableDocSet == null)
        {

            String[] auths = authorities.substring(1).split(authorities.substring(0, 1));

            readableDocSet = new BitDocSet(new FixedBitSet(searcher.maxDoc()));

            BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
            for(String current : auths)
            {
                bQuery.add(new TermQuery(new Term(QueryConstants.FIELD_READER, current)), Occur.SHOULD);
            }
            WrappedQuery wrapped = new WrappedQuery(bQuery.build());
            wrapped.setCache(false);

            DocSet aclDocs = searcher.getDocSet(wrapped);
            
            HashSet<Long> aclsFound = new HashSet<Long>(aclDocs.size());
            NumericDocValues aclDocValues = searcher.getLeafReader().getNumericDocValues(QueryConstants.FIELD_ACLID);
           
            for (DocIterator it = aclDocs.iterator(); it.hasNext(); /**/)
            {
                int docID = it.nextDoc();
                // Obtain the ACL ID for this ACL doc.
                long aclID = aclDocValues.get(docID);
                aclsFound.add(getLong(aclID));
            }
         
            if(aclsFound.size() > 0)
            {
                for(LeafReaderContext readerContext : searcher.getLeafReader().leaves() )
                {
                    int maxDoc = readerContext.reader().maxDoc();
                    NumericDocValues fieldValues = DocValuesCache.getNumericDocValues(QueryConstants.FIELD_ACLID, readerContext.reader());
                    if(fieldValues != null)
                    {
                        for(int i = 0; i < maxDoc ; i++)
                        {
                            long aclID = fieldValues.get(i);
                            Long key = getLong(aclID);
                            if(aclsFound.contains(key))
                            {
                                readableDocSet.add(readerContext.docBase + i);
                            }
                        }
                    }

                }
            }
            
            // Exclude the ACL docs from the results, we only want real docs that match.
            // Probably not very efficient, what we really want is remove(docID)
            readableDocSet = readableDocSet.andNot(aclDocs);
            searcher.cacheInsert(CacheConstants.ALFRESCO_READER_CACHE, authorities, readableDocSet);
        }
        
        // TODO: cache the full set? e.g. searcher.cacheInsert(CacheConstants.ALFRESCO_READERSET_CACHE, authorities, readableDocSet)
        // plus check of course, for presence in cache at start of method.
        return new SolrReaderSetScorer2(weight, readableDocSet, context, searcher);
    }
}
