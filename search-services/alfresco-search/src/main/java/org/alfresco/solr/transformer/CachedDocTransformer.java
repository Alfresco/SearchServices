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
package org.alfresco.solr.transformer;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_SOLR4_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.content.SolrContentStore;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.response.DocsStreamer;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrReturnFields;
import org.codehaus.janino.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andy
 *
 */
public class CachedDocTransformer extends DocTransformer
{
    protected final static Logger log = LoggerFactory.getLogger(CachedDocTransformer.class);

    private ResultContext context;
    private SolrReturnFields solrReturnFields;
    
    /* (non-Javadoc)
     * @see org.apache.solr.response.transform.DocTransformer#getName()
     */
    @Override
    public String getName()
    {
        return "Alfresco cached document transformer";
    }
    
    
    public void setContext( ResultContext context ) 
    {
        this.context = context;
    }
    
    /* (non-Javadoc)
     * @see org.apache.solr.response.transform.DocTransformer#transform(org.apache.solr.common.SolrDocument, int)
     */
    @Override
    public void transform(SolrDocument doc, int docid, float score) throws IOException
    {
        Collection<String> fieldNames = new ArrayList<>(doc.getFieldNames());
        solrReturnFields = new SolrReturnFields(context.getRequest().getParams().get("originalFl"), context.getRequest());
        for (String fieldName : fieldNames)
        {
           SchemaField schemaField = context.getSearcher().getSchema().getFieldOrNull(fieldName);

           if(schemaField != null)
           {
               boolean isTrasformedFieldName = isTrasformedField(fieldName);
               String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
               if (isRequestedField(alfrescoFieldName))
               {
                   if(schemaField.multiValued())
                   {
                       Collection<Object> values = doc.getFieldValues(fieldName);
                       doc.removeFields(fieldName);

                       if (!isTrasformedFieldName)
                       {
                           doc.addField(fieldName, values);
                       }
                       else
                       {
                           //Guard against null pointer in case data model field name does not match up with cachedDoc field name.
                           if(values != null) {
                               ArrayList<Object> newValues = new ArrayList<>(values.size());
                               for (Object value : values) {
                                   newValues.add(getFieldValue(value));
                               }
                               doc.removeFields(alfrescoFieldName);
                               doc.addField(alfrescoFieldName, newValues);
                           }
                       }
                   }
                   else
                   {
                       Object value = DocsStreamer.getValue(schemaField, (IndexableField) doc.getFieldValue(fieldName));
                       doc.removeFields(fieldName);

                       if (!isTrasformedFieldName)
                       {
                           doc.removeFields(fieldName);
                           doc.addField(fieldName, value);
                       }
                       else
                       {
                           alfrescoFieldName = transformToUnderscoreNotation(alfrescoFieldName);
                           doc.removeFields(alfrescoFieldName);
                           doc.addField(alfrescoFieldName, getFieldValue(value));
                       }
                   }
               }
               else
               {
                   if (!fieldName.equals("id"))
                   {
                       doc.remove(fieldName);
                       doc.remove(alfrescoFieldName);
                   }
               }
           }
        }
    }

    private boolean isRequestedField(String fieldName)
    {
        return solrReturnFields.wantsField(fieldName.replace(":", "_"));
    }

    private String transformToUnderscoreNotation(String value)
    {
        return value.contains(":") ? value.replace(":", "_") : value;
    }

    private boolean isTrasformedField(String fieldName)
    {
        int index = fieldName.lastIndexOf("@{");
        return index != -1;
    }

    private String removeLocale(String value)
    {
        int start = value.lastIndexOf('\u0000');
        if(start == -1){
            return value;
        } else {
            return value.substring(start + 1);
        }
    }

    private Object getFieldValue(Object value){
        if (value instanceof String)
            return removeLocale((String) value );
        else
            return value;
    }

    
    private String getFieldValueString(SolrDocument doc, String fieldName)
    {
        Object o = doc.getFieldValue(fieldName);
        if(o != null)
        {
            if(o instanceof IndexableField)
            {
                IndexableField field = (IndexableField)o;
                return  field.stringValue();
            }
            else if(o instanceof String)
            {
                return (String)o;
            }
            else
            {
                return o.toString();
            }
        }
        else
        {
            return null;
        }
    }
}
