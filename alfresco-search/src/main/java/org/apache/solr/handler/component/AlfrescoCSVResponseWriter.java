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
package org.apache.solr.handler.component;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.response.CSVResponseWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;

import java.io.IOException;
import java.io.Writer;

/**
 * A wrapper around the CSV writer for Alfresco-specific logic
 *
 * @author Gethin James
 */
public class AlfrescoCSVResponseWriter implements QueryResponseWriter
{

    //Uses the customCSVWriter
    CSVResponseWriter delegate = new CSVResponseWriter();

    @Override
    public void write(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
        delegate.write(writer, req, rsp);
    }

    @Override
    public void init(NamedList args) {
        delegate.init(args);
    }

    @Override
    public String getContentType(SolrQueryRequest request, SolrQueryResponse response) {
        return delegate.getContentType(request,response);
    }
}
