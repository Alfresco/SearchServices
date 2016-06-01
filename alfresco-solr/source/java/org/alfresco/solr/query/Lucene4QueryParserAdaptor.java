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

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.AnalysisMode;
import org.alfresco.repo.search.adaptor.lucene.LuceneFunction;
import org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor;
import org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserExpressionAdaptor;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.Ordering;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.search.SearchParameters;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;

/**
 * @author Andy
 *
 */
public class Lucene4QueryParserAdaptor implements LuceneQueryParserAdaptor<Query, Sort, ParseException>
{

    private Solr4QueryParser lqp;

    /**
     * @param lqp
     */
    public Lucene4QueryParserAdaptor(Solr4QueryParser lqp)
    {
        this.lqp = lqp;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getFieldQuery(java.lang.String, java.lang.String, org.alfresco.repo.search.adaptor.lucene.AnalysisMode, org.alfresco.repo.search.adaptor.lucene.LuceneFunction)
     */
    @Override
    public Query getFieldQuery(String field, String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction) throws ParseException
    {
        return lqp.getFieldQuery(field, queryText, analysisMode, luceneFunction);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getRangeQuery(java.lang.String, java.lang.String, java.lang.String, boolean, boolean, org.alfresco.repo.search.adaptor.lucene.AnalysisMode, org.alfresco.repo.search.adaptor.lucene.LuceneFunction)
     */
    @Override
    public Query getRangeQuery(String field, String lower, String upper, boolean includeLower, boolean includeUpper, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        return lqp.getRangeQuery(field, lower, upper, includeLower, includeUpper, analysisMode, luceneFunction);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getMatchAllQuery()
     */
    @Override
    public Query getMatchAllQuery() throws ParseException
    {
        return new MatchAllDocsQuery();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getMatchNoneQuery()
     */
    @Override
    public Query getMatchNoneQuery() throws ParseException
    {
        return new TermQuery(new Term("NO_TOKENS", "__"));
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getLikeQuery(java.lang.String, java.lang.String, org.alfresco.repo.search.adaptor.lucene.AnalysisMode)
     */
    @Override
    public Query getLikeQuery(String field, String sqlLikeClause, AnalysisMode analysisMode) throws ParseException
    {
        return lqp.getLikeQuery(field, sqlLikeClause, analysisMode);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getSearchParameters()
     */
    @Override
    public SearchParameters getSearchParameters()
    {
        return lqp.getSearchParameters();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getSortField(java.lang.String)
     */
    @Override
    public String getSortField(String field) throws ParseException
    {
        // TODO 
        //return field;
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getIdentifierQuery(java.lang.String, java.lang.String, org.alfresco.repo.search.adaptor.lucene.AnalysisMode, org.alfresco.repo.search.adaptor.lucene.LuceneFunction)
     */
    @Override
    public Query getIdentifierQuery(String field, String stringValue, AnalysisMode analysisMode, LuceneFunction luceneFunction) throws ParseException
    {
        String[] split = stringValue.split(";");
        if(split.length == 1)
        {
            return lqp.getFieldQuery(field, stringValue, AnalysisMode.IDENTIFIER, luceneFunction);
        }
        else
        {
            if(split[1].equalsIgnoreCase("PWC"))
            {
                return getMatchNoneQuery();
            }
            
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            BooleanQuery.Builder part1 = new BooleanQuery.Builder();
            part1.add(lqp.getFieldQuery(field, split[0], AnalysisMode.IDENTIFIER, luceneFunction), Occur.MUST);
            part1.add(lqp.getFieldQuery("@"+ContentModel.PROP_VERSION_LABEL.toString(), split[1], AnalysisMode.IDENTIFIER, luceneFunction), Occur.MUST);
            query.add(part1.build(), Occur.SHOULD);
            
            if(split[1].equals("1.0"))
            {
                BooleanQuery.Builder part2 = new BooleanQuery.Builder();
                part2.add(lqp.getFieldQuery(field, split[0], AnalysisMode.IDENTIFIER, luceneFunction), Occur.MUST);
                part2.add(lqp.getFieldQuery(QueryConstants.FIELD_ASPECT, ContentModel.ASPECT_VERSIONABLE.toString(), AnalysisMode.IDENTIFIER, luceneFunction), Occur.MUST_NOT);
                query.add(part2.build(), Occur.SHOULD);
            }
            return query.build();
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getIdentifieLikeQuery(java.lang.String, java.lang.String, org.alfresco.repo.search.adaptor.lucene.AnalysisMode)
     */
    @Override
    public Query getIdentifieLikeQuery(String field, String sqlLikeClause, AnalysisMode analysisMode) throws ParseException
    {
        return getLikeQuery(field, sqlLikeClause, analysisMode);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#sortFieldExists(java.lang.String)
     */
    @Override
    public boolean sortFieldExists(String noLocalField)
    {
        // TODO Auto-generated method stub
        //return false;
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getFieldQuery(java.lang.String, java.lang.String)
     */
    @Override
    public Query getFieldQuery(String field, String queryText) throws ParseException
    {
        return lqp.getFieldQuery(field, queryText);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#buildSort(java.util.List, org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext)
     */
    @Override
    public Sort buildSort(List<Ordering> list, FunctionEvaluationContext functionContext) throws ParseException
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getFuzzyQuery(java.lang.String, java.lang.String, java.lang.Float)
     */
    @Override
    public Query getFuzzyQuery(String luceneFieldName, String term, Float minSimilarity) throws ParseException
    {
        return lqp.getFuzzyQuery(luceneFieldName, term, minSimilarity);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getField()
     */
    @Override
    public String getField()
    {
        return lqp.getField();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getPhraseSlop()
     */
    @Override
    public int getPhraseSlop()
    {
        return lqp.getPhraseSlop();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getFieldQuery(java.lang.String, java.lang.String, org.alfresco.repo.search.adaptor.lucene.AnalysisMode, java.lang.Integer, org.alfresco.repo.search.adaptor.lucene.LuceneFunction)
     */
    @Override
    public Query getFieldQuery(String luceneFieldName, String term, AnalysisMode analysisMode, Integer slop, LuceneFunction luceneFunction) throws ParseException
    {
        return lqp.getFieldQuery(luceneFieldName, term, analysisMode, slop, luceneFunction);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getPrefixQuery(java.lang.String, java.lang.String, org.alfresco.repo.search.adaptor.lucene.AnalysisMode)
     */
    @Override
    public Query getPrefixQuery(String luceneFieldName, String term, AnalysisMode analysisMode) throws ParseException
    {
        return lqp.getPrefixQuery(luceneFieldName, term, analysisMode);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getSpanQuery(java.lang.String, java.lang.String, java.lang.String, int, boolean)
     */
    @Override
    public Query getSpanQuery(String field, String first, String last, int slop, boolean inOrder) throws ParseException
    {
        return lqp.getSpanQuery(field, first, last, slop, inOrder);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getWildcardQuery(java.lang.String, java.lang.String, org.alfresco.repo.search.adaptor.lucene.AnalysisMode)
     */
    @Override
    public Query getWildcardQuery(String luceneFieldName, String term, AnalysisMode analysisMode) throws ParseException
    {
        return lqp.getWildcardQuery(luceneFieldName, term, analysisMode);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getNegatedQuery(java.lang.Object)
     */
    @Override
    public Query getNegatedQuery(Query query) throws ParseException
    {
        LuceneQueryParserExpressionAdaptor<Query, ParseException> expressionAdaptor = getExpressionAdaptor();
        expressionAdaptor.addRequired(getMatchAllQuery());
        expressionAdaptor.addExcluded(query);
        return expressionAdaptor.getQuery();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getExpressionAdaptor()
     */
    @Override
    public LuceneQueryParserExpressionAdaptor<Query, ParseException> getExpressionAdaptor()
    {
        return new Lucene4QueryParserExpressionAdaptor();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getMatchAllNodesQuery()
     */
    @Override
    public Query getMatchAllNodesQuery()
    {
        return new TermQuery(new Term("ISNODE", "T"));
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor#getDatetimeSortField(java.lang.String, org.alfresco.service.cmr.dictionary.PropertyDefinition)
     */
    @Override
    public String getDatetimeSortField(String field, PropertyDefinition propertyDef)
    {
        throw new UnsupportedOperationException();
    }
    
    private class Lucene4QueryParserExpressionAdaptor implements LuceneQueryParserExpressionAdaptor<Query, ParseException>
    {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        /* (non-Javadoc)
         * @see org.alfresco.repo.search.impl.lucene.LuceneQueryParserExpressionAdaptor#addRequired(java.lang.Object)
         */
        @Override
        public void addRequired(Query q)
        {
            booleanQuery.add(q, Occur.MUST);
        }

        /* (non-Javadoc)
         * @see org.alfresco.repo.search.impl.lucene.LuceneQueryParserExpressionAdaptor#addExcluded(java.lang.Object)
         */
        @Override
        public void addExcluded(Query q)
        {
            booleanQuery.add(q, Occur.MUST_NOT);
        }

        /* (non-Javadoc)
         * @see org.alfresco.repo.search.impl.lucene.LuceneQueryParserExpressionAdaptor#addOptoinal(java.lang.Object)
         */
        @Override
        public void addOptional(Query q)
        {
            booleanQuery.add(q, Occur.SHOULD);
        }

        /* (non-Javadoc)
         * @see org.alfresco.repo.search.impl.lucene.LuceneQueryParserExpressionAdaptor#getQuery()
         */
        @Override
        public Query getQuery()  throws ParseException
        {
        	BooleanQuery query = booleanQuery.build();
            if(query.clauses().size() == 0)
            {
                return getMatchNoneQuery();
            }
            else if (query.clauses().size() == 1)
            {
                BooleanClause clause = query.clauses().get(0);
                if(clause.isProhibited())
                {
                    booleanQuery.add(getMatchAllQuery(), Occur.MUST);
                    return booleanQuery.build();
                }
                else
                {
                    return clause.getQuery();
                }
            }
            else
            {
                return query;
            }
        }
        
        public Query getNegatedQuery() throws ParseException
        {
        	BooleanQuery query = booleanQuery.build();
            if(query.clauses().size() == 0)
            {
                return getMatchAllQuery();
            }
            else if (query.clauses().size() == 1)
            {
                BooleanClause clause = query.clauses().get(0);
                if(clause.isProhibited())
                {
                    return clause.getQuery();
                }
                else
                {
                    return Lucene4QueryParserAdaptor.this.getNegatedQuery(getQuery());
                }
            }
            else
            {
                return Lucene4QueryParserAdaptor.this.getNegatedQuery(getQuery());
            }
        }

        /* (non-Javadoc)
         * @see org.alfresco.repo.search.impl.lucene.LuceneQueryParserExpressionAdaptor#addRequired(java.lang.Object, float)
         */
        @Override
        public void addRequired(Query q, float boost) throws ParseException
        {
            addRequired(new BoostQuery(q, boost));
        }

        /* (non-Javadoc)
         * @see org.alfresco.repo.search.impl.lucene.LuceneQueryParserExpressionAdaptor#addExcluded(java.lang.Object, float)
         */
        @Override
        public void addExcluded(Query q, float boost) throws ParseException
        {
            addExcluded(new BoostQuery(q, boost));
        }

        /* (non-Javadoc)
         * @see org.alfresco.repo.search.impl.lucene.LuceneQueryParserExpressionAdaptor#addOptional(java.lang.Object, float)
         */
        @Override
        public void addOptional(Query q, float boost) throws ParseException
        {
            addOptional(new BoostQuery(q, boost));
            
        }
        
    }


}
