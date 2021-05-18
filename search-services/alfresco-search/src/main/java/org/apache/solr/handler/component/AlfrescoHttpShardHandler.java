/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.apache.solr.handler.component;

import org.alfresco.solr.query.AbstractQParser;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;

public class AlfrescoHttpShardHandler extends HttpShardHandler {

  public AlfrescoHttpShardHandler(HttpShardHandlerFactory alfrescoHttpShardHandlerFactory, HttpClient httpClient) {
	  super(alfrescoHttpShardHandlerFactory, httpClient);
  }

  /**
   * Subclasses could modify the request based on the shard
   */
  protected QueryRequest makeQueryRequest(final ShardRequest sreq, ModifiableSolrParams params, String shard)
  {
      AlfrescoQueryRequest req = new AlfrescoQueryRequest(params, SolrRequest.METHOD.POST);
      return req;
  }
}

