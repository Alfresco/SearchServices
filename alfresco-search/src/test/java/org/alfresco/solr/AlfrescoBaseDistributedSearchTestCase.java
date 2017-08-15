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
package org.alfresco.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfresco.solr.stream.AlfrescoStreamHandler;
import org.apache.solr.BaseDistributedSearchTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

/**
 * @author Joel
 */

@SolrTestCaseJ4.SuppressSSL
public abstract class AlfrescoBaseDistributedSearchTestCase extends BaseDistributedSearchTestCase
{
    public AlfrescoBaseDistributedSearchTestCase()
    {
        System.setProperty("alfresco.test", "true");
    }

    protected QueryRequest getAlfrescoRequest(String json, SolrParams params) {
        QueryRequest request = new AlfrescoStreamHandler.AlfrescoQueryRequest(json, params);
        request.setMethod(SolrRequest.METHOD.POST);
        return request;
    }

    /**
     * Returns the QueryResponse from {@link #queryServer}
     */
    protected QueryResponse query(String json, ModifiableSolrParams params) throws Exception
    {
        params.set("distrib", "false");
        QueryRequest request = getAlfrescoRequest(json, params);
        QueryResponse controlRsp = request.process(controlClient);
        validateControlData(controlRsp);
        params.remove("distrib");
        setDistributedParams(params);
        QueryResponse rsp = queryServer(json, params);
        compareResponses(rsp, controlRsp);
        return rsp;
    }

    protected QueryResponse queryServer(String json, SolrParams params) throws SolrServerException, IOException
    {
        // query a random server
        int which = r.nextInt(clients.size());
        SolrClient client = clients.get(which);
        QueryRequest request = getAlfrescoRequest(json, params);
        return request.process(client);
    }


}

