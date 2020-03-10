/*
 * Copyright (C) 2005-2020 Alfresco Software Limited.
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

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.response.DocsStreamer;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrReturnFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Optional.of;

/**
 * @author Andy, Elia
 *
 */
public class AlfrescoFieldMapperTransformer extends DocTransformer
{
    protected final static Logger LOGGER = LoggerFactory.getLogger(AlfrescoFieldMapperTransformer.class);

    private ResultContext context;
    private SolrReturnFields solrReturnFields;

    @SuppressWarnings("unchecked")
    @Override
    public void transform(SolrDocument doc, int docid, float score)
    {
        Collection<String> fieldNames = new ArrayList<>(doc.getFieldNames());
        solrReturnFields = new SolrReturnFields(context.getRequest().getParams().get("originalFl"), context.getRequest());

        for (String fieldName : fieldNames)
        {
           SchemaField schemaField = context.getSearcher().getSchema().getFieldOrNull(fieldName);
           if(schemaField != null)
           {
               String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
               if (isRequestedField(alfrescoFieldName) || alfrescoFieldName.equals("id"))
               {
                   Object value = doc.getFieldValue(fieldName);
                   doc.removeFields(fieldName);
                   if (schemaField.multiValued())
                   {
                       Object collectionValue =
                               ((Collection<Object>) value).stream()
                                    .map(elem -> getFieldValue(schemaField, elem))
                                    .collect(Collectors.toSet());
                       doc.setField(alfrescoFieldName, collectionValue);
                   }
                   else
                   {
                       doc.setField(transformToUnderscoreNotation(alfrescoFieldName), getFieldValue(schemaField, value));
                   }
               }
               else
               {
                   doc.removeFields(alfrescoFieldName);
                   doc.removeFields(fieldName);
               }
           }
        }
    }

    @Override
    public String getName()
    {
        return "fmap";
    }

    @Override
    public void setContext( ResultContext context )
    {
        this.context = context;
    }

    private boolean isRequestedField(String fieldName)
    {
        return solrReturnFields.wantsField(transformToUnderscoreNotation(fieldName));
    }

    private String transformToUnderscoreNotation(String value)
    {
        return value.replace(":", "_");
    }

    private String removeLocale(String value)
    {
        int start = value.lastIndexOf('\u0000');
        if(start == -1)
        {
            return value;
        }
        else
        {
            return value.substring(start + 1);
        }
    }

    private Object getFieldValue(SchemaField schemaField, Object value)
    {
        if (value instanceof IndexableField)
        {
            Object indexedValue = DocsStreamer.getValue(schemaField, (IndexableField) value);
            return indexedValue instanceof String ? removeLocale((String) indexedValue) : indexedValue;
        }

        return value;
    }
}