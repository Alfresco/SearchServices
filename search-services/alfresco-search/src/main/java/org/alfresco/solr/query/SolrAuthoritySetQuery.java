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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.solr.data.GlobalReaders;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.FixedBitSet;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.PostFilter;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * @author Andy
 *
 */
public class SolrAuthoritySetQuery extends AbstractAuthoritySetQuery implements PostFilter
{
    public SolrAuthoritySetQuery(String authorities)
    {
        super(authorities);
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException
    {
        if(!(searcher instanceof SolrIndexSearcher))
        {
            throw new IllegalStateException("Must have a SolrIndexSearcher");
        }

        String[] auths = authorities.substring(1).split(authorities.substring(0, 1));

        SolrIndexSearcher solrIndexSearcher = (SolrIndexSearcher)searcher;
        Properties p = solrIndexSearcher.getSchema().getResourceLoader().getCoreProperties();
        boolean doPermissionChecks = Boolean.parseBoolean(p.getProperty("alfresco.doPermissionChecks", "true"));

        boolean hasGlobalRead = false;

        final HashSet<String> globalReaders = GlobalReaders.getReaders();

        for(String auth : auths)
        {
            if(globalReaders.contains(auth))
            {
                hasGlobalRead = true;
                break;
            }
        }

        if (hasGlobalRead || (doPermissionChecks == false))
        {
            return new MatchAllDocsQuery().createWeight(searcher, needsScores);
        }

        BitsFilter readFilter  = getACLFilter(auths, QueryConstants.FIELD_READER, solrIndexSearcher);
        BitsFilter ownerFilter = getOwnerFilter(auths, solrIndexSearcher);

        if (globalReaders.contains(PermissionService.OWNER_AUTHORITY))
        {
            readFilter.or(ownerFilter);
            return new ConstantScoreQuery(readFilter).createWeight(searcher, needsScores);
        }
        else
        {
            String[] ownerAuth = {PermissionService.OWNER_AUTHORITY};
            BitsFilter ownerReadFilter  = getACLFilter(ownerAuth, QueryConstants.FIELD_READER, solrIndexSearcher);
            ownerReadFilter.and(ownerFilter);
            readFilter.or(ownerReadFilter);
            return new ConstantScoreQuery(readFilter).createWeight(searcher, needsScores);
        }
    }

    public DelegatingCollector getFilterCollector(IndexSearcher searcher)
    {

        String[] auths = authorities.substring(1).split(authorities.substring(0, 1));

        SolrIndexSearcher solrIndexSearcher = (SolrIndexSearcher)searcher;
        Properties p = solrIndexSearcher.getSchema().getResourceLoader().getCoreProperties();
        boolean doPermissionChecks = Boolean.parseBoolean(p.getProperty("alfresco.doPermissionChecks", "true"));
        boolean hasGlobalRead = false;

        final HashSet<String> globalReaders = GlobalReaders.getReaders();

        for(String auth : auths)
        {
            if(globalReaders.contains(auth))
            {
                hasGlobalRead = true;
                break;
            }
        }

        if (hasGlobalRead || (doPermissionChecks == false))
        {
            return new AllAccessCollector();
        }

        try
        {
            HybridBitSet aclSet = getACLSet(auths, QueryConstants.FIELD_READER, solrIndexSearcher);
            BitsFilter ownerFilter = getOwnerFilter(auths, solrIndexSearcher);

            if (globalReaders.contains(PermissionService.OWNER_AUTHORITY))
            {
                return new AccessControlCollector(aclSet, ownerFilter);
            }
            else
            {
                String[] ownerAuth = {PermissionService.OWNER_AUTHORITY};
                HybridBitSet ownerAclSet = getACLSet(ownerAuth, QueryConstants.FIELD_READER, solrIndexSearcher);
                return new AccessControlCollectorWithoutOwnerRead(aclSet, ownerAclSet, ownerFilter);
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getCost()
    {
        return 202;
    }

    public void setCost(int cost)
    {

    }

    public boolean getCache() {
        return true;
    }

    public void setCache(boolean cache)
    {

    }

    public boolean getCacheSep()
    {
        return false;
    }

    public void setCacheSep(boolean sep)
    {

    }

    @Override
    public String toString(String field)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(QueryConstants.FIELD_AUTHORITYSET).append(':');
        stringBuilder.append(authorities);
        return stringBuilder.toString();
    }

    private BitsFilter getOwnerFilter(String[] auths, SolrIndexSearcher searcher) throws IOException
    {
        Builder builder = new BooleanQuery.Builder();
        for(String current : auths)
        {
            if (AuthorityType.getAuthorityType(current) == AuthorityType.USER)
            {
            	builder.add(new TermQuery(new Term(QueryConstants.FIELD_OWNER, current)), BooleanClause.Occur.SHOULD);
            }
        }

        BitsFilterCollector collector = new BitsFilterCollector(searcher.getTopReaderContext().leaves().size());
        searcher.search(builder.build(), collector);
        return collector.getBitsFilter();
    }

    class BitsFilterCollector implements Collector, LeafCollector
    {
        private List<FixedBitSet> sets;
        private FixedBitSet set;

        public BitsFilterCollector(int leafCount)
        {
            this.sets = new ArrayList<FixedBitSet>(leafCount);
        }

        public BitsFilter getBitsFilter() {
            return new BitsFilter(sets);
        }

        public boolean acceptsDocsOutOfOrder() {
            return false;
        }

        public void setScorer(Scorer scorer) {

        }

        public void collect(int doc) {
            set.set(doc);
        }

		@Override
		public LeafCollector getLeafCollector(LeafReaderContext context)
				throws IOException {
			   set = new FixedBitSet(context.reader().maxDoc());
	            sets.add(set);
			return this;
		}

		@Override
		public boolean needsScores() {
			// TODO Auto-generated method stub
			return false;
		}
    }

    class AccessControlCollector extends DelegatingCollector
    {
        private HybridBitSet aclIds;
        private NumericDocValues fieldValues;
        private BitsFilter ownerFilter;
        private FixedBitSet ownerDocs;

        public AccessControlCollector(HybridBitSet aclIds, BitsFilter ownerFilter)
        {
            this.aclIds=aclIds;
            this.ownerFilter = ownerFilter;
        }

        public boolean acceptsDocsOutOfOrder()
        {
            return false;
        }

        public void doSetNextReader(LeafReaderContext context) throws IOException
        {
        	super.doSetNextReader(context);
            this.fieldValues = DocValuesCache.getNumericDocValues(QueryConstants.FIELD_ACLID, context.reader());
            this.ownerDocs = ownerFilter.getBitSets().get(context.ord);
        }

        public void collect(int doc) throws IOException
        {
            long aclId = this.fieldValues.get(doc);

            if(aclIds.get(aclId) || ownerDocs.get(doc))
            {
                super.collect(doc);
            }
        }
    }

    class AccessControlCollectorWithoutOwnerRead extends DelegatingCollector
    {
        private HybridBitSet aclIds;
        private HybridBitSet ownerAclIds;
        private NumericDocValues fieldValues;
        private BitsFilter ownerFilter;
        private FixedBitSet ownerDocs;
        public AccessControlCollectorWithoutOwnerRead(HybridBitSet aclIds, HybridBitSet ownerAclIds, BitsFilter ownerFilter)
        {
            this.aclIds=aclIds;
            this.ownerAclIds = ownerAclIds;
            this.ownerFilter = ownerFilter;
        }

        public boolean acceptsDocsOutOfOrder()
        {
            return false;
        }

        public void doSetNextReader(LeafReaderContext context) throws IOException
        {
        	super.doSetNextReader(context);
            this.fieldValues = DocValuesCache.getNumericDocValues(QueryConstants.FIELD_ACLID, context.reader());
            this.ownerDocs = ownerFilter.getBitSets().get(context.ord);
        }

        public void collect(int doc) throws IOException
        {
            long aclId = this.fieldValues.get(doc);
            if(aclIds.get(aclId) || (ownerDocs.get(doc) && ownerAclIds.get(aclId)))
            {
                super.collect(doc);
            }
        }
    }

}
