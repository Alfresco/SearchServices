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

import org.alfresco.solr.AlfrescoSolrTestCaseJ4;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoFTSQParserPluginTest extends AlfrescoSolrTestCaseJ4 {

    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        // if you override setUp or tearDown, you better callf
        // the super classes version
        super.setUp();
        clearIndex();
        assertU(commit());
    }

    @Test
    public void testAftsQueries() throws Exception {

        assertU(delQ("*:*"));
        assertU(commit());

        String[] doc = {"id", "1",  "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc));
        assertU(commit());
        String[] doc1 = {"id", "2", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc1));

        String[] doc2 = {"id", "3", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc2));
        assertU(commit());
        String[] doc3 = {"id", "4", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc3));

        String[] doc4 = {"id", "5", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc4));
        assertU(commit());
        String[] doc5 = {"id", "6", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc5));
        assertU(commit());

        Thread.sleep(30000);

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        SolrServletRequest req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}");
        assertQ(req, "*[count(//doc)=6]");
    }
}