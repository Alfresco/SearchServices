/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

public class AlfrescoJsonQueryRequest extends QueryRequest
{
    private static final long serialVersionUID = 3259015603145833490L;
    private String json;

    public AlfrescoJsonQueryRequest(String json, SolrParams params)
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
