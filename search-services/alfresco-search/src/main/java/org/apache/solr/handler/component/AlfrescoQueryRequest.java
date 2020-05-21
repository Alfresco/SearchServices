/*-
 * #%L
 * Alfresco Solr Search
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

import java.util.Collection;
import java.util.Collections;

import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

/**
 * 
 *
 * @since solr 1.3
 */
public class AlfrescoQueryRequest extends QueryRequest
{
  private ContentStream contentStream = null;
  
  public AlfrescoQueryRequest()
  {
    super();
  }

  public AlfrescoQueryRequest( SolrParams q )
  {
    super(q);
  }
  
  public AlfrescoQueryRequest( SolrParams q, METHOD method )
  {
    super(q, method);
  }

  
  //---------------------------------------------------------------------------------
  //---------------------------------------------------------------------------------
  
  @Override
  public Collection<ContentStream> getContentStreams() {
      return contentStream == null ? null : Collections.singletonList(contentStream);
  }
  
  /**
 * @param contentStream the contentStream to set
 */
public void setContentStream(ContentStream contentStream)
{
    this.contentStream = contentStream;
}
}

