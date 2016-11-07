/*
 * Copyright (C) 2014 Alfresco Software Limited.
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

import java.util.Collection;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cloud consists of support methods for running solr queries in the cloud.
 * 
 * @author Ahmed Owian
 */
public class Cloud
{
    public static final String RESPONSE_SOLR_DOCUMENT_LIST = "responseSolrDocumentList";
    public static final String PARAM_GET_SOLR_DOCUMENT_LIST = "alfresco.getSolrDocumentList";
    protected final static Logger log = LoggerFactory.getLogger(Cloud.class);

    /**
     * Creates a query to select docs with any of the specified field values
     * @param fieldName the name of the field in the condition
     * @param operator currently only OR makes sense. To add AND, fieldName must become a list of fields. 
     * @param valueLists a bunch of possible values for the field
     * @return the resulting query string
     */
    @SuppressWarnings("rawtypes")
    String getQuery(String fieldName, String operator, Collection... valueLists)
    {
        StringBuilder query = new StringBuilder();
        for (Collection values : valueLists)
        {
            for (Object value : values)
            {
                String escapedValue = value.toString().replace(":", "\\:").replace("{", "\\{").replace("}", "\\}");
                query.append(fieldName).append(":").append(escapedValue).append(operator);
            }
        }

        if (query.length() >= operator.length())
        {
            // Removes the last appended operator from the query string
            query.delete(query.length() - operator.length(), query.length());
        }

        return query.toString();
    }

    /**
     * Returns whether or not a doc exists that satisfies the specified query
     * @param requestHandler the handler that handles the request
     * @param request the request object to put the query on
     * @param query the string that specifies the doc
     * @return <code>true</code> if the specified query returns a doc
     */
    boolean exists(SolrRequestHandler requestHandler, SolrQueryRequest request, String query)
    {
        ModifiableSolrParams params = new ModifiableSolrParams(request.getParams());
        // Sets 1 because this is effectively a boolean query to see if there exists a single match
        params.set("q", query).set("fl", "id").set("rows", "1");
        ResultContext rc = this.getResultContext(requestHandler, request, params);

        if (rc != null)
        {
// TODO Should we use rc.docs.matches() instead?
            if (rc.getDocList() != null) { return rc.getDocList().iterator().hasNext(); }
        }

        return false;
    }

    /**
     * Returns the doc list resulting from running the query
     * @param requestHandler the handler that handles the request
     * @param request the request object to put the query on
     * @param query the string that specifies the docs
     * @return the docs that come back from the query
     */
    DocList getDocList(SolrRequestHandler requestHandler, SolrQueryRequest request, String query)
    {
        // Getting the doc list is shard-specific, and not really cloud-friendly
        
        ModifiableSolrParams params = new ModifiableSolrParams(request.getParams());
        // Sets MAX_VALUE to get all the rows
        params.set("q", query).set("fl", QueryConstants.FIELD_SOLR4_ID).set("rows", Integer.MAX_VALUE);
        ResultContext rc = this.getResultContext(requestHandler, request, params);
        return rc != null ? rc.getDocList() : null;
    }

    /**
     * @param requestHandler the handler that handles the request
     * @param request the request object to put the params on
     * @param params Solr parameters
     * @return the result context from the handled request
     */
    ResultContext getResultContext(SolrRequestHandler requestHandler, SolrQueryRequest request, SolrParams params)
    {
        SolrQueryResponse solrRsp = getResponse(requestHandler, request, params);
        ResultContext rc = (ResultContext) solrRsp.getValues().get("response");
        return rc;
    }
    
    /**
     * Returns the SolrDocumentList from the response, assuming that the specified request handler uses the 
     * AlfrescoSearchHandler which puts it on the response values. 
     * @param requestHandler the handler that handles the request
     * @param request the request object to put the params on
     * @param params Solr parameters
     * @return the result context from the handled request
     */
    SolrDocumentList getSolrDocumentList(SolrRequestHandler requestHandler, SolrQueryRequest request, 
                ModifiableSolrParams params)
    {
        // Tells the AlfrescoSearchHandler to get the SolrDocumentList from the default results and add to response
        params.set(PARAM_GET_SOLR_DOCUMENT_LIST, true);
        SolrQueryResponse solrRsp = getResponse(requestHandler, request, params);
        SolrDocumentList docs = (SolrDocumentList) solrRsp.getValues().get(RESPONSE_SOLR_DOCUMENT_LIST);
        return docs;
    }
    
    /**
     * @param requestHandler the handler that handles the request
     * @param request the request object to put the params on
     * @param params Solr parameters
     * @return the response
     */
    SolrQueryResponse getResponse(SolrRequestHandler requestHandler, SolrQueryRequest request, SolrParams params)
    {
        request.setParams(params);
        log.info("Running query " + params.get("q"));
        SolrQueryResponse solrRsp = new SolrQueryResponse();
        requestHandler.handleRequest(request, solrRsp);
        return solrRsp;
    }
}