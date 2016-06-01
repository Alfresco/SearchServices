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

import java.io.IOException;
import java.util.ArrayList;

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.NumericUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.schema.SchemaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andy
 *
 */
public class DocValueDocTransformer extends DocTransformer
{
    protected final static Logger log = LoggerFactory.getLogger(DocValueDocTransformer.class);
    
    ResultContext context;

   
    
    /* (non-Javadoc)
     * @see org.apache.solr.response.transform.DocTransformer#getName()
     */
    @Override
    public String getName()
    {
        return "Alfresco doc value document transformer";
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
        for(String fieldName :context.getSearcher().getFieldNames())
        {
            SchemaField schemaField = context.getSearcher().getSchema().getFieldOrNull(fieldName);
            if(schemaField != null)
            {
                if(schemaField.hasDocValues())
                {
                    SortedDocValues sortedDocValues = context.getSearcher().getLeafReader().getSortedDocValues(fieldName);
                    if(sortedDocValues != null)
                    {
                        int ordinal = sortedDocValues.getOrd(docid);
                        if(ordinal > -1)
                        {
                            doc.removeFields(fieldName);
                            String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
                            doc.removeFields(alfrescoFieldName);
                            doc.addField(alfrescoFieldName, schemaField.getType().toObject(schemaField, sortedDocValues.lookupOrd(ordinal)));
                        }
                    }
                    
                    SortedSetDocValues sortedSetDocValues = context.getSearcher().getLeafReader().getSortedSetDocValues(fieldName);
                    if(sortedSetDocValues != null)
                    {
                        ArrayList<Object> newValues = new ArrayList<Object>();
                        sortedSetDocValues.setDocument(docid);
                        long ordinal;
                        while ( (ordinal = sortedSetDocValues.nextOrd()) !=  SortedSetDocValues.NO_MORE_ORDS)
                        {
                            newValues.add(schemaField.getType().toObject(schemaField, sortedSetDocValues.lookupOrd(ordinal)));       
                        }
                        doc.removeFields(fieldName);
                        String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
                        doc.removeFields(alfrescoFieldName);
                        doc.addField(alfrescoFieldName, newValues);
                    }
                    
                    
                    BinaryDocValues binaryDocValues = context.getSearcher().getLeafReader().getBinaryDocValues(fieldName);
                    if(binaryDocValues != null)
                    {
                        doc.removeFields(fieldName);
                        String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
                        doc.removeFields(alfrescoFieldName);
                        doc.addField(alfrescoFieldName, schemaField.getType().toObject(schemaField, binaryDocValues.get(docid)));
                    }
                    
                    if(schemaField.getType().getNumericType() != null)
                    {
                        NumericDocValues numericDocValues = context.getSearcher().getLeafReader().getNumericDocValues(fieldName);
                        if(numericDocValues != null)
                        {
                            doc.removeFields(fieldName);
                            String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
                            doc.removeFields(alfrescoFieldName);
                            switch(schemaField.getType().getNumericType())
                            {
                            case DOUBLE:
                                doc.addField(alfrescoFieldName,  Double.longBitsToDouble(numericDocValues.get(docid)));
                                break;
                            case FLOAT:
                                doc.addField(alfrescoFieldName,  Float.intBitsToFloat((int) numericDocValues.get(docid)));
                                break;
                            case INT:
                                doc.addField(alfrescoFieldName, (int) numericDocValues.get(docid));
                                break;
                            case LONG:
                                doc.addField(alfrescoFieldName, numericDocValues.get(docid));
                                break;
                            }
                        }
                        
                        SortedNumericDocValues sortedNumericDocValues = context.getSearcher().getLeafReader().getSortedNumericDocValues(fieldName);
                        if(sortedNumericDocValues != null)
                        {
                            sortedNumericDocValues.setDocument(docid);
                            doc.removeFields(fieldName);
                            String alfrescoFieldName = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
                            doc.removeFields(alfrescoFieldName);
                            ArrayList<Object> newValues = new ArrayList<Object>(sortedNumericDocValues.count()); 
                            if(sortedNumericDocValues.count() > 0)
                            {
                               
                                for(int i = 0; i < sortedNumericDocValues.count(); i++)
                                {
                                    switch(schemaField.getType().getNumericType())
                                    {
                                        case DOUBLE:
                                            newValues.add(NumericUtils.sortableLongToDouble(sortedNumericDocValues.valueAt(i)));
                                            break;
                                        case FLOAT:
                                            newValues.add(NumericUtils.sortableIntToFloat((int)sortedNumericDocValues.valueAt(i)));
                                            break;
                                        case INT:
                                            newValues.add((int)sortedNumericDocValues.valueAt(i));
                                            break;
                                        case LONG:
                                            newValues.add(sortedNumericDocValues.valueAt(i));
                                            break;
                                          
                                    }
                                }
                            }
                            doc.addField(alfrescoFieldName, newValues);
                            
                        }
                    }
                }
            }
        }
        
    }

    
}
