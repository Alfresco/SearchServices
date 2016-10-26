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

package org.alfresco.solr.query;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.RefCount;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoSolrSpellcheckerTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
    }
    @Before
    public void setUp() throws Exception
    {

        // if you override setUp or tearDown, you better call
        // the super classes version
        clearIndex();
        assertU(commit());
    }

    @Test
    public void testSpellcheckOutputFormat() throws Exception {

        assertU(delQ("*:*"));
        assertU(commit());

        String[] doc = {"id","1", "suggest", "YYYYYYY BBBBBBB","_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYYYYY BBBBBBB"};
        assertU(adoc(doc));
        assertU(commit());
        String[] doc1 = {"id","2", "suggest","YYYYYYA","_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYYYYA"};
        assertU(adoc(doc1));

        String[] doc2 = {"id","3", "suggest", "BBBBBBBB","_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "BBBBBBBB"};
        assertU(adoc(doc2));
        assertU(commit());
        String[] doc3 = {"id","4", "suggest", "CCCC","_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "CCCC"};
        assertU(adoc(doc3));

        String[] doc4 = {"id","5", "suggest", "DDDD","_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "DDDD"};
        assertU(adoc(doc4));
        assertU(commit());
        String[] doc5 = {"id","6", "suggest","EEEE", "_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "EEEE"};
        assertU(adoc(doc5));
        assertU(commit());

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("spellcheck.q", "YYYYY BBBBB");
        params.add("qt", "/afts");
        params.add("spellcheck", "true");
        params.add("start", "0");
        params.add("rows", "6");

        SolrQueryRequest req = areq(params,
                                    "{\"query\":\"(YYYYY BBBBB AND (id:(1 2 3 4 5 6)))\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}");
        assertQ(req,
                "*[count(//lst[@name='spellcheck']/lst[@name='suggestions']/lst[@name='collation'])=2]",
                "/response/lst[@name='spellcheck']/lst[@name='suggestions']/lst[@name='collation'][1]/int[@name='hits'][.='2']",
                "/response/lst[@name='spellcheck']/lst[@name='suggestions']/lst[@name='collation'][2]/int[@name='hits'][.='1']",
                "/response/lst[@name='spellcheck']/lst[@name='suggestions']/lst[@name='collation'][1]/str[@name='collationQueryString'][.='yyyyyya bbbbbbb']",
                "/response/lst[@name='spellcheck']/lst[@name='suggestions']/lst[@name='collation'][2]/str[@name='collationQueryString'][.='yyyyyyy bbbbbbb']",
                "/response/lst[@name='spellcheck']/lst[@name='suggestions']/lst[@name='collation'][1]/str[@name='collationQuery'][.='(yyyyyya bbbbbbb AND (id:(1 2 3 4 5 6)))']",
                "/response/lst[@name='spellcheck']/lst[@name='suggestions']/lst[@name='collation'][2]/str[@name='collationQuery'][.='(yyyyyyy bbbbbbb AND (id:(1 2 3 4 5 6)))']");
    }
}
