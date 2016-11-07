/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.solr.query.AbstractQParser;
import org.alfresco.util.GUID;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * @author Andy
 */
public class QueryLoggingComponent extends SearchComponent
{

    @Override
    public void finishStage(ResponseBuilder rb)
    {
        super.finishStage(rb);
        if (rb.stage != ResponseBuilder.STAGE_GET_FIELDS)
            return;
        try
        {
            log(rb);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#process(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void process(ResponseBuilder rb) throws IOException
    {
        log(rb);
    }

    private void log(ResponseBuilder rb) throws IOException
    {
        boolean isShard = rb.req.getParams().getBool(ShardParams.IS_SHARD, false);
        if (!isShard)
        {
            CoreContainer container = rb.req.getCore().getCoreDescriptor().getCoreContainer();
            SolrCore logCore = container.getCore(rb.req.getCore().getName() + "_qlog");
            if (logCore != null)
            {
                JSONObject json = (JSONObject) rb.req.getContext().get(AbstractQParser.ALFRESCO_JSON);

                SolrQueryRequest request = null;
                UpdateRequestProcessor processor = null;
                try
                {
                    request = new LocalSolrQueryRequest(logCore, new NamedList<>());
                    processor = logCore.getUpdateProcessingChain(null).createProcessor(request, new SolrQueryResponse());

                    AddUpdateCommand cmd = new AddUpdateCommand(request);
                    cmd.overwrite = true;
                    SolrInputDocument input = new SolrInputDocument();
                    input.addField("id", GUID.generate());
                    input.addField("_version_", "1");

                    input.addField("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

                    if (json != null)
                    {
                        try
                        {
                            ArrayList<String> authorityList = new ArrayList<String>(1);
                            JSONArray authorities = json.getJSONArray("authorities");
                            for (int i = 0; i < authorities.length(); i++)
                            {
                                String authorityString = authorities.getString(i);
                                authorityList.add(authorityString);
                            }

                            for (String authority : authorityList)
                            {
                                if (AuthorityType.getAuthorityType(authority) == AuthorityType.USER)
                                {
                                    input.addField("user", authority);
                                    break;
                                }
                            }
                        }
                        catch (JSONException e)
                        {
                            input.addField("user", "<UNKNOWN>");
                        }
                    }
                    else
                    {
                        input.addField("user", "<UNKNOWN>");
                    }

                    String userQuery = rb.req.getParams().get(SpellingParams.SPELLCHECK_Q);
                    if (userQuery == null)
                    {
                        if (json != null)
                        {
                            try
                            {
                                userQuery = json.getString("query");
                            }
                            catch (JSONException e)
                            {
                            }
                        }
                    }
                    if (userQuery == null)
                    {
                        userQuery = rb.req.getParams().get(CommonParams.Q);
                    }

                    if (userQuery != null)
                    {
                        input.addField("user_query", userQuery);
                    }

                    Query query = rb.getQuery();
                    input.addField("query", query.toString());

                    if (rb.getResults().docList != null)
                    {
                        input.addField("found", rb.getResults().docList.matches());
                    }
                    input.addField("time", rb.req.getRequestTimer().getTime());

                    cmd.solrDoc = input;
                    processor.processAdd(cmd);
                }

                finally
                {
                    if (processor != null)
                    {
                        processor.finish();
                    }
                    if (request != null)
                    {
                        request.close();
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getDescription()
     */
    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
