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

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.alfresco.solr.query.MimetypeGroupingQParserPlugin;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;


/**
 * @author Andy
 *
 */
public class RewriteFacetParametersComponent extends SearchComponent
{

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
         SolrQueryRequest req = rb.req;
         SolrParams params = req.getParams();
         
         ModifiableSolrParams fixed = new ModifiableSolrParams();
         fixFilterQueries(fixed, params, rb);
         fixFacetParams(fixed, params, rb);
         copyOtherQueryParams(fixed, params);
         fixRows(fixed, params, rb);
         
         if(fixed.get(CommonParams.SORT) != null)
         {
             fixed.remove(CommonParams.RQ);
         }
         
         req.setParams(fixed);
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
        HashMap<String, String> fieldMappings = new HashMap<>();
        HashMap<String, String> dateMappings = new HashMap<>();
        HashMap<String, String> rangeMappings = new HashMap<>();
        HashMap<String, String> pivotMappings = new HashMap<>();
        HashMap<String, String> intervalMappings = new HashMap<>();
        
        HashMap<String, String> statsFieldMappings = new HashMap<>();
        HashMap<String, String> statsFacetMappings = new HashMap<>();
       
        HashMap<String, String> functionMappings = new HashMap<>();
        
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
        rewriteFacetFieldOptions(fixed, params, "facet.mincount", fieldMappings);
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
      
        rewriteFacetFieldOptions(fixed, params, "facet.pivot.mincount", pivotMappings);
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
     * @param fixed ModifiableSolrParams
     * @param params SolrParams
     * @param string
     * @param fieldMappings
     */
    private void mapFacetFunctions(ModifiableSolrParams fixed, SolrParams params, String string, HashMap<String, String> facetFunctionMappings)
    {
        
        String[] facetFieldsOrig = params.getParams(string);
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
     * @param fixed
     * @param params
     */
    private void copyOtherQueryParams(ModifiableSolrParams fixed, SolrParams params)
    {
        for(Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); /**/)
        {
            String name = it.next();
            if(name.equals("fq") || name.startsWith("f.") || name.equals("facet.field") || name.equals("facet.date") || name.equals("facet.range") || name.equals("facet.pivot") || name.equals("facet.interval")|| name.startsWith("stats."))
            {
                // Already done 
                continue;
            }    
            else
            {
                fixed.set(name, params.getParams(name));
            }
        }
    }

    /**
     * @param fixed ModifiableSolrParams
     * @param params SolrParams
     * @param paramName String
     * @param fieldMappings HashMap<String, String>
     */
    private void rewriteFacetFieldOptions(ModifiableSolrParams fixed, SolrParams params, String paramName, HashMap<String, String> fieldMappings)
    {
        for(Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); /**/)
        {
            String name = it.next();
            if(name.startsWith("f."))
            {
                if(name.endsWith("."+paramName))
                {

                    String source = name.substring(2, name.length() - paramName.length() - 1);
                    if(fieldMappings.containsKey(source))
                    {
                        fixed.set("f."+fieldMappings.get(source)+"."+paramName, params.getParams(name));
                    }
                    else
                    {
                        fixed.set(name, params.getParams(name));
                    }


                }
                else
                {
                    fixed.set(name, params.getParams(name));
                }
            }       
        }
    }


    /**
     * @param fixed ModifiableSolrParams
     * @param params SolrParams
     * @param paramName String
     * @param fieldMappings HashMap<String, String>
     * @param req SolrQueryRequest
     */
    private void rewriteFacetFieldList(ModifiableSolrParams fixed, SolrParams params, String paramName, HashMap<String, String> fieldMappings, SolrQueryRequest req)
    {
        String[] facetFieldsOrig = params.getParams(paramName);
        if(facetFieldsOrig != null)
        {
            ArrayList<String> newFacetFields = new ArrayList<String>();
            for(String facetFields : facetFieldsOrig)
            {
                StringBuilder commaSeparated = new StringBuilder();
                StringBuilder mapping = new StringBuilder();
                StringBuilder unmapped = new StringBuilder();
                String[] fields = facetFields.split(",");
                
                for(String field : fields)
                {
                	String prefix = "";
                    field = field.trim();
                    
                    if(field.endsWith("()"))
                    {
                        // skip facet functions 
                        continue;
                    }
                    
                    if(field.startsWith("{!afts"))
                    {
                    	int index = field.indexOf("}");
                    	if((index > 0) && (index < (field.length() - 1)))
                    	{
                    		prefix = field.substring(0, index+1);
                    		field = field.substring(index+1);
                    	}
                    	
                    }
                    
                    if(req.getSchema().getFieldOrNull(field) != null)
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
            fixed.set(paramName,  newFacetFields.toArray(new String[newFacetFields.size()]));
        }
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
