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
package org.alfresco.solr;

import java.io.IOException;

import org.alfresco.service.cmr.search.SearchParameters;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andy
 *
 */
public class ContextAwareQuery extends Query
{
    protected final static Logger log = LoggerFactory.getLogger(ContextAwareQuery.class);

    private Query luceneQuery;
    
    private SearchParameters searchParameters;
    
    /**
     * @param luceneQuery Query
     * @param searchParameters SearchParameters
     */
    public ContextAwareQuery(Query luceneQuery, SearchParameters searchParameters)
    {
        this.luceneQuery = luceneQuery;
        this.searchParameters = searchParameters;
    }

    /**
     * @param field String
     * @return String
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    public String toString(String field)
    {
        return luceneQuery.toString(field);
    }

    /**
     * @param searcher IndexSearcher
     * @return Weight
     * @throws IOException
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.IndexSearcher)
     */
    public Weight createWeight(IndexSearcher searcher, boolean needsScore) throws IOException
    {
        return luceneQuery.createWeight(searcher, needsScore);
    }



    /**
     * @param reader IndexReader
     * @return Query
     * @throws IOException
     * @see org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader)
     */
    public Query rewrite(IndexReader reader) throws IOException
    {
        return luceneQuery.rewrite(reader);
    }
   
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((luceneQuery == null) ? 0 : luceneQuery.hashCode());
        result = prime * result + ((searchParameters == null) ? 0 : searchParameters.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContextAwareQuery other = (ContextAwareQuery) obj;
        if (luceneQuery == null)
        {
            if (other.luceneQuery != null)
                return false;
        }
        else if (!luceneQuery.equals(other.luceneQuery))
            return false;
        if (searchParameters == null)
        {
            if (other.searchParameters != null)
                return false;
        }
        else if (!searchParameters.equals(other.searchParameters))
            return false;
        return true;
    }

    public Query getLuceneQuery()
    {
        return luceneQuery;
    }



}
