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

import static org.alfresco.solr.AlfrescoSolrUtils.assertSummaryCorrect;
import static org.alfresco.solr.AlfrescoSolrUtils.createCoreUsingTemplate;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the different templates.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
public class TemplatesDistributedIT extends AbstractAlfrescoDistributedIT
{
    final static String JETTY_SERVER_ID = "TemplatesDistributedTest";

    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(0, JETTY_SERVER_ID, null);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }
    
    
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

