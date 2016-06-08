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

import java.util.Properties;

import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.adaptor.lucene.LuceneQueryParserAdaptor;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderContext;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.util.Version;
import org.apache.solr.core.CoreDescriptorDecorator;
import org.apache.solr.request.SolrQueryRequest;

/**
 * @author andyh
 */
public class Lucene4QueryBuilderContextSolrImpl implements LuceneQueryBuilderContext<Query, Sort, ParseException>
{
    private Solr4QueryParser lqp;

    private NamespacePrefixResolver namespacePrefixResolver;
    
    private LuceneQueryParserAdaptor<Query, Sort, ParseException> lqpa;

    /**
     * Context for building lucene queries
     *
     * @param dictionaryService
     * @param namespacePrefixResolver
     * @param tenantService
     * @param searchParameters
     * @param defaultSearchMLAnalysisMode
     * @param req
     * @param model
     */
    public Lucene4QueryBuilderContextSolrImpl(DictionaryService dictionaryService, NamespacePrefixResolver namespacePrefixResolver, TenantService tenantService,
            SearchParameters searchParameters, MLAnalysisMode defaultSearchMLAnalysisMode, SolrQueryRequest req, AlfrescoSolrDataModel model, FTSQueryParser.RerankPhase rerankPhase)
    {
          lqp = new Solr4QueryParser(req.getSchema(), Version.LUCENE_5_5_0, searchParameters.getDefaultFieldName(), req.getSchema().getQueryAnalyzer(), rerankPhase);
//        lqp.setDefaultOperator(AbstractLuceneQueryParser.OR_OPERATOR);
        lqp.setDictionaryService(dictionaryService);
        lqp.setNamespacePrefixResolver(namespacePrefixResolver);
        lqp.setTenantService(tenantService);
          lqp.setSearchParameters(searchParameters);
//        lqp.setDefaultSearchMLAnalysisMode(defaultSearchMLAnalysisMode);
//        lqp.setIndexReader(indexReader);
//        lqp.setAllowLeadingWildcard(true);
//        this.namespacePrefixResolver = namespacePrefixResolver;
        
          Properties props = new CoreDescriptorDecorator(req.getCore().getCoreDescriptor()).getProperties();
          int topTermSpanRewriteLimit = Integer.parseInt(props.getProperty("alfresco.topTermSpanRewriteLimit", "1000"));
          lqp.setTopTermSpanRewriteLimit(topTermSpanRewriteLimit);
          
          lqpa = new Lucene4QueryParserAdaptor(lqp);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderContext#getLuceneQueryParser()
     */
    public LuceneQueryParserAdaptor<Query, Sort, ParseException> getLuceneQueryParserAdaptor()
    {
        return lqpa;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderContext#getNamespacePrefixResolver()
     */
    public NamespacePrefixResolver getNamespacePrefixResolver()
    {
        return namespacePrefixResolver;
    }

}
