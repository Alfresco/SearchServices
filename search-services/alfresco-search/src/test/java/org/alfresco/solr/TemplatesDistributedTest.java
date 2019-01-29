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
package org.alfresco.solr;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.alfresco.solr.AlfrescoSolrUtils.*;

/**
 * Tests the different templates.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class TemplatesDistributedTest extends AbstractAlfrescoDistributedTest
{
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    final String JETTY_SERVER_ID = this.getClass().getSimpleName();

    @Rule
    public JettyServerRule jetty = new JettyServerRule(JETTY_SERVER_ID, 0, null, null);

    @Test
    public void newCoreUsinglshTemplate() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

        //Now create the new core with
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);
        SolrCore rankCore = createCoreUsingTemplate(coreContainer, coreAdminHandler, "templateWithrerank", AlfrescoCoreAdminHandler.DEFAULT_TEMPLATE, 2, 1);

        //Call custom actions
        SolrQueryResponse response = callHandler(coreAdminHandler, rankCore, "SUMMARY");
        assertSummaryCorrect(response, rankCore.getName());
    }
}

