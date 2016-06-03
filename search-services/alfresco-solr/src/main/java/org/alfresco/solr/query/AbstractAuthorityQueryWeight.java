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

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * Abstract {@link Weight} implementation for authority related queries.
 * 
 * @see AbstractAuthoritySetQuery
 */
public abstract class AbstractAuthorityQueryWeight extends Weight
{
    protected Query query;
    protected SolrIndexSearcher searcher;
    protected TFIDFSimilarity similarity;
    protected float value;
    protected float idf;
    protected float queryNorm;
    protected float queryWeight;
    protected Explanation idfExp;
    protected boolean needsScores;
    
    public AbstractAuthorityQueryWeight(SolrIndexSearcher searcher, boolean needsScores, Query query, String authTermName, String authTermText) throws IOException
    {
    	super(query);
        this.searcher = searcher;
        this.similarity = (TFIDFSimilarity) searcher.getSimilarity(true);
        CollectionStatistics collectionStats = searcher.collectionStatistics(authTermName);
        final IndexReaderContext context = searcher.getTopReaderContext();
        final Term term = new Term(authTermName, authTermText);
        final TermContext termContext = TermContext.build(context, term);
        TermStatistics termStats = searcher.termStatistics(term, termContext);
        idfExp = similarity.idfExplain(collectionStats, termStats);
        idf = idfExp.getValue();
        this.needsScores = needsScores;
    }
    
    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getValueForNormalization() throws IOException
    {
        return sumOfSquaredWeights();
    }

    @Override
    public void normalize(float queryNorm, float topLevelBoost)
    {
        this.queryNorm = queryNorm;
        queryWeight *= queryNorm;                   // normalize query weight
        value = queryWeight * idf;                  // idf for document
    }
    
    protected float sumOfSquaredWeights() throws IOException
    {
        queryWeight = idf;       // compute query weight
        return queryWeight * queryWeight;           // square it
    }
}
