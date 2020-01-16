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
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.Query;
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
import org.apache.solr.response.DocsStreamer;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.SolrCoreAware;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_FINGERPRINT;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_LID;

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

        String id = responseBuilder.req.getParams().get("id");
        NamedList response = responseBuilder.rsp.getValues();
        IndexSchema schema = responseBuilder.req.getSchema();
        SolrIndexSearcher searcher = responseBuilder.req.getSearcher();

        Query q;

        // Make a distinction:
        // if id is a number is taken as DBID, otherwise as LID
        if(isNumber(id))
        {
            long dbid = Long.parseLong(id);
            q = LegacyNumericRangeQuery.newLongRange("DBID", dbid, dbid + 1, true, false);
        }
        else
        {
            String query = id.startsWith("workspace") ? id : "workspace://SpacesStore/"+id;
            q = new TermQuery(new Term(FIELD_LID, query));
        }

        TopDocs docs = searcher.search(q, 1);
        Set<String> fields = new HashSet();
        fields.add("MINHASH");

        NamedList fingerPrint = new NamedList();
        List<Object> values = new ArrayList<>();
        if(docs.totalHits == 1) {
            ScoreDoc scoreDoc = docs.scoreDocs[0];
            Document doc = searcher.doc(scoreDoc.doc, fields);


            IndexableField[] minHashes = doc.getFields("MINHASH");
            for (IndexableField minHash : minHashes)
            {
                SchemaField sf = schema.getFieldOrNull(minHash.name());
                Object value = DocsStreamer.getValue(sf, minHash);
                values.add(value);
                fingerPrint.add("MINHASH", values);
            }
        }

        response.add("fingerprint", fingerPrint);
    }

    private boolean isNumber(String s) {
        for(int i=0; i<s.length(); i++) {
            if(!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public String getDescription() {
        return null;
    }
}
