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
package org.alfresco.solr.transformer;

import org.alfresco.solr.query.stream.AbstractStreamTest;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class CachedDocTransformerTest extends AbstractStreamTest
{
    public static final String JSON = "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}";

    @Rule
    public JettyServerRule jetty = new JettyServerRule(1, this);
    
    @Test
    public void transformDocument() throws Exception 
    {
        //Test 1: Running a simple query without invoking CachedDocTransformer, expected to see id,DBID and _version_
        QueryResponse resp = query(getDefaultTestClient(), true, JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts"));
        assertNotNull(resp);
        SolrDocumentList results = resp.getResults();
        SolrDocument doc = results.get(0);
        assertTrue(doc.size() == 3);
        assertNotNull(doc);
        String id = (String) doc.get("id");
        assertNotNull(id);
        long version = (long) doc.get("_version_");
        assertNotNull(version);
        long dbid = (long) doc.get("DBID");
        assertNotNull(dbid);
        //Not expected to see below as part of the solr response.
        String title = (String) doc.get("cm:title");
        assertNull(title);
        String owner = (String) doc.get("OWNER");
        assertNull(owner);
        
        //Test 2: Running simple query with CachedDocTransformer, expected to see all fields returned
        resp = query(getDefaultTestClient(), true, JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","*,[cached]"));
        SolrDocument docWithAllFields = resp.getResults().get(0);
        assertTrue(docWithAllFields.size() > 3);
        
        Integer version2 = (Integer) docWithAllFields.get("_version_");
        assertNotNull(version2);
        owner = ((ArrayList) docWithAllFields.get("OWNER")).toString();
        assertNotNull(owner);
        assertEquals("[mike]", owner);
        title = ((ArrayList) docWithAllFields.get("cm:title")).toString();
        assertEquals("[title1]", title);
        assertNotNull(title);
        long dbid2 = (long) docWithAllFields.get("DBID");
        assertNotNull(dbid2);
        
        //Test 3: Running simple query with CachedDocTransformer, expected to see selected fields returned
        resp = query(getDefaultTestClient(), true, JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id,DBID,[cached]"));
        
        assertNotNull(resp);
        SolrDocument docWithRequestedFields = resp.getResults().get(0);
        assertTrue(docWithRequestedFields.size() == 2);
        assertNotNull(docWithRequestedFields.get("id"));
        assertNotNull(docWithRequestedFields.get("DBID"));

        //Test 4: Running simple query with CachedDocTransformer on non default fields, expected to see selected fields returned
        resp = query(getDefaultTestClient(), true, JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id, cm_title,[cached]"));
        
        assertNotNull(resp);
        SolrDocument docWithRequestedFields3 = resp.getResults().get(0);
        assertTrue(docWithRequestedFields3.size() == 2);
        assertNotNull(docWithRequestedFields3.get("id"));
        title = (String) docWithRequestedFields3.getFieldValue("cm_title");
        assertEquals("title1", title);

        resp = query(getDefaultTestClient(), true, JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","cm_name, score, [cached]"));
        assertNotNull(resp);
        results = resp.getResults();
        docWithAllFields = results.get(0);
        assertTrue(docWithAllFields.size() == 2);
        assertNotNull(docWithAllFields.get("cm_name"));
        Float score = (Float) docWithAllFields.get("score");
        assertNotNull(score);

        resp = query(getDefaultTestClient(), true, JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","cm_title, cm_created, DBID, score, [cached]"));
        assertNotNull(resp);
        results = resp.getResults();
        docWithAllFields = results.get(0);
        assertTrue(docWithAllFields.size() == 4);
        assertNotNull(docWithAllFields.get("cm_title"));
        assertNotNull(docWithAllFields.get("cm_created"));
        assertNotNull(docWithAllFields.get("score"));
        assertNotNull(docWithAllFields.get("DBID"));

    }
}
