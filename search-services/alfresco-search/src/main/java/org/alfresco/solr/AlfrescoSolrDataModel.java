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
package org.alfresco.solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.opencmis.dictionary.CMISAbstractDictionaryService;
import org.alfresco.opencmis.dictionary.CMISDictionaryService;
import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.opencmis.dictionary.QNameFilter;
import org.alfresco.opencmis.search.CMISQueryOptions;
import org.alfresco.opencmis.search.CMISQueryOptions.CMISQueryMode;
import org.alfresco.opencmis.search.CMISQueryParser;
import org.alfresco.opencmis.search.CmisFunctionEvaluationContext;
import org.alfresco.repo.dictionary.CompiledModelsCache;
import org.alfresco.repo.dictionary.DictionaryComponent;
import org.alfresco.repo.dictionary.DictionaryDAOImpl;
import org.alfresco.repo.dictionary.Facetable;
import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2ModelDiff;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.i18n.StaticMessageLookup;
import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.repo.search.impl.QueryParserUtils;
import org.alfresco.repo.search.impl.parsers.AlfrescoFunctionEvaluationContext;
import org.alfresco.repo.search.impl.parsers.FTSParser;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.Ordering;
import org.alfresco.repo.search.impl.querymodel.QueryModelFactory;
import org.alfresco.repo.search.impl.querymodel.QueryOptions.Connective;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilder;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderContext;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryModelFactory;
import org.alfresco.repo.tenant.SingleTServiceImpl;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoClientDataModelServicesFactory.DictionaryKey;
import org.alfresco.solr.client.AlfrescoModel;
import org.alfresco.solr.query.Lucene4QueryBuilderContextSolrImpl;
import org.alfresco.solr.query.Solr4QueryParser;
import org.alfresco.solr.tracker.pool.DefaultTrackerPoolFactory;
import org.alfresco.solr.tracker.pool.TrackerPoolFactory;
import org.alfresco.util.NumericEncoder;
import org.alfresco.util.Pair;
import org.alfresco.util.cache.DefaultAsynchronouslyRefreshedCacheRegistry;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.LegacyNumericUtils;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.apache.solr.core.CoreDescriptorDecorator;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * @author Andy
 *
 */
public class AlfrescoSolrDataModel implements QueryConstants
{

    public static final String SHARED_PROPERTIES = "shared.properties";

    public static enum FieldUse
    {
        FTS,          // Term/Phrase/Range/Fuzzy/Prefix/Proximity/Wild
        ID,           // Exact/ExactRange - Comparison, In, Upper, Lower
        FACET,        // Field, Range, Query
        MULTI_FACET,  // Text fields will require cross language support to avoid tokenisation for facets
        STATS,        // Stats
        SORT,         // Locale
        SUGGESTION,
        COMPLETION, 
        HIGHLIGHT  
    }
    
    public static enum ContentFieldType
    {
        DOCID,
        SIZE,
        LOCALE, 
        MIMETYPE,
        ENCODING,
        TRANSFORMATION_STATUS,
        TRANSFORMATION_TIME,
        TRANSFORMATION_EXCEPTION
    }
    
    public static final String CONTENT_S_LOCALE_PREFIX = "content@s__locale@";
    public static final String CONTENT_M_LOCALE_PREFIX = "content@m__locale@";
    
    protected final static Logger log = LoggerFactory.getLogger(AlfrescoSolrDataModel.class);

    private static final String CHANGE_SET = "CHANGE_SET";

    private static final String  TX = "TX";
    
    public static final String DEFAULT_TENANT = "_DEFAULT_";
    
    private static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private static AlfrescoSolrDataModel model;

    private TenantService tenantService;

    private NamespaceDAO namespaceDAO;

    private DictionaryDAOImpl dictionaryDAO;
    
    private  Map<String,DictionaryComponent> dictionaryServices;

    private  Map<DictionaryKey,CMISAbstractDictionaryService> cmisDictionaryServices;
    
    private HashMap<String, Set<String>> modelErrors = new HashMap<String, Set<String>>();
    
    private HashSet<QName> suggestableProperties = new HashSet<QName>();
    
    private HashSet<QName> crossLocaleSearchDataTypes = new HashSet<QName>();
    
    private HashSet<QName> crossLocaleSearchProperties = new HashSet<QName>();
    
    private HashSet<QName> identifierProperties = new HashSet<QName>();
    private ThreadPoolExecutor threadPool;


    public void close() {
        threadPool.shutdown();
    }

    public AlfrescoSolrDataModel()
    {
        tenantService = new SingleTServiceImpl();

        dictionaryDAO = new DictionaryDAOImpl();
        dictionaryDAO.setTenantService(tenantService);
        
        try
        {
            CompiledModelsCache compiledModelsCache = new CompiledModelsCache();
            compiledModelsCache.setDictionaryDAO(dictionaryDAO);
            compiledModelsCache.setTenantService(tenantService);
            compiledModelsCache.setRegistry(new DefaultAsynchronouslyRefreshedCacheRegistry());
            TrackerPoolFactory trackerPoolFactory = new DefaultTrackerPoolFactory(new Properties(), "_dictionary_", "_internal_");
            threadPool = trackerPoolFactory.create();
            compiledModelsCache.setThreadPoolExecutor(threadPool);

            dictionaryDAO.setDictionaryRegistryCache(compiledModelsCache);
            dictionaryDAO.setResourceClassLoader(getResourceClassLoader());
            dictionaryDAO.init();
        }
        catch (Exception e) 
        {
            throw new AlfrescoRuntimeException("Failed to create dictionaryDAO ", e);
        }

        namespaceDAO = dictionaryDAO;

        QNameFilter qnameFilter = getQNameFilter();
        dictionaryServices = AlfrescoClientDataModelServicesFactory.constructDictionaryServices(qnameFilter, dictionaryDAO);
        DictionaryComponent dictionaryComponent = getDictionaryService(CMISStrictDictionaryService.DEFAULT);
        dictionaryComponent.setMessageLookup(new StaticMessageLookup());

        cmisDictionaryServices = AlfrescoClientDataModelServicesFactory.constructDictionaries(qnameFilter, namespaceDAO, dictionaryComponent, dictionaryDAO);
        
        Properties props = getCommonConfig();
        for (Object key : props.keySet())
        {
            String stringKey = (String) key;
            if (stringKey.startsWith("alfresco.suggestable.property."))
            {
                QName qName = QName.createQName(props.getProperty(stringKey));
                suggestableProperties.add(qName);
            }
            else if (stringKey.startsWith("alfresco.cross.locale.property."))
            {
                QName qName = QName.createQName(props.getProperty(stringKey));
                crossLocaleSearchProperties.add(qName);
            }
            else if (stringKey.startsWith("alfresco.cross.locale.datatype."))
            {
                QName qName = QName.createQName(props.getProperty(stringKey));
                crossLocaleSearchDataTypes.add(qName);
            }
            else if (stringKey.startsWith("alfresco.identifier.property."))
            {
                QName qName = QName.createQName(props.getProperty(stringKey));
                identifierProperties.add(qName);
            }
            
        }
        
        if(props.isEmpty())
        {
        	suggestableProperties.add(ContentModel.PROP_NAME);
        	suggestableProperties.add(ContentModel.PROP_TITLE);
        	suggestableProperties.add(ContentModel.PROP_DESCRIPTION);
        	suggestableProperties.add(ContentModel.PROP_CONTENT);
        	
        	crossLocaleSearchDataTypes.add(DataTypeDefinition.TEXT);
        	crossLocaleSearchDataTypes.add(DataTypeDefinition.CONTENT);
        	crossLocaleSearchDataTypes.add(DataTypeDefinition.MLTEXT);
        	
        	identifierProperties.add(ContentModel.PROP_CREATOR);
        	identifierProperties.add(ContentModel.PROP_MODIFIER);
        	identifierProperties.add(ContentModel.PROP_USERNAME);
        	identifierProperties.add(ContentModel.PROP_AUTHORITY_NAME);
        	
        }
        
    }

    public static String getTenantId(String tenant)
    {
        if((tenant == null) || tenant.equals(TenantService.DEFAULT_DOMAIN))
        {
            return DEFAULT_TENANT;
        }
        else
        {
            return tenant.replaceAll("!", "_-._");
        }
    }
    
    /**
     * 
     * @param tenant
     * @param aclId
     * @return <TENANT>:<ACLID>:A-<ACLID>
     */
    public static String getAclDocumentId(String tenant, Long aclId)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTenantId(tenant));
        builder.append("!");
        builder.append(NumericEncoder.encode(aclId));
        builder.append("!ACL");
        return builder.toString();
    }
    
    /**
     * 
     * @param aclChangeSetId
     * @return <TENANT>:CHANGE_SET:<aclChangeSetId <- max in doc if compressed>
     */
    public static String getAclChangeSetDocumentId(Long aclChangeSetId)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("TRACKER");
        builder.append("!");
        builder.append(CHANGE_SET);
        builder.append("!");
        builder.append(NumericEncoder.encode(aclChangeSetId));
        return builder.toString();
    }
    
    public static Long parseAclChangeSetId(String aclChangeSetDocumentId)
    {
        return parseIdFromDocumentId(aclChangeSetDocumentId);
    }
    
    public static Long parseTransactionId(String transactionDocumentId)
    {
        return parseIdFromDocumentId(transactionDocumentId);
    }
    
    private static Long parseIdFromDocumentId(String documentId)
    {
        String[] split = documentId.split("!");
        if (split.length > 0)
            return NumericEncoder.decodeLong(split[split.length - 1]);
        else 
            return null;
    }
    
    /**
     * Returns the Solr 4 id
     * @param tenant
     * @param aclId
     * @param dbid
     * @return <TENANT>!<ACLID>!<DBID>
     */
    public static String getNodeDocumentId(String tenant, Long aclId, Long dbid)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTenantId(tenant));
        builder.append("!");
        builder.append(NumericEncoder.encode(aclId));
        builder.append("!");
        builder.append(NumericEncoder.encode(dbid));
        return builder.toString();
    }
    
    public static class TenantAclIdDbId
    {
        public String tenant;
        public Long alcId;
        public Long dbId;
    }
    
    public static TenantAclIdDbId decodeNodeDocumentId(String id)
    {
        TenantAclIdDbId ids = new TenantAclIdDbId();
        String[] split = id.split("!");
        if (split.length > 0)
            ids.tenant = split[0];
        if (split.length > 1)
            ids.alcId = NumericEncoder.decodeLong(split[1]);
        if (split.length > 2)
            ids.dbId = NumericEncoder.decodeLong(split[2]);
        return ids;
    }
    
    /**
     * 
     * @param txId
     * @return <TENANT>:CHANGE_SET:<txId <- max in doc if compressed>
     */
    public static String getTransactionDocumentId(Long txId)
    {
        StringBuilder builder = new StringBuilder();
        // TODO - check and encode for ":"
        builder.append("TRACKER");
        builder.append("!");
        builder.append(TX);
        builder.append("!");
        builder.append(NumericEncoder.encode(txId));
        return builder.toString();
    }
    
    public static Term getLongTerm(String field, long longValue)
    {
        BytesRefBuilder bytesBuilder = new BytesRefBuilder();
        LegacyNumericUtils.longToPrefixCoded(longValue, 0, bytesBuilder);
        return new Term(field, bytesBuilder.get());
    }
    
    public static Term getLongTerm(String field, String stringValue)
    {
        long longValue = Long.parseLong(stringValue);
        return getLongTerm(field, longValue); 
    }
    
    
    public String getAlfrescoPropertyFromSchemaField(String schemaField)
    {
        int index = schemaField.lastIndexOf("@{");
        if(index == -1)
        {
            return schemaField;
        }
        
        String alfrescoQueryField = schemaField.substring(index+1);
        QName qName = QName.createQName(alfrescoQueryField);
        alfrescoQueryField = qName.toPrefixString(namespaceDAO);
        
        PropertyDefinition propertyDefinition = getPropertyDefinition(qName);
        if((propertyDefinition == null))
        { 
            return alfrescoQueryField;
        }
        if(!propertyDefinition.isIndexed() && !propertyDefinition.isStoredInIndex())
        {
            return alfrescoQueryField;
        }
        
        DataTypeDefinition dataTypeDefinition = propertyDefinition.getDataType();
        if(dataTypeDefinition.getName().equals(DataTypeDefinition.CONTENT))
        {
            if(schemaField.contains("__size@"))
            {
                return alfrescoQueryField + ".size";
            }
            else if(schemaField.contains("__locale@"))
            {
                return alfrescoQueryField + ".locale";
            }
            else if(schemaField.contains("__mimetype@"))
            {
                return alfrescoQueryField + ".mimetype";
            }
            else if(schemaField.contains("__encoding@"))
            {
                return alfrescoQueryField + ".encoding";
            }
            else if(schemaField.contains("__docid@"))
            {
                return alfrescoQueryField + ".docid";
            }
            else if(schemaField.contains("__tr_ex@"))
            {
                return alfrescoQueryField + ".tr_ex";
            }
            else if(schemaField.contains("__tr_time@"))
            {
                return alfrescoQueryField + ".tr_time";
            }
            else if(schemaField.contains("__tr_status@"))
            {
                return alfrescoQueryField + ".tr_status";
            }
            else
            {
                return alfrescoQueryField;
            }
        }
        else
        {
            return alfrescoQueryField;
        }
    }
    
 
    public static AlfrescoSolrDataModel getInstance()
    {
        readWriteLock.readLock().lock();
        try
        {
            if (model != null)
            {
                return model;
            }
        }
        finally
        {
            readWriteLock.readLock().unlock();
        }

        // not found

        readWriteLock.writeLock().lock();
        try
        {
            if (model == null)
            {
                model = new AlfrescoSolrDataModel();
            }
            return model;
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }

    }
    
    public NamespaceDAO getNamespaceDAO()
    {
        return namespaceDAO;
    }
    
    /**
     * TODO: Fix to load type filter/exclusions from somewhere sensible
     * @return QNameFilter
     */
    private QNameFilter getQNameFilter()
    {
        QNameFilter qnameFilter = null;
        FileSystemXmlApplicationContext ctx = null;

        File resourceDirectory = getResourceDirectory();
        // If we ever need to filter out models in the future, then we must put a filter somewhere.
        // Currently, we do not need to filter any models, so this filter does not exist.
        File filterContext = new File(resourceDirectory, "opencmis-qnamefilter-context.xml");

        if(!filterContext.exists())
        {
            log.info("No type filter context found at " + filterContext.getAbsolutePath() + ", no type filtering");
            return qnameFilter;
        }
        
        try
        {
            ctx = new FileSystemXmlApplicationContext(new String[] { "file:" + filterContext.getAbsolutePath() }, false);
            ctx.setClassLoader(this.getClass().getClassLoader());
            ctx.refresh();
            qnameFilter = (QNameFilter)ctx.getBean("cmisTypeExclusions");
            if(qnameFilter == null)
            {
                log.warn("Unable to find type filter at " + filterContext.getAbsolutePath() + ", no type filtering");
            }
        }
        catch(BeansException e)
        {
            log.warn("Unable to parse type filter at " + filterContext.getAbsolutePath() + ", no type filtering");
        }
        finally
        {
            if(ctx != null && ctx.getBeanFactory() != null && ctx.isActive())
            {
                ctx.close();
            }
        }

        return qnameFilter;
    }
    
    public static Properties getCommonConfig()
    {
        File resourceDirectory = getResourceDirectory();
        // If we ever need to filter out models in the future, then we must put a filter somewhere.
        // Currently, we do not need to filter any models, so this filter does not exist.
        File propertiesFile = new File(resourceDirectory, SHARED_PROPERTIES);

        Properties props = new Properties();
        if(!propertiesFile.exists())
        {
            log.info("No shared properties found at  " + propertiesFile.getAbsolutePath());
            return props;
        }
        
        try(InputStream is = new FileInputStream(propertiesFile))
        {
        	props.load(is);
        } 
        catch (IOException e) 
        {
        	log.info("Failed to read shared properties fat  " + propertiesFile.getAbsolutePath());
		}
        
        return props;
    }
    
    /**
     * @return ClassLoader
     */
    public ClassLoader getResourceClassLoader()
    {
        File f = getResourceDirectory();
        if (f.canRead() && f.isDirectory())
        {
            URL[] urls = new URL[1];

            try
            {
                URL url = f.toURI().normalize().toURL();
                urls[0] = url;
            }
            catch (MalformedURLException e)
            {
                throw new AlfrescoRuntimeException("Failed to add resources to classpath ", e);
            }

            return URLClassLoader.newInstance(urls, this.getClass().getClassLoader());
        }
        else
        {
            return this.getClass().getClassLoader();
        }
    }
    
    public static File getResourceDirectory()
    {
        return new File(SolrResourceLoader.locateSolrHome().toFile(), "conf");
    }  
    
    /**
     * Gets a DictionaryService, if an Alternative dictionary is specified it tries to get that.
     * It will attempt to get the DEFAULT dictionary service if null is specified or it can't find
     * a dictionary with the name of "alternativeDictionary"
     * @param alternativeDictionary - can be null;
     * @return DictionaryService
     */
    public DictionaryComponent getDictionaryService(String alternativeDictionary)
    {
        DictionaryComponent dictionaryComponent = null;
        
        if (alternativeDictionary != null && !alternativeDictionary.trim().isEmpty())
        {
            dictionaryComponent = dictionaryServices.get(alternativeDictionary);
        }
        
        if (dictionaryComponent == null)
        {
            dictionaryComponent = dictionaryServices.get(CMISStrictDictionaryService.DEFAULT);
        }
        return dictionaryComponent;
    }
    
    public IndexedField getIndexedFieldForContentPropertyMetadata(QName propertyQName, ContentFieldType type)
    {
        IndexedField indexedField = new IndexedField();
        PropertyDefinition propertyDefinition = getPropertyDefinition(propertyQName);
        if((propertyDefinition == null))
        { 
            return indexedField;
        }
        if(!propertyDefinition.isIndexed() && !propertyDefinition.isStoredInIndex())
        {
            return indexedField;
        }
        
        DataTypeDefinition dataTypeDefinition = propertyDefinition.getDataType();
        if(dataTypeDefinition.getName().equals(DataTypeDefinition.CONTENT))
        {
            StringBuilder builder = new StringBuilder();
            builder.append(dataTypeDefinition.getName().getLocalName());
            builder.append('@');
            // TODO wher we support multi value propertis correctly .... builder.append(propertyDefinition.isMultiValued() ? "m" : "s");
            builder.append('s');
            builder.append("_");
            builder.append('_');
            switch (type)
            {
            case DOCID:
                builder.append("docid");
                break;
            case ENCODING:
                builder.append("encoding");
                break;
            case LOCALE:
                builder.append("locale");
                break;
            case MIMETYPE:
                builder.append("mimetype");
                break;
            case SIZE:
                builder.append("size");
                break;
            case TRANSFORMATION_EXCEPTION:
                builder.append("tr_ex");
                break;
            case TRANSFORMATION_STATUS:
                builder.append("tr_status");
                break;
            case TRANSFORMATION_TIME:
                builder.append("tr_time");
                break;
            default:
                break;
            }
            builder.append('@');
            builder.append(propertyQName);
            indexedField.addField(builder.toString(), false, false);
            
        }
        return indexedField;      
        
    }
    
    
    public IndexedField getQueryableFields(QName propertyQName,  ContentFieldType type, FieldUse fieldUse)
    {
        if(type != null)
        {
            return getIndexedFieldForContentPropertyMetadata(propertyQName, type);
        }
        
        IndexedField indexedField = new IndexedField();
        PropertyDefinition propertyDefinition = getPropertyDefinition(propertyQName);
        if((propertyDefinition == null))
        { 
            indexedField.addField("_dummy_", false, false);
            return indexedField;
        }
        if(!propertyDefinition.isIndexed() && !propertyDefinition.isStoredInIndex())
        {
            indexedField.addField("_dummy_", false, false);
            return indexedField;
        }
        
        if(isTextField(propertyDefinition))
        {
           switch(fieldUse)
           {
           case COMPLETION:
               addCompletionFields(propertyDefinition, indexedField);
               break;
           case FACET:
               addFacetSearchFields(propertyDefinition, indexedField);
               break;
           case FTS:
               addFullTextSearchFields(propertyDefinition, indexedField);
               break;
           case ID:
               addIdentifierSearchFields(propertyDefinition, indexedField);
               break;
           case MULTI_FACET:
               addMultiSearchFields(propertyDefinition, indexedField);
               break;
           case SORT:
               addSortSearchFields(propertyDefinition, indexedField);
               break;
           case STATS:
               addStatsSearchFields(propertyDefinition, indexedField);
               break;
           case SUGGESTION:
               if(isSuggestable(propertyQName))
               {
                   indexedField.addField("suggest", false, false);
               }
               addCompletionFields(propertyDefinition, indexedField);
               break;
           case HIGHLIGHT:
               addHighlightSearchFields(propertyDefinition, indexedField);
               break;
           }
        }
        else
        {
            indexedField.addField(getFieldForNonText(propertyDefinition), false, false);
        }
        return indexedField;
    }
    
    /*
     * Adds best completion fields in order of preference 
     */
   
    private void addCompletionFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
        if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
        {
        	if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
        	{
                indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
        	}
        	else
        	{
        		 indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
        	}
        }
        else if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.TRUE))
        {
        	if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
        	{
                indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
        	}
        	else
        	{
        		 indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
        	}
        }
        else if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE))
        {
            indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
        }
                
    }
    
    /*
     * Adds best fts fields in order of preference 
     */
   
    private void addFullTextSearchFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
        if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.TRUE)
                || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
        {
            indexedField.addField(getFieldForText(true, true, false, propertyDefinition), true, false);
            if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
        	{
                indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
        	}
        }
        else
        {
            indexedField.addField(getFieldForText(true, false, false, propertyDefinition), true, false);
            indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
        }        
    }
    
    private void addHighlightSearchFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
        if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.TRUE)
                || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
        {
        	if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
        	{
                indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
                indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
        	}
        	else
        	{
        		 indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
        	}
        }
        else
        {
            indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
        }        
    }
    
    /*
     * Adds best identifier fields in order of preference 
     */
   
    private void addIdentifierSearchFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
        if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
                || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
        {
            
            indexedField.addField(getFieldForText(true, false, false, propertyDefinition), true, false);
            indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
        }
        else
        {
            indexedField.addField(getFieldForText(true, true, false, propertyDefinition), true, false);
            if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
        	{
                indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
        	}
        }        
    }
    
    /*
     * Adds best identifier fields in order of preference 
     */
   
    private void addFacetSearchFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
        if(propertyDefinition.getDataType().getName().equals(DataTypeDefinition.TEXT))
        {
            if (!isIdentifierTextProperty(propertyDefinition.getName()))
            {
                if(propertyDefinition.getFacetable() == Facetable.TRUE)
                {
                    indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
                }
            }
        }
        
        
        if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
                || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH)
                || isIdentifierTextProperty(propertyDefinition.getName()))
        {
            
            indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
        }
        else
        {
        	if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
        	{
                indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
        	}
        	else
        	{
        		 indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
        	}
        }        
    }
    
    private void addMultiSearchFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
    	if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
    			|| (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH)
    			|| isIdentifierTextProperty(propertyDefinition.getName()))
    	{

    		indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
    	}
    	else
    	{
    		if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
    		{
    			indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
    		}
    		else
    		{
    			indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
    		}
    	}       
    }
    
    private void addStatsSearchFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
        addFacetSearchFields(propertyDefinition, indexedField);
    }
    
    private void addSortSearchFields( PropertyDefinition propertyDefinition , IndexedField indexedField)
    {
        // Can only order on single valued fields
        DataTypeDefinition dataTypeDefinition = propertyDefinition.getDataType();
        if(dataTypeDefinition.getName().equals(DataTypeDefinition.TEXT))
        {	
            if(propertyDefinition.isMultiValued() == false)
            {
                if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
                        || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
                {
                    indexedField.addField(getFieldForText(false, false, true, propertyDefinition), false, true);
                }
                else if (isIdentifierTextProperty(propertyDefinition.getName()))
                {
                    indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);   
                }
                else
                {
                	if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
            		{
            			indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
            		}
            		else
            		{
            			indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
            		}
                }
            }
        }

        if(dataTypeDefinition.getName().equals(DataTypeDefinition.MLTEXT))
        {
            if(propertyDefinition.isMultiValued() == false)
            {
                if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
                        || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
                {
                    indexedField.addField(getFieldForText(false, false, true, propertyDefinition), false, true);
                }
                else
                {
                	if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
            		{
            			indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
            		}
            		else
            		{
            			indexedField.addField(getFieldForText(true, true, false, propertyDefinition), false, false);
            		}
                }
            }
        }
    }
    
    
    /**
     * Get all the field names into which we must copy the source data
     * 
     * @param propertyQName QName
     * @return IndexedField
     */
    public IndexedField getIndexedFieldNamesForProperty(QName propertyQName)
    {
        // TODO: Cache and throw on model refresh
        
        IndexedField indexedField = new IndexedField();
        PropertyDefinition propertyDefinition = getPropertyDefinition(propertyQName);
        if((propertyDefinition == null))
        { 
            return indexedField;
        }
        if(!propertyDefinition.isIndexed() && !propertyDefinition.isStoredInIndex())
        {
            return indexedField;
        }

        DataTypeDefinition dataTypeDefinition = propertyDefinition.getDataType();
        if(isTextField(propertyDefinition))
        { 
            if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.TRUE)
                    || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
            {
                indexedField.addField(getFieldForText(true, true, false, propertyDefinition), true, false);
                if(crossLocaleSearchDataTypes.contains(propertyDefinition.getDataType().getName()) || crossLocaleSearchProperties.contains(propertyDefinition.getName()))
        		{
        			indexedField.addField(getFieldForText(false, true, false, propertyDefinition), false, false);
        		}
            }
            
            if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
                    || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH
                    || isIdentifierTextProperty(propertyDefinition.getName())))
            {
                indexedField.addField(getFieldForText(true, false, false, propertyDefinition), true, false);
                indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
                
            }

            if(dataTypeDefinition.getName().equals(DataTypeDefinition.TEXT))
            {
                if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
                        || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
                {
                    if(propertyDefinition.isMultiValued() == false)
                    {
                        indexedField.addField(getFieldForText(false, false, true, propertyDefinition), false, true);
                    }
                }
                else if (!isIdentifierTextProperty(propertyDefinition.getName()))
                {
                    if(propertyDefinition.getFacetable() == Facetable.TRUE)
                    {
                        indexedField.addField(getFieldForText(false, false, false, propertyDefinition), false, false);
                    }
                }
            }
            
            if(dataTypeDefinition.getName().equals(DataTypeDefinition.MLTEXT))
            {
                if ((propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.FALSE)
                        || (propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.BOTH))
                {
                    if(propertyDefinition.isMultiValued() == false)
                    {
                        indexedField.addField(getFieldForText(true, false, true, propertyDefinition), true, true);
                    }
                }   
            }
            
            if(isSuggestable(propertyQName))
            {
                indexedField.addField("suggest_@"+propertyDefinition.getName().toString(), false, false);
            }
        }
        else
        {
            indexedField.addField(getFieldForNonText(propertyDefinition), false, false);
        }

        return indexedField;

    }

    /**
     * @param propertyQName QName
     * @return boolean
     */
    private boolean isIdentifierTextProperty(QName propertyQName)
    {
    	return identifierProperties.contains(propertyQName);
    }

    private boolean isTextField(PropertyDefinition propertyDefinition)
    {
        QName propertyDataTypeQName = propertyDefinition.getDataType().getName();
        if(propertyDataTypeQName.equals(DataTypeDefinition.MLTEXT))
        {
            return true;
        }
        else if(propertyDataTypeQName.equals(DataTypeDefinition.CONTENT))
        {
            return true;
        }
        else if(propertyDataTypeQName.equals(DataTypeDefinition.TEXT))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
         
    private boolean isSuggestable(QName propertyQName)
    {
        if(propertyQName == null)
        {
            return false;
        }
        return suggestableProperties.contains(propertyQName);
    }
    
    private boolean hasDocValues(PropertyDefinition propertyDefinition)
    {

        if(isTextField(propertyDefinition))
        {
            // We only call this if text is untokenised and localised 
            if(propertyDefinition.getFacetable() == Facetable.FALSE)
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        else
        {
            if(propertyDefinition.getFacetable() == Facetable.FALSE)
            {
                return false;
            }
            else if(propertyDefinition.getFacetable() == Facetable.TRUE)
            {
                return true;
            }
            else
            {
                if(propertyDefinition.getIndexTokenisationMode() == IndexTokenisationMode.TRUE)
                {
                    return false;
                }
                else
                {
                    return isPrimitive(propertyDefinition.getDataType());
                }
            }
        }

    }
    
    /**
     * @param dataType DataTypeDefinition
     * @return boolean
     */
    private boolean isPrimitive(DataTypeDefinition dataType)
    {
        if(dataType.getName().equals(DataTypeDefinition.INT))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.LONG))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.FLOAT))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.DOUBLE))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.DATE))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.DATETIME))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.BOOLEAN))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.CATEGORY))
        {
            return true;
        }
        else if(dataType.getName().equals(DataTypeDefinition.NODE_REF))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public String getFieldForNonText(PropertyDefinition propertyDefinition)
    {
        StringBuilder builder = new StringBuilder();
        QName qName = propertyDefinition.getDataType().getName();
        builder.append(qName.getLocalName());
        builder.append("@");
        builder.append(propertyDefinition.isMultiValued() ? "m" : "s");
        builder.append(hasDocValues(propertyDefinition) ? "d" : "_");
        builder.append("@");
        builder.append(propertyDefinition.getName().toString());
        return builder.toString();
    }
    
    private String getFieldForText(boolean localised, boolean tokenised, boolean sort, PropertyDefinition propertyDefinition)
    {
        StringBuilder builder = new StringBuilder();
        QName qName = propertyDefinition.getDataType().getName();
        builder.append(qName.getLocalName());
        builder.append("@");
        QName propertyDataTypeQName = propertyDefinition.getDataType().getName();
        if(propertyDataTypeQName.equals(DataTypeDefinition.MLTEXT))
        {
            builder.append('m');
        }
        else  if(propertyDataTypeQName.equals(DataTypeDefinition.CONTENT))
        {
            builder.append('s');
        }
        else
        {
            builder.append(propertyDefinition.isMultiValued() ? "m" : "s");
        }
        if(sort || localised || tokenised || propertyDataTypeQName.equals(DataTypeDefinition.CONTENT) ||  propertyDataTypeQName.equals(DataTypeDefinition.MLTEXT))
        {
            builder.append('_');
        }
        else
        {
            builder.append(hasDocValues(propertyDefinition) ? "d" : "_");
        }
        builder.append('_');
        if(!sort)
        {
            builder.append(localised ? "l" : "_");
            builder.append(tokenised ? "t" : "_");
        }
        else
        {
            builder.append("sort");
        }
        builder.append("@");
        builder.append(propertyDefinition.getName().toString());
        return builder.toString();
    }
    
    
 
    

    /**
     * @param propertyQName QName
     * @return PropertyDefinition
     */
    public PropertyDefinition getPropertyDefinition(QName propertyQName)
    {
        PropertyDefinition propertyDef = getDictionaryService(CMISStrictDictionaryService.DEFAULT).getProperty(propertyQName);
        return propertyDef;
    }
    
    
    
    
//    public PropertyDefinition getPropertyDefinition(QName propertyQName)
//    {
//        PropertyDefinition propertyDef = getDictionaryService(CMISStrictDictionaryService.DEFAULT).getProperty(propertyQName);
//        if ((propertyDef != null) && (propertyDef.getName().equals(ContentModel.PROP_AUTHOR)))
//        {
//            return new PropertyDefinitionWrapper(propertyDef);
//        }
//        else if ((propertyDef != null) && (propertyDef.getName().equals(ContentModel.PROP_CREATOR)))
//        {
//            return new PropertyDefinitionWrapper(propertyDef);
//        }
//        else if ((propertyDef != null) && (propertyDef.getName().equals(ContentModel.PROP_MODIFIER)))
//        {
//            return new PropertyDefinitionWrapper(propertyDef);
//        }
//        return propertyDef;
//    }
    
    
    /**
     * @param model M2Model
     * @return boolean
     */
    public boolean putModel(M2Model model)
    {
        Set<String> errors = validateModel(model);
        if(errors.size() == 0)
        {
            modelErrors.remove(model.getName());
            dictionaryDAO.putModelIgnoringConstraints(model);
            return true;
        }
        else
        {
            if(!modelErrors.containsKey(model.getName()))
            {
                modelErrors.put(model.getName(), errors);
                log.warn(errors.iterator().next());
            }
            return false;
        }
       
    }

    
    private Set<String> validateModel(M2Model model)
    {
        HashSet<String> errors = new HashSet<String>();
        try 
        { 
            dictionaryDAO.getCompiledModel(QName.createQName(model.getName(), namespaceDAO)); 
        } 
        catch (DictionaryException e) 
        {
            // No model to diff
            return errors;
        }
        catch(NamespaceException e)
        {
            // namespace unknown - no model 
            return errors;
        }
        
        
        List<M2ModelDiff> modelDiffs = dictionaryDAO.diffModelIgnoringConstraints(model);
        
        for (M2ModelDiff modelDiff : modelDiffs)
        {
            if (modelDiff.getDiffType().equals(M2ModelDiff.DIFF_UPDATED))
            {
                errors.add("Model not updated: "+model.getName()  + "   Failed to validate model update - found non-incrementally updated " + modelDiff.getElementType() + " '" + modelDiff.getElementName() + "'");
            }
        }
        return errors;
    }
  
    
    public M2Model getM2Model(QName modelQName)
    {
        return dictionaryDAO.getCompiledModel(modelQName).getM2Model();
    }

    public void afterInitModels()
    {
        for (CMISAbstractDictionaryService cds : cmisDictionaryServices.values())
        {
            cds.afterDictionaryInit();
        }
    }
    
    public org.alfresco.repo.search.impl.querymodel.Query parseCMISQueryToAlfrescoAbstractQuery(CMISQueryMode mode, SearchParameters searchParameters,
            SolrQueryRequest req, String alternativeDictionary, CmisVersion cmisVersion) 
    {
        // convert search parameters to cmis query options
        // TODO: how to handle store ref
        CMISQueryOptions options = new CMISQueryOptions(searchParameters.getQuery(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        options.setQueryMode(CMISQueryMode.CMS_WITH_ALFRESCO_EXTENSIONS);
        options.setDefaultFieldName(searchParameters.getDefaultFieldName());
        // TODO: options.setDefaultFTSConnective()
        // TODO: options.setDefaultFTSFieldConnective()
        options.setIncludeInTransactionData(!searchParameters.excludeDataInTheCurrentTransaction());
        options.setLocales(searchParameters.getLocales());
        options.setMlAnalaysisMode(searchParameters.getMlAnalaysisMode());
        options.setQueryParameterDefinitions(searchParameters.getQueryParameterDefinitions());
        for(String name : searchParameters.getQueryTemplates().keySet())
        {
        	String template = searchParameters.getQueryTemplates().get(name);
        	options.addQueryTemplate(name, template);
        }

        // parse cmis syntax
        CapabilityJoin joinSupport = (mode == CMISQueryMode.CMS_STRICT) ? CapabilityJoin.NONE : CapabilityJoin.INNERONLY;
        CmisFunctionEvaluationContext functionContext = getCMISFunctionEvaluationContext(mode, cmisVersion, alternativeDictionary);
        
        CMISDictionaryService cmisDictionary = getCMISDictionary(alternativeDictionary, cmisVersion);
        
        CMISQueryParser parser = new CMISQueryParser(options, cmisDictionary, joinSupport);
        org.alfresco.repo.search.impl.querymodel.Query queryModelQuery = parser.parse(new LuceneQueryModelFactory<Query, Sort, SyntaxError>(), functionContext);

        if (queryModelQuery.getSource() != null)
        {
            List<Set<String>> selectorGroups = queryModelQuery.getSource().getSelectorGroups(functionContext);
            if (selectorGroups.size() == 0)
            {
                throw new UnsupportedOperationException("No selectors");
            }
            if (selectorGroups.size() > 1)
            {
                throw new UnsupportedOperationException("Advanced join is not supported");
            }
        }
        return queryModelQuery;
    }
    
    public CmisFunctionEvaluationContext getCMISFunctionEvaluationContext(CMISQueryMode mode, CmisVersion cmisVersion, String alternativeDictionary)
    {
        BaseTypeId[] validScopes = (mode == CMISQueryMode.CMS_STRICT) ? CmisFunctionEvaluationContext.STRICT_SCOPES : CmisFunctionEvaluationContext.ALFRESCO_SCOPES;
        CmisFunctionEvaluationContext functionContext = new CmisFunctionEvaluationContext();
        functionContext.setCmisDictionaryService(getCMISDictionary(alternativeDictionary, cmisVersion));
        functionContext.setValidScopes(validScopes);
        return functionContext;
    }
    
    /**
     * Gets the CMISDictionaryService, if an Alternative dictionary is specified it tries to get that.
     * It will attempt to get the DEFAULT dictionary service if null is specified or it can't find
     * a dictionary with the name of "alternativeDictionary"
     * @param alternativeDictionary - can be null;
     * @return CMISDictionaryService
     */
    public CMISDictionaryService getCMISDictionary(String alternativeDictionary, CmisVersion cmisVersion)
    {
        CMISDictionaryService cmisDictionary = null;
        
        if (alternativeDictionary != null && !alternativeDictionary.trim().isEmpty())
        {
            DictionaryKey key = new DictionaryKey(cmisVersion, alternativeDictionary);
            cmisDictionary = cmisDictionaryServices.get(key);
        }
        
        if (cmisDictionary == null)
        {
            DictionaryKey key = new DictionaryKey(cmisVersion, CMISStrictDictionaryService.DEFAULT);
            cmisDictionary = cmisDictionaryServices.get(key);
        }
        return cmisDictionary;
    }

    /**
     *
     * @return List<AlfrescoModel>
     */
    public List<AlfrescoModel> getAlfrescoModels()
    {

        ArrayList<AlfrescoModel> answer = new ArrayList<AlfrescoModel>();
        for (QName modelName : dictionaryDAO.getModels())
        {
            M2Model m2Model = dictionaryDAO.getCompiledModel(modelName).getM2Model();
            answer.add(new AlfrescoModel(m2Model, getDictionaryService(CMISStrictDictionaryService.DEFAULT).getModel(modelName).getChecksum(ModelDefinition.XMLBindingType.DEFAULT)));
        }
        return answer;

    }

    /**
     * @return Map<String, Set<String>>
     */
    public Map<String, Set<String>> getModelErrors()
    {
       return modelErrors;
    }
    
    
//    public static final class TextSortFieldComparator extends FieldComparator<String>
//    {
//
//        private final String[] values;
//
//        private String[] currentReaderValues;
//
//        private final String field;
//
//        final Collator collator;
//
//        private String bottom;
//        
//        private String top;
//
//        TextSortFieldComparator(int numHits, String field, Locale locale)
//        {
//            values = new String[numHits];
//            this.field = field;
//            collator = Collator.getInstance(locale);
//        }
//
//        /*
//         * (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparator#compare(int, int)
//         */
//        @Override
//        public int compare(int slot1, int slot2)
//        {
//            final String val1 = values[slot1];
//            final String val2 = values[slot2];
//            if (val1 == null)
//            {
//                if (val2 == null)
//                {
//                    return 0;
//                }
//                return -1;
//            }
//            else if (val2 == null)
//            {
//                return 1;
//            }
//            return collator.compare(val1, val2);
//        }
//
//        /*
//         * (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparator#compareBottom(int)
//         */
//        @Override
//        public int compareBottom(int doc)
//        {
//            final String val2 = stripLocale(currentReaderValues[doc]);
//            if (bottom == null)
//            {
//                if (val2 == null)
//                {
//                    return 0;
//                }
//                return -1;
//            }
//            else if (val2 == null)
//            {
//                return 1;
//            }
//            return collator.compare(bottom, val2);
//        }
//
//        /*
//         * (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparator#copy(int, int)
//         */
//        @Override
//        public void copy(int slot, int doc)
//        {
//            values[slot] = stripLocale(currentReaderValues[doc]);
//        }
//
////        public void setNextReader(IndexReader reader, int docBase) throws IOException
////        {
////            currentReaderValues = FieldCache.DEFAULT.getStrings(reader, field);
////        }
//
//        /*
//         * (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparator#setBottom(int)
//         */
//        @Override
//        public void setBottom(final int bottom)
//        {
//            this.bottom = values[bottom];
//        }
//
////        public Comparable value(int slot)
////        {
////            return values[slot];
////        }
//
//        private String stripLocale(String withLocale)
//        {
//            if (withLocale == null)
//            {
//                return withLocale;
//            }
//            else if (withLocale.startsWith("\u0000"))
//            {
//                return withLocale.substring(withLocale.indexOf('\u0000', 1) + 1);
//            }
//            else
//            {
//                return withLocale;
//            }
//        }
//
//        /* (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparator#setTopValue(java.lang.Object)
//         */
//        @Override
//        public void setTopValue(String value)
//        {
//           this.top = value;
//            
//        }
//
//        /* (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparator#compareTop(int)
//         */
//        @Override
//        public int compareTop(int doc) throws IOException
//        {
//            final String val2 = stripLocale(currentReaderValues[doc]);
//            if (top == null)
//            {
//                if (val2 == null)
//                {
//                    return 0;
//                }
//                return -1;
//            }
//            else if (val2 == null)
//            {
//                return 1;
//            }
//            return collator.compare(top, val2);
//        }
//
//        /* (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparator#setNextReader(org.apache.lucene.index.AtomicReaderContext)
//         */
//        @Override
//        public FieldComparator setNextReader(AtomicReaderContext context) throws IOException
//        {
//            currentReaderValues = FieldCache.DEFAULT.getTerms(reader, field, setDocsWithField)getStrings(context, field);
//        }
//    }

//    public static class MLTextSortFieldComparatorSource extends FieldComparatorSource
//    {
//
//        /*
//         * (non-Javadoc)
//         * @see org.apache.lucene.search.FieldComparatorSource#newComparator(java.lang.String, int, int, boolean)
//         */
//        @Override
//        public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException
//        {
//            return new MLTextSortFieldComparator(numHits, fieldname, I18NUtil.getLocale());
//        }
//
//    }

//    public static final class MLTextSortFieldComparator extends FieldComparator
//    {
//
//        private final String[] values;
//
//        private String[] currentReaderValues;
//
//        private final String field;
//
//        final Collator collator;
//
//        private String bottom;
//
//        Locale collatorLocale;
//
//        MLTextSortFieldComparator(int numHits, String field, Locale collatorLocale)
//        {
//            values = new String[numHits];
//            this.field = field;
//            this.collatorLocale = collatorLocale;
//            collator = Collator.getInstance(collatorLocale);
//        }
//
//        public int compare(int slot1, int slot2)
//        {
//            final String val1 = values[slot1];
//            final String val2 = values[slot2];
//            if (val1 == null)
//            {
//                if (val2 == null)
//                {
//                    return 0;
//                }
//                return -1;
//            }
//            else if (val2 == null)
//            {
//                return 1;
//            }
//            return collator.compare(val1, val2);
//        }
//
//        public int compareBottom(int doc)
//        {
//            final String val2 = findBestValue(currentReaderValues[doc]);
//            if (bottom == null)
//            {
//                if (val2 == null)
//                {
//                    return 0;
//                }
//                return -1;
//            }
//            else if (val2 == null)
//            {
//                return 1;
//            }
//            return collator.compare(bottom, val2);
//        }
//
//        public void copy(int slot, int doc)
//        {
//            values[slot] = findBestValue(currentReaderValues[doc]);
//        }
//
//        public void setNextReader(IndexReader reader, int docBase) throws IOException
//        {
//            currentReaderValues = FieldCache.DEFAULT.getStrings(reader, field);
//        }
//
//        public void setBottom(final int bottom)
//        {
//            this.bottom = values[bottom];
//        }
//
//        public Comparable value(int slot)
//        {
//            return values[slot];
//        }
//
//        private String findBestValue(String withLocale)
//        {
//            // split strin into MLText object
//            if (withLocale == null)
//            {
//                return withLocale;
//            }
//            else if (withLocale.startsWith("\u0000"))
//            {
//                MLText mlText = new MLText();
//                String[] parts = withLocale.split("\u0000");
//                for (int i = 0; (i + 2) <= parts.length; i += 3)
//                {
//                    Locale locale = null;
//                    String[] localeParts = parts[i + 1].split("_");
//                    if (localeParts.length == 1)
//                    {
//                        locale = new Locale(localeParts[0]);
//                    }
//                    else if (localeParts.length == 2)
//                    {
//                        locale = new Locale(localeParts[0], localeParts[1]);
//                    }
//                    else if (localeParts.length == 3)
//                    {
//                        locale = new Locale(localeParts[0], localeParts[1], localeParts[2]);
//                    }
//                    if (locale != null)
//                    {
//                        if (i + 2 == parts.length)
//                        {
//                            mlText.addValue(locale, "");
//                        }
//                        else
//                        {
//                            mlText.addValue(locale, parts[i + 2]);
//                        }
//                    }
//                }
//                return mlText.getClosestValue(collatorLocale);
//            }
//            else
//            {
//                return withLocale;
//            }
//        }
//    }

   
    public static class IndexedField
    {

        private List<FieldInstance> fields = new LinkedList<>();

        public IndexedField()
        {
            super();
        }
        
        public IndexedField(String prefix, boolean localised, boolean sort)
        {
            this();
            addField(prefix, localised, sort);
        }
        
        public List<FieldInstance> getFields()
        {
            return fields;
        }

        public void addField(String prefix, boolean localised, boolean sort)
        {
            fields.add(new FieldInstance(prefix, localised, sort));
        }
        
    }
    
    public static class FieldInstance
    {
        String field;
        boolean localised;
        boolean sort;
        
        /**
         * @param field String
         * @param localised boolean
         * @param sort boolean
         */
        public FieldInstance(String field, boolean localised, boolean sort)
        {
            this.field = field;
            this.localised = localised;
            this.sort = sort;
        }

        public String getField()
        {
            return field;
        }
        
        public boolean isLocalised()
        {
            return localised;
        }

        public boolean isSort()
        {
            return sort;
        }   
    }

    /**
     *
     * @param mode CMISQueryMode
     * @param searchParametersAndFilter Pair<SearchParameters, Boolean>
     * @param req SolrQueryRequest
     * @param queryModelQuery Query
     * @param cmisVersion CmisVersion
     * @param alternativeDictionary String
     * @return Query
     * @throws ParseException
     */
     public Query getCMISQuery(CMISQueryMode mode, Pair<SearchParameters, Boolean> searchParametersAndFilter, SolrQueryRequest req, org.alfresco.repo.search.impl.querymodel.Query queryModelQuery, CmisVersion cmisVersion, String alternativeDictionary) throws ParseException
    {
        SearchParameters searchParameters = searchParametersAndFilter.getFirst();
        Boolean isFilter = searchParametersAndFilter.getSecond();
        
        CmisFunctionEvaluationContext functionContext = getCMISFunctionEvaluationContext(mode, cmisVersion, alternativeDictionary);

        Set<String> selectorGroup = queryModelQuery.getSource().getSelectorGroups(functionContext).get(0);

        LuceneQueryBuilderContext<Query, Sort, ParseException> luceneContext = getLuceneQueryBuilderContext(searchParameters, req, alternativeDictionary, FTSQueryParser.RerankPhase.SINGLE_PASS);
        @SuppressWarnings("unchecked")
        LuceneQueryBuilder<Query, Sort, ParseException> builder = (LuceneQueryBuilder<Query, Sort, ParseException>) queryModelQuery;
        org.apache.lucene.search.Query luceneQuery = builder.buildQuery(selectorGroup, luceneContext, functionContext);

        ContextAwareQuery contextAwareQuery = new ContextAwareQuery(luceneQuery, Boolean.TRUE.equals(isFilter) ? null : searchParameters);
        return contextAwareQuery;
    }
     
     public LuceneQueryBuilderContext<Query, Sort, ParseException> getLuceneQueryBuilderContext(SearchParameters searchParameters, SolrQueryRequest req, String alternativeDictionary, FTSQueryParser.RerankPhase rerankPhase)
     {
         Lucene4QueryBuilderContextSolrImpl luceneContext = new Lucene4QueryBuilderContextSolrImpl(getDictionaryService(alternativeDictionary), namespaceDAO, tenantService, searchParameters,
                 MLAnalysisMode.EXACT_LANGUAGE, req, this, rerankPhase);
         return luceneContext;
     }

     public Solr4QueryParser getLuceneQueryParser(SearchParameters searchParameters, SolrQueryRequest req, FTSQueryParser.RerankPhase rerankPhase)
     {
         Analyzer analyzer =  req.getSchema().getQueryAnalyzer();
         Solr4QueryParser parser = new Solr4QueryParser(req, Version.LUCENE_5_5_0, searchParameters.getDefaultFieldName(), analyzer, rerankPhase);
         parser.setNamespacePrefixResolver(namespaceDAO);
         parser.setDictionaryService(getDictionaryService(CMISStrictDictionaryService.DEFAULT));
         parser.setTenantService(tenantService);
         parser.setSearchParameters(searchParameters);
         parser.setAllowLeadingWildcard(true);
         
         Properties props = new CoreDescriptorDecorator(req.getCore().getCoreDescriptor()).getProperties();
         int topTermSpanRewriteLimit = Integer.parseInt(props.getProperty("alfresco.topTermSpanRewriteLimit", "1000"));
         parser.setTopTermSpanRewriteLimit(topTermSpanRewriteLimit);

         return parser;
     }

    /**
     * @param searchParametersAndFilter Pair<SearchParameters, Boolean>
     * @param req SolrQueryRequest
     * @return Query
     * @throws ParseException
     */
     public Query getFTSQuery(Pair<SearchParameters, Boolean> searchParametersAndFilter, SolrQueryRequest req, FTSQueryParser.RerankPhase rerankPhase) throws ParseException
     {

         SearchParameters searchParameters = searchParametersAndFilter.getFirst();
         Boolean isFilter = searchParametersAndFilter.getSecond();

         QueryModelFactory factory = new LuceneQueryModelFactory<Query, Sort, SyntaxError>();
         AlfrescoFunctionEvaluationContext functionContext = new AlfrescoSolr4FunctionEvaluationContext(namespaceDAO, getDictionaryService(CMISStrictDictionaryService.DEFAULT), NamespaceService.CONTENT_MODEL_1_0_URI, req.getSchema());

         FTSParser.Mode mode;

         if (searchParameters.getDefaultFTSOperator() == org.alfresco.service.cmr.search.SearchParameters.Operator.AND)
         {
             mode = FTSParser.Mode.DEFAULT_CONJUNCTION;
         }
         else
         {
             mode = FTSParser.Mode.DEFAULT_DISJUNCTION;
         }

         Constraint constraint = FTSQueryParser.buildFTS(searchParameters.getQuery(), factory, functionContext, null, null, mode,
                 searchParameters.getDefaultFTSOperator() == org.alfresco.service.cmr.search.SearchParameters.Operator.OR ? Connective.OR : Connective.AND,
                         searchParameters.getQueryTemplates(), searchParameters.getDefaultFieldName(), rerankPhase);
         org.alfresco.repo.search.impl.querymodel.Query queryModelQuery = factory.createQuery(null, null, constraint, new ArrayList<Ordering>());

         @SuppressWarnings("unchecked")
         LuceneQueryBuilder<Query, Sort, ParseException> builder = (LuceneQueryBuilder<Query, Sort, ParseException>) queryModelQuery;

         LuceneQueryBuilderContext<Query, Sort, ParseException> luceneContext = getLuceneQueryBuilderContext(searchParameters, req, CMISStrictDictionaryService.DEFAULT, rerankPhase);

         Set<String> selectorGroup = null;
         if (queryModelQuery.getSource() != null)
         {
             List<Set<String>> selectorGroups = queryModelQuery.getSource().getSelectorGroups(functionContext);

             if (selectorGroups.size() == 0)
             {
                 throw new UnsupportedOperationException("No selectors");
             }

             if (selectorGroups.size() > 1)
             {
                 throw new UnsupportedOperationException("Advanced join is not supported");
             }

             selectorGroup = selectorGroups.get(0);
         }
         Query luceneQuery = builder.buildQuery(selectorGroup, luceneContext, functionContext);
         // query needs some search parameters fro correct caching ....

         ContextAwareQuery contextAwareQuery = new ContextAwareQuery(luceneQuery, Boolean.TRUE.equals(isFilter) ? null : searchParameters);
         return contextAwareQuery;
     }
     
    /**
     *
     * @param potentialProperty String
     * @param fieldUse FieldUse
     * @param req SolrQueryRequest
     * @return String
     */
     public String  mapProperty(String  potentialProperty,  FieldUse fieldUse, SolrQueryRequest req)
     {
    	 return mapProperty(potentialProperty, fieldUse, req, 0);
     }
     
     
     public String  mapProperty(String  potentialProperty,  FieldUse fieldUse, SolrQueryRequest req, int position)
     {
         if(potentialProperty.equals("asc") || potentialProperty.equals("desc") || potentialProperty.equals("_docid_"))
         {
             return potentialProperty;
         }
         
         if(potentialProperty.equalsIgnoreCase("score") || potentialProperty.equalsIgnoreCase("SEARCH_SCORE"))
         {
             return "score";
         }
         
         if(req.getSchema().getFieldOrNull(potentialProperty) != null)
         {
        	 return mapNonPropertyFields(potentialProperty);
         }
         
         AlfrescoFunctionEvaluationContext functionContext = new AlfrescoSolr4FunctionEvaluationContext(getNamespaceDAO(),  getDictionaryService(CMISStrictDictionaryService.DEFAULT), NamespaceService.CONTENT_MODEL_1_0_URI, req.getSchema());

         Pair<String, String> fieldNameAndEnding = QueryParserUtils.extractFieldNameAndEnding(potentialProperty);
         String luceneField =  functionContext.getLuceneFieldName(fieldNameAndEnding.getFirst());

         
         PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition(NamespaceService.CONTENT_MODEL_1_0_URI, getNamespaceDAO(), getDictionaryService(CMISStrictDictionaryService.DEFAULT), fieldNameAndEnding.getFirst());
         
         String solrSortField = null;
         if(propertyDef != null)
         {

             IndexedField fields = AlfrescoSolrDataModel.getInstance().getQueryableFields(propertyDef.getName(), getTextField(fieldNameAndEnding.getSecond()), fieldUse);
             if(fields.getFields().size() > 0)
             {
            	 if(fields.getFields().size() > position)
            	 {
            		 solrSortField = fields.getFields().get(position).getField();
            	 }
            	 else
            	 {
                     solrSortField = fields.getFields().get(0).getField();
            	 }
             }
             else
             {
                 solrSortField = mapNonPropertyFields(luceneField);
             }
         }
         else
         {
             solrSortField = mapNonPropertyFields(luceneField);
         }
         return solrSortField;
       
     }
     /**
      * @param queryField String
      * @return String
      */
     public String mapNonPropertyFields(String queryField)
     {
         switch(queryField)
         {
         case "ID":
             return "LID";
         case "EXACTTYPE":
             return "TYPE";
         default:
             return queryField;
                   
         }
     }

     /**
      * @param ending String
      * @return ContentFieldType
      */
     public ContentFieldType getTextField(String ending)
     {
         switch(ending)
         {
         case FIELD_MIMETYPE_SUFFIX:
             return ContentFieldType.MIMETYPE;
         case FIELD_SIZE_SUFFIX:
             return ContentFieldType.SIZE;
         case FIELD_LOCALE_SUFFIX:
             return ContentFieldType.LOCALE;
         case FIELD_ENCODING_SUFFIX:
             return ContentFieldType.ENCODING;
         case FIELD_TRANSFORMATION_STATUS_SUFFIX:
         case FIELD_TRANSFORMATION_TIME_SUFFIX:
         case FIELD_TRANSFORMATION_EXCEPTION_SUFFIX:
         default:
             return null;
                 
         }
         
     }

     public void setCMDefaultUri()
     {
         if(getNamespaceDAO().getURIs().contains(NamespaceService.CONTENT_MODEL_1_0_URI))
         {
             getNamespaceDAO().addPrefix("", NamespaceService.CONTENT_MODEL_1_0_URI);
         }
     }
     
}
