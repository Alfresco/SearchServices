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

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.FixedBitSet;
import org.apache.solr.search.BitDocSet;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;


/**
 * @author Andy
 *
 */
public abstract class AbstractSolrCachingScorer extends Scorer
{
    protected static class LongCache 
		{
			private static int CACHE_SIZE = 1000000;
			
			private LongCache(){}
	
			static final Long cache[] = new Long[CACHE_SIZE];
	
			static {
				for(int i = 0; i < cache.length; i++)
					cache[i] = new Long(i);
			}
		}

	protected static Long getLong(long l) {
		if(l > LongCache.CACHE_SIZE)
		{
			return Long.valueOf(l);
		}
		else
		{
			return LongCache.cache[(int)l];
		}
	}

	
    
    SolrCachingScorerDoIdSetIterator iterator;
    
    AbstractSolrCachingScorer(Weight weight, DocSet in, LeafReaderContext context, SolrIndexSearcher searcher)
    {
        super(weight);
        iterator = new SolrCachingScorerDoIdSetIterator(in, context, searcher);
    }

    @Override
    public float score() throws IOException
    {
        return 1.0f;
    }

    // TODO: implement
    @Override
    public int freq() throws IOException
    {
        return 1;
    }
    
    @Override
	public int docID() 
    {
    	return iterator.docID();
	}

	@Override
	public DocIdSetIterator iterator() 
	{
		return iterator;
	}



	private static class SolrCachingScorerDoIdSetIterator extends DocIdSetIterator
    {
    	BitDocSet matches;

        int doc = -1;

        FixedBitSet bitSet;

        LeafReaderContext context;
        
        SolrCachingScorerDoIdSetIterator(DocSet in, LeafReaderContext context, SolrIndexSearcher searcher)
        {
        	  this.context = context;
              
              if (in instanceof BitDocSet)
              {
                  matches = (BitDocSet) in;
              }
              else
              {
                  this.matches = new BitDocSet(new FixedBitSet(searcher.maxDoc()));
                  for (DocIterator it = in.iterator(); it.hasNext(); /* */)
                  {
                      matches.addUnique(it.nextDoc());
                  }
              }
              bitSet = matches.getBits();
              
              doc = getBase() - 1;
        }
        
        private boolean next()
        {        
            if(doc+1 < bitSet.length())
            {
                doc = bitSet.nextSetBit(doc+1);
                return (doc != NO_MORE_DOCS)  && (doc < (getBase()  + context.reader().maxDoc()));
            }
            else
            {
                return false;
            }
        }
        
        private int getBase()
        {
            return context.docBase;
        }
        
        @Override
        public int nextDoc() throws IOException
        {
        	while (next())
        	{
        		return docID();
        	}
            return NO_MORE_DOCS;
        }

        
        @Override
        public int docID()
        {
            // TODO: check this expression as for next()
            if ((doc > -1) && (doc != NO_MORE_DOCS))
            {
                return doc - getBase();
            }
            return doc;
        }

        @Override
        public int advance(int target) throws IOException
        {	
        	while (next())
        	{
        		final int current = docID();
        		if (current >= target)
        		{
        			return current;
        		}
        	}
        	return NO_MORE_DOCS;
        }

        // TODO: implement
        @Override
        public long cost()
        {
           return 1;
        }
    }
}
