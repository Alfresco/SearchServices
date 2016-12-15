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

package org.alfresco.solr.component;

import java.io.IOException;
import java.util.*;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.content.SolrContentStore;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.*;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.SolrCoreAware;

/**
 * @author Joel Bernstein
 * @since 5.2
 */

public class FingerPrintComponent extends SearchComponent implements SolrCoreAware
{
    public static final String COMPONENT_NAME = "fingerprint";

    public void inform(SolrCore core) {

    }

    public void prepare(ResponseBuilder responseBuilder) {

    }

    public void process(ResponseBuilder responseBuilder) throws IOException {

        if(!responseBuilder.req.getParams().getBool(FingerPrintComponent.COMPONENT_NAME, false)) {
            return;
        }

        SolrContentStore solrContentStore = getContentStore(responseBuilder.req);

        NamedList response = responseBuilder.rsp.getValues();
        String id = responseBuilder.req.getParams().get("id");

        long dbid = fetchDBID(id, responseBuilder.req.getSearcher());
        if(dbid == -1 && isNumber(id) ) {
            dbid = Long.parseLong(id);
        }

        NamedList fingerPrint = new NamedList();
        if(dbid > -1) {
            SolrInputDocument solrDoc = solrContentStore.retrieveDocFromSolrContentStore(AlfrescoSolrDataModel.getTenantId(TenantService.DEFAULT_DOMAIN), dbid);
            if (solrDoc != null) {
                SolrInputField mh = solrDoc.getField("MINHASH");
                if (mh != null) {
                    Collection col = mh.getValues();
                    List l = new ArrayList();
                    l.addAll(col);
                    fingerPrint.add("MINHASH", l);
                }
            }
        }

        response.add("fingerprint", fingerPrint);
    }

    private long fetchDBID(String UUID, SolrIndexSearcher searcher) throws IOException {
        String query = "workspace://SpacesStore/"+UUID;
        TermQuery q = new TermQuery(new Term(QueryConstants.FIELD_LID, query));
        TopDocs docs = searcher.search(q, 1);
        Set<String> fields = new HashSet();
        fields.add(QueryConstants.FIELD_DBID);
        if(docs.totalHits == 1) {
            ScoreDoc scoreDoc = docs.scoreDocs[0];
            Document doc = searcher.doc(scoreDoc.doc, fields);
            IndexableField dbidField = doc.getField(QueryConstants.FIELD_DBID);
            return dbidField.numericValue().longValue();
        }

        return -1;
    }

    private boolean isNumber(String s) {
        for(int i=0; i<s.length(); i++) {
            if(!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }


    private SolrContentStore getContentStore(SolrQueryRequest req)
    {
        if(req.getSearcher() != null)
        {
            CoreContainer coreContainer = req.getSearcher().getCore().getCoreDescriptor().getCoreContainer();
            AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler) coreContainer.getMultiCoreHandler();
            SolrInformationServer srv = (SolrInformationServer) coreAdminHandler.getInformationServers().get(req.getSearcher().getCore().getName());
            return srv.getSolrContentStore();
        }
        return null;
    }

    public String getDescription() {
        return null;
    }
}
