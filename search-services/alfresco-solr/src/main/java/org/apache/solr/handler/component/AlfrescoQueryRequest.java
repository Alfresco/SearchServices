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
package org.apache.solr.handler.component;

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

