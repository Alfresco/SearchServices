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

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Weight;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.PostFilter;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Query for a set of denied authorities.
 * 
 * @author Joel Bernstein
 */
public class SolrDenySetQuery extends AbstractAuthoritySetQuery implements PostFilter
{
    public SolrDenySetQuery(String authorities)
    {
        super(authorities);
    }

    public int getCost()
    {
        return 201;
    }

    public void setCost(int cost)
    {

    }

    public void setCache(boolean cache)
    {

    }

    public boolean getCache() {
        return true;
    }

    public boolean getCacheSep()
    {
        return false;
    }

    public void setCacheSep(boolean cacheSep)
    {
    }
    
    @Override
    public Weight createWeight(IndexSearcher searcher, boolean requiresScore) throws IOException
    {
        if(!(searcher instanceof SolrIndexSearcher))
        {
            throw new IllegalStateException("Must have a SolrIndexSearcher");
        }

        String[] auths = authorities.substring(1).split(authorities.substring(0, 1));
        BitsFilter denyFilter  = getACLFilter(auths, QueryConstants.FIELD_DENIED, (SolrIndexSearcher) searcher);
        return new ConstantScoreQuery(denyFilter).createWeight(searcher, false);
    }

    public DelegatingCollector getFilterCollector(IndexSearcher searcher)
    {
        String[] auths = authorities.substring(1).split(authorities.substring(0, 1));
        try
        {
            HybridBitSet denySet = getACLSet(auths, QueryConstants.FIELD_DENIED, (SolrIndexSearcher) searcher);
            if(denySet instanceof EmptyHybridBitSet)
            {
                return new AllAccessCollector();
            }
            else
            {
                return new AccessControlCollector(denySet);
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

        @Override
    public String toString(String field)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(QueryConstants.FIELD_DENYSET).append(':');
        stringBuilder.append(authorities);
        return stringBuilder.toString();
    }

    class AccessControlCollector extends DelegatingCollector
    {
        private HybridBitSet aclIds;
        private NumericDocValues fieldValues;

        public AccessControlCollector(HybridBitSet aclIds)
        {
            this.aclIds=aclIds;
        }

        public boolean acceptsDocsOutOfOrder() 
        {
            return false;
        }

        public void doSetNextReader(LeafReaderContext context) throws IOException 
        {
        	super.doSetNextReader(context);
            this.fieldValues = DocValuesCache.getNumericDocValues(QueryConstants.FIELD_ACLID, context.reader());
        }

        public void collect(int doc) throws IOException{
        	
        		long aclId = this.fieldValues.get(doc);

        		if(!aclIds.get(aclId))
        		{
        			super.collect(doc);
        		}
        	
        }
    }
}
