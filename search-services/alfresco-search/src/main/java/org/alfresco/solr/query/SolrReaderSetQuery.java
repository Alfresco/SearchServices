/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.solr.query;

import java.io.IOException;
import java.util.Set;

import org.alfresco.repo.search.adaptor.QueryConstants;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * @author Andy
 * @author Matt Ward
 */
public class SolrReaderSetQuery extends AbstractAuthoritySetQuery
{
    public SolrReaderSetQuery(String authorities)
    {
        super(authorities);
    }
    
    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScore) throws IOException
    {
        if(!(searcher instanceof SolrIndexSearcher))
        {
            throw new IllegalStateException("Must have a SolrIndexSearcher");
        }
        return new SolrReaderSetQueryWeight((SolrIndexSearcher)searcher, this, authorities);
    }

    @Override
    public String toString(String field)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(QueryConstants.FIELD_READERSET).append(':');
        stringBuilder.append(authorities);
        return stringBuilder.toString();
    }

    private class SolrReaderSetQueryWeight extends AbstractAuthorityQueryWeight
    {
        public SolrReaderSetQueryWeight(SolrIndexSearcher searcher, Query query, String readers) throws IOException
        {
            super(searcher, false, query, QueryConstants.FIELD_READERSET, readers);
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException
        {
            LeafReader reader = context.reader();
            return SolrReaderSetScorer2.createReaderSetScorer(this, context, searcher, authorities, reader);
        }
        
        @Override
        public void extractTerms(Set<Term> terms) 
        {
        	terms.add(new Term(QueryConstants.FIELD_READERSET, authorities));
        }  
    }
}
