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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.alfresco.solr.ContextAwareQuery;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.PostFilter;

public class PostFilterQuery extends Query implements PostFilter
{
    private int cost;
    private final Query query;

    public PostFilterQuery(int cost, Query query)
    {
        this.cost = cost;
        if (query == null) throw new IllegalStateException("query cannot be null");
        this.query = query;
    }

    @Override
    public int hashCode() {
        //DON'T INCLUDE COST??
        int result = cost;
        result = 31 * result + query.hashCode();
        return result;
    }

    //THIS WAS WRONG public int hashcode()

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostFilterQuery)) return false;

        PostFilterQuery that = (PostFilterQuery) o;

        //DON'T INCLUDE COST?? if (cost != that.cost) return false;
        return query.equals(that.query);
    }

    public int getCost()
    {
        return cost;
    }

    @Override
    public void setCost(int cost)
    {
       this.cost = cost;
    }

    public boolean getCache()
    {
        return false;
    }

    @Override
    public void setCache(boolean cache)
    {

    }

    public boolean getCacheSep()
    {
        return false;
    }

    @Override
    public void setCacheSep(boolean cacheSep)
    {

    }

    public String toString(String s)
    {
        return s;
    }

    public DelegatingCollector getFilterCollector(IndexSearcher searcher)
    {
        List<PostFilter> postFilters = new ArrayList<PostFilter>();
        getPostFilters(query, postFilters);

        Collections.sort(postFilters, new PostFilterComp());

        List<DelegatingCollector> delegatingCollectors = new ArrayList<DelegatingCollector>();
        for(PostFilter postFilter : postFilters)
        {
            DelegatingCollector delegatingCollector = postFilter.getFilterCollector(searcher);
            if(!(delegatingCollector instanceof AllAccessCollector)) {
                delegatingCollectors.add(delegatingCollector);
            }
        }

        if(delegatingCollectors.size() == 0)
        {
            return new AllAccessCollector();
        }
        else if(delegatingCollectors.size() == 1)
        {
            return delegatingCollectors.get(0);
        }
        else
        {
            return new WrapperCollector(delegatingCollectors);
        }
    }

    private static class WrapperCollector extends DelegatingCollector
    {
        private DelegatingCollector innerDelegate;
        private CollectorSink sink;

        public WrapperCollector(List<DelegatingCollector> delegatingCollectors)
        {
            for(DelegatingCollector delegatingCollector : delegatingCollectors)
            {
                if(innerDelegate == null)
                {
                    innerDelegate = delegatingCollector;
                }
                else
                {
                    innerDelegate.setLastDelegate(delegatingCollector);
                }
            }

            this.sink = new CollectorSink();
            innerDelegate.setLastDelegate(this.sink);
        }

        public void setScorer(Scorer scorer) throws IOException
        {
            super.setScorer(scorer);
            innerDelegate.setScorer(scorer);
        }

        protected void doSetNextReader(LeafReaderContext context) throws IOException
        {
        	super.doSetNextReader(context);
        	innerDelegate.getLeafCollector(context);
        }
      

		public void collect(int doc) throws IOException
        {
            innerDelegate.collect(doc);
            if(sink.doc == doc) {
                sink.doc = -1;
                super.collect(doc);
            }
        }
    }

    private static class CollectorSink extends DelegatingCollector
    {
        public int doc = -1;

        public void collect(int doc) throws IOException
        {
            this.doc = doc;
        }

        public void doSetNextReader(LeafReaderContext context)
        {

        }
    }

    private void getPostFilters(Query q, List<PostFilter> postFilters)
    {
        if(q instanceof BooleanQuery)
        {
            BooleanQuery bq = (BooleanQuery) q;
            List<BooleanClause> clauses = bq.clauses();
            for (BooleanClause clause : clauses)
            {
                Query q1 = clause.getQuery();
                getPostFilters(q1, postFilters);
            }
        }
        else if(q instanceof ContextAwareQuery)
        {
            ContextAwareQuery cq = (ContextAwareQuery)q;
            getPostFilters(cq.getLuceneQuery(), postFilters);
        }
        else if(q instanceof PostFilter)
        {
            postFilters.add((PostFilter)q);
        }
        else if(q instanceof BoostQuery)
        {
        	BoostQuery bq = (BoostQuery)q;
        	getPostFilters(bq.getQuery(), postFilters);
        }
    }

    private class PostFilterComp implements Comparator<PostFilter>
    {
        public int compare(PostFilter a, PostFilter b)
        {
            int costa = a.getCost();
            int costb = b.getCost();
            if(costa == costb)
            {
                return 0;
            }
            else if(costa < costb)
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
    }

}