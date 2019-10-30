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
package org.alfresco.solr.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBiMap;

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.alfresco.solr.query.MimetypeGroupingQParserPlugin;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Update facet parameters to reference Solr fields rather than ACS properties.
 *
 * @author Andy
 */
public class RewriteFacetParametersComponent extends SearchComponent
{
    private static final Logger log = LoggerFactory.getLogger(RewriteFacetParametersComponent.class);
    
    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
        SolrQueryRequest req = rb.req;
        SolrParams params = req.getParams();

        ModifiableSolrParams fixed = new ModifiableSolrParams();
        ModifiableSolrParams allParamsWithFix = new ModifiableSolrParams(params);
        fixFilterQueries(fixed, params, rb);
        fixFacetParams(fixed, params, rb);
        fixRows(fixed, params, rb);
        
        Set<String> fixedParameterNames = fixed.getParameterNames();
        for (String fixedParam : fixedParameterNames)
        {
            allParamsWithFix.set(fixedParam, fixed.getParams(fixedParam));
        }

        if (allParamsWithFix.get(CommonParams.SORT) != null)
        {
            allParamsWithFix.remove(CommonParams.RQ);
        }
        
        req.setParams(allParamsWithFix);
    }
    
    /**
     * Prevents users from requesting a large number of rows by
     * replacing an enormous row value with a maximum value that will
     * not cause a run time exception.
     * @param fixed
     * @param params
     * @param rb
     */
    private void fixRows(ModifiableSolrParams fixed, SolrParams params, ResponseBuilder rb)
    {
        String rows = params.get("rows");
        if(rows != null && !rows.isEmpty())
        {
            Integer row = new Integer(rows);
            // Avoid +1 in SOLR code which produces null:java.lang.NegativeArraySizeException at at org.apache.lucene.util.PriorityQueue.<init>(PriorityQueue.java:56)
            if(row >  1000000)
            {
                fixed.remove("rows");
                fixed.add("rows", "1000000");
            }
        }
    }
    private void fixFilterQueries(ModifiableSolrParams fixed, SolrParams params, ResponseBuilder rb)
    {
        for(Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); /**/)
        {
            String name = it.next();
            
            if(name.equals("fq"))
            {
                String[] values = params.getParams(name);
                if(values != null)
                {
                    String[] fixedValues = new String[values.length];
                    for(int i = 0; i < values.length; i++)
                    {
                        String value = values[i];
                        if(value.startsWith("{!"))
                        {
                            fixedValues[i] = value;
                        }
                        else
                        {
                            if(value.startsWith("contentSize():"))
                            {
                                value = "cm:content.size:" + removeQuotes(value.substring("contentSize():".length()));
                            }
                            else if(value.startsWith("mimetype():"))
                            {
                                value = removeQuotes(value.substring("mimetype():".length()));
                                ArrayList<String> expand = MimetypeGroupingQParserPlugin.getReverseMappings().get(value);
                                if(expand == null)
                                {
                                    value = "cm:content.mimetype:\""+value+"\"";
                                }
                                else
                                {
                                    StringBuilder builder = new StringBuilder();
                                    builder.append("cm:content.mimetype:(");
                                    for(int j = 0; j < expand.size(); j++)
                                    {
                                        if(j > 0)
                                        {
                                            builder.append(" OR ");
                                        }
                                        builder.append('"');
                                        builder.append(expand.get(j));
                                        builder.append('"');
                                    }
                                    builder.append(')');
                                    value = builder.toString();
                                    
                                }
                            }
                            fixedValues[i] = "{!afts}"+value;
                        }
                    }
                    fixed.add(name, fixedValues);
                }
                
            }
        }
    }

    private String removeQuotes(String quoted)
    {
        if(quoted.startsWith("\"") && quoted.endsWith("\""))
        {
            return quoted.substring(1, quoted.length()-1);
        }
        else
        {
            return quoted;
        }
    }


    /**
     * @param fixed ModifiableSolrParams
     * @param params SolrParams
     * @param rb ResponseBuilder
     * @return SolrParams
     */
    private SolrParams fixFacetParams(ModifiableSolrParams fixed, SolrParams params, ResponseBuilder rb)
    {
        // Use BiMaps so that these can be quickly inverted in RewriteFacetCountsComponent.
        Map<String, String> fieldMappings = HashBiMap.create();
        Map<String, String> dateMappings = HashBiMap.create();
        Map<String, String> rangeMappings = HashBiMap.create();
        Map<String, String> pivotMappings = HashBiMap.create();
        Map<String, String> intervalMappings = HashBiMap.create();
        
        Map<String, String> statsFieldMappings = HashBiMap.create();
        Map<String, String> statsFacetMappings = HashBiMap.create();
       
        Map<String, String> functionMappings = HashBiMap.create();
        
        rewriteFacetFieldList(fixed, params, "facet.field", fieldMappings, rb.req);
        rewriteFacetFieldList(fixed, params, "facet.date", dateMappings, rb.req);
        rewriteFacetFieldList(fixed, params, "facet.range", rangeMappings, rb.req);
        rewriteFacetFieldList(fixed, params, "facet.pivot", pivotMappings, rb.req);
        rewriteFacetFieldList(fixed, params, "facet.interval", intervalMappings, rb.req);

        rewriteFacetFieldList(fixed, params, "stats.field", statsFieldMappings, rb.req);
        rewriteFacetFieldList(fixed, params, "stats.facet", statsFacetMappings, rb.req);
        
        mapFacetFunctions(fixed, params, "facet.field", functionMappings);
        
        rewriteFacetFieldOptions(fixed, params, "facet.prefix", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.contains", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.contains.ignoreCase", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.sort", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.limit", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.offset", fieldMappings);
        rewriteMincountFacetFieldOption(fixed, params, "facet.mincount", fieldMappings,rb.req);
        rewriteFacetFieldOptions(fixed, params, "facet.missing", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.method", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.enum.cache.minDF", fieldMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.enum.cache.minDF", fieldMappings);
        
        rewriteFacetFieldOptions(fixed, params, "facet.range.start", rangeMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.range.end", rangeMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.range.gap", rangeMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.range.hardend", rangeMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.range.include", rangeMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.range.other", rangeMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.range.method", rangeMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.range.limit", rangeMappings);

        rewriteMincountFacetFieldOption(fixed, params, "facet.pivot.mincount", pivotMappings, rb.req);
        rewriteFacetFieldOptions(fixed, params, "facet.sort", pivotMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.limit", pivotMappings);
        rewriteFacetFieldOptions(fixed, params, "facet.offset", pivotMappings);
        
        rewriteFacetFieldOptions(fixed, params, "facet.interval.set", intervalMappings);
        
       
        
        // TODO: 
        //    f.<stats_field>.stats.facet=<new Field> 
        //    would require a more complex rewrite  
        
       
        
        rb.rsp.add("_original_parameters_", params);
        rb.rsp.add("_field_mappings_", fieldMappings);
        rb.rsp.add("_date_mappings_", dateMappings);
        rb.rsp.add("_range_mappings_", rangeMappings);
        rb.rsp.add("_pivot_mappings_", pivotMappings);
        rb.rsp.add("_interval_mappings_", intervalMappings);
        rb.rsp.add("_stats_field_mappings_", statsFieldMappings);
        rb.rsp.add("_stats_facet_mappings_", statsFacetMappings);
        rb.rsp.add("_facet_function_mappings_", functionMappings);
        
        return fixed;
    }


    /**
     * Populate a map with the mappings from functions in the query to functions including the Solr filter handler.
     *
     * @param fixed The updated params object.
     * @param params The original params object.
     * @param paramName The name of the parameter (e.g. "facet.field").
     * @param facetFunctionMappings The map to update.
     */
    private void mapFacetFunctions(ModifiableSolrParams fixed, SolrParams params, String paramName, Map<String, String> facetFunctionMappings)
    {
        String[] facetFieldsOrig = params.getParams(paramName);
        if(facetFieldsOrig != null)
        {
            for(String facetFields : facetFieldsOrig)
            {
                String[] fields = facetFields.split(",");

                for(String field : fields)
                {
                    field = field.trim();
                    if(field.endsWith("()"))
                    {
                        if(isMimetypeAndHasFQ(params, field))
                        {
                            String function =  "{!" + field.substring(0, field.length()-2)+ " group=false }";
                            fixed.add("fq", function);
                            facetFunctionMappings.put(field,  function);
                        }
                        else
                        {
                            String function =  "{!" + field.substring(0, field.length()-2)+ " group=true}";
                            fixed.add("fq", function);
                            facetFunctionMappings.put(field,  function);
                        }
                    }
                }
            }   
        }
    }


    /**
     * @param params
     * @param field
     * @return
     */
    private boolean isMimetypeAndHasFQ(SolrParams params, String field)
    {
        if(!field.equals("mimetype()"))
        {
            return false;
        }
        else
        {
        	String[] filterQueries = params.getParams("fq");
        	if(filterQueries != null)
        	{
        		for(String fq : filterQueries)
        		{
        			if(fq.startsWith("mimetype():"))
        			{
        				return true;
        			}
        		}
        	}
        }
        return false;
    }

    /**
     * Ensure the mincount for all given facets is at least 1 to prevent exposing sensitive buckets to users without permission.
     *
     * @param fixed The updated params object.
     * @param params The original params object.
     * @param paramName The name of the mincount parameter to rewrite (e.g. "facet.mincount" or "facet.pivot.mincount").
     * @param fieldMappings A list of mappings from Alfresco property names to Solr field names.
     * @param req The Solr request
     */
    protected void rewriteMincountFacetFieldOption(ModifiableSolrParams fixed, SolrParams params, String paramName, Map<String, String> fieldMappings, SolrQueryRequest req)
    {
        rewriteFacetFieldOptions(fixed, params, paramName, fieldMappings);

        String shardPurpose = req.getParams().get(ShardParams.SHARDS_PURPOSE);
        boolean isInitialDistributedRequest = (shardPurpose==null);
        /*
        * After the initial Search request, in case of Sharding the Solr node will build additional http requests to the other shards.
        * These requests may have specific parameters rewritten by Solr internally.
        * It's not recommended to rewrite them, because some side effect may happen.
        * Only the initial request parameters may be rewritten.
        * */
        if(isInitialDistributedRequest)
        {
            // Update any existing mincount entries for facets.
            Map<String, Integer> updatedValues = new HashMap<>();
            for (Iterator<String> renamedParameters = fixed.getParameterNamesIterator(); renamedParameters.hasNext(); )
            {
                String solrParameter = renamedParameters.next();
                if (solrParameter.endsWith("." + paramName))
                {
                    // If mincount is set then renamedParameters must be an integer.
                    int value = Integer.valueOf(fixed.get(solrParameter));
                    // Don't directly edit the params while we're iterating through the entries.
                    updatedValues.put(solrParameter, Math.max(value, 1));
                }
            }
            // Now we've finished iterating, update the fixed parameters.
            for (Map.Entry<String, Integer> updatedParameterValues : updatedValues.entrySet())
            {
                fixed.set(updatedParameterValues.getKey(), updatedParameterValues.getValue());
            }

            String paramValue = params.get(paramName);
            int value = 0;
            if (paramValue != null)
            {
                value = Integer.valueOf(paramValue);
            }
            fixed.set(paramName, Math.max(value, 1));
        }
    }

    /**
     * Update any existing facet parameters to use the Solr field names.
     *
     * @param fixed The updated params object.
     * @param params The original params object.
     * @param paramName The name of the parameter to rewrite (e.g. "facet.limit" or "f.fieldName.facet.limit").
     * @param fieldMappings A list of mappings from Alfresco property names to Solr field names.
     */
    private void rewriteFacetFieldOptions(ModifiableSolrParams fixed, SolrParams params, String paramName, Map<String, String> fieldMappings)
    {
        for(Iterator<String> originalParamsIterator = params.getParameterNamesIterator(); originalParamsIterator.hasNext(); /**/)
        {
            String originalSolrParam = originalParamsIterator.next(); //it contains the alfresco field name
            if(originalSolrParam.startsWith("f."))
            {
                if(originalSolrParam.endsWith("."+paramName))
                {
                    String originalAlfrescoFieldName = originalSolrParam.substring(2, originalSolrParam.length() - paramName.length() - 1);
                    if(fieldMappings.containsKey(originalAlfrescoFieldName))
                    {
                        String solrFieldName = fieldMappings.get(originalAlfrescoFieldName);
                        fixed.set("f."+ solrFieldName +"."+paramName, params.getParams(originalSolrParam));
                    }
                    else
                    {
                        fixed.set(originalSolrParam, params.getParams(originalSolrParam));
                    }
                }
            }       
        }
    }

    /**
     * Tokenizes a string based on comma's except for the ones in single or double
     * qoutes.
     * @param line
     * @return 
     */
    public static String[] parseFacetField(String line)
    {
      if(StringUtils.isEmpty(line))
      {
          throw new RuntimeException("String input is requried");
      }
      String[] tokens = line.split(",(?=(?:[^'|\"]*\"[^'|\"]*\")*[^'|\"]*$)", -1);
      return tokens;
        
    }
    /**
     * Update the facet fields to use Solr field names rather than ACS property names.
     *
     * @param fixed The updated params object.
     * @param params The original params object.
     * @param paramName The name of the mincount parameter to rewrite (e.g. "facet.mincount" or "facet.pivot.mincount").
     * @param fieldMappings A list of mappings from Alfresco property names to Solr field names.
     * @param req The original request.
     * @return An array of the facet field names.
     */
    private List<String> rewriteFacetFieldList(ModifiableSolrParams fixed, SolrParams params, String paramName,
                Map<String, String> fieldMappings, SolrQueryRequest req)
    {
        String shardPurpose = req.getParams().get(ShardParams.SHARDS_PURPOSE);
        boolean isRefinementRequest = false;
        //Fix for https://issues.alfresco.com/jira/browse/MNT-21015
        if (shardPurpose != null) {
            int shardPurposeCode = Integer.parseInt(shardPurpose);
            log.debug("ShardPurpose: " + shardPurpose);
            isRefinementRequest = ((shardPurposeCode & ShardRequest.PURPOSE_REFINE_FACETS) != 0) || ((shardPurposeCode & ShardRequest.PURPOSE_REFINE_PIVOT_FACETS) != 0);
        }
        String[] facetFieldsOrig = params.getParams(paramName);
        List<String> facetFieldList = new ArrayList<>();
        if(facetFieldsOrig != null)
        {
            ArrayList<String> newFacetFields = new ArrayList<String>();
            for(String facetFields : facetFieldsOrig)
            {
                StringBuilder commaSeparated = new StringBuilder();
                StringBuilder mapping = new StringBuilder();
                StringBuilder unmapped = new StringBuilder();
               
                String[] fields = parseFacetField(facetFields);
                
                for(String field : fields)
                {
                	String prefix = "";
                    field = field.trim();
                    
                    if(field.endsWith("()"))
                    {
                        // skip facet functions 
                        continue;
                    }
                    
                    if(field.startsWith("{!") &&!(isRefinementRequest))
                    {
                    	int index = field.indexOf("}");
                    	if((index > 0) && (index < (field.length() - 1)))
                    	{
                    		prefix = field.substring(0, index+1);
                    		field = field.substring(index+1);
                    	}
                    }

                    boolean noMappingIsRequired = req.getSchema().getFieldOrNull(field) != null|| isRefinementRequest;
                    if(noMappingIsRequired)
                    {
                        if(commaSeparated.length() > 0)
                        {
                            commaSeparated.append(",");
                            mapping.append(",");
                            unmapped.append(",");
                        }
                        commaSeparated.append(prefix).append(field);
                        mapping.append(field);
                        unmapped.append(field);
                        facetFieldList.add(field);
                    }
                    else
                    {
                        String mappedField = AlfrescoSolrDataModel.getInstance().mapProperty(field, FieldUse.FACET, req);
                        
                        if(commaSeparated.length() > 0)
                        {
                            commaSeparated.append(",");
                            mapping.append(",");
                            unmapped.append(",");
                        }
                        commaSeparated.append(prefix).append(mappedField);
                        mapping.append(mappedField);
                        unmapped.append(field);
                        facetFieldList.add(mappedField);
                    }
                }
                if(!facetFields.equals(commaSeparated.toString()))
                {
                    fieldMappings.put(unmapped.toString(), mapping.toString());
                }
                if(commaSeparated.length() > 0)
                {
                    newFacetFields.add(commaSeparated.toString());
                }
            }
            fixed.set(paramName, newFacetFields.toArray(new String[newFacetFields.size()]));
        }

        return facetFieldList;
    }

    
    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#process(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void process(ResponseBuilder rb) throws IOException
    {
        
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getDescription()
     */
    @Override
    public String getDescription()
    {
        return "RewriteFacetParameters";
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getSource()
     */
    @Override
    public String getSource()
    {
        return "";
    }

}
