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
import org.apache.solr.core.CoreContainer;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.schema.SchemaField;
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
        SolrInputDocument cachedDoc = null;
        try
        {
            String id = getFieldValueString(doc, FIELD_SOLR4_ID);
            TenantAclIdDbId tenantAndDbId = AlfrescoSolrDataModel.decodeNodeDocumentId(id);
            CoreContainer coreContainer = context.getSearcher().getCore().getCoreContainer();
            AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler) coreContainer.getMultiCoreHandler();
            SolrInformationServer srv = (SolrInformationServer) coreAdminHandler.getInformationServers().get(context.getSearcher().getCore().getName());
            SolrContentStore solrContentStore = srv.getSolrContentStore();
            cachedDoc = solrContentStore.retrieveDocFromSolrContentStore(tenantAndDbId.tenant, tenantAndDbId.dbId);
        }
        catch(StringIndexOutOfBoundsException e)
        {
            // ignore invalid forms ....
        }
        
        if(cachedDoc != null)
        {
            Collection<String> fieldNames = cachedDoc.getFieldNames();
            for (String fieldName : fieldNames)
            {
               SchemaField schemaField = context.getSearcher().getSchema().getFieldOrNull(fieldName);
               if(schemaField != null)
               {
                   doc.removeFields(fieldName);
                   if(schemaField.multiValued())
                   {
                       int index = fieldName.lastIndexOf("@{");
                       if(index == -1)
                       { 
                           doc.addField(fieldName, cachedDoc.getFieldValues(fieldName));
                       }
                       else
                       {
                           String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
                           Collection<Object> values = cachedDoc.getFieldValues(fieldName);

                           //Guard against null pointer in case data model field name does not match up with cachedDoc field name.
                           if(values != null) {
                               ArrayList<Object> newValues = new ArrayList<Object>(values.size());
                               for (Object value : values) {
                                   if (value instanceof String) {
                                       String stringValue = (String) value;
                                       int start = stringValue.lastIndexOf('\u0000');
                                       if (start == -1) {
                                           newValues.add(stringValue);
                                       } else {
                                           newValues.add(stringValue.substring(start + 1));
                                       }
                                   } else {
                                       newValues.add(value);
                                   }

                               }
                               doc.removeFields(alfrescoFieldName);
                               doc.addField(alfrescoFieldName, newValues);
                           }
                       }
                   }
                   else
                   {
                       int index = fieldName.lastIndexOf("@{");
                       if(index == -1)
                       { 
                            doc.addField(fieldName, cachedDoc.getFieldValue(fieldName));
                       }
                       else
                       {
                           String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
                           alfrescoFieldName = alfrescoFieldName.contains(":") ? alfrescoFieldName.replace(":", "_") : alfrescoFieldName;
                           Object value = cachedDoc.getFieldValue(fieldName);
                           if(value instanceof String)
                           {
                               String stringValue = (String) value;
                               int start = stringValue.lastIndexOf('\u0000');
                               if(start == -1)
                               {
                                   doc.removeFields(alfrescoFieldName);
                                   doc.addField(alfrescoFieldName, stringValue);
                               }
                               else
                               {
                                   doc.removeFields(alfrescoFieldName);
                                   doc.addField(alfrescoFieldName, stringValue.substring(start+1));
                               }
                           }
                           else
                           {
                               doc.removeFields(alfrescoFieldName); 
                               doc.addField(alfrescoFieldName, value);
                           }
                       }
                   }
               }
            }
        }

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
