/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr.highlight;

import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.AlfrescoSolrHighlighter;
import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.highlight.SolrHighlighter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_VERSION;
import static org.alfresco.solr.AlfrescoSolrUtils.assertSummaryCorrect;
import static org.alfresco.solr.AlfrescoSolrUtils.createCoreUsingTemplate;
import static org.alfresco.solr.AlfrescoSolrUtils.getCore;

/**
 * Tests Alfresco-specific logic.
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoHighligherTest extends AbstractAlfrescoDistributedTest
{
    public static final String TEXT_FIELD = "content@s___t@{http://www.alfresco.org/model/content/1.0}content";

    @Rule
    public DefaultAlrescoCoreRule jetty = new DefaultAlrescoCoreRule(this.getClass().getSimpleName());

    @Test
    public void testConfig() throws Exception
    {
        SolrCore defaultCore = jetty.getDefaultCore();
        SolrHighlighter highlighter = HighlightComponent.getHighlighter(defaultCore);
        assertTrue("wrong highlighter: " + highlighter.getClass(), highlighter instanceof AlfrescoSolrHighlighter);
    }
    
}
