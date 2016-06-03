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
import java.util.Collection;
import java.util.HashMap;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;

/**
 * @author Andy
 *
 */
public class RewriteFacetCountsComponent extends SearchComponent
{

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#finishStage(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void finishStage(ResponseBuilder rb)
    {
        /// wait until after distributed faceting is done 
        if (!rb.doFacets || rb.stage != ResponseBuilder.STAGE_GET_FIELDS) 
        {
            return;
        }
        else 
        {
            process(rb);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
       // Nothing to do
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#process(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void process(ResponseBuilder rb)
    {
        // rewrite

        rewrite(rb, "_field_mappings_", "facet_counts", "facet_fields");
        rewrite(rb, "_date_mappings_", "facet_counts", "facet_dates");
        rewrite(rb, "_range_mappings_", "facet_counts", "facet_ranges");
        
        rewrite(rb, "_pivot_mappings_", "facet_counts", "facet_pivot");
        rewritePivotFields(rb, "facet_counts", "facet_pivot");
        // TODO: rewrite(rb, "_interval_mappings_", "facet_counts", "facet_fields");
        
        rewrite(rb, "_stats_field_mappings_", "stats", "stats_fields");
        
        copyAnalytics(rb, "facet_counts", "facet_fields");
        
        HashMap<String, String> mappings = (HashMap<String, String>)rb.rsp.getValues().get("_stats_field_mappings_");
        if(mappings != null)
        {
            for(String key : mappings.keySet())
            {
                rewrite(rb, "_stats_facet_mappings_", "stats", "stats_fields", key, "facets");
            }
        }
    }
    
    
    /**
     * @param rb
     */
    private void copyAnalytics(ResponseBuilder rb, String ... sections)
    {
        NamedList<Object>  found = (NamedList<Object>) rb.rsp.getValues();
        for(String section : sections)
        {
            found = (NamedList<Object>)found.get(section);
            if(found == null)
            {
                return;
            }
        }

        NamedList<Object>  analytics = (NamedList<Object>) rb.rsp.getValues();
        analytics =  (NamedList<Object>)analytics.get("analytics");
        if(analytics != null)
        {
            for(int i = 0; i < analytics.size(); i++)
            {
                String name = analytics.getName(i);
                Object value = analytics.getVal(i);
                found.add(name, value);
            }
        }
        
        



    }


    /**
     * @param rb
     */
    private void rewritePivotFields(ResponseBuilder rb, String ... sections)
    {
        HashMap<String, String> mappings = (HashMap<String, String>)rb.rsp.getValues().get("_pivot_mappings_");
        if(mappings != null)
        {
            NamedList<Object>  found = (NamedList<Object>) rb.rsp.getValues();
            for(String section : sections)
            {
                found = (NamedList<Object>)found.get(section);
                if(found == null)
                {
                    return;
                }
            }
            for(int i = 0; i < found.size(); i++)
            {
                String pivotName = found.getName(i);
                String[] fromParts = pivotName.split(",");
                String mapping = mappings.get(pivotName);
                String[] toParts = mapping != null ? mapping.split(",") : fromParts;
                Collection<NamedList<Object>> current = (Collection<NamedList<Object>>)found.getVal(i);
                processPivot(fromParts, toParts, current, 0);
            }
        }
        
    }
    
    private void processPivot(String[] fromParts, String[] toParts, Collection<NamedList<Object>> current, int level)
    {
        for(NamedList<Object> entry : current)
        {
            for(int i = 0; i < entry.size(); i++)
            {
                String name = entry.getName(i);
                if(name.equals("field"))
                {
                    entry.setVal(i, fromParts[level].trim());
                }
                else if(name.equals("pivot"))
                {
                    Collection<NamedList<Object>> pivot = (Collection<NamedList<Object>>)entry.getVal(i);
                    processPivot(fromParts, toParts, pivot, level+1);
                }
                else
                {
                    // leave alone
                }
            }
        }
    }

    /**
     * @param rb
     */
    private void rewrite(ResponseBuilder rb, String mappingName, String ... sections)
    {
        HashMap<String, String> mappings = (HashMap<String, String>)rb.rsp.getValues().get(mappingName);
        if(mappings != null)
        {
            HashMap<String, String> reverse = getReverseLookUp(mappings);
     
            NamedList<Object>  found = (NamedList<Object>) rb.rsp.getValues();
            for(String section : sections)
            {
                found = (NamedList<Object>)found.get(section);
                if(found == null)
                {
                    return;
                }
            }
            
            for(int i = 0; i < found.size(); i++)
            {
                String name = found.getName(i);
                String newName = reverse.get(name);
                if(newName != null)
                {
                    found.setName(i, newName);
                }
            }
            
          
        }
    }

    
    private HashMap<String, String> getReverseLookUp(HashMap<String, String> map)
    {
        HashMap<String, String> reverse = new HashMap<String, String>();
        for(String key : map.keySet())
        {
            String value = map.get(key);
            reverse.put(value,  key);
        }
        return reverse;
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getDescription()
     */
    @Override
    public String getDescription()
    {
        return "RewriteFacetCounts";
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
