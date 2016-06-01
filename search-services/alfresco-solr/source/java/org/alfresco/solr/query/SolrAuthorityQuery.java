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
import java.util.Set;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Query for docs the supplied authority is able to read.
 * 
 * @author Matt Ward
 */
public class SolrAuthorityQuery extends AbstractAuthorityQuery
{
    public SolrAuthorityQuery(String authority)
    {
        super(authority);
    }

    @Override
    public String toString(String field)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(QueryConstants.FIELD_AUTHORITY).append(':');
        stringBuilder.append(authority);
        return stringBuilder.toString();
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException
    {
        if(!(searcher instanceof SolrIndexSearcher))
        {
            throw new IllegalStateException("Must have a SolrIndexSearcher");
        }
        return new SolrAuthorityQueryWeight((SolrIndexSearcher)searcher, needsScores, this, authority);
    }
    
    private class SolrAuthorityQueryWeight extends AbstractAuthorityQueryWeight
    {
        public SolrAuthorityQueryWeight(SolrIndexSearcher searcher, boolean needsScores, Query query, String authority) throws IOException
        {
            super(searcher, needsScores, query, QueryConstants.FIELD_AUTHORITY, authority);
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException
        {
            return SolrAuthorityScorer.createAuthorityScorer(this, context, searcher, authority);
        }

		@Override
		public void extractTerms(Set<Term> terms) 
		{
			terms.add(new Term(QueryConstants.FIELD_AUTHORITY, authority));
		}   
    }
}
