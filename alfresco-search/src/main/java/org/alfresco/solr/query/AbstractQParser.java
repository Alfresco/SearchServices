/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.Operator;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.alfresco.util.Pair;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SortSpec;
import org.apache.solr.search.SortSpecParsing;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.update.processor.LangDetectLanguageIdentifierUpdateProcessor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.util.I18NUtil;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

/**
 * @author Andy
 */
public abstract class AbstractQParser extends QParser implements QueryConstants
{
    private static char[] SEPARATORS = new char[] { ':', ',', '-', '!', '+', '=', ';', '~', '/' };

    protected final static Logger log = LoggerFactory.getLogger(AbstractQParser.class);

    protected boolean authset;

    public static final String ALFRESCO_JSON = "ALFRESCO_JSON";

    private static final String AUTHORITY_FILTER_FROM_JSON = "AUTHORITY_FILTER_FROM_JSON";

    private static final String TENANT_FILTER_FROM_JSON = "TENANT_FILTER_FROM_JSON";
    
    private static final String RERANK_QUERY_FROM_CONTEXT = "RERANK_QUERY_FROM_CONTEXT";

    static final String languages[] = {
        "af", "ar", "bg", "bn", "cs", "da", "de", "el", "en", "es", "et", "fa", "fi", "fr", "gu",
        "he", "hi", "hr", "hu", "id", "it", "ja", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ne",
        "nl", "no", "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sv", "sw", "ta", "te",
        "th", "tl", "tr", "uk", "ur", "vi", "zh-cn", "zh-tw"
      };
    
    static 
    {
        try {
            List<String> profileData = new ArrayList<>();
            for (String language : languages) {
                InputStream stream = LangDetectLanguageIdentifierUpdateProcessor.class.getResourceAsStream("langdetect-profiles/" + language);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                profileData.add(new String(IOUtils.toCharArray(reader)));
                reader.close();
            }
            DetectorFactory.loadProfile(profileData);
            DetectorFactory.setSeed(0);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load profile data, will return empty languages always!", e);
        }
    }

	private boolean autoDetectQueryLocale = false;

	private HashSet<String> autoDetectQueryLocales = new HashSet<String>();
	
	private HashSet<String> fixedQueryLocales = new HashSet<String>();
    
    /**
     * @param qstr String
     * @param localParams SolrParams
     * @param params SolrParams
     * @param req SolrQueryRequest
     * @param args 
     */
    public AbstractQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, NamedList<Object> args)
    {
        super(qstr, localParams, params, req);
        if(args != null)
        {
        	Object arg = args.get("autoDetectQueryLocale");
        	if(arg != null)
        	{
        		this.autoDetectQueryLocale = Boolean.parseBoolean(arg.toString());
        	}
        	
        	arg = args.get("autoDetectQueryLocales");
        	if(arg != null)
        	{
        		String[] locales = arg.toString().split(",");
        		for(String locale : locales)
        		{
        			String mappedLanguage = isKnownLocale(locale);
        			if(mappedLanguage != null)
        			{
        				autoDetectQueryLocales.add(mappedLanguage);
        			}
        		}
        	}
        	
        	
        	arg = args.get("fixedQueryLocales");
        	if(arg != null)
        	{
        		String[] locales = arg.toString().split(",");
        		for(String locale : locales)
        		{
        			String mappedLanguage = isKnownLocale(locale);
        			if(mappedLanguage != null)
        			{
        				fixedQueryLocales.add(mappedLanguage);
        			}
        		}
        	}
        }
    }
    
    private String isKnownLocale(String locale)
    {
    	for(String test : languages)
    	{
    		if(locale.equalsIgnoreCase(test))
    		{
    			return test;
    		}
    	}
    	return null;
    }

    
    protected Pair<SearchParameters, Boolean> getSearchParameters()
    {
        SearchParameters searchParameters = new SearchParameters();
        
        Boolean isFilter = Boolean.FALSE;

        Iterable<ContentStream> streams = req.getContentStreams();

        JSONObject json = (JSONObject) req.getContext().get(ALFRESCO_JSON);

        if (json == null)
        {
            if (streams != null)
            {

                try
                {
                    Reader reader = null;
                    for (ContentStream stream : streams)
                    {
                        reader = new BufferedReader(new InputStreamReader(stream.getStream(), "UTF-8"));
                    }

                    // TODO - replace with streaming-based solution e.g. SimpleJSON ContentHandler
                    if (reader != null)
                    {
                        json = new JSONObject(new JSONTokener(reader));
                        req.getContext().put(ALFRESCO_JSON, json);
                    }
                }
                catch (JSONException e)
                {
                    // This is expected when there is no json element to the request
                }
                catch (IOException e)
                {
                    throw new AlfrescoRuntimeException("IO Error parsing query parameters", e);
                }
            }
        }

        if (json != null)
        {
            try
            {
                if (getString() != null)
                {
                    if (getString().equals(AUTHORITY_FILTER_FROM_JSON))
                    {
                        isFilter =Boolean.TRUE;
                        
                        ArrayList<String> tenantList = new ArrayList<String>(1);
                        JSONArray tenants = json.getJSONArray("tenants");
                        for (int i = 0; i < tenants.length(); i++)
                        {
                            String tenantString = tenants.getString(i);
                            tenantList.add(tenantString);
                        }

                        ArrayList<String> authorityList = new ArrayList<String>(1);
                        JSONArray authorities = json.getJSONArray("authorities");
                        for (int i = 0; i < authorities.length(); i++)
                        {
                            String authorityString = authorities.getString(i);
                            authorityList.add(authorityString);
                        }

                        char separator = getSeparator(authorityList);

                        StringBuilder authQuery = new StringBuilder();
                        StringBuilder denyQuery = new StringBuilder();

                        for (String tenant : tenantList)
                        {
                            for (String authority : authorityList)
                            {
                                if (separator == 0)
                                {
                                    if (authQuery.length() > 0)
                                    {
                                        authQuery.append(" ");
                                        denyQuery.append(" ");                                        
                                    }
                                    switch (AuthorityType.getAuthorityType(authority))
                                    {
                                    case USER:
                                        authQuery.append("|AUTHORITY:\"").append(authority).append("\"");
                                        denyQuery.append("|DENIED:\"").append(authority).append("\"");
                                        break;
                                    case GROUP:
                                    case EVERYONE:
                                    case GUEST:
                                        if (tenant.length() == 0)
                                        {
                                            // Default tenant matches 4.0
                                            authQuery.append("|AUTHORITY:\"").append(authority).append("\"");
                                            denyQuery.append("|DENIED:\"").append(authority).append("\"");
                                        }
                                        else
                                        {
                                            authQuery.append("|AUTHORITY:\"").append(authority).append("@").append(tenant).append("\"");
                                            denyQuery.append("|DENIED:\"").append(authority).append("@").append(tenant).append("\"");
                                        }
                                        break;
                                    default:
                                        authQuery.append("|AUTHORITY:\"").append(authority).append("\"");
                                        denyQuery.append("|DENIED:\"").append(authority).append("\"");
                                        break;
                                    }
                                }
                                else
                                {
                                    if(authQuery.length() == 0)
                                    {
                                        authset = true;
                                        authQuery.append("|AUTHSET:\"");
                                        denyQuery.append("|DENYSET:\"");
                                    }

                                    switch (AuthorityType.getAuthorityType(authority))
                                    {
                                    case USER:
                                        authQuery.append(separator).append(authority);
                                        denyQuery.append(separator).append(authority);
                                        break;
                                    case GROUP:
                                    case EVERYONE:
                                    case GUEST:
                                        if (tenant.length() == 0)
                                        {
                                            // Default tenant matches 4.0
                                            authQuery.append(separator).append(authority);
                                            denyQuery.append(separator).append(authority);
                                        }
                                        else
                                        {
                                            authQuery.append(separator).append(authority).append("@").append(tenant);
                                            denyQuery.append(separator).append(authority).append("@").append(tenant);
                                        }
                                        break;
                                    default:
                                        authQuery.append(separator).append(authority);
                                        denyQuery.append(separator).append(authority);
                                        break;
                                    }
                                }

                            }
                        }
                        if(separator != 0)
                        {
                            authQuery.append("\"");
                            denyQuery.append("\"");
                        }

                        if (authQuery.length() > 0)
                        {
                            // Default to true for safety reasons.
                            final boolean anyDenyDenies = json.optBoolean("anyDenyDenies", true);
                            
                            if (anyDenyDenies)
                            {
                                authQuery.insert(0, "(").
                                    append(") AND NOT (").
                                    append(denyQuery).
                                    append(")");
                                // Record that the clause has been added.
                                // We only ever set this to true for solr4+
                                req.getContext().put("processedDenies", Boolean.TRUE);
                            }
                            searchParameters.setQuery(authQuery.toString());
                        }
                    }
                    else if (getString().equals(TENANT_FILTER_FROM_JSON))
                    {
                        isFilter =Boolean.TRUE;
                        
                        ArrayList<String> tenantList = new ArrayList<String>(1);
                        JSONArray tenants = json.getJSONArray("tenants");
                        for (int i = 0; i < tenants.length(); i++)
                        {
                            String tenantString = tenants.getString(i);
                            tenantList.add(tenantString);
                        }

                        StringBuilder tenantQuery = new StringBuilder();
                        for (String tenant : tenantList)
                        {
                            if (tenantQuery.length() > 0)
                            {
                                tenantQuery.append(" ");
                            }

                            if (tenant.length() > 0)

                            {
                                tenantQuery.append("|TENANT:\"").append(tenant).append("\"");
                            }
                            else
                            {
                                // TODO: Need to check for the default tenant or no tenant (4.0) or we force a reindex
                                // requirement later ...
                                // Better to add default tenant to the 4.0 index
                                tenantQuery.append("|TENANT:\"").append("_DEFAULT_").append("\"");
                                // tenantQuery.append(" |(+ISNODE:T -TENANT:*)");
                            }

                        }
                        searchParameters.setQuery(tenantQuery.toString());
                    }
                    else if (getString().equals(RERANK_QUERY_FROM_CONTEXT))
                    {
                    	String searchTerm = getParam("spellcheck.q");
                    	searchParameters.setQuery(searchTerm);
                    }
                }
                else
                {
                    String query = json.getString("query");
                    if (query != null)
                    {
                        searchParameters.setQuery(query);
                    }
                }

                JSONArray locales = json.getJSONArray("locales");
                for (int i = 0; i < locales.length(); i++)
                {
                    String localeString = locales.getString(i);
                    Locale locale = DefaultTypeConverter.INSTANCE.convert(Locale.class, localeString);
                    searchParameters.addLocale(locale);
                }

                JSONArray templates = json.getJSONArray("templates");
                for (int i = 0; i < templates.length(); i++)
                {
                    JSONObject template = templates.getJSONObject(i);
                    String name = template.getString("name");
                    String queryTemplate = template.getString("template");
                    searchParameters.addQueryTemplate(name, queryTemplate);
                }

                JSONArray allAttributes = json.getJSONArray("allAttributes");
                for (int i = 0; i < allAttributes.length(); i++)
                {
                    String allAttribute = allAttributes.getString(i);
                    searchParameters.addAllAttribute(allAttribute);
                }

                searchParameters.setDefaultFTSOperator(Operator.valueOf(json.getString("defaultFTSOperator")));
                searchParameters.setDefaultFTSFieldConnective(Operator.valueOf(json.getString("defaultFTSFieldOperator")));
                if (json.has("mlAnalaysisMode"))
                {
                    searchParameters.setMlAnalaysisMode(MLAnalysisMode.valueOf(json.getString("mlAnalaysisMode")));
                }
                searchParameters.setNamespace(json.getString("defaultNamespace"));

                JSONArray textAttributes = json.getJSONArray("textAttributes");
                for (int i = 0; i < textAttributes.length(); i++)
                {
                    String textAttribute = textAttributes.getString(i);
                    searchParameters.addAllAttribute(textAttribute);
                }
                
                
                searchParameters.setQueryConsistency(QueryConsistency.valueOf(json.getString("queryConsistency")));

            }
            catch (JSONException e)
            {
                // This is expected when there is no json element to the request
            }
        }

        if (json != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(json.toString());
            }
        }

        if (searchParameters.getQuery() == null)
        {
            searchParameters.setQuery(getString());
        }

        if (searchParameters.getLocales().size() == 0)
        {
            searchParameters.addLocale(I18NUtil.getLocale());
        }

        String defaultField = getParam(CommonParams.DF);
        if (defaultField != null)
        {
            searchParameters.setDefaultFieldName(defaultField);
        }

        if(autoDetectQueryLocale)
        {
        	String searchTerm = getParam("spellcheck.q");
        	if (searchTerm != null)
        	{
        		searchParameters.setSearchTerm(searchTerm);
        		List<DetectedLanguage> detetcted = detectLanguage(searchTerm);
        		if((detetcted != null) && (detetcted.size() > 0))
        		{
        			Locale detectedLocale = Locale.forLanguageTag(detetcted.get(0).getLangCode());
        			if(localeIsNotIncluded(searchParameters, detectedLocale))
        			{
        				searchParameters.addLocale( Locale.forLanguageTag(detectedLocale.getLanguage()));
        			}
        		}

        	}
        }
        
        if(fixedQueryLocales.size() > 0)
        {
        	for(String locale : fixedQueryLocales)
        	{
        		searchParameters.addLocale( Locale.forLanguageTag(locale));
        	}
        }
        
        // searchParameters.setMlAnalaysisMode(getMLAnalysisMode());
        searchParameters.setNamespace(NamespaceService.CONTENT_MODEL_1_0_URI);

        return new Pair<SearchParameters, Boolean>(searchParameters, isFilter);
    }

    /**
     * @param searchParameters SearchParameters
     * @param detectedLocale Locale
     * @return boolean
     */
    private boolean localeIsNotIncluded(SearchParameters searchParameters, Locale detectedLocale)
    {
        for(Locale locale : searchParameters.getLocales())
        {
            if(locale.getLanguage().equals(detectedLocale.getLanguage()))
            {
                return false;
            }
        }
        return true;
    }


    /**
     * @param authorityList ArrayList<String>
     * @return char
     */
    private char getSeparator(ArrayList<String> authorityList)
    {
        StringBuilder builder = new StringBuilder();
        for(String auth : authorityList)
        {
            builder.append(auth);
        }
       String test = builder.toString();
       
       for(int i = 0; i < SEPARATORS.length; i++)
       {
           if(test.indexOf(SEPARATORS[i]) == -1)
           {
               return SEPARATORS[i];
           }
       }
       return 0;
    }

    /* (non-Javadoc)
     * @see org.apache.solr.search.QParser#getSort(boolean)
     */
    @Override
    public SortSpec getSortSpec(boolean useGlobalParams) throws SyntaxError
    {
        
        getQuery(); // ensure query is parsed first

        String sortStr = null;
        String startS = null;
        String rowsS = null;

        if (localParams != null) {
          sortStr = localParams.get(CommonParams.SORT);
          startS = localParams.get(CommonParams.START);
          rowsS = localParams.get(CommonParams.ROWS);

          // if any of these parameters are present, don't go back to the global params
          if (sortStr != null || startS != null || rowsS != null) {
            useGlobalParams = false;
          }
        }

        if (useGlobalParams) {
          if (sortStr ==null) {
              sortStr = params.get(CommonParams.SORT);
          }
          if (startS==null) {
            startS = params.get(CommonParams.START);
          }
          if (rowsS==null) {
            rowsS = params.get(CommonParams.ROWS);
          }
        }

        int start = startS != null ? Integer.parseInt(startS) : 0;
        int rows = rowsS != null ? Integer.parseInt(rowsS) : 10;

        // Fix sort fields here
        if(sortStr != null)
        {
            StringBuilder builder = new StringBuilder();
            StringBuilder propertyBuilder = null;
            char c;
            for(int i = 0; i < sortStr.length(); i++)
            {
                c = sortStr.charAt(i);
                if(propertyBuilder == null)
                {
                    if(!Character.isWhitespace(c) && (c != ','))
                    {
                        propertyBuilder = new StringBuilder();
                        propertyBuilder.append(c);
                    }
                    else
                    {
                        builder.append(c);
                    }
                }
                else
                {
                    if(Character.isWhitespace(c) || (c == ','))
                    {
                        String toAppend = AlfrescoSolrDataModel.getInstance().mapProperty(propertyBuilder.toString(), FieldUse.SORT, getReq());
                        builder.append(toAppend);
                        builder.append(c);
                        propertyBuilder = null;
                    }
                    else
                    {
                        propertyBuilder.append(c);
                    }
                }
            }
            if(propertyBuilder != null)
            {
                String toAppend =  AlfrescoSolrDataModel.getInstance().mapProperty(propertyBuilder.toString(), FieldUse.SORT, getReq());
                builder.append(toAppend);
            }
            sortStr = builder.toString();
        }
        
        if(sortStr != null)
        {
            sortStr = sortStr.replaceAll("^ID(\\s)", "id$1");
            sortStr = sortStr.replaceAll("(\\s)ID(\\s)", "$1id$2");
        }
        SortSpec sort = SortSpecParsing.parseSortSpec(sortStr, req);

        sort.setOffset(start);
        sort.setCount(rows);
        return sort;
    }

 
    
    private List<DetectedLanguage> detectLanguage(String content) {
    	if (content.trim().length() == 0) { // to be consistent with the tika impl?
    		log.debug("No input text to detect language from, returning empty list");
    		return Collections.emptyList();
    	}

    	try {
    		Detector detector = DetectorFactory.create();
    		detector.append(content);
    		ArrayList<Language> langlist = detector.getProbabilities();
    		ArrayList<DetectedLanguage> solrLangList = new ArrayList<>();
    		for (Language l: langlist) 
    		{
    			if((autoDetectQueryLocales.size() == 0) || (autoDetectQueryLocales.contains(l.lang)))
    			{
    			    solrLangList.add(new DetectedLanguage(l.lang, l.prob));
    			}
    		}
    		return solrLangList;
    	} catch (LangDetectException e) {
    		log.debug("Could not determine language, returning empty list: ", e);
    		return Collections.emptyList();
    	}
    }

    public class DetectedLanguage {
        private final String langCode;
        private final Double certainty;

        DetectedLanguage(String lang, Double certainty) {
            this.langCode = lang;
            this.certainty = certainty;
        }

        /**
         * Returns the detected language code
         * @return language code as a string
         */
        public String getLangCode() {
            return langCode;
        }

        /**
         * Returns the detected certainty for this language
         * @return certainty as a value between 0.0 and 1.0 where 1.0 is 100% certain
         */
        public Double getCertainty() {
            return certainty;
        }
    }
}
