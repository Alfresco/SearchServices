/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.solr.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.search.impl.QueryParserUtils;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.util.Pair;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.io.stream.*;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.request.SolrQueryRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class AlfrescoStreamHandler extends StreamHandler
{

    /*
    * This method is called when creating a stream context for TupleStream.
    * The stream context is then used to convey the json payload to the stream sources
    */

    public StreamContext getStreamContext(SolrQueryRequest req)
    {
        StreamContext streamContext = new StreamContext();
        Iterable<ContentStream> streams = req.getContentStreams();
        if (streams != null)
        {
            try
            {
                Reader reader = null;
                for (ContentStream stream : streams)
                {
                    reader = new BufferedReader(new InputStreamReader(
                            stream.getStream(), "UTF-8"));
                }

                if (reader != null)
                {
                    JSONObject json = new JSONObject(new JSONTokener(reader));
                    streamContext.put("request-factory", new AlfrescoRequestFactory(json.toString()));
                    return streamContext;
                }
            }
            catch (JSONException e)
            {
                // This is expected when there is no json element to the
                // request
            }
            catch (IOException e)
            {
                throw new AlfrescoRuntimeException(
                        "IO Error parsing query parameters", e);
            }
        }

        //No json payload was found so just return the base RequestFactory
        streamContext.put("request-factory", new RequestFactory());
        return streamContext;
    }

    /**
     * Rewrites the name of the field passed in to a field name that solr can understand.
     * Reuses the Alfresco Data model logic for this.
     * @param field - the name of a field.
     * @return a field in the index
     */
    public static String getIndexedField(String field) {

        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();

        Pair<String, String> fieldNameAndEnding = QueryParserUtils.extractFieldNameAndEnding(field);
        PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition(NamespaceService.CONTENT_MODEL_1_0_URI, dataModel.getNamespaceDAO(), dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT), fieldNameAndEnding.getFirst());

        if(propertyDef != null)
        {
            AlfrescoSolrDataModel.IndexedField fields = dataModel.getQueryableFields(propertyDef.getName(), dataModel.getTextField(fieldNameAndEnding.getSecond()), AlfrescoSolrDataModel.FieldUse.SORT);
            if(fields.getFields().size() > 0)
            {
                return fields.getFields().get(0).getField();
            }
        }
        return dataModel.mapNonPropertyFields(field);
    }

    public static class AlfrescoRequestFactory extends RequestFactory
    {
        private String alfrescoJson;

        public AlfrescoRequestFactory(String alfrescoJson)
        {
            this.alfrescoJson = alfrescoJson;
        }

        public QueryRequest getRequest(SolrParams solrParams)
        {
            ModifiableSolrParams modifiableSolrParams = (ModifiableSolrParams)solrParams;
            modifiableSolrParams.set(CommonParams.QT, "/afts");
            modifiableSolrParams.set(FacetParams.FACET, "true");
            modifiableSolrParams.set(CommonParams.FQ, "{!afts}AUTHORITY_FILTER_FROM_JSON");
            AlfrescoQueryRequest alfrescoQueryRequest = new AlfrescoQueryRequest(alfrescoJson, solrParams);
            alfrescoQueryRequest.setMethod(SolrRequest.METHOD.POST);
            return alfrescoQueryRequest;
        }
    }

    public static class AlfrescoQueryRequest extends QueryRequest
    {
        private String json;

        public AlfrescoQueryRequest(String json, SolrParams params)
        {
            super(params);
            this.json =json;
        }

        public Collection<ContentStream> getContentStreams()
        {
            List<ContentStream> streams = new ArrayList<ContentStream>();
            streams.add(new ContentStreamBase.StringStream(json));
            return streams;
        }
    }
}
