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

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.CSVResponseWriter;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SolrReturnFields;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;

/**
 * A wrapper around the CSV writer for Alfresco-specific logic
 *
 * @author Gethin James
 */
public class AlfrescoCSVResponseWriter implements QueryResponseWriter
{

    CSVResponseWriter delegate = new CSVResponseWriter();

    @Override
    public void write(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
/**        SolrParams params = req.getParams();
        SolrIndexSearcher solrIndexSearcher = req.getSearcher();
//        String fl = params.get("fl");

//        rsp.setReturnFields( new SolrReturnFields("id,DOC_TYPE,content@s__size@,OWNER,TYPE, content@s__mimetype@*", req) );

        Object responseObj = rsp.getResponse();
        if (responseObj instanceof ResultContext) {
            DocList ids = ((ResultContext)responseObj).getDocList();
            Iterator<SolrDocument> docsStreamer = ((ResultContext)responseObj).getProcessedDocuments();
        }
**/

 //       rsp.setReturnFields( new SolrReturnFields("id,DOC_TYPE,OWNER,TYPE,[cached]", req) );
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
