/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
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
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.adaptor.lucene.AnalysisMode;
import org.alfresco.repo.search.adaptor.lucene.LuceneFunction;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.repo.search.impl.QueryParserUtils;
import org.alfresco.repo.search.impl.lucene.analysis.MLTokenDuplicator;
import org.alfresco.repo.search.impl.parsers.FTSQueryException;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser.RerankPhase;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoAnalyzerWrapper;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.ContentFieldType;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldInstance;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.alfresco.solr.AlfrescoSolrDataModel.IndexedField;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.component.FingerPrintComponent;
import org.alfresco.solr.content.SolrContentStore;
import org.alfresco.util.CachingDateFormat;
import org.alfresco.util.Pair;
import org.alfresco.util.SearchLanguageConversion;
import org.antlr.misc.OrderedHashSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.commongrams.CommonGramsFilter;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper.TopTermsSpanBooleanQueryRewrite;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.HttpShardHandlerFactory;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.update.UpdateShardHandlerConfig;
import org.jaxen.saxpath.SAXPathException;
import org.jaxen.saxpath.base.XPathReader;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * @author Andy
 *
 */
public class Solr4QueryParser extends QueryParser implements QueryConstants
{

    /**
     *            IndexSchema
     * @param matchVersion
     * @param f
     * @param a
     */
    public Solr4QueryParser(SolrQueryRequest req, Version matchVersion, String f, Analyzer a,
            FTSQueryParser.RerankPhase rerankPhase)
    {
        super(f, a);
        setAllowLeadingWildcard(true);
        setAnalyzeRangeTerms(true);
        this.rerankPhase = rerankPhase;
        this.schema = req.getSchema();
        this.solrContentStore = getContentStore(req);
        this.solrParams = req.getParams();
        SolrCore core = req.getCore();
        if(core != null) {
            this.shardHandlerFactory = core.getCoreDescriptor().getCoreContainer().getShardHandlerFactory();
        }
        this.request = req;

    }

    private RerankPhase rerankPhase;
    private SolrContentStore solrContentStore;
    private SolrParams solrParams;
    private ShardHandlerFactory shardHandlerFactory;
    private SolrQueryRequest request;
    /**
     * Extracts the contentStore from SolrQueryRequest.
     * @param req
     * @return
     */
    private SolrContentStore getContentStore(SolrQueryRequest req)
    {
        if(req.getSearcher() != null)
        {
            CoreContainer coreContainer = req.getSearcher().getCore().getCoreDescriptor().getCoreContainer();
            AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler) coreContainer.getMultiCoreHandler();
            SolrInformationServer srv = (SolrInformationServer) coreAdminHandler.getInformationServers().get(req.getSearcher().getCore().getName());
            return srv.getSolrContentStore();
        }
        return null;
    }

    IndexSchema schema;

    @SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(Solr4QueryParser.class);

    protected NamespacePrefixResolver namespacePrefixResolver;

    protected DictionaryService dictionaryService;

    private TenantService tenantService;

    private SearchParameters searchParameters;

    private MLAnalysisMode mlAnalysisMode = MLAnalysisMode.EXACT_LANGUAGE_AND_ALL;

    private int internalSlop = 0;

    int topTermSpanRewriteLimit = 1000;

    /**
     * @param topTermSpanRewriteLimit
     *            the topTermSpanRewriteLimit to set
     */
    public void setTopTermSpanRewriteLimit(int topTermSpanRewriteLimit)
    {
        this.topTermSpanRewriteLimit = topTermSpanRewriteLimit;
    }

    /**
     * @param searchParameters
     */
    public void setSearchParameters(SearchParameters searchParameters)
    {
        this.searchParameters = searchParameters;
    }

    /**
     * @param namespacePrefixResolver
     */
    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver)
    {
        this.namespacePrefixResolver = namespacePrefixResolver;
    }

    /**
     * @param tenantService
     */
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    public SearchParameters getSearchParameters()
    {
        return searchParameters;
    }

    @Override
    protected Query getFieldQuery(String field, String queryText, int slop) throws ParseException
    {
        try
        {
            internalSlop = slop;
            Query query;
            query = getFieldQuery(field, queryText);
            return query;
        } 
        finally
        {
            internalSlop = 0;
        }

    }

    /**
     * @param field
     * @param queryText
     * @param analysisMode
     * @param slop
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     * @throws IOException
     */
    public Query getFieldQuery(String field, String queryText, AnalysisMode analysisMode, int slop,
            LuceneFunction luceneFunction) throws ParseException
    {
        try
        {
            internalSlop = slop;
            Query query = getFieldQuery(field, queryText, analysisMode, luceneFunction);
            return query;
        } 
        finally
        {
            internalSlop = 0;
        }

    }

    /**
     * @param field
     * @param sqlLikeClause
     * @param analysisMode
     * @return the query
     * @throws ParseException
     */
    public Query getLikeQuery(String field, String sqlLikeClause, AnalysisMode analysisMode) throws ParseException
    {
        String luceneWildCardExpression = translate(sqlLikeClause);
        return getWildcardQuery(field, luceneWildCardExpression, AnalysisMode.LIKE);
    }

    private String translate(String string)
    {
        StringBuilder builder = new StringBuilder(string.length());

        boolean lastWasEscape = false;

        for (int i = 0; i < string.length(); i++)
        {
            char c = string.charAt(i);
            if (lastWasEscape)
            {
                builder.append(c);
                lastWasEscape = false;
            } else
            {
                if (c == '\\')
                {
                    lastWasEscape = true;
                } else if (c == '%')
                {
                    builder.append('*');
                } else if (c == '_')
                {
                    builder.append('?');
                } else if (c == '*')
                {
                    builder.append('\\');
                    builder.append(c);
                } else if (c == '?')
                {
                    builder.append('\\');
                    builder.append(c);
                } else
                {
                    builder.append(c);
                }
            }
        }
        if (lastWasEscape)
        {
            throw new FTSQueryException("Escape character at end of string " + string);
        }

        return builder.toString();
    }

    /**
     * @param field
     * @param queryText
     * @param analysisMode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     * @throws IOException
     */
    public Query getDoesNotMatchFieldQuery(String field, String queryText, AnalysisMode analysisMode,
            LuceneFunction luceneFunction) throws ParseException
    {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        Query allQuery = new MatchAllDocsQuery();
        Query matchQuery = getFieldQuery(field, queryText, analysisMode, luceneFunction);
        if ((matchQuery != null))
        {
            query.add(allQuery, Occur.MUST);
            query.add(matchQuery, Occur.MUST_NOT);
        } else
        {
            throw new UnsupportedOperationException();
        }
        return query.build();
    }

    public Query getFieldQuery(String field, String queryText) throws ParseException
    {
        return getFieldQuery(field, queryText, AnalysisMode.DEFAULT, LuceneFunction.FIELD);
    }

    /**
     * @param field
     * @param first
     * @param last
     * @param slop
     * @param inOrder
     * @return the query
     * @throws ParseException
     */
    public Query getSpanQuery(String field, String first, String last, int slop, boolean inOrder) throws ParseException
    {
        if (field.equals(FIELD_PATH))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_PATH);
        } 
        else if (field.equals(FIELD_PATHWITHREPEATS))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_PATHWITHREPEATS);
        } else if (field.equals(FIELD_TEXT))
        {
            Set<String> text = searchParameters.getTextAttributes();
            if ((text == null) || (text.size() == 0))
            {
                Query query = getSpanQuery(PROPERTY_FIELD_PREFIX + ContentModel.PROP_CONTENT.toString(), first, last,
                        slop, inOrder);
                return query;
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : text)
                {
                    Query part = getSpanQuery(fieldName, first, last, slop, inOrder);
                    query.add(part, Occur.SHOULD);
                }
                return query.build();
            }
        } else if (field.equals(FIELD_CLASS))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_CLASS);
        } else if (field.equals(FIELD_TYPE))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_TYPE);
        } else if (field.equals(FIELD_EXACTTYPE))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_EXACTTYPE);
        } else if (field.equals(FIELD_ASPECT))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_ASPECT);
        } else if (field.equals(FIELD_EXACTASPECT))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_EXACTASPECT);
        } else if (isPropertyField(field))
        {
            return spanQueryBuilder(field, first, last, slop, inOrder);
        } else if (field.equals(FIELD_ALL))
        {
            Set<String> all = searchParameters.getAllAttributes();
            if ((all == null) || (all.size() == 0))
            {
                Collection<QName> contentAttributes = dictionaryService.getAllProperties(null);
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (QName qname : contentAttributes)
                {
                    Query part = getSpanQuery(PROPERTY_FIELD_PREFIX + qname.toString(), first, last, slop, inOrder);
                    query.add(part, Occur.SHOULD);
                }
                return query.build();
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : all)
                {
                    Query part = getSpanQuery(fieldName, first, last, slop, inOrder);
                    query.add(part, Occur.SHOULD);
                }
                return query.build();
            }

        } else if (field.equals(FIELD_ISUNSET))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_ISUNSET);
        } else if (field.equals(FIELD_ISNULL))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_ISNULL);
        } else if (field.equals(FIELD_ISNOTNULL))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_ISNOTNULL);
        } else if (field.equals(FIELD_EXISTS))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_EXISTS);
        } else if (QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(), namespacePrefixResolver,
                dictionaryService, field) != null)
        {
            Collection<QName> contentAttributes = dictionaryService
                    .getAllProperties(QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(),
                            namespacePrefixResolver, dictionaryService, field).getName());
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            for (QName qname : contentAttributes)
            {
                Query part = getSpanQuery(PROPERTY_FIELD_PREFIX + qname.toString(), first, last, slop, inOrder);
                query.add(part, Occur.SHOULD);
            }
            return query.build();
        } else if (field.equals(FIELD_FTSSTATUS))
        {
            throw new UnsupportedOperationException("Span is not supported for " + FIELD_FTSSTATUS);
        } else if (field.equals(FIELD_TAG))
        {
            return null;
        } else if (isPropertyField(field))
        {
            return spanQueryBuilder(field, first, last, slop, inOrder);
        } else
        {
            BytesRef firstBytes = analyzeMultitermTerm(field, first);
            BytesRef lastBytes = analyzeMultitermTerm(field, last);
            SpanQuery firstTerm = new SpanTermQuery(new Term(field, firstBytes));
            SpanQuery lastTerm = new SpanTermQuery(new Term(field, lastBytes));
            return new SpanNearQuery(new SpanQuery[]
            { firstTerm, lastTerm }, slop, inOrder);
        }
    }

    protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException
    {
        return getFieldQuery(field, queryText, AnalysisMode.DEFAULT, LuceneFunction.FIELD);
    }

    /**
     * @param field
     * @param queryText
     * @param analysisMode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public Query getFieldQuery(String field, String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        try
        {
            if (field.equals(FIELD_PATH))
            {
                return createPathQuery(queryText, false);
            } else if (field.equals(FIELD_PATHWITHREPEATS))
            {
                return createPathQuery(queryText, true);
            } else if (field.equals(FIELD_TEXT))
            {
                return createTextQuery(queryText, analysisMode, luceneFunction);
            } else if (field.equals(FIELD_ID))
            {
                return createIdQuery(queryText);
            } else if (field.equals(FIELD_SOLR4_ID))
            {
                return createSolr4IdQuery(queryText);
            } else if (field.equals(FIELD_DBID))
            {
                return createDbidQuery(queryText);
            } else if (field.equals(FIELD_ACLID))
            {
                return createAclIdQuery(queryText);
            } else if (field.equals(FIELD_OWNER))
            {
                return createOwnerQuery(queryText);
            } else if (field.equals(FIELD_OWNERSET))
            {
                return createOwnerSetQuery(queryText);
            } else if (field.equals(FIELD_READER))
            {
                return createReaderQuery(queryText);
            } else if (field.equals(FIELD_READERSET))
            {
                return createReaderSetQuery(queryText);
            } else if (field.equals(FIELD_AUTHORITY))
            {
                return createAuthorityQuery(queryText);
            } else if (field.equals(FIELD_AUTHORITYSET))
            {
                return createAuthoritySetQuery(queryText);
            } else if (field.equals(FIELD_DENIED))
            {
                return createDeniedQuery(queryText);
            } else if (field.equals(FIELD_DENYSET))
            {
                return createDenySetQuery(queryText);
            } else if (field.equals(FIELD_ISROOT))
            {
                return createIsRootQuery(queryText);
            } else if (field.equals(FIELD_ISCONTAINER))
            {
                return createIsContainerQuery(queryText);
            } else if (field.equals(FIELD_ISNODE))
            {
                return createIsNodeQuery(queryText);
            } else if (field.equals(FIELD_TX))
            {
                return createTransactionQuery(queryText);
            } else if (field.equals(FIELD_INTXID))
            {
                return createInTxIdQuery(queryText);
            } else if (field.equals(FIELD_INACLTXID))
            {
                return createInAclTxIdQuery(queryText);
            } else if (field.equals(FIELD_PARENT))
            {
                return createParentQuery(queryText);
            } else if (field.equals(FIELD_PRIMARYPARENT))
            {
                return createPrimaryParentQuery(queryText);
            } else if (field.equals(FIELD_QNAME))
            {
                return createQNameQuery(queryText);
            } else if (field.equals(FIELD_PRIMARYASSOCQNAME))
            {
                return createPrimaryAssocQNameQuery(queryText);
            } else if (field.equals(FIELD_PRIMARYASSOCTYPEQNAME))
            {
                return createPrimaryAssocTypeQNameQuery(queryText);
            } else if (field.equals(FIELD_ASSOCTYPEQNAME))
            {
                return createAssocTypeQNameQuery(queryText);
            } else if (field.equals(FIELD_CLASS))
            {
                ClassDefinition target = QueryParserUtils.matchClassDefinition(searchParameters.getNamespace(),
                        namespacePrefixResolver, dictionaryService, queryText);
                if (target == null)
                {
                    return new TermQuery(new Term(FIELD_TYPE, "_unknown_"));
                }
                return getFieldQuery(target.isAspect() ? FIELD_ASPECT : FIELD_TYPE, queryText, analysisMode,
                        luceneFunction);
            } else if (field.equals(FIELD_TYPE))
            {
                return createTypeQuery(queryText, false);
            } else if (field.equals(FIELD_EXACTTYPE))
            {
                return createTypeQuery(queryText, true);
            } else if (field.equals(FIELD_ASPECT))
            {
                return createAspectQuery(queryText, false);
            } else if (field.equals(FIELD_EXACTASPECT))
            {
                return createAspectQuery(queryText, true);
            } else if (isPropertyField(field))
            {
                Query query = attributeQueryBuilder(field, queryText, new FieldQuery(), analysisMode, luceneFunction);
                return query;
            } else if (field.equals(FIELD_ALL))
            {
                return createAllQuery(queryText, analysisMode, luceneFunction);
            } else if (field.equals(FIELD_ISUNSET))
            {
                return createIsUnsetQuery(queryText, analysisMode, luceneFunction);
            } else if (field.equals(FIELD_ISNULL))
            {
                return createIsNullQuery(queryText, analysisMode, luceneFunction);
            } else if (field.equals(FIELD_ISNOTNULL))
            {
                return createIsNotNull(queryText, analysisMode, luceneFunction);
            } else if (field.equals(FIELD_EXISTS))
            {
                return createExistsQuery(queryText, analysisMode, luceneFunction);
            } else if (QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(),
                    namespacePrefixResolver, dictionaryService, field) != null)
            {
                return createDataTypeDefinitionQuery(field, queryText, analysisMode, luceneFunction);
            } else if (field.equals(FIELD_FTSSTATUS))
            {
                return createTermQuery(field, queryText);
            } else if (field.equals(FIELD_TXID))
            {
                return createTxIdQuery(queryText);
            } else if (field.equals(FIELD_ACLTXID))
            {
                return createAclTxIdQuery(queryText);
            } else if (field.equals(FIELD_TXCOMMITTIME))
            {
                return createTxCommitTimeQuery(queryText);
            } else if (field.equals(FIELD_ACLTXCOMMITTIME))
            {
                return createAclTxCommitTimeQuery(queryText);
            } else if (field.equals(FIELD_TAG))
            {
                return createTagQuery(queryText);
            } else if (field.equals(FIELD_SITE))
            {
                return createSiteQuery(queryText);
            } else if (field.equals(FIELD_PNAME))
            {
                return createPNameQuery(queryText);
            } else if (field.equals(FIELD_NPATH))
            {
                return createNPathQuery(queryText);
            } else if (field.equals(FIELD_TENANT))
            {
                return createTenantQuery(queryText);
            } else if (field.equals(FIELD_ANCESTOR))
            {
                return createAncestorQuery(queryText);
            } else if (field.equals(FIELD_FINGERPRINT))
            {
                String[] parts = queryText.split("_");
                Collection values = null;
                String nodeId = parts[0];

                JSONObject json = (JSONObject)request.getContext().get(AbstractQParser.ALFRESCO_JSON);
                String fingerPrint = null;
                if(json != null) {
                    //Was the fingerprint passed in
                    String fingerPrintKey = "fingerprint." + nodeId;
                    if(json.has(fingerPrintKey)) {
                        fingerPrint = (String) json.get("fingerprint." + nodeId);
                        if (fingerPrint != null) {
                            List l = new ArrayList();
                            String[] hashes = fingerPrint.split(" ");
                            for (String hash : hashes) {
                                l.add(hash);
                            }
                            values = l;
                        }
                    }
                } else {
                    json = new JSONObject();
                }


                //Is the fingerprint in the local SolrContentStore
                if(values == null)
                {
                    long dbid = fetchDBID(nodeId);
                    if(dbid == -1 && isNumber(nodeId))
                    {
                        dbid = Long.parseLong(nodeId);
                    }

                    if(dbid > -1)
                    {
                        SolrInputDocument solrDoc = solrContentStore.retrieveDocFromSolrContentStore(AlfrescoSolrDataModel.getTenantId(TenantService.DEFAULT_DOMAIN), dbid);
                        if (solrDoc != null)
                        {
                            SolrInputField mh = solrDoc.getField("MINHASH");
                            if (mh != null)
                            {
                                values = mh.getValues();
                            }
                        }
                    }
                }

                String shards = this.solrParams.get("shards");
                if(values == null && shards != null)
                {
                    //we are in distributed mode
                    //Fetch the fingerPrint from the shards.
                    //The UUID and DBID will both work for method call.
                    values = fetchFingerPrint(shards, nodeId);
                }

                //If we're in distributed mode then add the fingerprint to the json
                if(values != null && shards != null && fingerPrint == null)
                {
                    ModifiableSolrParams newParams = new ModifiableSolrParams();
                    newParams.add(solrParams);
                    solrParams = newParams;
                    json.put("fingerprint." + nodeId, join(values, " "));
                    String jsonString = json.toString();
                    newParams.add(AbstractQParser.ALFRESCO_JSON, jsonString);
                    request.getContext().put(AbstractQParser.ALFRESCO_JSON, json);
                    request.setParams(newParams);
                }

                return createFingerPrintQuery(field, queryText, values, analysisMode, luceneFunction);

            } else
            {
                return getFieldQueryImpl(field, queryText, analysisMode, luceneFunction);
            }

        } catch (SAXPathException e)
        {
            throw new ParseException("Failed to parse XPath...\n" + e.getMessage());
        } catch (IOException e)
        {
            throw new ParseException("IO: " + e.getMessage());
        }

    }

    private boolean isNumber(String s) {
        for(int i=0; i<s.length(); i++) {
            if(!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private String join(Collection col, String delimiter){
        StringBuilder builder = new StringBuilder();
        for(Object o : col){
            if(builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(o.toString());
        }
        return builder.toString();
    }


    private long fetchDBID(String UUID) throws IOException {
        SolrIndexSearcher searcher = request.getSearcher();
        String query = UUID.startsWith("workspace") ? UUID : "workspace://SpacesStore/"+UUID;
        TermQuery q = new TermQuery(new Term(FIELD_LID, query));
        TopDocs docs = searcher.search(q, 1);
        Set<String> fields = new HashSet();
        fields.add(FIELD_DBID);
        if(docs.totalHits == 1) {
            ScoreDoc scoreDoc = docs.scoreDocs[0];
            Document doc = searcher.doc(scoreDoc.doc, fields);
            IndexableField dbidField = doc.getField(FIELD_DBID);
            return dbidField.numericValue().longValue();
        }

        return -1;
    }

    private Collection fetchFingerPrint(String shards, String nodeId) {
        shards = shards.replace(",", "|");
        List<String> urls = ((HttpShardHandlerFactory)shardHandlerFactory).makeURLList(shards);
        ExecutorService executorService =  null;
        List<Future> futures = new ArrayList();
        Collection fingerPrint = null;
        try {
            executorService = Executors.newCachedThreadPool();
            for (String url : urls) {
                futures.add(executorService.submit(new FingerPrintFetchTask(url, nodeId)));
            }

            for (Future<Collection> future : futures) {
                Collection col = future.get();
                if(col != null) {
                    fingerPrint = col;
                }
            }

        }catch(Exception e) {
            logger.error(e);
        } finally {
            executorService.shutdown();
        }
        return fingerPrint;
    }




    private class FingerPrintFetchTask implements Callable<Collection> {
        private String url;
        private String id;

        public FingerPrintFetchTask(String url, String id) {
            this.url = url;
            this.id = id;
        }

        public Collection call() throws Exception {
            HttpSolrClient solrClient = null;
            CloseableHttpClient closeableHttpClient = null;
            try {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.add(FingerPrintComponent.COMPONENT_NAME, "true");
                params.add("id", id);
                params.add("qt","/fingerprint");
                closeableHttpClient = HttpClientUtil.createClient(getClientParams());
                solrClient = new HttpSolrClient.Builder(url).withHttpClient(closeableHttpClient).build();;
                QueryRequest request = new QueryRequest(params, SolrRequest.METHOD.POST);
                QueryResponse response = request.process(solrClient);
                NamedList dataResponse = response.getResponse();
                NamedList fingerprint = (NamedList) dataResponse.get("fingerprint");
                return (Collection)fingerprint.get("MINHASH");
            } finally {
                closeableHttpClient.close();
                solrClient.close();
            }
        }

        protected ModifiableSolrParams getClientParams() {
            ModifiableSolrParams clientParams = new ModifiableSolrParams();
            clientParams.set(HttpClientUtil.PROP_SO_TIMEOUT, UpdateShardHandlerConfig.DEFAULT_DISTRIBUPDATESOTIMEOUT);
            clientParams.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, UpdateShardHandlerConfig.DEFAULT_DISTRIBUPDATECONNTIMEOUT);
            return clientParams;
        }
    }

    /**
     * @param field
     * @param queryText
     * @param analysisMode
     *
     * @param luceneFunction
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private Query createFingerPrintQuery(String field, String queryText, Collection values, AnalysisMode analysisMode,
            LuceneFunction luceneFunction) throws IOException, ParseException
    {
        String[] parts = queryText.split("_");
        if (parts.length == 0)
        {
            return createIsNodeQuery("T");
        }

        if (values != null)
        {
            int bandSize = 1;
            float fraction = -1;
            float truePositive = 1;
            if (parts.length > 1)
            {
                fraction = Float.parseFloat(parts[1]);
                if (fraction > 1)
                {
                    fraction /= 100;
                }
            }
            if (parts.length > 2)
            {
                truePositive = Float.parseFloat(parts[2]);
                if (truePositive > 1)
                {
                    truePositive /= 100;
                }
                bandSize = computeBandSize(values.size(), fraction, truePositive);
            }
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            BooleanQuery.Builder childBuilder = new BooleanQuery.Builder();
            int rowInBand = 0;
            for (Object token : values)
            {
                TermQuery tq = new TermQuery(new Term("MINHASH", token.toString()));
                if (bandSize == 1)
                {
                    builder.add(new ConstantScoreQuery(tq), Occur.SHOULD);
                } else
                {
                    childBuilder.add(new ConstantScoreQuery(tq), Occur.MUST);
                    rowInBand++;
                    if (rowInBand == bandSize)
                    {
                        builder.add(new ConstantScoreQuery(childBuilder.setDisableCoord(true).build()),
                                Occur.SHOULD);
                        childBuilder = new BooleanQuery.Builder();
                        rowInBand = 0;
                    }
                }
            }
            // Avoid a dubious narrow band .... wrap around and pad with the
            // start
            if (childBuilder.build().clauses().size() > 0)
            {
                for (Object token : values)
                {
                    TermQuery tq = new TermQuery(new Term("MINHASH", token.toString()));
                    childBuilder.add(new ConstantScoreQuery(tq), Occur.MUST);
                    rowInBand++;
                    if (rowInBand == bandSize)
                    {
                        builder.add(new ConstantScoreQuery(childBuilder.setDisableCoord(true).build()),
                                Occur.SHOULD);
                        break;
                    }
                }
            }
            builder.setDisableCoord(true);
            if (parts.length == 2)
            {
                builder.setMinimumNumberShouldMatch((int) (Math.ceil(values.size() * fraction)));
            }
            Query q = builder.build();
            return q;
        } else
        {
            return getFieldQueryImpl(field, queryText, analysisMode, luceneFunction);
        }
    }

    private int computeBandSize(int numHash, double sim, double expectedTruePositive)
    {
        for (int bands = 1; bands <= numHash; bands++)
        {
            int rowsInBand = numHash / bands;
            double truePositive = 1 - Math.pow(1 - Math.pow(sim, rowsInBand), bands);
            if (truePositive > expectedTruePositive)
            {
                return rowsInBand;
            }
        }
        return 1;
    }

    private ArrayList<String> getTokens(IndexableField indexableField) throws IOException
    {
        ArrayList<String> tokens = new ArrayList<String>();

        TokenStream ts = indexableField.tokenStream(schema.getIndexAnalyzer(), null);
        CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
        ts.reset();
        while (ts.incrementToken())
        {
            String token = new String(termAttribute.buffer(), 0, termAttribute.length());
            tokens.add(token);
        }
        ts.end();
        ts.close();

        return tokens;
    }

    /**
     * @param queryText
     * @return
     */
    private org.apache.lucene.search.Query createNPathQuery(String queryText)
    {
        return createTermQuery(FIELD_NPATH, queryText);
    }

    /**
     * @param queryText
     * @return
     */
    private Query createPNameQuery(String queryText)
    {
        return createTermQuery(FIELD_PNAME, queryText);
    }

    /**
     * @param queryText
     * @return
     */
    private Query createSiteQuery(String queryText)
    {
        if (queryText.equals("_EVERYTHING_"))
        {
            return createTermQuery(FIELD_ISNODE, "T");
        } 
        else if (queryText.equals("_ALL_SITES_"))
        {
            BooleanQuery.Builder invertedRepositoryQuery = new BooleanQuery.Builder();
            invertedRepositoryQuery.add(createTermQuery(FIELD_ISNODE, "T"), Occur.MUST);
            invertedRepositoryQuery.add(createTermQuery(FIELD_SITE, "_REPOSITORY_"), Occur.MUST_NOT);
            return invertedRepositoryQuery.build();
        } 
        else
        {
            return createTermQuery(FIELD_SITE, queryText);
        }
    }

    private boolean isPropertyField(String field)
    {
        if (field.startsWith(PROPERTY_FIELD_PREFIX))
        {
            return true;
        }
        int index = field.lastIndexOf('@');
        if (index > -1)
        {
            PropertyDefinition pDef = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                    namespacePrefixResolver, dictionaryService, field.substring(index + 1));
            if (pDef != null)
            {
                IndexedField indexedField = AlfrescoSolrDataModel.getInstance()
                        .getIndexedFieldNamesForProperty(pDef.getName());
                for (FieldInstance instance : indexedField.getFields())
                {
                    if (instance.getField().equals(field))
                    {
                        return true;
                    }
                }
                return false;
            } else
            {
                return false;
            }
        } else
        {
            return false;
        }

    }

    protected Query createTenantQuery(String queryText) throws ParseException
    {

        if (queryText.length() > 0)
        {

            return getFieldQueryImplWithIOExceptionWrapped(FIELD_TENANT, queryText, AnalysisMode.DEFAULT,
                    LuceneFunction.FIELD);

        } 
        else
        {
            return getFieldQueryImplWithIOExceptionWrapped(FIELD_TENANT, "_DEFAULT_", AnalysisMode.DEFAULT,
                    LuceneFunction.FIELD);
        }

    }

    protected Query createAncestorQuery(String queryText) throws ParseException
    {
        return createNodeRefQuery(FIELD_ANCESTOR, queryText);
    }

    /**
     * @param tag
     *            (which will then be ISO9075 encoded)
     * @return
     * @throws ParseException
     */
    protected Query createTagQuery(String tag) throws ParseException
    {
        return createTermQuery(FIELD_TAG, tag.toLowerCase());
    }

    private Query getFieldQueryImplWithIOExceptionWrapped(String field, String queryText, AnalysisMode analysisMode,
            LuceneFunction luceneFunction) throws ParseException
    {
        try
        {
            return getFieldQueryImpl(field, queryText, analysisMode, luceneFunction);
        } 
        catch (IOException e)
        {
            throw new ParseException("IO: " + e.getMessage());
        }
    }

    /**
     * @param queryText
     * @return
     */
    protected Query createDbidQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_DBID, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    protected Query createTxIdQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_TXID, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    protected Query createAclTxIdQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_ACLTXID, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    protected Query createTxCommitTimeQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_TXCOMMITTIME, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    protected Query createAclTxCommitTimeQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_ACLTXCOMMITTIME, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    protected Query createDataTypeDefinitionQuery(String field, String queryText, AnalysisMode analysisMode,
            LuceneFunction luceneFunction) throws ParseException
    {
        Collection<QName> contentAttributes = dictionaryService
                .getAllProperties(QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(),
                        namespacePrefixResolver, dictionaryService, field).getName());
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        for (QName qname : contentAttributes)
        {
            // The super implementation will create phrase queries etc if
            // required
            Query part = getFieldQuery(PROPERTY_FIELD_PREFIX + qname.toString(), queryText, analysisMode,
                    luceneFunction);
            if (part != null)
            {
                query.add(part, Occur.SHOULD);
            } else
            {
                query.add(createNoMatchQuery(), Occur.SHOULD);
            }
        }
        return query.build();
    }

    protected Query createIsNotNull(String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        PropertyDefinition pd = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, queryText);
        if (pd != null)
        {
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            query.add(createTermQuery(FIELD_PROPERTIES, pd.getName().toString()), Occur.MUST);
            query.add(createTermQuery(FIELD_NULLPROPERTIES, pd.getName().toString()), Occur.MUST_NOT);
            ;
            return query.build();
        } else
        {
            BooleanQuery.Builder query = new BooleanQuery.Builder();

            Query presenceQuery = getWildcardQuery(queryText, "*");
            if (presenceQuery != null)
            {
                query.add(presenceQuery, Occur.MUST);
            }
            return query.build();
        }
    }

    protected Query createIsNullQuery(String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        PropertyDefinition pd = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, queryText);
        if (pd != null)
        {
            return createTermQuery(FIELD_NULLPROPERTIES, pd.getName().toString());
        } else
        {
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            Query presenceQuery = getWildcardQuery(queryText, "*");
            if (presenceQuery != null)
            {
                query.add(createIsNodeQuery("T"), Occur.MUST);
                query.add(presenceQuery, Occur.MUST_NOT);
            }
            return query.build();
        }
    }

    protected Query createIsUnsetQuery(String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        PropertyDefinition pd = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, queryText);
        if (pd != null)
        {
            ClassDefinition containerClass = pd.getContainerClass();
            QName container = containerClass.getName();
            String classType = containerClass.isAspect() ? FIELD_ASPECT : FIELD_TYPE;
            Query typeQuery = getFieldQuery(classType, container.toString(), analysisMode, luceneFunction);

            BooleanQuery.Builder query = new BooleanQuery.Builder();
            Query presenceQuery = createTermQuery(FIELD_PROPERTIES, pd.getName().toString());
            if (presenceQuery != null)
            {
                query.add(typeQuery, Occur.MUST);
                query.add(presenceQuery, Occur.MUST_NOT);
            }

            return query.build();
        } else
        {
            BooleanQuery.Builder query = new BooleanQuery.Builder();

            Query presenceQuery = getWildcardQuery(queryText, "*");
            if (presenceQuery != null)
            {
                query.add(presenceQuery, Occur.MUST_NOT);
            }
            return query.build();
        }
    }

    protected Query createExistsQuery(String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        PropertyDefinition pd = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, queryText);
        if (pd != null)
        {
            return createTermQuery(FIELD_PROPERTIES, pd.getName().toString());
        } else
        {
            BooleanQuery.Builder query = new BooleanQuery.Builder();

            Query presenceQuery = getWildcardQuery(queryText, "*");
            if (presenceQuery != null)
            {
                query.add(createIsNodeQuery("T"), Occur.MUST);
                query.add(presenceQuery, Occur.MUST_NOT);
            }
            return query.build();
        }
    }

    protected Query createAllQuery(String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        // Set<String> all = searchParameters.getAllAttributes();
        // if ((all == null) || (all.size() == 0))
        // {
        // Collection<QName> contentAttributes =
        // dictionaryService.getAllProperties(null);
        // BooleanQuery query = new BooleanQuery();
        // for (QName qname : contentAttributes)
        // {
        // // The super implementation will create phrase queries etc if
        // required
        // Query part = getFieldQuery(PROPERTY_FIELD_PREFIX + qname.toString(),
        // queryText, analysisMode, luceneFunction);
        // if (part != null)
        // {
        // query.add(part, Occur.SHOULD);
        // }
        // else
        // {
        // query.add(createNoMatchQuery(), Occur.SHOULD);
        // }
        // }
        // return query;
        // }
        // else
        // {
        // BooleanQuery query = new BooleanQuery();
        // for (String fieldName : all)
        // {
        // Query part = getFieldQuery(fieldName, queryText, analysisMode,
        // luceneFunction);
        // if (part != null)
        // {
        // query.add(part, Occur.SHOULD);
        // }
        // else
        // {
        // query.add(createNoMatchQuery(), Occur.SHOULD);
        // }
        // }
        // return query;
        // }
        throw new UnsupportedOperationException();
    }

    protected Query createAspectQuery(String queryText, boolean exactOnly)
    {
        AspectDefinition target = QueryParserUtils.matchAspectDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, queryText);
        if (target == null)
        {
            return new TermQuery(new Term(FIELD_ASPECT, "_unknown_"));
        }

        if (exactOnly)
        {
            QName targetQName = target.getName();
            TermQuery termQuery = new TermQuery(new Term(FIELD_ASPECT, targetQName.toString()));

            return termQuery;
        } 
        else
        {
            Collection<QName> subclasses = dictionaryService.getSubAspects(target.getName(), true);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            for (QName qname : subclasses)
            {
                AspectDefinition current = dictionaryService.getAspect(qname);
                if (target.getName().equals(current.getName()) || current.getIncludedInSuperTypeQuery())
                {
                    TermQuery termQuery = new TermQuery(new Term(FIELD_ASPECT, qname.toString()));
                    if (termQuery != null)
                    {
                        booleanQuery.add(termQuery, Occur.SHOULD);
                    }
                }
            }
            return booleanQuery.build();
        }

    }

    protected Query createTypeQuery(String queryText, boolean exactOnly) throws ParseException
    {
        TypeDefinition target = QueryParserUtils.matchTypeDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, queryText);
        if (target == null)
        {
            return new TermQuery(new Term(FIELD_TYPE, "_unknown_"));
        }
        if (exactOnly)
        {
            QName targetQName = target.getName();
            TermQuery termQuery = new TermQuery(new Term(FIELD_TYPE, targetQName.toString()));
            return termQuery;
        } 
        else
        {
            Collection<QName> subclasses = dictionaryService.getSubTypes(target.getName(), true);
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            for (QName qname : subclasses)
            {
                TypeDefinition current = dictionaryService.getType(qname);
                if (target.getName().equals(current.getName()) || current.getIncludedInSuperTypeQuery())
                {
                    TermQuery termQuery = new TermQuery(new Term(FIELD_TYPE, qname.toString()));
                    if (termQuery != null)
                    {
                        booleanQuery.add(termQuery, Occur.SHOULD);
                    }
                }
            }
            return booleanQuery.build();
        }
    }

    protected Query createInTxIdQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_INTXID, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    protected Query createInAclTxIdQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_INACLTXID, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    @SuppressWarnings("deprecation")
    protected Query createTransactionQuery(String queryText)
    {
        return createTermQuery(FIELD_TX, queryText);
    }

    protected Query createIsNodeQuery(String queryText)
    {
        return createTermQuery(FIELD_ISNODE, queryText);
    }

    protected Query createIsContainerQuery(String queryText)
    {
        return createTermQuery(FIELD_ISCONTAINER, queryText);
    }

    protected Query createIsRootQuery(String queryText)
    {
        return createTermQuery(FIELD_ISROOT, queryText);
    }

    protected Query createTermQuery(String field, String queryText)
    {
        TermQuery termQuery = new TermQuery(new Term(field, queryText));
        return termQuery;
    }

    protected Query createPrimaryParentQuery(String queryText)
    {
        return createNodeRefQuery(FIELD_PRIMARYPARENT, queryText);
    }

    protected Query createParentQuery(String queryText)
    {
        return createNodeRefQuery(FIELD_PARENT, queryText);
    }

    protected Query createNodeRefQuery(String field, String queryText)
    {
        if (tenantService.isTenantUser() && (queryText.contains(StoreRef.URI_FILLER)))
        {
            // assume NodeRef, since it contains StorRef URI filler
            queryText = tenantService.getName(new NodeRef(queryText)).toString();
        }
        return createTermQuery(field, queryText);
    }

    protected Query createTextQuery(String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
            throws ParseException
    {
        Set<String> text = searchParameters.getTextAttributes();
        if ((text == null) || (text.size() == 0))
        {
            Query query = getFieldQuery(PROPERTY_FIELD_PREFIX + ContentModel.PROP_CONTENT.toString(), queryText,
                    analysisMode, luceneFunction);
            if (query == null)
            {
                return createNoMatchQuery();
            }
            return query;
        } else
        {
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            for (String fieldName : text)
            {
                Query part = getFieldQuery(fieldName, queryText, analysisMode, luceneFunction);
                if (part != null)
                {
                    query.add(part, Occur.SHOULD);
                } else
                {
                    query.add(createNoMatchQuery(), Occur.SHOULD);
                }
            }
            return query.build();
        }
    }

    @SuppressWarnings("unchecked")
    protected Query getFieldQueryImpl(String field, String queryText, AnalysisMode analysisMode,
            LuceneFunction luceneFunction) throws ParseException, IOException
    {
        // make sure the field exists or return a dummy query so we have no
        // error ....ACE-3231
        SchemaField schemaField = schema.getFieldOrNull(field);
        boolean isNumeric = false;
        if (schemaField == null)
        {
            return new TermQuery(new Term("_dummy_", "_miss_"));
        } 
        else
        {
            isNumeric = (schemaField.getType().getNumericType() != null);
        }

        // Use the analyzer to get all the tokens, and then build a TermQuery,
        // PhraseQuery, or noth

        // TODO: Untokenised columns with functions require special handling

        if (luceneFunction != LuceneFunction.FIELD)
        {
            throw new UnsupportedOperationException(
                    "Field queries are not supported on lucene functions (UPPER, LOWER, etc)");
        }

        // if the incoming string already has a language identifier we strip it
        // iff and addit back on again

        String localePrefix = "";

        String toTokenise = queryText;

        if (queryText.startsWith("{"))
        {
            int position = queryText.indexOf("}");
            if (position > 0)
            {
                String language = queryText.substring(0, position + 1);
                Locale locale = new Locale(queryText.substring(1, position));
                String token = queryText.substring(position + 1);
                boolean found = false;
                for (Locale current : Locale.getAvailableLocales())
                {
                    if (current.toString().equalsIgnoreCase(locale.toString()))
                    {
                        found = true;
                        break;
                    }
                }
                if (found)
                {
                    localePrefix = language;
                    toTokenise = token;
                } else
                {
                    // toTokenise = token;
                }
            }
        }

        String testText = toTokenise;
        boolean requiresMLTokenDuplication = false;
        String localeString = null;
        if (isPropertyField(field) && (localePrefix.length() == 0))
        {
            if ((queryText.length() > 0) && (queryText.charAt(0) == '\u0000'))
            {
                int position = queryText.indexOf("\u0000", 1);
                testText = queryText.substring(position + 1);
                requiresMLTokenDuplication = true;
                localeString = queryText.substring(1, position);

            }
        }

        // find the positions of any escaped * and ? and ignore them

        Set<Integer> wildcardPoistions = getWildcardPositions(testText);

        TokenStream source = null;
        ArrayList<PackedTokenAttributeImpl> list = new ArrayList<PackedTokenAttributeImpl>();
        boolean severalTokensAtSamePosition = false;
        PackedTokenAttributeImpl nextToken;
        int positionCount = 0;

        try
        {
            source = getAnalyzer().tokenStream(field, new StringReader(toTokenise));
            source.reset();
            while (source.incrementToken())
            {
                CharTermAttribute cta = source.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAtt = source.getAttribute(OffsetAttribute.class);
                TypeAttribute typeAtt = null;
                if (source.hasAttribute(TypeAttribute.class))
                {
                    typeAtt = source.getAttribute(TypeAttribute.class);
                }
                PositionIncrementAttribute posIncAtt = null;
                if (source.hasAttribute(PositionIncrementAttribute.class))
                {
                    posIncAtt = source.getAttribute(PositionIncrementAttribute.class);
                }
                nextToken = new PackedTokenAttributeImpl();
                nextToken.setEmpty().copyBuffer(cta.buffer(), 0, cta.length());
                nextToken.setOffset(offsetAtt.startOffset(), offsetAtt.endOffset());
                if (typeAtt != null)
                {
                    nextToken.setType(typeAtt.type());
                }
                if (posIncAtt != null)
                {
                    nextToken.setPositionIncrement(posIncAtt.getPositionIncrement());
                }

                list.add(nextToken);
                if (nextToken.getPositionIncrement() != 0)
                    positionCount += nextToken.getPositionIncrement();
                else
                    severalTokensAtSamePosition = true;
            }
        } 
        finally
        {
            try
            {
                if (source != null)
                {
                    source.close();
                }
            } catch (IOException e)
            {
                // ignore
            }
        }

        // add any alpha numeric wildcards that have been missed
        // Fixes most stop word and wild card issues

        for (int index = 0; index < testText.length(); index++)
        {
            char current = testText.charAt(index);
            if (((current == '*') || (current == '?')) && wildcardPoistions.contains(index))
            {
                StringBuilder pre = new StringBuilder(10);
                if (index == 0)
                {
                    // "*" and "?" at the start

                    boolean found = false;
                    for (int j = 0; j < list.size(); j++)
                    {
                        PackedTokenAttributeImpl test = list.get(j);
                        if ((test.startOffset() <= 0) && (0 < test.endOffset()))
                        {
                            found = true;
                            break;
                        }
                    }
                    if (!found && (list.size() == 0))
                    {
                        // Add new token followed by * not given by the
                        // tokeniser
                        PackedTokenAttributeImpl newToken = new PackedTokenAttributeImpl();
                        newToken.setEmpty().append("", 0, 0);
                        newToken.setType("ALPHANUM");
                        if (requiresMLTokenDuplication)
                        {
                            Locale locale = I18NUtil.parseLocale(localeString);
                            @SuppressWarnings("resource")
                            MLTokenDuplicator duplicator = new MLTokenDuplicator(locale, MLAnalysisMode.EXACT_LANGUAGE);
                            Iterator<PackedTokenAttributeImpl> it = duplicator.buildIterator(newToken);
                            if (it != null)
                            {
                                int count = 0;
                                while (it.hasNext())
                                {
                                    list.add(it.next());
                                    count++;
                                    if (count > 1)
                                    {
                                        severalTokensAtSamePosition = true;
                                    }
                                }
                            }
                        }
                        // content
                        else
                        {
                            list.add(newToken);
                        }
                    }
                } 
                else if (index > 0)
                {
                    // Add * and ? back into any tokens from which it has been
                    // removed

                    boolean tokenFound = false;
                    for (int j = 0; j < list.size(); j++)
                    {
                        PackedTokenAttributeImpl test = list.get(j);
                        if ((test.startOffset() <= index) && (index < test.endOffset()))
                        {
                            if (requiresMLTokenDuplication)
                            {
                                String termText = test.toString();
                                int position = termText.indexOf("}");
                                String language = termText.substring(0, position + 1);
                                String token = termText.substring(position + 1);
                                if (index >= test.startOffset() + token.length())
                                {
                                    test.setEmpty();
                                    test.append(language + token + current);
                                }
                            } 
                            else
                            {
                                if (index >= test.startOffset() + test.length())
                                {
                                    test.setEmpty();
                                    test.append(test.toString() + current);
                                }
                            }
                            tokenFound = true;
                            break;
                        }
                    }

                    if (!tokenFound)
                    {
                        for (int i = index - 1; i >= 0; i--)
                        {
                            char c = testText.charAt(i);
                            if (Character.isLetterOrDigit(c))
                            {
                                boolean found = false;
                                for (int j = 0; j < list.size(); j++)
                                {
                                    PackedTokenAttributeImpl test = list.get(j);
                                    if ((test.startOffset() <= i) && (i < test.endOffset()))
                                    {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found)
                                {
                                    break;
                                } 
                                else
                                {
                                    pre.insert(0, c);
                                }
                            } 
                            else
                            {
                                break;
                            }
                        }
                        if (pre.length() > 0)
                        {
                            // Add new token followed by * not given by the
                            // tokeniser
                            PackedTokenAttributeImpl newToken = new PackedTokenAttributeImpl();
                            newToken.setEmpty().append(pre.toString());
                            newToken.setOffset(index - pre.length(), index);
                            newToken.setType("ALPHANUM");
                            if (requiresMLTokenDuplication)
                            {
                                Locale locale = I18NUtil.parseLocale(localeString);
                                @SuppressWarnings("resource")
                                MLTokenDuplicator duplicator = new MLTokenDuplicator(locale,
                                        MLAnalysisMode.EXACT_LANGUAGE);
                                Iterator<PackedTokenAttributeImpl> it = duplicator.buildIterator(newToken);
                                if (it != null)
                                {
                                    int count = 0;
                                    while (it.hasNext())
                                    {
                                        list.add(it.next());
                                        count++;
                                        if (count > 1)
                                        {
                                            severalTokensAtSamePosition = true;
                                        }
                                    }
                                }
                            }
                            // content
                            else
                            {
                                list.add(newToken);
                            }
                        }
                    }
                }

                StringBuilder post = new StringBuilder(10);
                if (index > 0)
                {
                    for (int i = index + 1; i < testText.length(); i++)
                    {
                        char c = testText.charAt(i);
                        if (Character.isLetterOrDigit(c))
                        {
                            boolean found = false;
                            for (int j = 0; j < list.size(); j++)
                            {
                                PackedTokenAttributeImpl test = list.get(j);
                                if ((test.startOffset() <= i) && (i < test.endOffset()))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            if (found)
                            {
                                break;
                            } 
                            else
                            {
                                post.append(c);
                            }
                        } 
                        else
                        {
                            break;
                        }
                    }
                    if (post.length() > 0)
                    {
                        // Add new token followed by * not given by the
                        // tokeniser
                        PackedTokenAttributeImpl newToken = new PackedTokenAttributeImpl();
                        newToken.setEmpty().append(post.toString());
                        newToken.setOffset(index + 1, index + 1 + post.length());
                        newToken.setType("ALPHANUM");
                        if (requiresMLTokenDuplication)
                        {
                            Locale locale = I18NUtil.parseLocale(localeString);
                            @SuppressWarnings("resource")
                            MLTokenDuplicator duplicator = new MLTokenDuplicator(locale, MLAnalysisMode.EXACT_LANGUAGE);
                            Iterator<PackedTokenAttributeImpl> it = duplicator.buildIterator(newToken);
                            if (it != null)
                            {
                                int count = 0;
                                while (it.hasNext())
                                {
                                    list.add(it.next());
                                    count++;
                                    if (count > 1)
                                    {
                                        severalTokensAtSamePosition = true;
                                    }
                                }
                            }
                        }
                        // content
                        else
                        {
                            list.add(newToken);
                        }
                    }
                }

            }
        }

        // Put in real position increments as we treat them correctly
     
        int curentIncrement = -1;
        for (PackedTokenAttributeImpl c : list)
        {
            if (curentIncrement == -1)
            {
                curentIncrement = c.getPositionIncrement();
            } else if (c.getPositionIncrement() > 0)
            {
                curentIncrement = c.getPositionIncrement();
            } else
            {
                c.setPositionIncrement(curentIncrement);
            }
        }

        // Fix up position increments for in phrase isolated wildcards
        
        boolean lastWasWild = false;
        for(int i = 0; i < list.size() -1; i++)
        {
        	for(int j = list.get(i).endOffset() + 1; j < list.get(i + 1).startOffset() - 1; j++)
        	{
        		if(wildcardPoistions.contains(j))
        		{
        			if(!lastWasWild)
        			{
        			    list.get(i+1).setPositionIncrement(list.get(i+1).getPositionIncrement() + 1);
        			}
        			lastWasWild = true;
        		}
        		else
        		{
        			lastWasWild = false;
        		}
        	}
        }
        
        Collections.sort(list, new Comparator<PackedTokenAttributeImpl>()
        {

            public int compare(PackedTokenAttributeImpl o1, PackedTokenAttributeImpl o2)
            {
                int dif = o1.startOffset() - o2.startOffset();
                return dif;

            }
        });

        // Combined * and ? based strings - should redo the tokeniser

        // Build tokens by position

        LinkedList<LinkedList<PackedTokenAttributeImpl>> tokensByPosition = new LinkedList<LinkedList<PackedTokenAttributeImpl>>();
        LinkedList<PackedTokenAttributeImpl> currentList = null;
        int lastStart = 0;
        for (PackedTokenAttributeImpl c : list)
        {
            if (c.startOffset() == lastStart)
            {
                if (currentList == null)
                {
                    currentList = new LinkedList<PackedTokenAttributeImpl>();
                    tokensByPosition.add(currentList);
                }
                currentList.add(c);
            } else
            {
                currentList = new LinkedList<PackedTokenAttributeImpl>();
                tokensByPosition.add(currentList);
                currentList.add(c);
            }
            lastStart = c.startOffset();
        }

        // Build all the token sequences and see which ones get strung together

        OrderedHashSet<LinkedList<PackedTokenAttributeImpl>> allTokenSequencesSet = new OrderedHashSet<LinkedList<PackedTokenAttributeImpl>>();
        for (LinkedList<PackedTokenAttributeImpl> tokensAtPosition : tokensByPosition)
        {
            OrderedHashSet<LinkedList<PackedTokenAttributeImpl>> positionalSynonymSequencesSet = new OrderedHashSet<LinkedList<PackedTokenAttributeImpl>>();

            OrderedHashSet<LinkedList<PackedTokenAttributeImpl>> newAllTokenSequencesSet = new OrderedHashSet<LinkedList<PackedTokenAttributeImpl>>();

            FOR_FIRST_TOKEN_AT_POSITION_ONLY: for (PackedTokenAttributeImpl t : tokensAtPosition)
            {
                PackedTokenAttributeImpl replace = new PackedTokenAttributeImpl();
                replace.setEmpty().append(t);
                replace.setOffset(t.startOffset(), t.endOffset());
                replace.setType(t.type());
                replace.setPositionIncrement(t.getPositionIncrement());

                boolean tokenFoundSequence = false;
                for (LinkedList<PackedTokenAttributeImpl> tokenSequence : allTokenSequencesSet)
                {
                    LinkedList<PackedTokenAttributeImpl> newEntry = new LinkedList<PackedTokenAttributeImpl>();
                    newEntry.addAll(tokenSequence);
                    if ((newEntry.getLast().endOffset() == replace.endOffset())
                            && replace.type().equals(SynonymFilter.TYPE_SYNONYM))
                    {
                        if ((newEntry.getLast().startOffset() == replace.startOffset())
                                && newEntry.getLast().type().equals(SynonymFilter.TYPE_SYNONYM))
                        {
                            positionalSynonymSequencesSet.add(tokenSequence);
                            newEntry.add(replace);
                            tokenFoundSequence = true;
                        } 
                        else if (newEntry.getLast().type().equals(CommonGramsFilter.GRAM_TYPE))
                        {
                            if (newEntry.toString().endsWith(replace.toString()))
                            {
                                // already in the gram
                                positionalSynonymSequencesSet.add(tokenSequence);
                                tokenFoundSequence = true;
                            }
                            else
                            {
                                // need to replace the synonym in the current
                                // gram
                                tokenFoundSequence = true;
                                StringBuffer old = new StringBuffer(newEntry.getLast().toString());
                                old.replace(replace.startOffset() - newEntry.getLast().startOffset(),
                                        replace.endOffset() - newEntry.getLast().startOffset(), replace.toString());
                                PackedTokenAttributeImpl newToken = new PackedTokenAttributeImpl();
                                newToken.setEmpty().append(old.toString());
                                newToken.setOffset(newEntry.getLast().startOffset(), newEntry.getLast().endOffset());
                                newEntry.removeLast();
                                newEntry.add(newToken);
                            }
                        }
                    } 
                    else if ((newEntry.getLast().startOffset() < replace.startOffset())
                            && (newEntry.getLast().endOffset() < replace.endOffset()))
                    {
                        if (newEntry.getLast().type().equals(SynonymFilter.TYPE_SYNONYM)
                                && replace.type().equals(SynonymFilter.TYPE_SYNONYM))
                        {
                            positionalSynonymSequencesSet.add(tokenSequence);
                        }
                        newEntry.add(replace);
                        tokenFoundSequence = true;
                    }
                    newAllTokenSequencesSet.add(newEntry);
                }
                if (false == tokenFoundSequence)
                {
                    for (LinkedList<PackedTokenAttributeImpl> tokenSequence : newAllTokenSequencesSet)
                    {
                        LinkedList<PackedTokenAttributeImpl> newEntry = new LinkedList<PackedTokenAttributeImpl>();
                        newEntry.addAll(tokenSequence);
                        if ((newEntry.getLast().endOffset() == replace.endOffset())
                                && replace.type().equals(SynonymFilter.TYPE_SYNONYM))
                        {
                            if ((newEntry.getLast().startOffset() == replace.startOffset())
                                    && newEntry.getLast().type().equals(SynonymFilter.TYPE_SYNONYM))
                            {
                                positionalSynonymSequencesSet.add(tokenSequence);
                                newEntry.add(replace);
                                tokenFoundSequence = true;
                            } else if (newEntry.getLast().type().equals(CommonGramsFilter.GRAM_TYPE))
                            {
                                if (newEntry.toString().endsWith(replace.toString()))
                                {
                                    // already in the gram
                                    positionalSynonymSequencesSet.add(tokenSequence);
                                    tokenFoundSequence = true;
                                } else
                                {
                                    // need to replace the synonym in the
                                    // current gram
                                    tokenFoundSequence = true;
                                    StringBuffer old = new StringBuffer(newEntry.getLast().toString());
                                    old.replace(replace.startOffset() - newEntry.getLast().startOffset(),
                                            replace.endOffset() - newEntry.getLast().startOffset(), replace.toString());
                                    PackedTokenAttributeImpl newToken = new PackedTokenAttributeImpl();
                                    newToken.setEmpty().append(old.toString());
                                    newToken.setOffset(newEntry.getLast().startOffset(),
                                            newEntry.getLast().endOffset());
                                    newEntry.removeLast();
                                    newEntry.add(newToken);
                                    positionalSynonymSequencesSet.add(newEntry);
                                }
                            }
                        } else if ((newEntry.getLast().startOffset() < replace.startOffset())
                                && (newEntry.getLast().endOffset() < replace.endOffset()))
                        {
                            if (newEntry.getLast().type().equals(SynonymFilter.TYPE_SYNONYM)
                                    && replace.type().equals(SynonymFilter.TYPE_SYNONYM))
                            {
                                positionalSynonymSequencesSet.add(tokenSequence);
                                newEntry.add(replace);
                                tokenFoundSequence = true;
                            }
                        }
                    }
                }
                if (false == tokenFoundSequence)
                {
                    LinkedList<PackedTokenAttributeImpl> newEntry = new LinkedList<PackedTokenAttributeImpl>();
                    newEntry.add(replace);
                    newAllTokenSequencesSet.add(newEntry);
                }
                // Limit the max number of permutations we consider
                if (newAllTokenSequencesSet.size() > 64)
                {
                    break FOR_FIRST_TOKEN_AT_POSITION_ONLY;
                }
            }
            allTokenSequencesSet = newAllTokenSequencesSet;
            allTokenSequencesSet.addAll(positionalSynonymSequencesSet);

        }

        LinkedList<LinkedList<PackedTokenAttributeImpl>> allTokenSequences = new LinkedList<LinkedList<PackedTokenAttributeImpl>>(
                allTokenSequencesSet);

        // build the unique

        LinkedList<LinkedList<PackedTokenAttributeImpl>> fixedTokenSequences = new LinkedList<LinkedList<PackedTokenAttributeImpl>>();
        for (LinkedList<PackedTokenAttributeImpl> tokenSequence : allTokenSequences)
        {
            LinkedList<PackedTokenAttributeImpl> fixedTokenSequence = new LinkedList<PackedTokenAttributeImpl>();
            fixedTokenSequences.add(fixedTokenSequence);
            PackedTokenAttributeImpl replace = null;
            for (PackedTokenAttributeImpl c : tokenSequence)
            {
                if (replace == null)
                {
                    StringBuilder prefix = new StringBuilder();
                    for (int i = c.startOffset() - 1; i >= 0; i--)
                    {
                        char test = testText.charAt(i);
                        if (((test == '*') || (test == '?')) && wildcardPoistions.contains(i))
                        {
                            prefix.insert(0, test);
                        } else
                        {
                            break;
                        }
                    }
                    String pre = prefix.toString();
                    if (requiresMLTokenDuplication)
                    {
                        String termText = c.toString();
                        int position = termText.indexOf("}");
                        String language = termText.substring(0, position + 1);
                        String token = termText.substring(position + 1);
                        replace = new PackedTokenAttributeImpl();
                        replace.setEmpty().append(language + pre + token);
                        replace.setOffset(c.startOffset() - pre.length(), c.endOffset());
                        replace.setType(c.type());
                        replace.setPositionIncrement(c.getPositionIncrement());
                    } else
                    {
                        String termText = c.toString();
                        replace = new PackedTokenAttributeImpl();
                        replace.setEmpty().append(pre + termText);
                        replace.setOffset(c.startOffset() - pre.length(), c.endOffset());
                        replace.setType(c.type());
                        replace.setPositionIncrement(c.getPositionIncrement());
                    }
                } else
                {
                    StringBuilder prefix = new StringBuilder();
                    StringBuilder postfix = new StringBuilder();
                    StringBuilder builder = prefix;
                    for (int i = c.startOffset() - 1; i >= replace.endOffset(); i--)
                    {
                        char test = testText.charAt(i);
                        if (((test == '*') || (test == '?')) && wildcardPoistions.contains(i))
                        {
                            builder.insert(0, test);
                        } else
                        {
                            builder = postfix;
                            postfix.setLength(0);
                        }
                    }
                    String pre = prefix.toString();
                    String post = postfix.toString();

                    // Does it bridge?
                    if ((pre.length() > 0) && (replace.endOffset() + pre.length()) == c.startOffset())
                    {
                        String termText = c.toString();
                        if (requiresMLTokenDuplication)
                        {
                            int position = termText.indexOf("}");
                            @SuppressWarnings("unused")
                            String language = termText.substring(0, position + 1);
                            String token = termText.substring(position + 1);
                            int oldPositionIncrement = replace.getPositionIncrement();
                            String replaceTermText = replace.toString();
                            replace = new PackedTokenAttributeImpl();
                            replace.setEmpty().append(replaceTermText + pre + token);
                            replace.setOffset(replace.startOffset(), c.endOffset());
                            replace.setType(replace.type());
                            replace.setPositionIncrement(oldPositionIncrement);
                        } else
                        {
                            int oldPositionIncrement = replace.getPositionIncrement();
                            String replaceTermText = replace.toString();
                            replace = new PackedTokenAttributeImpl();
                            replace.setEmpty().append(replaceTermText + pre + termText);
                            replace.setOffset(replace.startOffset(), c.endOffset());
                            replace.setType(replace.type());
                            replace.setPositionIncrement(oldPositionIncrement);
                        }
                    } else
                    {
                        String termText = c.toString();
                        if (requiresMLTokenDuplication)
                        {
                            int position = termText.indexOf("}");
                            String language = termText.substring(0, position + 1);
                            String token = termText.substring(position + 1);
                            String replaceTermText = replace.toString();
                            PackedTokenAttributeImpl last = new PackedTokenAttributeImpl();
                            last.setEmpty().append(replaceTermText + post);
                            last.setOffset(replace.startOffset(), replace.endOffset() + post.length());
                            last.setType(replace.type());
                            last.setPositionIncrement(replace.getPositionIncrement());
                            fixedTokenSequence.add(last);
                            replace = new PackedTokenAttributeImpl();
                            replace.setEmpty().append(language + pre + token);
                            replace.setOffset(c.startOffset() - pre.length(), c.endOffset());
                            replace.setType(c.type());
                            replace.setPositionIncrement(c.getPositionIncrement());
                        } else
                        {
                            String replaceTermText = replace.toString();
                            PackedTokenAttributeImpl last = new PackedTokenAttributeImpl();
                            last.setEmpty().append(replaceTermText + post);
                            last.setOffset(replace.startOffset(), replace.endOffset() + post.length());
                            last.setType(replace.type());
                            last.setPositionIncrement(replace.getPositionIncrement());
                            fixedTokenSequence.add(last);
                            replace = new PackedTokenAttributeImpl();
                            replace.setEmpty().append(pre + termText);
                            replace.setOffset(c.startOffset() - pre.length(), c.endOffset());
                            replace.setType(c.type());
                            replace.setPositionIncrement(c.getPositionIncrement());
                        }
                    }
                }
            }
            // finish last
            if (replace != null)
            {
                StringBuilder postfix = new StringBuilder();
                if ((replace.endOffset() >= 0) && (replace.endOffset() < testText.length()))
                {
                    for (int i = replace.endOffset(); i < testText.length(); i++)
                    {
                        char test = testText.charAt(i);
                        if (((test == '*') || (test == '?')) && wildcardPoistions.contains(i))
                        {
                            postfix.append(test);
                        } else
                        {
                            break;
                        }
                    }
                }
                String post = postfix.toString();
                int oldPositionIncrement = replace.getPositionIncrement();
                String replaceTermText = replace.toString();
                PackedTokenAttributeImpl terminal = new PackedTokenAttributeImpl();
                terminal.setEmpty().append(replaceTermText + post);
                terminal.setOffset(replace.startOffset(), replace.endOffset() + post.length());
                terminal.setType(replace.type());
                terminal.setPositionIncrement(oldPositionIncrement);
                fixedTokenSequence.add(terminal);
            }
        }

        // rebuild fixed list

        ArrayList<PackedTokenAttributeImpl> fixed = new ArrayList<PackedTokenAttributeImpl>();
        for (LinkedList<PackedTokenAttributeImpl> tokenSequence : fixedTokenSequences)
        {
            for (PackedTokenAttributeImpl token : tokenSequence)
            {
                fixed.add(token);
            }
        }

        // reorder by start position and increment

        Collections.sort(fixed, new Comparator<PackedTokenAttributeImpl>()
        {

            public int compare(PackedTokenAttributeImpl o1, PackedTokenAttributeImpl o2)
            {
                int dif = o1.startOffset() - o2.startOffset();
                if (dif != 0)
                {
                    return dif;
                } else
                {
                    return o1.getPositionIncrement() - o2.getPositionIncrement();
                }
            }
        });

        // make sure we remove any tokens we have duplicated

        @SuppressWarnings("rawtypes")
        OrderedHashSet unique = new OrderedHashSet();
        unique.addAll(fixed);
        fixed = new ArrayList<PackedTokenAttributeImpl>(unique);

        list = fixed;

        // add any missing locales back to the tokens

        if (localePrefix.length() > 0)
        {
            for (int j = 0; j < list.size(); j++)
            {
                PackedTokenAttributeImpl currentToken = list.get(j);
                String termText = currentToken.toString();
                currentToken.setEmpty();
                currentToken.append(localePrefix + termText);
            }
        }

        SchemaField sf = schema.getField(field);

        boolean isShingled = false;
        @SuppressWarnings("resource")
        TokenizerChain tokenizerChain = (sf.getType().getQueryAnalyzer() instanceof TokenizerChain)
                ? ((TokenizerChain) sf.getType().getQueryAnalyzer()) : null;
        if (tokenizerChain != null)
        {
            for (TokenFilterFactory factory : tokenizerChain.getTokenFilterFactories())
            {
                if (factory instanceof ShingleFilterFactory)
                {
                    isShingled = true;
                    break;
                }
            }
        }
        @SuppressWarnings("resource")
        AlfrescoAnalyzerWrapper analyzerWrapper = (sf.getType().getQueryAnalyzer() instanceof AlfrescoAnalyzerWrapper)
                ? ((AlfrescoAnalyzerWrapper) sf.getType().getQueryAnalyzer()) : null;
        if (analyzerWrapper != null)
        {
            // assume if there are no term positions it is shingled ....
            isShingled = true;
        }

        boolean forceConjuncion = rerankPhase == RerankPhase.QUERY_PHASE;

        if (list.size() == 0)
        {
            return null;
        }
        else if (list.size() == 1)
        {
            nextToken = list.get(0);
            String termText = nextToken.toString();
            if (!isNumeric && (termText.contains("*") || termText.contains("?")))
            {
                return newWildcardQuery(new Term(field, termText));
            } 
            else
            {
                return newTermQuery(new Term(field, termText));
            }
        } 
        else
        {
            if (severalTokensAtSamePosition)
            {
                if (positionCount == 1)
                {
                    // no phrase query:
                    Builder q = newBooleanQuery();
                    for (int i = 0; i < list.size(); i++)
                    {
                        Query currentQuery;
                        nextToken = list.get(i);
                        String termText = nextToken.toString();
                        if (termText.contains("*") || termText.contains("?"))
                        {
                            currentQuery = newWildcardQuery(new Term(field, termText));
                        } else
                        {
                            currentQuery = newTermQuery(new Term(field, termText));
                        }
                        q.add(currentQuery, BooleanClause.Occur.SHOULD);
                    }
                    return q.build();
                } 
                else if (forceConjuncion)
                {
                    BooleanQuery.Builder or = new BooleanQuery.Builder();

                    for (LinkedList<PackedTokenAttributeImpl> tokenSequence : fixedTokenSequences)
                    {
                        BooleanQuery.Builder and = new BooleanQuery.Builder();
                        for (int i = 0; i < tokenSequence.size(); i++)
                        {
                            nextToken = (PackedTokenAttributeImpl) tokenSequence.get(i);
                            String termText = nextToken.toString();

                            Term term = new Term(field, termText);
                            if ((termText != null) && (termText.contains("*") || termText.contains("?")))
                            {
                                org.apache.lucene.search.WildcardQuery wildQuery = new org.apache.lucene.search.WildcardQuery(
                                        term);
                                and.add(wildQuery, Occur.MUST);
                            } else
                            {
                                TermQuery termQuery = new TermQuery(term);
                                and.add(termQuery, Occur.MUST);
                            }
                        }
                        if (and.build().clauses().size() > 0)
                        {
                            or.add(and.build(), Occur.SHOULD);
                        }
                    }
                    return or.build();
                }
                // shingle
                else if (sf.omitPositions() && isShingled)
                {

                    ArrayList<PackedTokenAttributeImpl> nonContained = getNonContained(list);
                    Query currentQuery;

                    BooleanQuery.Builder weakPhrase = new BooleanQuery.Builder();
                    for (PackedTokenAttributeImpl shingleToken : nonContained)
                    {
                        String termText = shingleToken.toString();
                        Term term = new Term(field, termText);

                        if ((termText != null) && (termText.contains("*") || termText.contains("?")))
                        {
                            currentQuery = new org.apache.lucene.search.WildcardQuery(term);
                        } else
                        {
                            currentQuery = new TermQuery(term);
                        }
                        weakPhrase.add(currentQuery, Occur.MUST);
                    }

                    return weakPhrase.build();

                }
                // Word delimiter factory and other odd things generate complex
                // token patterns
                // Smart skip token sequences with small tokens that generate
                // toomany wildcards
                // Fall back to the larger pattern
                // e.g Site1* will not do (S ite 1*) or (Site 1*) if 1* matches
                // too much (S ite1*) and (Site1*) will still be OK
                // If we skip all (for just 1* in the input) this is still an
                // issue.
                else
                {

                    return generateSpanOrQuery(field, fixedTokenSequences);

                }
            } 
            else
            {
                if (forceConjuncion)
                {
                    BooleanQuery.Builder or = new BooleanQuery.Builder();

                    for (LinkedList<PackedTokenAttributeImpl> tokenSequence : fixedTokenSequences)
                    {
                        BooleanQuery.Builder and = new BooleanQuery.Builder();
                        for (int i = 0; i < tokenSequence.size(); i++)
                        {
                            nextToken = (PackedTokenAttributeImpl) tokenSequence.get(i);
                            String termText = nextToken.toString();

                            Term term = new Term(field, termText);
                            if ((termText != null) && (termText.contains("*") || termText.contains("?")))
                            {
                                org.apache.lucene.search.WildcardQuery wildQuery = new org.apache.lucene.search.WildcardQuery(
                                        term);
                                and.add(wildQuery, Occur.MUST);
                            } 
                            else
                            {
                                TermQuery termQuery = new TermQuery(term);
                                and.add(termQuery, Occur.MUST);
                            }
                        }
                        if (and.build().clauses().size() > 0)
                        {
                            or.add(and.build(), Occur.SHOULD);
                        }
                    }
                    return or.build();
                } 
                else
                {
                    SpanQuery spanQuery = null;
                    ArrayList<SpanQuery> atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                    int gap = 0;
                    for (int i = 0; i < list.size(); i++)
                    {
                        nextToken = list.get(i);
                        String termText = nextToken.toString();
                        Term term = new Term(field, termText);
                        if (getEnablePositionIncrements())
                        {
                            SpanQuery nextSpanQuery;
                            if ((termText != null) && (termText.contains("*") || termText.contains("?")))
                            {
                                org.apache.lucene.search.WildcardQuery wildQuery = new org.apache.lucene.search.WildcardQuery(
                                        term);
                                SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery> wrapper = new SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery>(
                                        wildQuery);
                                wrapper.setRewriteMethod(new TopTermsSpanBooleanQueryRewrite(topTermSpanRewriteLimit));
                                nextSpanQuery = wrapper;
                            } 
                            else
                            {
                                nextSpanQuery = new SpanTermQuery(term);
                            }
                            if (gap == 0)
                            {
                                atSamePositionSpanOrQueryParts.add(nextSpanQuery);
                            } 
                            else
                            {
                                if (atSamePositionSpanOrQueryParts.size() == 0)
                                {
                                    if (spanQuery == null)
                                    {
                                        spanQuery = nextSpanQuery;
                                    } 
                                    else
                                    {
                                        spanQuery = new SpanNearQuery(new SpanQuery[]
                                        { spanQuery, nextSpanQuery }, (gap - 1) + internalSlop, internalSlop < 2);
                                    }
                                    atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                                } 
                                else if (atSamePositionSpanOrQueryParts.size() == 1)
                                {
                                    if (spanQuery == null)
                                    {
                                        spanQuery = atSamePositionSpanOrQueryParts.get(0);
                                    } 
                                    else
                                    {
                                        spanQuery = new SpanNearQuery(new SpanQuery[]
                                        { spanQuery, atSamePositionSpanOrQueryParts.get(0) }, (gap - 1) + internalSlop,
                                                internalSlop < 2);
                                    }
                                    atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                                    atSamePositionSpanOrQueryParts.add(nextSpanQuery);
                                } 
                                else
                                {
                                    if (spanQuery == null)
                                    {
                                        spanQuery = new SpanOrQuery(
                                                atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                                        {}));
                                    } 
                                    else
                                    {
                                        spanQuery = new SpanNearQuery(new SpanQuery[]
                                        { spanQuery,
                                                new SpanOrQuery(atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                                                {})) }, (gap - 1) + internalSlop, internalSlop < 2);
                                    }
                                    atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                                    atSamePositionSpanOrQueryParts.add(nextSpanQuery);
                                }
                            }
                            gap = nextToken.getPositionIncrement();
                        } 
                        else
                        {
                            SpanQuery nextSpanQuery;
                            if ((termText != null) && (termText.contains("*") || termText.contains("?")))
                            {
                                org.apache.lucene.search.WildcardQuery wildQuery = new org.apache.lucene.search.WildcardQuery(
                                        term);
                                SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery> wrapper = new SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery>(
                                        wildQuery);
                                wrapper.setRewriteMethod(new TopTermsSpanBooleanQueryRewrite(topTermSpanRewriteLimit));
                                nextSpanQuery = wrapper;
                            } 
                            else
                            {
                                nextSpanQuery = new SpanTermQuery(term);
                            }
                            if (spanQuery == null)
                            {
                                spanQuery = new SpanOrQuery(nextSpanQuery);
                            } 
                            else
                            {
                                spanQuery = new SpanOrQuery(spanQuery, nextSpanQuery);
                            }
                        }
                    }
                    if (atSamePositionSpanOrQueryParts.size() == 0)
                    {
                        return spanQuery;
                    } 
                    else if (atSamePositionSpanOrQueryParts.size() == 1)
                    {
                        if (spanQuery == null)
                        {
                            spanQuery = atSamePositionSpanOrQueryParts.get(0);
                        } 
                        else
                        {
                            spanQuery = new SpanNearQuery(new SpanQuery[]
                            { spanQuery, atSamePositionSpanOrQueryParts.get(0) }, (gap - 1) + internalSlop,
                                    internalSlop < 2);
                        }
                        return spanQuery;
                    } 
                    else
                    {
                        if (spanQuery == null)
                        {
                            spanQuery = new SpanOrQuery(atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                            {}));
                        } 
                        else
                        {
                            spanQuery = new SpanNearQuery(new SpanQuery[]
                            { spanQuery, new SpanOrQuery(atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                                    {})) }, (gap - 1) + internalSlop, internalSlop < 2);
                        }
                        return spanQuery;
                    }
                }
            }
        }
    }

    /**
     * @param list
     * @return
     */
    private ArrayList<PackedTokenAttributeImpl> getNonContained(ArrayList<PackedTokenAttributeImpl> list)
    {
        ArrayList<PackedTokenAttributeImpl> nonContained = new ArrayList<PackedTokenAttributeImpl>();
        NEXT_CANDIDATE: for (PackedTokenAttributeImpl candidate : list)
        {
            NEXT_TEST: for (PackedTokenAttributeImpl test : list)
            {
                if (candidate == test)
                {
                    continue NEXT_TEST;
                } else if ((test.startOffset() == candidate.startOffset())
                        && (candidate.endOffset() == test.endOffset())
                        && (test.toString().equals(candidate.toString())))
                {
                    continue NEXT_TEST;
                } else if ((test.startOffset() <= candidate.startOffset())
                        && (candidate.endOffset() <= test.endOffset())
                        && (test.toString().contains(candidate.toString())))
                {
                    continue NEXT_CANDIDATE;
                }
            }
            nonContained.add(candidate);
        }
        return nonContained;
    }

    /**
     * @param field
     * @param fixedTokenSequences
     *            LinkedList<LinkedList<PackedTokenAttributeImpl>>
     * @return Query
     */
    protected SpanQuery generateSpanOrQuery(String field,
            LinkedList<LinkedList<PackedTokenAttributeImpl>> fixedTokenSequences)
    {
        PackedTokenAttributeImpl nextToken;
        ArrayList<SpanQuery> spanOrQueryParts = new ArrayList<SpanQuery>();

        for (LinkedList<PackedTokenAttributeImpl> tokenSequence : fixedTokenSequences)
        {
            int gap = 1;
            SpanQuery spanQuery = null;
            ArrayList<SpanQuery> atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();

            // MNT-13239: if all tokens's positions are incremented by one then
            // create flat nearQuery
            if (getEnablePositionIncrements() && isAllTokensSequentiallyShifted(tokenSequence))
            {
                // there will be no tokens at same position
                List<SpanQuery> wildWrappedList = new ArrayList<SpanQuery>(tokenSequence.size());
                for (PackedTokenAttributeImpl token : tokenSequence)
                {
                    String termText = token.toString();
                    Term term = new Term(field, termText);
                    SpanQuery nextSpanQuery = wrapWildcardTerms(term);
                    wildWrappedList.add(nextSpanQuery);
                }
                if (wildWrappedList.size() == 1)
                {
                    spanQuery = wildWrappedList.get(0);
                } else
                {
                    spanQuery = new SpanNearQuery(wildWrappedList.toArray(new SpanQuery[wildWrappedList.size()]), 0,
                            true);
                }
            } else
            {
                for (int i = 0; i < tokenSequence.size(); i++)
                {
                    nextToken = (PackedTokenAttributeImpl) tokenSequence.get(i);
                    String termText = nextToken.toString();

                    Term term = new Term(field, termText);

                    if (getEnablePositionIncrements())
                    {
                        SpanQuery nextSpanQuery = wrapWildcardTerms(term);
                        if (gap == 0)
                        {
                            atSamePositionSpanOrQueryParts.add(nextSpanQuery);
                        } else
                        {
                            if (atSamePositionSpanOrQueryParts.size() == 0)
                            {
                                if (spanQuery == null)
                                {
                                    spanQuery = nextSpanQuery;
                                } else
                                {
                                    spanQuery = new SpanNearQuery(new SpanQuery[]
                                    { spanQuery, nextSpanQuery }, (gap - 1) + internalSlop, internalSlop < 2);
                                }
                                atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                            } else if (atSamePositionSpanOrQueryParts.size() == 1)
                            {
                                if (spanQuery == null)
                                {
                                    spanQuery = atSamePositionSpanOrQueryParts.get(0);
                                } else
                                {
                                    spanQuery = new SpanNearQuery(new SpanQuery[]
                                    { spanQuery, atSamePositionSpanOrQueryParts.get(0) }, (gap - 1) + internalSlop,
                                            internalSlop < 2);
                                }
                                atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                                atSamePositionSpanOrQueryParts.add(nextSpanQuery);
                            } else
                            {
                                if (spanQuery == null)
                                {
                                    spanQuery = new SpanOrQuery(atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                                    {}));
                                } else
                                {
                                    spanQuery = new SpanNearQuery(new SpanQuery[]
                                    { spanQuery, spanQuery = new SpanOrQuery(
                                            atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                                            {})) }, (gap - 1) + internalSlop, internalSlop < 2);
                                }
                                atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                                atSamePositionSpanOrQueryParts.add(nextSpanQuery);
                            }
                        }
                        gap = nextToken.getPositionIncrement();

                    } else
                    {
                        SpanQuery nextSpanQuery;
                        if ((termText != null) && (termText.contains("*") || termText.contains("?")))
                        {
                            org.apache.lucene.search.WildcardQuery wildQuery = new org.apache.lucene.search.WildcardQuery(
                                    term);
                            SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery> wrapper = new SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery>(
                                    wildQuery);
                            wrapper.setRewriteMethod(new TopTermsSpanBooleanQueryRewrite(topTermSpanRewriteLimit));
                            nextSpanQuery = wrapper;
                        } else
                        {
                            nextSpanQuery = new SpanTermQuery(term);
                        }
                        if (spanQuery == null)
                        {
                            spanQuery = new SpanOrQuery(nextSpanQuery);
                        } else
                        {
                            spanQuery = new SpanOrQuery(spanQuery, nextSpanQuery);
                        }
                    }
                }
            }

            if (atSamePositionSpanOrQueryParts.size() == 0)
            {
                spanOrQueryParts.add(spanQuery);
            } else if (atSamePositionSpanOrQueryParts.size() == 1)
            {
                if (spanQuery == null)
                {
                    spanQuery = atSamePositionSpanOrQueryParts.get(0);
                } else
                {
                    spanQuery = new SpanNearQuery(new SpanQuery[]
                    { spanQuery, atSamePositionSpanOrQueryParts.get(0) }, (gap - 1) + internalSlop, internalSlop < 2);
                }
                atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                spanOrQueryParts.add(spanQuery);
            } else
            {
                if (spanQuery == null)
                {
                    spanQuery = new SpanOrQuery(atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                    {}));
                } else
                {
                    spanQuery = new SpanNearQuery(new SpanQuery[]
                    { spanQuery, new SpanOrQuery(atSamePositionSpanOrQueryParts.toArray(new SpanQuery[]
                            {})) }, (gap - 1) + internalSlop, internalSlop < 2);
                }
                atSamePositionSpanOrQueryParts = new ArrayList<SpanQuery>();
                spanOrQueryParts.add(spanQuery);
            }
        }
        if (spanOrQueryParts.size() == 1)
        {
            return spanOrQueryParts.get(0);
        } else
        {
            return new SpanOrQuery(spanOrQueryParts.toArray(new SpanQuery[]
            {}));
        }
    }

    private SpanQuery wrapWildcardTerms(org.apache.lucene.index.Term term)
    {
        String termText = term.text();
        SpanQuery nextSpanQuery;
        if ((termText != null) && (termText.contains("*") || termText.contains("?")))
        {
            org.apache.lucene.search.WildcardQuery wildQuery = new org.apache.lucene.search.WildcardQuery(term);
            SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery> wrapper = new SpanMultiTermQueryWrapper<org.apache.lucene.search.WildcardQuery>(
                    wildQuery);
            wrapper.setRewriteMethod(new TopTermsSpanBooleanQueryRewrite(topTermSpanRewriteLimit));
            nextSpanQuery = wrapper;
        } else
        {
            nextSpanQuery = new SpanTermQuery(term);
        }
        return nextSpanQuery;
    }

    protected boolean isAllTokensSequentiallyShifted(List<PackedTokenAttributeImpl> tokenSequence)
    {
        if (0 == internalSlop)
        {
            // check if all tokens have gap = 1
            for (PackedTokenAttributeImpl tokenToCheck : tokenSequence)
            {
                if (1 != tokenToCheck.getPositionIncrement())
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Set<Integer> getWildcardPositions(String string)
    {
        HashSet<Integer> wildcardPositions = new HashSet<Integer>();

        boolean lastWasEscape = false;

        for (int i = 0; i < string.length(); i++)
        {
            char c = string.charAt(i);
            if (lastWasEscape)
            {
                lastWasEscape = false;
            } else
            {
                if (c == '\\')
                {
                    lastWasEscape = true;
                } else if (c == '*')
                {
                    wildcardPositions.add(i);
                } else if (c == '?')
                {
                    wildcardPositions.add(i);
                }
            }
        }

        return wildcardPositions;
    }

    /**
     * @exception ParseException
     *                throw in overridden method to disallow
     */

    protected Query getRangeQuery(String field, String part1, String part2, boolean startInclusive,
            boolean endInclusive) throws ParseException
    {
        return getRangeQuery(field, part1, part2, startInclusive, endInclusive, AnalysisMode.IDENTIFIER,
                LuceneFunction.FIELD);
    }

    protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException
    {
        return getRangeQuery(field, part1, part2, inclusive, inclusive, AnalysisMode.IDENTIFIER, LuceneFunction.FIELD);
    }

    /**
     * @param field
     * @param part1
     * @param part2
     * @param includeLower
     * @param includeUpper
     * @param analysisMode
     * @param luceneFunction
     * @return the query
     * @exception ParseException
     *                throw in overridden method to disallow
     */
    public Query getRangeQuery(String field, String part1, String part2, boolean includeLower, boolean includeUpper,
            AnalysisMode analysisMode, LuceneFunction luceneFunction) throws ParseException
    {
        if (field.equals(FIELD_PATH))
        {
            throw new UnsupportedOperationException("Range Queries are not support for " + FIELD_PATH);
        } else if (field.equals(FIELD_PATHWITHREPEATS))
        {
            throw new UnsupportedOperationException("Range Queries are not support for " + FIELD_PATHWITHREPEATS);
        } else if (field.equals(FIELD_TEXT))
        {
            Set<String> text = searchParameters.getTextAttributes();
            if ((text == null) || (text.size() == 0))
            {
                Query query = getRangeQuery(PROPERTY_FIELD_PREFIX + ContentModel.PROP_CONTENT.toString(), part1, part2,
                        includeLower, includeUpper, analysisMode, luceneFunction);
                if (query == null)
                {
                    return createNoMatchQuery();
                }

                return query;
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : text)
                {
                    Query part = getRangeQuery(fieldName, part1, part2, includeLower, includeUpper, analysisMode,
                            luceneFunction);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            }

        } else if (field.equals(FIELD_CASCADETX))
        {
            SchemaField sf = schema.getField(FIELD_CASCADETX);
            if (sf != null)
            {
                String start = null;
                try
                {
                    analyzeMultitermTerm(FIELD_CASCADETX, part1);
                    start = part1;
                } catch (Exception e)
                {

                }
                String end = null;
                try
                {
                    analyzeMultitermTerm(FIELD_CASCADETX, part2);
                    end = part2;
                } catch (Exception e)
                {

                }
                return sf.getType().getRangeQuery(null, sf, start, end, includeLower, includeUpper);
            } else
            {
                throw new UnsupportedOperationException();
            }
        }
        // FIELD_ID uses the default
        // FIELD_DBID uses the default
        // FIELD_ISROOT uses the default
        // FIELD_ISCONTAINER uses the default
        // FIELD_ISNODE uses the default
        // FIELD_TX uses the default
        // FIELD_PARENT uses the default
        // FIELD_PRIMARYPARENT uses the default
        // FIELD_QNAME uses the default
        // FIELD_PRIMARYASSOCTYPEQNAME uses the default
        // FIELD_ASSOCTYPEQNAME uses the default
        // FIELD_CLASS uses the default
        // FIELD_TYPE uses the default
        // FIELD_EXACTTYPE uses the default
        // FIELD_ASPECT uses the default
        // FIELD_EXACTASPECT uses the default
        // FIELD_TYPE uses the default
        // FIELD_TYPE uses the default
        if (isPropertyField(field))
        {
            Pair<String, String> fieldNameAndEnding = QueryParserUtils.extractFieldNameAndEnding(field);

            String expandedFieldName = null;
            PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                    namespacePrefixResolver, dictionaryService, fieldNameAndEnding.getFirst());
            IndexTokenisationMode tokenisationMode = IndexTokenisationMode.TRUE;
            if (propertyDef != null)
            {
                tokenisationMode = propertyDef.getIndexTokenisationMode();
                if (tokenisationMode == null)
                {
                    tokenisationMode = IndexTokenisationMode.TRUE;
                }
            } else
            {
                expandedFieldName = expandAttributeFieldName(field);
            }

            if (propertyDef != null)
            {
                // LOWER AND UPPER
                if (luceneFunction != LuceneFunction.FIELD)
                {
                    if (luceneFunction == LuceneFunction.LOWER)
                    {
                        if ((false == part1.toLowerCase().equals(part1))
                                || (false == part2.toLowerCase().equals(part2)))
                        {
                            return createNoMatchQuery();
                        }
                    }

                    if (luceneFunction == LuceneFunction.UPPER)
                    {
                        if ((false == part1.toUpperCase().equals(part1))
                                || (false == part2.toUpperCase().equals(part2)))
                        {
                            return createNoMatchQuery();
                        }
                    }

                    if (propertyDef.getDataType().getName().equals(DataTypeDefinition.TEXT))
                    {
                        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
                        List<Locale> locales = searchParameters.getLocales();
                        List<Locale> expandedLocales = new ArrayList<Locale>();
                        for (Locale locale : (((locales == null) || (locales.size() == 0))
                                ? Collections.singletonList(I18NUtil.getLocale()) : locales))
                        {
                            expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, false));
                        }
                        for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                                ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
                        {
                            addLocaleSpecificUntokenisedTextRangeFunction(expandedFieldName, propertyDef, part1, part2,
                                    includeLower, includeUpper, luceneFunction, booleanQuery, locale, tokenisationMode);
                        }
                        return booleanQuery.build();
                    } else
                    {
                        throw new UnsupportedOperationException("Lucene Function");
                    }
                }

                if (propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT))
                {
                    return buildTextMLTextOrContentRange(field, part1, part2, includeLower, includeUpper, analysisMode,
                            expandedFieldName, propertyDef, tokenisationMode);
                } else if (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT))
                {
                    String solrField = null;
                    switch (fieldNameAndEnding.getSecond())
                    {
                    case FIELD_SIZE_SUFFIX:
                        solrField = AlfrescoSolrDataModel.getInstance()
                                .getQueryableFields(propertyDef.getName(), ContentFieldType.SIZE, FieldUse.ID)
                                .getFields().get(0).getField();
                        break;
                    case FIELD_MIMETYPE_SUFFIX:
                        solrField = AlfrescoSolrDataModel.getInstance()
                                .getQueryableFields(propertyDef.getName(), ContentFieldType.MIMETYPE, FieldUse.ID)
                                .getFields().get(0).getField();
                        break;
                    case FIELD_ENCODING_SUFFIX:
                        solrField = AlfrescoSolrDataModel.getInstance()
                                .getQueryableFields(propertyDef.getName(), ContentFieldType.ENCODING, FieldUse.ID)
                                .getFields().get(0).getField();
                        break;
                    case FIELD_LOCALE_SUFFIX:
                        solrField = AlfrescoSolrDataModel.getInstance()
                                .getQueryableFields(propertyDef.getName(), ContentFieldType.LOCALE, FieldUse.ID)
                                .getFields().get(0).getField();
                        break;
                    }
                    if (solrField != null)
                    {
                        String start = null;
                        try
                        {
                            analyzeMultitermTerm(solrField, part1);
                            start = part1;
                        } catch (Exception e)
                        {

                        }
                        String end = null;
                        try
                        {
                            analyzeMultitermTerm(solrField, part2);
                            end = part2;
                        } catch (Exception e)
                        {

                        }

                        SchemaField sf = schema.getField(solrField);
                        return sf.getType().getRangeQuery(null, sf, start, end, includeLower, includeUpper);
                    } else
                    {
                        return buildTextMLTextOrContentRange(field, part1, part2, includeLower, includeUpper,
                                analysisMode, expandedFieldName, propertyDef, tokenisationMode);
                    }
                } else if (propertyDef.getDataType().getName().equals(DataTypeDefinition.TEXT))
                {
                    return buildTextMLTextOrContentRange(field, part1, part2, includeLower, includeUpper, analysisMode,
                            expandedFieldName, propertyDef, tokenisationMode);
                } else if (propertyDef.getDataType().getName().equals(DataTypeDefinition.DATETIME)
                        || propertyDef.getDataType().getName().equals(DataTypeDefinition.DATE))
                {
                    Pair<Date, Integer> dateAndResolution1 = parseDateString(part1);
                    Pair<Date, Integer> dateAndResolution2 = parseDateString(part2);

                    BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
                    IndexedField indexedField = AlfrescoSolrDataModel.getInstance()
                            .getQueryableFields(propertyDef.getName(), null, FieldUse.ID);
                    for (FieldInstance instance : indexedField.getFields())
                    {
                        String start = dateAndResolution1 == null ? part1
                                : (includeLower ? getDateStart(dateAndResolution1) : getDateEnd(dateAndResolution1));
                        String end = dateAndResolution2 == null ? part2
                                : (includeUpper ? getDateEnd(dateAndResolution2) : getDateStart(dateAndResolution2));
                        if (start.equals("*"))
                        {
                            start = null;
                        }
                        if (end.equals("*"))
                        {
                            end = null;
                        }

                        SchemaField sf = schema.getField(instance.getField());

                        Query query = sf.getType().getRangeQuery(null, sf, start, end, includeLower, includeUpper);
                        if (query != null)
                        {
                            bQuery.add(query, Occur.SHOULD);
                        }
                    }
                    return bQuery.build();
                } else
                {
                    String solrField = AlfrescoSolrDataModel.getInstance()
                            .getQueryableFields(propertyDef.getName(), null, FieldUse.ID).getFields().get(0).getField();

                    String start = null;
                    try
                    {
                        analyzeMultitermTerm(solrField, part1);
                        start = part1;
                    } catch (Exception e)
                    {

                    }
                    String end = null;
                    try
                    {
                        analyzeMultitermTerm(solrField, part2);
                        end = part2;
                    } catch (Exception e)
                    {

                    }

                    SchemaField sf = schema.getField(solrField);
                    return sf.getType().getRangeQuery(null, sf, start, end, includeLower, includeUpper);
                }
            } else
            {
                throw new UnsupportedOperationException();
            }
        } else if (field.equals(FIELD_ALL))
        {
            Set<String> all = searchParameters.getAllAttributes();
            if ((all == null) || (all.size() == 0))
            {
                Collection<QName> contentAttributes = dictionaryService.getAllProperties(null);
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (QName qname : contentAttributes)
                {
                    Query part = getRangeQuery(PROPERTY_FIELD_PREFIX + qname.toString(), part1, part2, includeLower,
                            includeUpper, analysisMode, luceneFunction);
                    query.add(part, Occur.SHOULD);
                }
                return query.build();
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : all)
                {
                    Query part = getRangeQuery(fieldName, part1, part2, includeLower, includeUpper, analysisMode,
                            luceneFunction);
                    query.add(part, Occur.SHOULD);
                }
                return query.build();
            }

        }
        // FIELD_ISUNSET uses the default
        // FIELD_ISNULL uses the default
        // FIELD_ISNOTNULL uses the default
        // FIELD_EXISTS uses the default
        else if (QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(), namespacePrefixResolver,
                dictionaryService, field) != null)
        {
            Collection<QName> contentAttributes = dictionaryService
                    .getAllProperties(QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(),
                            namespacePrefixResolver, dictionaryService, field).getName());
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            for (QName qname : contentAttributes)
            {
                Query part = getRangeQuery(PROPERTY_FIELD_PREFIX + qname.toString(), part1, part2, includeLower,
                        includeUpper, analysisMode, luceneFunction);
                query.add(part, Occur.SHOULD);
            }
            return query.build();
        }
        // FIELD_FTSSTATUS uses the default
        if (field.equals(FIELD_TAG))
        {
            throw new UnsupportedOperationException("Range Queries are not support for " + FIELD_TAG);
        } else
        {
            // None property - leave alone
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @param field
     * @param part1
     * @param part2
     * @param includeLower
     * @param includeUpper
     * @param analysisMode
     * @param expandedFieldName
     * @param propertyDef
     * @param tokenisationMode
     * @return
     * @throws ParseException
     */
    private Query buildTextMLTextOrContentRange(String field, String part1, String part2, boolean includeLower,
            boolean includeUpper, AnalysisMode analysisMode, String expandedFieldName, PropertyDefinition propertyDef,
            IndexTokenisationMode tokenisationMode) throws ParseException
    {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        List<Locale> locales = searchParameters.getLocales();
        List<Locale> expandedLocales = new ArrayList<Locale>();
        for (Locale locale : (((locales == null) || (locales.size() == 0))
                ? Collections.singletonList(I18NUtil.getLocale()) : locales))
        {
            expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, false));
        }
        for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
        {

            try
            {
                addTextRange(field, propertyDef, part1, part2, includeLower, includeUpper, analysisMode,
                        expandedFieldName, propertyDef, tokenisationMode, booleanQuery, locale);
            } catch (IOException e)
            {
                throw new ParseException(
                        "Failed to tokenise: <" + part1 + "> or <" + part2 + ">   for " + propertyDef.getName());
            }

        }
        return booleanQuery.build();
    }

    private String expandAttributeFieldName(String field)
    {
        return PROPERTY_FIELD_PREFIX + QueryParserUtils.expandQName(searchParameters.getNamespace(),
                namespacePrefixResolver, field.substring(1));
    }

    protected String getToken(String field, String value, AnalysisMode analysisMode) throws ParseException
    {
        try (TokenStream source = getAnalyzer().tokenStream(field, new StringReader(value)))
        {
            String tokenised = null;

            while (source.incrementToken())
            {
                CharTermAttribute cta = source.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAtt = source.getAttribute(OffsetAttribute.class);
                TypeAttribute typeAtt = null;
                if (source.hasAttribute(TypeAttribute.class))
                {
                    typeAtt = source.getAttribute(TypeAttribute.class);
                }
                PositionIncrementAttribute posIncAtt = null;
                if (source.hasAttribute(PositionIncrementAttribute.class))
                {
                    posIncAtt = source.getAttribute(PositionIncrementAttribute.class);
                }
                PackedTokenAttributeImpl token = new PackedTokenAttributeImpl();
                token.setEmpty().copyBuffer(cta.buffer(), 0, cta.length());
                token.setOffset(offsetAtt.startOffset(), offsetAtt.endOffset());
                if (typeAtt != null)
                {
                    token.setType(typeAtt.type());
                }
                if (posIncAtt != null)
                {
                    token.setPositionIncrement(posIncAtt.getPositionIncrement());
                }

                tokenised = token.toString();
            }
            return tokenised;
        } catch (IOException e)
        {
            throw new ParseException("IO" + e.getMessage());
        }

    }

    @Override
    public Query getPrefixQuery(String field, String termStr) throws ParseException
    {
        return getPrefixQuery(field, termStr, AnalysisMode.PREFIX);
    }

    @SuppressWarnings("deprecation")
    public Query getPrefixQuery(String field, String termStr, AnalysisMode analysisMode) throws ParseException
    {
        if (field.equals(FIELD_PATH))
        {
            throw new UnsupportedOperationException("Prefix Queries are not support for " + FIELD_PATH);
        } else if (field.equals(FIELD_PATHWITHREPEATS))
        {
            throw new UnsupportedOperationException("Prefix Queries are not support for " + FIELD_PATHWITHREPEATS);
        } else if (field.equals(FIELD_TEXT))
        {
            Set<String> text = searchParameters.getTextAttributes();
            if ((text == null) || (text.size() == 0))
            {
                Query query = getPrefixQuery(PROPERTY_FIELD_PREFIX + ContentModel.PROP_CONTENT.toString(), termStr,
                        analysisMode);
                if (query == null)
                {
                    return createNoMatchQuery();
                }

                return query;
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : text)
                {
                    Query part = getPrefixQuery(fieldName, termStr, analysisMode);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            }
        } else if (field.equals(FIELD_ID))
        {
            boolean lowercaseExpandedTerms = getLowercaseExpandedTerms();
            try
            {
                setLowercaseExpandedTerms(false);
                return super.getPrefixQuery(FIELD_LID, termStr);
            } finally
            {
                setLowercaseExpandedTerms(lowercaseExpandedTerms);
            }
        } else if (field.equals(FIELD_DBID) || field.equals(FIELD_ISROOT) || field.equals(FIELD_ISCONTAINER)
                || field.equals(FIELD_ISNODE) || field.equals(FIELD_TX) || field.equals(FIELD_PARENT)
                || field.equals(FIELD_PRIMARYPARENT) || field.equals(FIELD_QNAME)
                || field.equals(FIELD_PRIMARYASSOCTYPEQNAME) || field.equals(FIELD_ASSOCTYPEQNAME))
        {
            boolean lowercaseExpandedTerms = getLowercaseExpandedTerms();
            try
            {
                setLowercaseExpandedTerms(false);
                return super.getPrefixQuery(field, termStr);
            } finally
            {
                setLowercaseExpandedTerms(lowercaseExpandedTerms);
            }
        } else if (field.equals(FIELD_CLASS))
        {
            return super.getPrefixQuery(field, termStr);
            // throw new UnsupportedOperationException("Prefix Queries are not
            // support for "+FIELD_CLASS);
        } else if (field.equals(FIELD_TYPE))
        {
            return super.getPrefixQuery(field, termStr);
            // throw new UnsupportedOperationException("Prefix Queries are not
            // support for "+FIELD_TYPE);
        } else if (field.equals(FIELD_EXACTTYPE))
        {
            return super.getPrefixQuery(field, termStr);
            // throw new UnsupportedOperationException("Prefix Queries are not
            // support for "+FIELD_EXACTTYPE);
        } else if (field.equals(FIELD_ASPECT))
        {
            return super.getPrefixQuery(field, termStr);
            // throw new UnsupportedOperationException("Prefix Queries are not
            // support for "+FIELD_ASPECT);
        } else if (field.equals(FIELD_EXACTASPECT))
        {
            return super.getPrefixQuery(field, termStr);
            // throw new UnsupportedOperationException("Prefix Queries are not
            // support for "+FIELD_EXACTASPECT);
        } else if (isPropertyField(field))
        {
            return attributeQueryBuilder(field, termStr, new PrefixQuery(), analysisMode, LuceneFunction.FIELD);
        } else if (field.equals(FIELD_ALL))
        {
            Set<String> all = searchParameters.getAllAttributes();
            if ((all == null) || (all.size() == 0))
            {
                Collection<QName> contentAttributes = dictionaryService.getAllProperties(null);
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (QName qname : contentAttributes)
                {
                    // The super implementation will create phrase queries etc
                    // if required
                    Query part = getPrefixQuery(PROPERTY_FIELD_PREFIX + qname.toString(), termStr, analysisMode);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : all)
                {
                    Query part = getPrefixQuery(fieldName, termStr, analysisMode);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            }
        } else if (field.equals(FIELD_ISUNSET))
        {
            throw new UnsupportedOperationException("Prefix Queries are not support for " + FIELD_ISUNSET);
        } else if (field.equals(FIELD_ISNULL))
        {
            throw new UnsupportedOperationException("Prefix Queries are not support for " + FIELD_ISNULL);
        } else if (field.equals(FIELD_ISNOTNULL))
        {
            throw new UnsupportedOperationException("Prefix Queries are not support for " + FIELD_ISNOTNULL);
        } else if (field.equals(FIELD_EXISTS))
        {
            throw new UnsupportedOperationException("Prefix Queries are not support for " + FIELD_EXISTS);
        } else if (QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(), namespacePrefixResolver,
                dictionaryService, field) != null)
        {
            Collection<QName> contentAttributes = dictionaryService
                    .getAllProperties(QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(),
                            namespacePrefixResolver, dictionaryService, field).getName());
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            for (QName qname : contentAttributes)
            {
                // The super implementation will create phrase queries etc if
                // required
                Query part = getPrefixQuery(PROPERTY_FIELD_PREFIX + qname.toString(), termStr, analysisMode);
                if (part != null)
                {
                    query.add(part, Occur.SHOULD);
                } else
                {
                    query.add(createNoMatchQuery(), Occur.SHOULD);
                }
            }
            return query.build();
        } else if (field.equals(FIELD_FTSSTATUS))
        {
            throw new UnsupportedOperationException("Prefix Queries are not support for " + FIELD_FTSSTATUS);
        } else if (field.equals(FIELD_TAG))
        {
            return super.getPrefixQuery(field, termStr);
        } else if (field.equals(FIELD_SITE))
        {
            return super.getPrefixQuery(field, termStr);
        } else if (field.equals(FIELD_NPATH))
        {
            return super.getPrefixQuery(field, termStr);
        } else if (field.equals(FIELD_PNAME))
        {
            return super.getPrefixQuery(field, termStr);
        } else
        {
            return super.getPrefixQuery(field, termStr);
        }
    }

    @Override
    public Query getWildcardQuery(String field, String termStr) throws ParseException
    {
        return getWildcardQuery(field, termStr, AnalysisMode.WILD);
    }

    @SuppressWarnings("deprecation")
    public Query getWildcardQuery(String field, String termStr, AnalysisMode analysisMode) throws ParseException
    {
        if (field.equals(FIELD_PATH))
        {
            throw new UnsupportedOperationException("Wildcard Queries are not support for " + FIELD_PATH);
        } else if (field.equals(FIELD_PATHWITHREPEATS))
        {
            throw new UnsupportedOperationException("Wildcard Queries are not support for " + FIELD_PATHWITHREPEATS);
        } else if (field.equals(FIELD_TEXT))
        {
            Set<String> text = searchParameters.getTextAttributes();
            if ((text == null) || (text.size() == 0))
            {
                Query query = getWildcardQuery(PROPERTY_FIELD_PREFIX + ContentModel.PROP_CONTENT.toString(), termStr,
                        analysisMode);
                if (query == null)
                {
                    return createNoMatchQuery();
                }

                return query;
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : text)
                {
                    Query part = getWildcardQuery(fieldName, termStr, analysisMode);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            }
        } else if (field.equals(FIELD_ID))
        {
            boolean lowercaseExpandedTerms = getLowercaseExpandedTerms();
            try
            {
                setLowercaseExpandedTerms(false);
                return super.getWildcardQuery(FIELD_LID, termStr);
            } finally
            {
                setLowercaseExpandedTerms(lowercaseExpandedTerms);
            }
        } else if (field.equals(FIELD_DBID) || field.equals(FIELD_ISROOT) || field.equals(FIELD_ISCONTAINER)
                || field.equals(FIELD_ISNODE) || field.equals(FIELD_TX) || field.equals(FIELD_PARENT)
                || field.equals(FIELD_PRIMARYPARENT) || field.equals(FIELD_QNAME)
                || field.equals(FIELD_PRIMARYASSOCTYPEQNAME) || field.equals(FIELD_ASSOCTYPEQNAME))
        {
            boolean lowercaseExpandedTerms = getLowercaseExpandedTerms();
            try
            {
                setLowercaseExpandedTerms(false);
                return super.getWildcardQuery(field, termStr);
            } finally
            {
                setLowercaseExpandedTerms(lowercaseExpandedTerms);
            }
        } else if (field.equals(FIELD_CLASS))
        {
            return super.getWildcardQuery(field, termStr);
            // throw new UnsupportedOperationException("Wildcard Queries are not
            // support for "+FIELD_CLASS);
        } else if (field.equals(FIELD_TYPE))
        {
            return super.getWildcardQuery(field, termStr);
            // throw new UnsupportedOperationException("Wildcard Queries are not
            // support for "+FIELD_TYPE);
        } else if (field.equals(FIELD_EXACTTYPE))
        {
            return super.getWildcardQuery(field, termStr);
            // throw new UnsupportedOperationException("Wildcard Queries are not
            // support for "+FIELD_EXACTTYPE);
        } else if (field.equals(FIELD_ASPECT))
        {
            return super.getWildcardQuery(field, termStr);
            // throw new UnsupportedOperationException("Wildcard Queries are not
            // support for "+FIELD_ASPECT);
        } else if (field.equals(FIELD_EXACTASPECT))
        {
            return super.getWildcardQuery(field, termStr);
            // throw new UnsupportedOperationException("Wildcard Queries are not
            // support for "+FIELD_EXACTASPECT);
        } else if (isPropertyField(field))
        {
            return attributeQueryBuilder(field, termStr, new WildcardQuery(), analysisMode, LuceneFunction.FIELD);
        } else if (field.equals(FIELD_ALL))
        {
            Set<String> all = searchParameters.getAllAttributes();
            if ((all == null) || (all.size() == 0))
            {
                Collection<QName> contentAttributes = dictionaryService.getAllProperties(null);
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (QName qname : contentAttributes)
                {
                    // The super implementation will create phrase queries etc
                    // if required
                    Query part = getWildcardQuery(PROPERTY_FIELD_PREFIX + qname.toString(), termStr, analysisMode);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : all)
                {
                    Query part = getWildcardQuery(fieldName, termStr, analysisMode);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            }
        } else if (field.equals(FIELD_ISUNSET))
        {
            throw new UnsupportedOperationException("Wildcard Queries are not support for " + FIELD_ISUNSET);
        } else if (field.equals(FIELD_ISNULL))
        {
            throw new UnsupportedOperationException("Wildcard Queries are not support for " + FIELD_ISNULL);
        } else if (field.equals(FIELD_ISNOTNULL))
        {
            throw new UnsupportedOperationException("Wildcard Queries are not support for " + FIELD_ISNOTNULL);
        } else if (field.equals(FIELD_EXISTS))
        {
            throw new UnsupportedOperationException("Wildcard Queries are not support for " + FIELD_EXISTS);
        } else if (QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(), namespacePrefixResolver,
                dictionaryService, field) != null)
        {
            Collection<QName> contentAttributes = dictionaryService
                    .getAllProperties(QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(),
                            namespacePrefixResolver, dictionaryService, field).getName());
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            for (QName qname : contentAttributes)
            {
                // The super implementation will create phrase queries etc if
                // required
                Query part = getWildcardQuery(PROPERTY_FIELD_PREFIX + qname.toString(), termStr, analysisMode);
                if (part != null)
                {
                    query.add(part, Occur.SHOULD);
                } else
                {
                    query.add(createNoMatchQuery(), Occur.SHOULD);
                }
            }
            return query.build();
        } else if (field.equals(FIELD_FTSSTATUS))
        {
            throw new UnsupportedOperationException("Wildcard Queries are not support for " + FIELD_FTSSTATUS);
        } else if (field.equals(FIELD_TAG))
        {
            return super.getWildcardQuery(field, termStr);
        } else if (field.equals(FIELD_SITE))
        {
            return super.getWildcardQuery(field, termStr);
        } else if (field.equals(FIELD_PNAME))
        {
            return super.getWildcardQuery(field, termStr);
        } else if (field.equals(FIELD_NPATH))
        {
            return super.getWildcardQuery(field, termStr);
        } else
        {
            return super.getWildcardQuery(field, termStr);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Query getFuzzyQuery(String field, String termStr, float minSimilarity) throws ParseException
    {
        if (field.equals(FIELD_PATH))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_PATH);
        } else if (field.equals(FIELD_PATHWITHREPEATS))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_PATHWITHREPEATS);
        } else if (field.equals(FIELD_TEXT))
        {
            Set<String> text = searchParameters.getTextAttributes();
            if ((text == null) || (text.size() == 0))
            {
                Query query = getFuzzyQuery(PROPERTY_FIELD_PREFIX + ContentModel.PROP_CONTENT.toString(), termStr,
                        minSimilarity);
                if (query == null)
                {
                    return createNoMatchQuery();
                }

                return query;
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : text)
                {
                    Query part = getFuzzyQuery(fieldName, termStr, minSimilarity);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            }
        } else if (field.equals(FIELD_ID) || field.equals(FIELD_DBID) || field.equals(FIELD_ISROOT)
                || field.equals(FIELD_ISCONTAINER) || field.equals(FIELD_ISNODE) || field.equals(FIELD_TX)
                || field.equals(FIELD_PARENT) || field.equals(FIELD_PRIMARYPARENT) || field.equals(FIELD_QNAME)
                || field.equals(FIELD_PRIMARYASSOCTYPEQNAME) || field.equals(FIELD_ASSOCTYPEQNAME))
        {
            boolean lowercaseExpandedTerms = getLowercaseExpandedTerms();
            try
            {
                setLowercaseExpandedTerms(false);
                return super.getFuzzyQuery(field, termStr, minSimilarity);
            } finally
            {
                setLowercaseExpandedTerms(lowercaseExpandedTerms);
            }
        } else if (field.equals(FIELD_CLASS))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_CLASS);
        } else if (field.equals(FIELD_TYPE))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_TYPE);
        } else if (field.equals(FIELD_EXACTTYPE))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_EXACTTYPE);
        } else if (field.equals(FIELD_ASPECT))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_ASPECT);
        } else if (field.equals(FIELD_EXACTASPECT))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_EXACTASPECT);
        } else if (isPropertyField(field))
        {
            return attributeQueryBuilder(field, termStr, new FuzzyQuery(minSimilarity), AnalysisMode.FUZZY,
                    LuceneFunction.FIELD);
        } else if (field.equals(FIELD_ALL))
        {
            Set<String> all = searchParameters.getAllAttributes();
            if ((all == null) || (all.size() == 0))
            {
                Collection<QName> contentAttributes = dictionaryService.getAllProperties(null);
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (QName qname : contentAttributes)
                {
                    // The super implementation will create phrase queries etc
                    // if required
                    Query part = getFuzzyQuery(PROPERTY_FIELD_PREFIX + qname.toString(), termStr, minSimilarity);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            } else
            {
                BooleanQuery.Builder query = new BooleanQuery.Builder();
                for (String fieldName : all)
                {
                    Query part = getFuzzyQuery(fieldName, termStr, minSimilarity);
                    if (part != null)
                    {
                        query.add(part, Occur.SHOULD);
                    } else
                    {
                        query.add(createNoMatchQuery(), Occur.SHOULD);
                    }
                }
                return query.build();
            }
        } else if (field.equals(FIELD_ISUNSET))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_ISUNSET);
        } else if (field.equals(FIELD_ISNULL))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_ISNULL);
        } else if (field.equals(FIELD_ISNOTNULL))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_ISNOTNULL);
        } else if (field.equals(FIELD_EXISTS))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_EXISTS);
        } else if (QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(), namespacePrefixResolver,
                dictionaryService, field) != null)
        {
            Collection<QName> contentAttributes = dictionaryService
                    .getAllProperties(QueryParserUtils.matchDataTypeDefinition(searchParameters.getNamespace(),
                            namespacePrefixResolver, dictionaryService, field).getName());
            BooleanQuery.Builder query = new BooleanQuery.Builder();
            for (QName qname : contentAttributes)
            {
                // The super implementation will create phrase queries etc if
                // required
                Query part = getFuzzyQuery(PROPERTY_FIELD_PREFIX + qname.toString(), termStr, minSimilarity);
                if (part != null)
                {
                    query.add(part, Occur.SHOULD);
                } else
                {
                    query.add(createNoMatchQuery(), Occur.SHOULD);
                }
            }
            return query.build();
        } else if (field.equals(FIELD_FTSSTATUS))
        {
            throw new UnsupportedOperationException("Fuzzy Queries are not support for " + FIELD_FTSSTATUS);
        } else if (field.equals(FIELD_TAG))
        {
            return super.getFuzzyQuery(field, termStr, minSimilarity);
        } else if (field.equals(FIELD_SITE))
        {
            return super.getFuzzyQuery(field, termStr, minSimilarity);
        } else if (field.equals(FIELD_PNAME))
        {
            return super.getFuzzyQuery(field, termStr, minSimilarity);
        } else if (field.equals(FIELD_NPATH))
        {
            return super.getFuzzyQuery(field, termStr, minSimilarity);
        } else
        {
            return super.getFuzzyQuery(field, termStr, minSimilarity);
        }
    }

    /**
     * @param dictionaryService
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * @param field
     * @param queryText
     * @param analysisMode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     */
    public Query getSuperFieldQuery(String field, String queryText, AnalysisMode analysisMode,
            LuceneFunction luceneFunction) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(field, queryText, analysisMode, luceneFunction);
    }

    /**
     * @param field
     * @param termStr
     * @param minSimilarity
     * @return the query
     * @throws ParseException
     */
    public Query getSuperFuzzyQuery(String field, String termStr, float minSimilarity) throws ParseException
    {
        return super.getFuzzyQuery(field, termStr, minSimilarity);
    }

    /**
     * @param field
     * @param termStr
     * @return the query
     * @throws ParseException
     */
    public Query getSuperPrefixQuery(String field, String termStr) throws ParseException
    {
        return super.getPrefixQuery(field, termStr);
    }

    /**
     * @param field
     * @param termStr
     * @return the query
     * @throws ParseException
     */
    public Query getSuperWildcardQuery(String field, String termStr) throws ParseException
    {
        return super.getWildcardQuery(field, termStr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.lucene.queryParser.QueryParser#newWildcardQuery(org.apache.
     * lucene.index.Term)
     */
    @Override
    protected Query newWildcardQuery(Term t)
    {
        if (t.text().equals("*"))
        {
            BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
            bQuery.add(createTermQuery(FIELD_FIELDS, t.field()), Occur.SHOULD);
            bQuery.add(createTermQuery(FIELD_PROPERTIES, t.field()), Occur.SHOULD);
            return bQuery.build();
        } else if (t.text().contains("\\"))
        {
            String regexp = SearchLanguageConversion.convert(SearchLanguageConversion.DEF_LUCENE,
                    SearchLanguageConversion.DEF_REGEX, t.text());
            return new RegexpQuery(new Term(t.field(), regexp));
        } else
        {
            org.apache.lucene.search.WildcardQuery query = new org.apache.lucene.search.WildcardQuery(t);
            query.setRewriteMethod(new MultiTermQuery.TopTermsScoringBooleanQueryRewrite(topTermSpanRewriteLimit));
            return query;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.queryParser.QueryParser#newPrefixQuery(org.apache.
     * lucene.index.Term)
     */
    @Override
    protected Query newPrefixQuery(Term prefix)
    {
        if (prefix.text().contains("\\"))
        {
            String regexp = SearchLanguageConversion.convert(SearchLanguageConversion.DEF_LUCENE,
                    SearchLanguageConversion.DEF_REGEX, prefix.text());
            return new RegexpQuery(new Term(prefix.field(), regexp));
        } else
        {
            return super.newPrefixQuery(prefix);
        }

    }

    public interface SubQuery
    {
        /**
         * @param field
         * @param queryText
         * @param analysisMode
         * @param luceneFunction
         * @return the query
         * @throws ParseException
         */
        Query getQuery(String field, String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
                throws ParseException;
    }

    class FieldQuery implements SubQuery
    {
        public Query getQuery(String field, String queryText, AnalysisMode analysisMode, LuceneFunction luceneFunction)
                throws ParseException
        {
            return getSuperFieldQuery(field, queryText, analysisMode, luceneFunction);
        }
    }

    class FuzzyQuery implements SubQuery
    {
        float minSimilarity;

        FuzzyQuery(float minSimilarity)
        {
            this.minSimilarity = minSimilarity;
        }

        public Query getQuery(String field, String termStr, AnalysisMode analysisMode, LuceneFunction luceneFunction)
                throws ParseException
        {
            return getSuperFuzzyQuery(field, translateLocale(termStr), minSimilarity);
        }
    }

    private String translateLocale(String localised)
    {
        if (localised.startsWith("\u0000"))
        {
            if (localised.startsWith("\u0000\u0000"))
            {
                if (localised.length() < 3)
                {
                    return "";
                } else
                {
                    return localised.substring(2);
                }
            } else
            {
                int end = localised.indexOf('\u0000', 1);
                if (end == -1)
                {
                    return localised;
                } else
                {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("{");
                    buffer.append(localised.substring(1, end));
                    buffer.append("}");
                    buffer.append(localised.substring(end + 1));
                    return buffer.toString();
                }
            }
        } else
        {
            return localised;
        }
    }

    class PrefixQuery implements SubQuery
    {
        public Query getQuery(String field, String termStr, AnalysisMode analysisMode, LuceneFunction luceneFunction)
                throws ParseException
        {
            StringBuilder builder = new StringBuilder(termStr.length() + 1);
            builder.append(termStr);
            builder.append("*");
            return getSuperFieldQuery(field, builder.toString(), analysisMode, luceneFunction);
            // return getSuperPrefixQuery(field, termStr);
        }
    }

    class WildcardQuery implements SubQuery
    {
        public Query getQuery(String field, String termStr, AnalysisMode analysisMode, LuceneFunction luceneFunction)
                throws ParseException
        {
            return getSuperFieldQuery(field, termStr, analysisMode, luceneFunction);
            // return getSuperWildcardQuery(field, termStr);
        }
    }

    private Query spanQueryBuilder(String field, String first, String last, int slop, boolean inOrder)
            throws ParseException
    {
        String propertyFieldName = field.substring(1);
        String expandedFieldName = null;

        PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, propertyFieldName);
        IndexTokenisationMode tokenisationMode = IndexTokenisationMode.TRUE;
        if (propertyDef != null)
        {
            tokenisationMode = propertyDef.getIndexTokenisationMode();
            if (tokenisationMode == null)
            {
                tokenisationMode = IndexTokenisationMode.TRUE;
            }
        } else
        {
            expandedFieldName = expandAttributeFieldName(field);
        }

        if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT)))
        {
            // Build a sub query for each locale and or the results together -
            // the analysis will take care of
            // cross language matching for each entry
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, false));
            }
            for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
            {
                addMLTextSpanQuery(field, propertyDef, first, last, slop, inOrder, expandedFieldName, propertyDef,
                        tokenisationMode, booleanQuery, locale);
            }
            return booleanQuery.build();
        }
        // Content
        else if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
        {

            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales
                        .addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, addContentCrossLocaleWildcards()));
            }

            return addContentSpanQuery(field, propertyDef, first, last, slop, inOrder, expandedFieldName,
                    expandedLocales);

        } else if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.TEXT)))
        {
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, false));
            }
            for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
            {

                addTextSpanQuery(field, propertyDef, first, last, slop, inOrder, expandedFieldName, tokenisationMode,
                        booleanQuery, locale);

            }
            return booleanQuery.build();
        } else
        {
            throw new UnsupportedOperationException(
                    "Span queries are only supported for d:text, d:mltext and d:content data types");
        }
    }

    private Query attributeQueryBuilder(String field, String queryText, SubQuery subQueryBuilder,
            AnalysisMode analysisMode, LuceneFunction luceneFunction) throws ParseException
    {
        // TODO: Fix duplicate token generation for mltext, content and text.
        // -locale expansion here and in tokeisation -> duplicates

        // Get type info etc

        // TODO: additional suffixes

        Pair<String, String> fieldNameAndEnding = QueryParserUtils.extractFieldNameAndEnding(field);

        String expandedFieldName = null;
        QName propertyQName;
        PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition(searchParameters.getNamespace(),
                namespacePrefixResolver, dictionaryService, fieldNameAndEnding.getFirst());
        IndexTokenisationMode tokenisationMode = IndexTokenisationMode.TRUE;
        if (propertyDef != null)
        {
            tokenisationMode = propertyDef.getIndexTokenisationMode();
            if (tokenisationMode == null)
            {
                tokenisationMode = IndexTokenisationMode.TRUE;
            }
            propertyQName = propertyDef.getName();
        } else
        {
            expandedFieldName = expandAttributeFieldName(field);
            propertyQName = QName.createQName(fieldNameAndEnding.getFirst());
        }

        if (isAllStar(queryText))
        {
            return createTermQuery(FIELD_PROPERTIES, propertyQName.toString());
        }

        if (luceneFunction != LuceneFunction.FIELD)
        {
            if ((tokenisationMode == IndexTokenisationMode.FALSE) || (tokenisationMode == IndexTokenisationMode.BOTH))
            {
                if (luceneFunction == LuceneFunction.LOWER)
                {
                    if (false == queryText.toLowerCase().equals(queryText))
                    {
                        return createNoMatchQuery();
                    }
                }
                if (luceneFunction == LuceneFunction.UPPER)
                {
                    if (false == queryText.toUpperCase().equals(queryText))
                    {
                        return createNoMatchQuery();
                    }
                }

                return functionQueryBuilder(expandedFieldName, fieldNameAndEnding.getSecond(), propertyQName,
                        propertyDef, tokenisationMode, queryText, luceneFunction);
            }
        }

        // Mime type
        if (fieldNameAndEnding.getSecond().equals(FIELD_MIMETYPE_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                return subQueryBuilder.getQuery(AlfrescoSolrDataModel.getInstance()
                        .getQueryableFields(propertyQName, ContentFieldType.MIMETYPE, FieldUse.ID).getFields().get(0)
                        .getField(), queryText, analysisMode, luceneFunction);
            }

        } else if (fieldNameAndEnding.getSecond().equals(FIELD_SIZE_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                return subQueryBuilder.getQuery(AlfrescoSolrDataModel.getInstance()
                        .getQueryableFields(propertyQName, ContentFieldType.SIZE, FieldUse.ID).getFields().get(0)
                        .getField(), queryText, analysisMode, luceneFunction);

            }

        } else if (fieldNameAndEnding.getSecond().equals(FIELD_LOCALE_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                return subQueryBuilder.getQuery(AlfrescoSolrDataModel.getInstance()
                        .getQueryableFields(propertyQName, ContentFieldType.LOCALE, FieldUse.ID).getFields().get(0)
                        .getField(), queryText, analysisMode, luceneFunction);

            }

        } else if (fieldNameAndEnding.getSecond().equals(FIELD_ENCODING_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                return subQueryBuilder.getQuery(AlfrescoSolrDataModel.getInstance()
                        .getQueryableFields(propertyQName, ContentFieldType.ENCODING, FieldUse.ID).getFields().get(0)
                        .getField(), queryText, analysisMode, luceneFunction);

            }

        } else if (fieldNameAndEnding.getSecond().equals(FIELD_TRANSFORMATION_STATUS_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                return subQueryBuilder
                        .getQuery(
                                AlfrescoSolrDataModel.getInstance()
                                        .getQueryableFields(propertyQName, ContentFieldType.TRANSFORMATION_STATUS,
                                                FieldUse.ID)
                                        .getFields().get(0).getField(),
                                queryText, analysisMode, luceneFunction);

            }

        } else if (fieldNameAndEnding.getSecond().equals(FIELD_TRANSFORMATION_TIME_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                return subQueryBuilder
                        .getQuery(
                                AlfrescoSolrDataModel.getInstance()
                                        .getQueryableFields(propertyQName, ContentFieldType.TRANSFORMATION_TIME,
                                                FieldUse.ID)
                                        .getFields().get(0).getField(),
                                queryText, analysisMode, luceneFunction);

            }

        } else if (fieldNameAndEnding.getSecond().equals(FIELD_TRANSFORMATION_EXCEPTION_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                return subQueryBuilder.getQuery(
                        AlfrescoSolrDataModel.getInstance()
                                .getQueryableFields(propertyQName, ContentFieldType.TRANSFORMATION_EXCEPTION,
                                        FieldUse.ID)
                                .getFields().get(0).getField(),
                        queryText, analysisMode, luceneFunction);

            }

        }

        // Already in expanded form

        // ML

        if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT)))
        {
            // Build a sub query for each locale and or the results together -
            // the analysis will take care of
            // cross language matching for each entry
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, false));
            }
            for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
            {
                addMLTextAttributeQuery(field, propertyDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                        expandedFieldName, propertyDef, tokenisationMode, booleanQuery, locale);
            }
            return getNonEmptyBooleanQuery(booleanQuery.build());
        }
        // Content
        else if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
        {
            // Identifier request are ignored for content

            // Build a sub query for each locale and or the results together -
            // - add an explicit condition for the locale

            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales
                        .addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, addContentCrossLocaleWildcards()));
            }

            return addContentAttributeQuery(propertyDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                    expandedFieldName, expandedLocales);

        } else if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.TEXT)))
        {
            // if (propertyQName.equals(ContentModel.PROP_USER_USERNAME) ||
            // propertyQName.equals(ContentModel.PROP_USERNAME) ||
            // propertyQName.equals(ContentModel.PROP_AUTHORITY_NAME))
            // {
            // // nasty work around for solr support for user and group look up
            // as we can not support lowercased identifiers in the model
            // if(isLucene())
            // {
            // return subQueryBuilder.getQuery(expandedFieldName, queryText,
            // analysisMode, luceneFunction);
            // }
            // }

            boolean withWildCards = propertyQName.equals(ContentModel.PROP_USER_USERNAME)
                    || propertyQName.equals(ContentModel.PROP_USERNAME)
                    || propertyQName.equals(ContentModel.PROP_AUTHORITY_NAME);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, withWildCards));
            }
            for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
            {
                Locale fixedLocale = locale;
                if (fixedLocale.getLanguage().equals("*"))
                {
                    fixedLocale = new Locale("??");
                }
                addTextAttributeQuery(field, propertyDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                        expandedFieldName, tokenisationMode, booleanQuery, fixedLocale);

            }
            return getNonEmptyBooleanQuery(booleanQuery.build());
        } else
        {
            // Date does not support like
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.DATETIME)))
            {
                if (analysisMode == AnalysisMode.LIKE)
                {
                    throw new UnsupportedOperationException("Wild cards are not supported for the datetime type");
                }
            }

            // expand date for loose date parsing
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.DATETIME)
                    || propertyDef.getDataType().getName().equals(DataTypeDefinition.DATE)))
            {
                Pair<Date, Integer> dateAndResolution = parseDateString(queryText);

                BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
                IndexedField indexedField = AlfrescoSolrDataModel.getInstance()
                        .getQueryableFields(propertyDef.getName(), null, FieldUse.FTS);
                for (FieldInstance instance : indexedField.getFields())
                {
                    if (dateAndResolution != null)
                    {
                        Query query = newRangeQuery(instance.getField(), getDateStart(dateAndResolution),
                                getDateEnd(dateAndResolution), true, true);
                        if (query != null)
                        {
                            bQuery.add(query, Occur.SHOULD);
                        }
                    } else
                    {
                        Query query = subQueryBuilder.getQuery(instance.getField(), queryText, AnalysisMode.DEFAULT,
                                luceneFunction);
                        if (query != null)
                        {
                            bQuery.add(query, Occur.SHOULD);
                        }
                    }
                }
                if (bQuery.build().clauses().size() > 0)
                {
                    return bQuery.build();
                } else
                {
                    return createNoMatchQuery();
                }
            }

            if ((propertyDef != null) && (tenantService.isTenantUser())
                    && (propertyDef.getDataType().getName().equals(DataTypeDefinition.NODE_REF))
                    && (queryText.contains(StoreRef.URI_FILLER)))
            {
                // ALF-6202
                queryText = tenantService.getName(new NodeRef(queryText)).toString();
            }

            // Sort and id is only special for MLText, text, and content
            // Dates are not special in this case
            if (propertyDef != null)
            {
                BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
                IndexedField indexedField = AlfrescoSolrDataModel.getInstance()
                        .getQueryableFields(propertyDef.getName(), null, FieldUse.FTS);
                for (FieldInstance instance : indexedField.getFields())
                {
                    Query query = subQueryBuilder.getQuery(instance.getField(), queryText, AnalysisMode.DEFAULT,
                            luceneFunction);
                    if (query != null)
                    {
                        bQuery.add(query, Occur.SHOULD);
                    }
                }
                if (bQuery.build().clauses().size() > 0)
                {
                    return bQuery.build();
                } else
                {
                    return createNoMatchQuery();
                }
            } else
            {
                Query query = subQueryBuilder.getQuery(expandedFieldName, queryText, AnalysisMode.DEFAULT,
                        luceneFunction);
                if (query != null)
                {
                    return query;
                } else
                {
                    return createNoMatchQuery();
                }
            }
        }
    }

    /**
     * @param queryText
     * @return
     */
    private boolean isAllStar(String queryText)
    {
        if ((queryText == null) || (queryText.length() == 0))
        {
            return false;
        }
        for (char c : queryText.toCharArray())
        {
            if (c != '*')
            {
                return false;
            }
        }
        return true;

    }

    /**
     * @param dateAndResolution
     * @return
     */
    private String getDateEnd(Pair<Date, Integer> dateAndResolution)
    {
        Calendar cal = Calendar.getInstance(I18NUtil.getLocale());
        cal.setTime(dateAndResolution.getFirst());
        switch (dateAndResolution.getSecond())
        {
        case Calendar.YEAR:
            cal.set(Calendar.MONTH, cal.getActualMaximum(Calendar.MONTH));
        case Calendar.MONTH:
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        case Calendar.DAY_OF_MONTH:
            cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
        case Calendar.HOUR_OF_DAY:
            cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
        case Calendar.MINUTE:
            cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
        case Calendar.SECOND:
            cal.set(Calendar.MILLISECOND, cal.getActualMaximum(Calendar.MILLISECOND));
        case Calendar.MILLISECOND:
        default:
        }
        SimpleDateFormat formatter = CachingDateFormat.getSolrDatetimeFormat();
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(cal.getTime());
    }

    /**
     * @param dateAndResolution
     * @return
     */
    private String getDateStart(Pair<Date, Integer> dateAndResolution)
    {
        Calendar cal = Calendar.getInstance(I18NUtil.getLocale());
        cal.setTime(dateAndResolution.getFirst());
        switch (dateAndResolution.getSecond())
        {
        case Calendar.YEAR:
            cal.set(Calendar.MONTH, cal.getActualMinimum(Calendar.MONTH));
        case Calendar.MONTH:
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        case Calendar.DAY_OF_MONTH:
            cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        case Calendar.HOUR_OF_DAY:
            cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        case Calendar.MINUTE:
            cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        case Calendar.SECOND:
            cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        case Calendar.MILLISECOND:
        default:
        }
        SimpleDateFormat formatter = CachingDateFormat.getSolrDatetimeFormat();
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(cal.getTime());
    }

    private Pair<Date, Integer> parseDateString(String dateString)
    {
        try
        {
            Pair<Date, Integer> result = CachingDateFormat.lenientParse(dateString, Calendar.YEAR);
            return result;
        } catch (java.text.ParseException e)
        {
            SimpleDateFormat oldDf = CachingDateFormat.getDateFormat();
            try
            {
                Date date = oldDf.parse(dateString);
                return new Pair<Date, Integer>(date, Calendar.SECOND);
            } catch (java.text.ParseException ee)
            {
                if (dateString.equalsIgnoreCase("min"))
                {
                    Calendar cal = Calendar.getInstance(I18NUtil.getLocale());
                    cal.set(Calendar.YEAR, cal.getMinimum(Calendar.YEAR));
                    cal.set(Calendar.DAY_OF_YEAR, cal.getMinimum(Calendar.DAY_OF_YEAR));
                    cal.set(Calendar.HOUR_OF_DAY, cal.getMinimum(Calendar.HOUR_OF_DAY));
                    cal.set(Calendar.MINUTE, cal.getMinimum(Calendar.MINUTE));
                    cal.set(Calendar.SECOND, cal.getMinimum(Calendar.SECOND));
                    cal.set(Calendar.MILLISECOND, cal.getMinimum(Calendar.MILLISECOND));
                    return new Pair<Date, Integer>(cal.getTime(), Calendar.MILLISECOND);
                } else if (dateString.equalsIgnoreCase("now"))
                {
                    return new Pair<Date, Integer>(new Date(), Calendar.MILLISECOND);
                } else if (dateString.equalsIgnoreCase("today"))
                {
                    Calendar cal = Calendar.getInstance(I18NUtil.getLocale());
                    cal.setTime(new Date());
                    cal.set(Calendar.HOUR_OF_DAY, cal.getMinimum(Calendar.HOUR_OF_DAY));
                    cal.set(Calendar.MINUTE, cal.getMinimum(Calendar.MINUTE));
                    cal.set(Calendar.SECOND, cal.getMinimum(Calendar.SECOND));
                    cal.set(Calendar.MILLISECOND, cal.getMinimum(Calendar.MILLISECOND));
                    return new Pair<Date, Integer>(cal.getTime(), Calendar.DAY_OF_MONTH);
                } else if (dateString.equalsIgnoreCase("max"))
                {
                    Calendar cal = Calendar.getInstance(I18NUtil.getLocale());
                    cal.set(Calendar.YEAR, cal.getMaximum(Calendar.YEAR));
                    cal.set(Calendar.DAY_OF_YEAR, cal.getMaximum(Calendar.DAY_OF_YEAR));
                    cal.set(Calendar.HOUR_OF_DAY, cal.getMaximum(Calendar.HOUR_OF_DAY));
                    cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
                    cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
                    cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));
                    return new Pair<Date, Integer>(cal.getTime(), Calendar.MILLISECOND);
                } else
                {
                    return null; // delegate to SOLR date parsing
                }
            }
        }
    }

    protected Query functionQueryBuilder(String expandedFieldName, String ending, QName propertyQName,
            PropertyDefinition propertyDef, IndexTokenisationMode tokenisationMode, String queryText,
            LuceneFunction luceneFunction) throws ParseException
    {

        // Mime type
        if (ending.equals(FIELD_MIMETYPE_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        } else if (ending.equals(FIELD_SIZE_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        } else if (ending.equals(FIELD_LOCALE_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        } else if (ending.equals(FIELD_ENCODING_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        } else if (ending.equals(FIELD_CONTENT_DOC_ID_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        } else if (ending.equals(FIELD_TRANSFORMATION_EXCEPTION_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        } else if (ending.equals(FIELD_TRANSFORMATION_TIME_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        } else if (ending.equals(FIELD_TRANSFORMATION_STATUS_SUFFIX))
        {
            if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
            {
                throw new UnsupportedOperationException("Lucene Function");
            }

        }

        // Already in expanded form

        // ML

        if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT)))
        {
            // Build a sub query for each locale and or the results together -
            // the analysis will take care of
            // cross language matching for each entry
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, false));
            }
            for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
            {

                addLocaleSpecificUntokenisedMLOrTextFunction(expandedFieldName, propertyDef, queryText, luceneFunction,
                        booleanQuery, locale, tokenisationMode);

            }
            return booleanQuery.build();
        }
        // Content
        else if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT)))
        {
            throw new UnsupportedOperationException("Lucene functions not supported for content");
        } else if ((propertyDef != null) && (propertyDef.getDataType().getName().equals(DataTypeDefinition.TEXT)))
        {
            if (propertyQName.equals(ContentModel.PROP_USER_USERNAME)
                    || propertyQName.equals(ContentModel.PROP_USERNAME)
                    || propertyQName.equals(ContentModel.PROP_AUTHORITY_NAME))
            {
                throw new UnsupportedOperationException("Functions are not supported agaisnt special text fields");
            }

            boolean withWildCards = propertyQName.equals(ContentModel.PROP_USER_USERNAME)
                    || propertyQName.equals(ContentModel.PROP_USERNAME)
                    || propertyQName.equals(ContentModel.PROP_AUTHORITY_NAME);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            List<Locale> locales = searchParameters.getLocales();
            List<Locale> expandedLocales = new ArrayList<Locale>();
            for (Locale locale : (((locales == null) || (locales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : locales))
            {
                expandedLocales.addAll(MLAnalysisMode.getLocales(mlAnalysisMode, locale, withWildCards));
            }
            for (Locale locale : (((expandedLocales == null) || (expandedLocales.size() == 0))
                    ? Collections.singletonList(I18NUtil.getLocale()) : expandedLocales))
            {
                addLocaleSpecificUntokenisedMLOrTextFunction(expandedFieldName, propertyDef, queryText, luceneFunction,
                        booleanQuery, locale, tokenisationMode);

            }
            return booleanQuery.build();
        } else
        {
            throw new UnsupportedOperationException("Lucene Function");
        }
    }

    protected TermQuery createNoMatchQuery()
    {
        return new TermQuery(new Term("NO_TOKENS", "__"));
    }

    /**
     * Returns null if all clause words were filtered away by the analyzer
     * 
     * @param booleanQuery
     *            - initial BooleanQuery
     * @return BooleanQuery or <code>null</code> if booleanQuery has no clauses
     */
    protected BooleanQuery getNonEmptyBooleanQuery(BooleanQuery booleanQuery)
    {
        if (booleanQuery.clauses().size() > 0)
        {
            return booleanQuery;
        } else
        {
            return null;
        }
    }

    protected Query createSolr4IdQuery(String queryText)
    {
        return createTermQuery(FIELD_SOLR4_ID, queryText);
    }

    // Previous SOLR

    protected Query createIdQuery(String queryText)
    {
        if (NodeRef.isNodeRef(queryText))
        {
            return createNodeRefQuery(FIELD_LID, queryText);
        } else
        {
            return createNodeRefQuery(FIELD_ID, queryText);
        }
    }

    protected Query createPathQuery(String queryText, boolean withRepeats) throws SAXPathException
    {
        XPathReader reader = new XPathReader();
        SolrXPathHandler handler = new SolrXPathHandler();
        handler.setNamespacePrefixResolver(namespacePrefixResolver);
        handler.setDictionaryService(dictionaryService);
        reader.setXPathHandler(handler);
        reader.parse(queryText);
        SolrPathQuery pathQuery = handler.getQuery();
        pathQuery.setRepeats(withRepeats);
        return new SolrCachingPathQuery(pathQuery);
    }

    protected Query createQNameQuery(String queryText) throws SAXPathException
    {
        XPathReader reader = new XPathReader();
        SolrXPathHandler handler = new SolrXPathHandler();
        handler.setNamespacePrefixResolver(namespacePrefixResolver);
        handler.setDictionaryService(dictionaryService);
        reader.setXPathHandler(handler);
        reader.parse("//" + queryText);
        SolrPathQuery pathQuery = handler.getQuery();
        return new SolrCachingPathQuery(pathQuery);
    }

    protected Query createPrimaryAssocQNameQuery(String queryText) throws SAXPathException
    {
        XPathReader reader = new XPathReader();
        SolrXPathHandler handler = new SolrXPathHandler();
        handler.setNamespacePrefixResolver(namespacePrefixResolver);
        handler.setDictionaryService(dictionaryService);
        reader.setXPathHandler(handler);
        reader.parse("//" + queryText);
        SolrPathQuery pathQuery = handler.getQuery();
        pathQuery.setPathField(FIELD_PRIMARYASSOCQNAME);
        return new SolrCachingPathQuery(pathQuery);
    }

    protected Query createPrimaryAssocTypeQNameQuery(String queryText) throws SAXPathException
    {
        XPathReader reader = new XPathReader();
        SolrXPathHandler handler = new SolrXPathHandler();
        handler.setNamespacePrefixResolver(namespacePrefixResolver);
        handler.setDictionaryService(dictionaryService);
        reader.setXPathHandler(handler);
        reader.parse("//" + queryText);
        SolrPathQuery pathQuery = handler.getQuery();
        pathQuery.setPathField(FIELD_PRIMARYASSOCTYPEQNAME);
        return new SolrCachingPathQuery(pathQuery);
    }

    protected Query createAssocTypeQNameQuery(String queryText) throws SAXPathException
    {
        XPathReader reader = new XPathReader();
        SolrXPathHandler handler = new SolrXPathHandler();
        handler.setNamespacePrefixResolver(namespacePrefixResolver);
        handler.setDictionaryService(dictionaryService);
        reader.setXPathHandler(handler);
        reader.parse("//" + queryText);
        SolrPathQuery pathQuery = handler.getQuery();
        pathQuery.setPathField(FIELD_ASSOCTYPEQNAME);
        return new SolrCachingPathQuery(pathQuery);
    }

    /**
     * @param queryText
     * @return
     */
    protected Query createAclIdQuery(String queryText) throws ParseException
    {
        return getFieldQueryImplWithIOExceptionWrapped(FIELD_ACLID, queryText, AnalysisMode.DEFAULT,
                LuceneFunction.FIELD);
    }

    /**
     * @param queryText
     * @return
     */
    protected Query createOwnerQuery(String queryText) throws ParseException
    {
        return new SolrOwnerQuery(queryText);
    }

    /**
     * @param queryText
     * @return
     */
    protected Query createReaderQuery(String queryText) throws ParseException
    {
        return new SolrReaderQuery(queryText);
    }

    /**
     * @param queryText
     * @return
     */
    protected Query createAuthorityQuery(String queryText) throws ParseException
    {
        //return new SolrAuthorityQuery(queryText);
        return new SolrAuthoritySetQuery(","+queryText);
    }

    // TODO: correct field names
    protected Query addContentAttributeQuery(PropertyDefinition pDef, String queryText, SubQuery subQueryBuilder,
            AnalysisMode analysisMode, LuceneFunction luceneFunction, String expandedFieldName,
            List<Locale> expandedLocales) throws ParseException
    {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        for (Locale locale : expandedLocales)
        {
            if (locale.toString().length() == 0)
            {
                IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(), null,
                        FieldUse.FTS);
                for (FieldInstance field : indexedField.getFields())
                {
                    if (!field.isLocalised())
                    {
                        Query subQuery = subQueryBuilder.getQuery(field.getField(), queryText, analysisMode,
                                luceneFunction);
                        if (subQuery != null)
                        {
                            booleanQuery.add(subQuery, Occur.SHOULD);
                        }
                    }
                }
            } else
            {
                StringBuilder builder = new StringBuilder(queryText.length() + 10);
                builder.append("\u0000").append(locale.toString()).append("\u0000").append(queryText);
                IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(), null,
                        FieldUse.FTS);
                for (FieldInstance field : indexedField.getFields())
                {
                    if (field.isLocalised())
                    {
                        Query subQuery = subQueryBuilder.getQuery(field.getField(), builder.toString(), analysisMode,
                                luceneFunction);
                        if (subQuery != null)
                        {
                            booleanQuery.add(subQuery, Occur.SHOULD);
                        }
                    }
                }
            }
        }
        return getNonEmptyBooleanQuery(booleanQuery.build());
    }

    protected void addLocaleSpecificUntokenisedMLOrTextFunction(String expandedFieldName, PropertyDefinition pDef,
            String queryText, LuceneFunction luceneFunction, Builder booleanQuery, Locale locale,
            IndexTokenisationMode tokenisationMode)
    {
        // Query subQuery = new CaseInsensitiveFieldQuery(new
        // Term(getFieldName(expandedFieldName, locale, tokenisationMode,
        // IndexTokenisationMode.FALSE), getFixedFunctionQueryText(
        // queryText, locale, tokenisationMode, IndexTokenisationMode.FALSE)));
        // booleanQuery.add(subQuery, Occur.SHOULD);
        //
        // if (booleanQuery.getClauses().length == 0)
        // {
        // booleanQuery.add(createNoMatchQuery(), Occur.SHOULD);
        // }
        throw new UnsupportedOperationException();
    }

    private FieldInstance getFieldInstance(String baseFieldName, PropertyDefinition pDef, Locale locale,
            IndexTokenisationMode preferredIndexTokenisationMode)
    {
        if (pDef != null)
        {

            switch (preferredIndexTokenisationMode)
            {
            case BOTH:
                throw new IllegalStateException("Preferred mode can not be BOTH");
            case FALSE:
                if (locale.toString().length() == 0)
                {
                    IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(),
                            null, FieldUse.ID);
                    for (FieldInstance field : indexedField.getFields())
                    {
                        if (!field.isLocalised())
                        {
                            return field;
                        }
                    }
                } else
                {
                    IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(),
                            null, FieldUse.ID);
                    for (FieldInstance field : indexedField.getFields())
                    {
                        if (field.isLocalised())
                        {
                            return field;
                        }
                    }
                }
                break;
            case TRUE:
                if (locale.toString().length() == 0)
                {
                    IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(),
                            null, FieldUse.FTS);
                    for (FieldInstance field : indexedField.getFields())
                    {
                        if (!field.isLocalised())
                        {
                            return field;
                        }
                    }
                } else
                {
                    IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(),
                            null, FieldUse.FTS);
                    for (FieldInstance field : indexedField.getFields())
                    {
                        if (field.isLocalised())
                        {
                            return field;
                        }
                    }
                }
                break;
            }
            return new FieldInstance("_dummy_", false, false);

        }

        return new FieldInstance(baseFieldName, false, false);

    }

    protected void addLocaleSpecificUntokenisedTextRangeFunction(String expandedFieldName, PropertyDefinition pDef,
            String lower, String upper, boolean includeLower, boolean includeUpper, LuceneFunction luceneFunction,
            Builder booleanQuery, Locale locale, IndexTokenisationMode tokenisationMode) throws ParseException
    {
        // String field = getFieldName(expandedFieldName, locale,
        // tokenisationMode, IndexTokenisationMode.FALSE);
        //
        // StringBuilder builder = new StringBuilder();
        // builder.append("\u0000").append(locale.toString()).append("\u0000").append(lower);
        // String first = getToken(field, builder.toString(),
        // AnalysisMode.IDENTIFIER);
        //
        // builder = new StringBuilder();
        // builder.append("\u0000").append(locale.toString()).append("\u0000").append(upper);
        // String last = getToken(field, builder.toString(),
        // AnalysisMode.IDENTIFIER);
        //
        // Query query = new CaseInsensitiveFieldRangeQuery(field, first, last,
        // includeLower, includeUpper);
        // booleanQuery.add(query, Occur.SHOULD);

        throw new UnsupportedOperationException();

    }

    protected void addMLTextAttributeQuery(String field, PropertyDefinition pDef, String queryText,
            SubQuery subQueryBuilder, AnalysisMode analysisMode, LuceneFunction luceneFunction,
            String expandedFieldName, PropertyDefinition propertyDef, IndexTokenisationMode tokenisationMode,
            Builder booleanQuery, Locale locale) throws ParseException
    {

        addMLTextOrTextAttributeQuery(field, pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                expandedFieldName, tokenisationMode, booleanQuery, locale);
    }

    private void addMLTextOrTextAttributeQuery(String field, PropertyDefinition pDef, String queryText,
            SubQuery subQueryBuilder, AnalysisMode analysisMode, LuceneFunction luceneFunction,
            String expandedFieldName, IndexTokenisationMode tokenisationMode, Builder booleanQuery, Locale locale)
                    throws ParseException
    {

        boolean lowercaseExpandedTerms = getLowercaseExpandedTerms();
        try
        {
            switch (tokenisationMode)
            {
            case BOTH:
                switch (analysisMode)
                {
                default:
                case DEFAULT:
                    addLocaleSpecificMLOrTextAttribute(pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                            booleanQuery, locale, expandedFieldName, tokenisationMode, IndexTokenisationMode.TRUE);

                    if (ContentModel.PROP_NAME.equals(pDef.getName()))
                    {
                        setLowercaseExpandedTerms(false);
                        addLocaleSpecificMLOrTextAttribute(pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                                booleanQuery, locale, expandedFieldName, tokenisationMode, IndexTokenisationMode.FALSE);
                    }

                    break;
                case TOKENISE:
                    addLocaleSpecificMLOrTextAttribute(pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                            booleanQuery, locale, expandedFieldName, tokenisationMode, IndexTokenisationMode.TRUE);
                    break;
                case IDENTIFIER:
                case FUZZY:
                case PREFIX:
                case WILD:
                case LIKE:
                    setLowercaseExpandedTerms(false);
                    addLocaleSpecificMLOrTextAttribute(pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                            booleanQuery, locale, expandedFieldName, tokenisationMode, IndexTokenisationMode.FALSE);

                    break;
                }
                break;
            case FALSE:
                setLowercaseExpandedTerms(false);
                addLocaleSpecificMLOrTextAttribute(pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                        booleanQuery, locale, expandedFieldName, tokenisationMode, IndexTokenisationMode.FALSE);
                break;
            case TRUE:
            default:
                addLocaleSpecificMLOrTextAttribute(pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                        booleanQuery, locale, expandedFieldName, tokenisationMode, IndexTokenisationMode.TRUE);
                break;
            }
        } finally
        {
            setLowercaseExpandedTerms(lowercaseExpandedTerms);
        }

    }

    protected void addTextAttributeQuery(String field, PropertyDefinition pDef, String queryText,
            SubQuery subQueryBuilder, AnalysisMode analysisMode, LuceneFunction luceneFunction,
            String expandedFieldName, IndexTokenisationMode tokenisationMode, Builder booleanQuery, Locale locale)
                    throws ParseException
    {

        addMLTextOrTextAttributeQuery(field, pDef, queryText, subQueryBuilder, analysisMode, luceneFunction,
                expandedFieldName, tokenisationMode, booleanQuery, locale);
    }

    private void addLocaleSpecificMLOrTextAttribute(PropertyDefinition pDef, String queryText, SubQuery subQueryBuilder,
            AnalysisMode analysisMode, LuceneFunction luceneFunction, Builder booleanQuery, Locale locale,
            String textFieldName, IndexTokenisationMode tokenisationMode,
            IndexTokenisationMode preferredTokenisationMode) throws ParseException {

        FieldInstance fieldInstance = getFieldInstance(textFieldName, pDef, locale, preferredTokenisationMode);
        StringBuilder builder = new StringBuilder(queryText.length() + 10);
        if (fieldInstance.isLocalised())
        {
            builder.append("\u0000").append(locale.toString()).append("\u0000");
        }
        builder.append(queryText);
        Query subQuery = subQueryBuilder.getQuery(fieldInstance.getField(), builder.toString(), analysisMode,
                luceneFunction);
        if (subQuery != null)
        {
            booleanQuery.add(subQuery, Occur.SHOULD);
        }
    }

    protected void addTextRange(String field, PropertyDefinition pDef, String part1, String part2, boolean includeLower,
            boolean includeUpper, AnalysisMode analysisMode, String fieldName, PropertyDefinition propertyDef,
            IndexTokenisationMode tokenisationMode, Builder booleanQuery, Locale locale)
                    throws ParseException, IOException
    {
        switch (tokenisationMode)
        {
        case BOTH:
            switch (analysisMode)
            {
            case DEFAULT:
            case TOKENISE:
                addLocaleSpecificTextRange(fieldName, pDef, part1, part2, includeLower, includeUpper, booleanQuery,
                        locale, analysisMode, tokenisationMode, IndexTokenisationMode.TRUE);
                break;
            case IDENTIFIER:
                addLocaleSpecificTextRange(fieldName, pDef, part1, part2, includeLower, includeUpper, booleanQuery,
                        locale, analysisMode, tokenisationMode, IndexTokenisationMode.FALSE);
                break;
            case WILD:
            case LIKE:
            case PREFIX:
            case FUZZY:
            default:
                throw new UnsupportedOperationException();
            }
            break;
        case FALSE:
            addLocaleSpecificTextRange(fieldName, pDef, part1, part2, includeLower, includeUpper, booleanQuery, locale,
                    analysisMode, tokenisationMode, IndexTokenisationMode.FALSE);
            break;
        case TRUE:
            addLocaleSpecificTextRange(fieldName, pDef, part1, part2, includeLower, includeUpper, booleanQuery, locale,
                    analysisMode, tokenisationMode, IndexTokenisationMode.TRUE);
            break;
        default:
        }

    }

    private void addLocaleSpecificTextRange(String expandedFieldName, PropertyDefinition pDef, String part1,
            String part2, boolean includeLower, boolean includeUpper, Builder booleanQuery, Locale locale,
            AnalysisMode analysisMode, IndexTokenisationMode tokenisationMode,
            IndexTokenisationMode preferredTokenisationMode) throws ParseException, IOException
    {
        FieldInstance fieldInstance = getFieldInstance(expandedFieldName, pDef, locale, preferredTokenisationMode);

        String firstString = null;
        if ((part1 != null) && !part1.equals("\u0000"))
        {
            if (fieldInstance.isLocalised())
            {
                firstString = getFirstTokenForRange(getLocalePrefixedText(part1, locale), fieldInstance);
                if (firstString == null)
                {
                    firstString = "{" + locale.getLanguage() + "}";
                }
            } else
            {
                firstString = getFirstTokenForRange(part1, fieldInstance);
            }
        } else
        {
            if (fieldInstance.isLocalised())
            {
                firstString = "{" + locale.getLanguage() + "}";
            } else
            {
                firstString = null;
            }
        }

        String lastString = null;
        if ((part2 != null) && !part2.equals("\uffff"))
        {
            if (fieldInstance.isLocalised())
            {
                lastString = getFirstTokenForRange(getLocalePrefixedText(part2, locale), fieldInstance);
                if (lastString == null)
                {
                    lastString = "{" + locale.getLanguage() + "}\uffff";
                }
            } else
            {
                lastString = getFirstTokenForRange(part2, fieldInstance);
            }
        } else
        {
            if (fieldInstance.isLocalised())
            {
                lastString = "{" + locale.getLanguage() + "}\uffff";
            } else
            {
                lastString = null;
            }
        }

        TermRangeQuery query = new TermRangeQuery(fieldInstance.getField(),
                firstString == null ? null : new BytesRef(firstString),
                lastString == null ? null : new BytesRef(lastString), includeLower, includeUpper);
        booleanQuery.add(query, Occur.SHOULD);
    }

    private String getFirstTokenForRange(String string, FieldInstance field) throws IOException
    {
        PackedTokenAttributeImpl nextToken;
        TokenStream source = null;
        ;

        try
        {
            source = getAnalyzer().tokenStream(field.getField(), new StringReader(string));
            source.reset();
            while (source.incrementToken())
            {
                CharTermAttribute cta = source.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAtt = source.getAttribute(OffsetAttribute.class);
                TypeAttribute typeAtt = null;
                if (source.hasAttribute(TypeAttribute.class))
                {
                    typeAtt = source.getAttribute(TypeAttribute.class);
                }
                PositionIncrementAttribute posIncAtt = null;
                if (source.hasAttribute(PositionIncrementAttribute.class))
                {
                    posIncAtt = source.getAttribute(PositionIncrementAttribute.class);
                }
                nextToken = new PackedTokenAttributeImpl();
                nextToken.setEmpty().copyBuffer(cta.buffer(), 0, cta.length());
                nextToken.setOffset(offsetAtt.startOffset(), offsetAtt.endOffset());
                if (typeAtt != null)
                {
                    nextToken.setType(typeAtt.type());
                }
                if (posIncAtt != null)
                {
                    nextToken.setPositionIncrement(posIncAtt.getPositionIncrement());
                }

                return nextToken.toString();
            }
        } finally
        {
            try
            {
                if (source != null)
                {
                    source.close();
                }
            } catch (IOException e)
            {
                // ignore
            }
        }
        return null;
    }

    protected void addTextSpanQuery(String field, PropertyDefinition pDef, String first, String last, int slop,
            boolean inOrder, String expandedFieldName, IndexTokenisationMode tokenisationMode, Builder booleanQuery,
                                    Locale locale) {
        addMLTextOrTextSpanQuery(field, pDef, first, last, slop, inOrder, expandedFieldName, tokenisationMode,
                booleanQuery, locale);
    }

    protected org.apache.lucene.search.Query addContentSpanQuery(String afield, PropertyDefinition pDef, String first,
            String last, int slop, boolean inOrder, String expandedFieldName, List<Locale> expandedLocales)
    {
        try
        {
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            for (Locale locale : expandedLocales)
            {
                if (locale.toString().length() == 0)
                {
                    IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(),
                            null, FieldUse.FTS);
                    for (FieldInstance field : indexedField.getFields())
                    {
                        if (!field.isLocalised())
                        {
                            SpanQuery firstQuery = buildSpanOrQuery(first, field);
                            SpanQuery lastQuery = buildSpanOrQuery(last, field);
                            SpanNearQuery result = new SpanNearQuery(new SpanQuery[]
                            { firstQuery, lastQuery }, slop, inOrder);
                            booleanQuery.add(result, Occur.SHOULD);
                        }
                    }
                } else
                {
                    IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(),
                            null, FieldUse.FTS);
                    for (FieldInstance field : indexedField.getFields())
                    {
                        if (field.isLocalised())
                        {
                            SpanQuery firstQuery = buildSpanOrQuery(getLocalePrefixedText(first, locale), field);
                            SpanQuery lastQuery = buildSpanOrQuery(getLocalePrefixedText(last, locale), field);
                            SpanNearQuery result = new SpanNearQuery(new SpanQuery[]
                            { firstQuery, lastQuery }, slop, inOrder);
                            booleanQuery.add(result, Occur.SHOULD);
                        }
                    }
                }
            }
            return getNonEmptyBooleanQuery(booleanQuery.build());
        } catch (IOException ioe)
        {
            return createNoMatchQuery();
        }

    }

    private String getLocalePrefixedText(String text, Locale locale)
    {
        StringBuilder builder = new StringBuilder(text.length() + 10);
        builder.append("\u0000").append(locale.toString()).append("\u0000").append(text);
        return builder.toString();
    }

    /**
     * @param first
     * @param field
     * @return SpanOrQuery
     * @throws IOException
     */
    private SpanQuery buildSpanOrQuery(String first, FieldInstance field) throws IOException
    {
        ArrayList<SpanQuery> spanOrQueryParts = new ArrayList<SpanQuery>();

        PackedTokenAttributeImpl nextToken;
        TokenStream source = null;

        try
        {
            source = getAnalyzer().tokenStream(field.getField(), new StringReader(first));
            source.reset();
            while (source.incrementToken())
            {
                CharTermAttribute cta = source.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAtt = source.getAttribute(OffsetAttribute.class);
                TypeAttribute typeAtt = null;
                if (source.hasAttribute(TypeAttribute.class))
                {
                    typeAtt = source.getAttribute(TypeAttribute.class);
                }
                PositionIncrementAttribute posIncAtt = null;
                if (source.hasAttribute(PositionIncrementAttribute.class))
                {
                    posIncAtt = source.getAttribute(PositionIncrementAttribute.class);
                }
                nextToken = new PackedTokenAttributeImpl();
                nextToken.setEmpty().copyBuffer(cta.buffer(), 0, cta.length());
                nextToken.setOffset(offsetAtt.startOffset(), offsetAtt.endOffset());
                if (typeAtt != null)
                {
                    nextToken.setType(typeAtt.type());
                }
                if (posIncAtt != null)
                {
                    nextToken.setPositionIncrement(posIncAtt.getPositionIncrement());
                }

                SpanQuery termQuery = new SpanTermQuery(new Term(field.getField(), nextToken.toString()));
                spanOrQueryParts.add(termQuery);
            }
        } finally
        {
            try
            {
                if (source != null)
                {
                    source.close();
                }
            } catch (IOException e)
            {
                // ignore
            }
        }

        if (spanOrQueryParts.size() == 1)
        {
            return spanOrQueryParts.get(0);
        } else
        {
            return new SpanOrQuery(spanOrQueryParts.toArray(new SpanQuery[]
            {}));
        }
    }

    protected void addMLTextSpanQuery(String field, PropertyDefinition pDef, String first, String last, int slop,
            boolean inOrder, String expandedFieldName, PropertyDefinition propertyDef,
            IndexTokenisationMode tokenisationMode, Builder booleanQuery, Locale locale) {
        addMLTextOrTextSpanQuery(field, pDef, first, last, slop, inOrder, expandedFieldName, tokenisationMode,
                booleanQuery, locale);
    }

    private void addMLTextOrTextSpanQuery(String afield, PropertyDefinition pDef, String first, String last, int slop,
            boolean inOrder, String expandedFieldName, IndexTokenisationMode tokenisationMode, Builder booleanQuery,
            Locale locale)
    {
        try
        {

            if (locale.toString().length() == 0)
            {
                IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(), null,
                        FieldUse.FTS);
                for (FieldInstance field : indexedField.getFields())
                {
                    if (!field.isLocalised())
                    {
                        SpanQuery firstQuery = buildSpanOrQuery(first, field);
                        SpanQuery lastQuery = buildSpanOrQuery(last, field);
                        SpanNearQuery result = new SpanNearQuery(new SpanQuery[]
                        { firstQuery, lastQuery }, slop, inOrder);
                        booleanQuery.add(result, Occur.SHOULD);
                    }
                }
            } else
            {
                IndexedField indexedField = AlfrescoSolrDataModel.getInstance().getQueryableFields(pDef.getName(), null,
                        FieldUse.FTS);
                for (FieldInstance field : indexedField.getFields())
                {
                    if (field.isLocalised())
                    {
                        SpanQuery firstQuery = buildSpanOrQuery(getLocalePrefixedText(first, locale), field);
                        SpanQuery lastQuery = buildSpanOrQuery(getLocalePrefixedText(last, locale), field);
                        SpanNearQuery result = new SpanNearQuery(new SpanQuery[]
                        { firstQuery, lastQuery }, slop, inOrder);
                        booleanQuery.add(result, Occur.SHOULD);
                    }
                }
            }

        } catch (IOException ioe)
        {

        }
    }

    public boolean addContentCrossLocaleWildcards()
    {
        return false;
    }

    protected Query createOwnerSetQuery(String queryText) throws ParseException
    {
        return new SolrOwnerSetQuery(queryText);
    }

    protected Query createReaderSetQuery(String queryText) throws ParseException
    {
        return new SolrReaderSetQuery(queryText);
    }

    protected Query createAuthoritySetQuery(String queryText) throws ParseException
    {
        return new SolrAuthoritySetQuery(queryText);
    }

    protected Query createDeniedQuery(String queryText) throws ParseException
    {
        return new SolrDeniedQuery(queryText);
    }

    protected Query createDenySetQuery(String queryText) throws ParseException
    {
        return new SolrDenySetQuery(queryText);
    }

    private BytesRef analyzeMultitermTerm(String field, String part) {
        return analyzeMultitermTerm(field, part, getAnalyzer());
    }

    protected BytesRef analyzeMultitermTerm(String field, String part, Analyzer analyzerIn) {
        if (analyzerIn == null) analyzerIn = getAnalyzer();

        try (TokenStream source = analyzerIn.tokenStream(field, part)) {
            source.reset();

            TermToBytesRefAttribute termAtt = source.getAttribute(TermToBytesRefAttribute.class);

            if (!source.incrementToken())
                throw new IllegalArgumentException("analyzer returned no terms for multiTerm term: " + part);
            BytesRef bytes = BytesRef.deepCopyOf(termAtt.getBytesRef());
            if (source.incrementToken())
                throw new IllegalArgumentException("analyzer returned too many terms for multiTerm term: " + part);
            source.end();
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Error analyzing multiTerm term: " + part, e);
        }
    }

    private boolean analyzeRangeTerms = true;

    protected Query newRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
        final BytesRef start;
        final BytesRef end;

        if (part1 == null) {
            start = null;
        } else {
            start = analyzeRangeTerms ? analyzeMultitermTerm(field, part1) : new BytesRef(part1);
        }

        if (part2 == null) {
            end = null;
        } else {
            end = analyzeRangeTerms ? analyzeMultitermTerm(field, part2) : new BytesRef(part2);
        }

        final TermRangeQuery query = new TermRangeQuery(field, start, end, startInclusive, endInclusive);

        query.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_REWRITE);
        return query;
    }


}
