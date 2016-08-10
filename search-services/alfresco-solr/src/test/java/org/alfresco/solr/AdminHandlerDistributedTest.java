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

import org.alfresco.distributed.AbstractAlfrescoDistributedTest;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tests the custom Alfresco Handler.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AdminHandlerDistributedTest extends AbstractAlfrescoDistributedTest
{
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    final String JETTY_SERVER_ID = this.getClass().getSimpleName();
    static final String CORE_NAME = "newcoretesting";
    static Properties extra = new Properties();
    static
    {
        extra.put("we.are.testing","yes");
        extra.put("solr.maxBooleanClauses","5");
    }

    @Rule
    public JettyServerRule jetty = new JettyServerRule(JETTY_SERVER_ID, 0, extra, DEFAULT_TEST_CORENAME);

    @Test
    public void newCoreUsingAdminHandler() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

        //First check the properties
        Properties props = getCore(coreContainer, DEFAULT_TEST_CORENAME).getCoreDescriptor().getSubstitutableProperties();
        assertNotNull(props);
        assertEquals("yes", props.get("we.are.testing"));
        assertEquals("5", props.get("solr.maxBooleanClauses"));

        //Now create the new core
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);
        SolrQueryRequest request = new LocalSolrQueryRequest(getCore(coreContainer, DEFAULT_TEST_CORENAME),
                params(CoreAdminParams.ACTION, "newcore",
                        "storeRef", "workspace://SpacesStore",
                        "coreName", CORE_NAME,
                        "template", "rerank"));
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        assertEquals(CORE_NAME, response.getValues().get("core"));

        //Get a reference to the new core
        SolrCore testingCore = getCore(coreContainer, CORE_NAME);
        assertNotNull(testingCore);

        //Call custom actions
        response = callHandler(coreAdminHandler, testingCore, "check");
        assertNotNull(response);
        response = callHandler(coreAdminHandler, testingCore, "summary");
        assertNotNull(response);
        assertNotNull(response.getValues().get("Summary"));
        response = callHandler(coreAdminHandler, testingCore, "log4j");
        assertNotNull(response);
        response = callHandler(coreAdminHandler, testingCore, "Report");
        assertNotNull(response);
        assertNotNull(response.getValues().get("report"));
    }

    private SolrQueryResponse callHandler(AlfrescoCoreAdminHandler coreAdminHandler, SolrCore testingCore, String action) {
        SolrQueryRequest request = new LocalSolrQueryRequest(testingCore,
                params(CoreAdminParams.ACTION, action, CoreAdminParams.CORE, CORE_NAME));
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        return response;
    }

    private SolrCore getCore(CoreContainer coreContainer, String coreName) {
        return coreContainer.getCores().stream()
                .filter(aCore ->coreName.equals(aCore.getName()))
                .findFirst().get();
        //coreContainer.cores.find { it.name == coreName }
    }

}

