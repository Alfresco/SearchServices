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

import java.util.ArrayList;

import org.alfresco.solr.query.stream.AbstractStreamTest;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Rule;
import org.junit.Test;

import org.junit.Assert;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class CachedDocTransformerTest extends AbstractStreamTest
{
    
    @Rule
    public JettyServerRule jetty = new JettyServerRule(1, this);
    
    @Test
    public void transformDocument() throws Exception 
    {
        //Test 1: Running a simple query without invoking CachedDocTransformer, expected to see id,DBID and _version_
        QueryResponse resp = query(getDefaultTestClient(), true, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                params("q", "*", "qt", "/afts", "shards.qt", "/afts"));
        Assert.assertNotNull(resp);
        SolrDocumentList results = resp.getResults();
        SolrDocument doc = results.get(0);
        Assert.assertTrue(doc.size() == 3);
        Assert.assertNotNull(doc);
        String id = (String) doc.get("id");
        Assert.assertNotNull(id);
        long version = (long) doc.get("_version_");
        Assert.assertNotNull(version);
        long dbid = (long) doc.get("DBID");
        Assert.assertNotNull(dbid);
        //Not expected to see below as part of the solr response.
        String title = (String) doc.get("cm:title");
        Assert.assertNull(title);
        String owner = (String) doc.get("OWNER");
        Assert.assertNull(owner);
        
        //Test 2: Running simple query with CachedDocTransformer, expected to see all fields returned
        resp = query(getDefaultTestClient(), true, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","[cached]*"));
        SolrDocument docWithAllFields = resp.getResults().get(0);
        Assert.assertTrue(docWithAllFields.size() > 3);
        
        Integer version2 = (Integer) docWithAllFields.get("_version_");
        Assert.assertNotNull(version2);
        owner = ((ArrayList) docWithAllFields.get("OWNER")).toString();
        Assert.assertNotNull(owner);
        Assert.assertEquals("[mike]", owner);
        title = ((ArrayList) docWithAllFields.get("cm:title")).toString();
        Assert.assertEquals("[title1]", title);
        Assert.assertNotNull(title);
        long dbid2 = (long) docWithAllFields.get("DBID");
        Assert.assertNotNull(dbid2);
        
        //Test 3: Running simple query with CachedDocTransformer, expected to see selected fields returned
        resp = query(getDefaultTestClient(), true, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id,DBID,[cached]"));
        
        Assert.assertNotNull(resp);
        SolrDocument docWithRequestedFields = resp.getResults().get(0);
        Assert.assertTrue(docWithRequestedFields.size() == 2);
        //Test 4: Running simple query with CachedDocTransformer on non default fields, expected to see selected fields returned
//         resp = query(getDefaultTestClient(), true, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
//                params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","[cached]cm*"));
//
//        Assert.assertNotNull(resp);
//        SolrDocument docWithRequestedFields2 = resp.getResults().get(0);
//        Assert.assertTrue(docWithRequestedFields2.size() == 16);
        
        resp = query(getDefaultTestClient(), true, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id, cm_title,[cached]"));
        
        Assert.assertNotNull(resp);
        SolrDocument docWithRequestedFields3 = resp.getResults().get(0);
        Assert.assertTrue(docWithRequestedFields3.size() == 2);
    }
}
