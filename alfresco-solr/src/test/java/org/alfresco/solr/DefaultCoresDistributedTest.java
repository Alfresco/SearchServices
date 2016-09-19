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

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertNotNull;
import static org.alfresco.solr.AlfrescoSolrUtils.assertSummaryCorrect;
import static org.alfresco.solr.AlfrescoSolrUtils.createCoreUsingTemplate;
import static org.alfresco.solr.AlfrescoSolrUtils.getCore;

/**
 * Tests creating the default alfresco cores with workspace,archive stores.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DefaultCoresDistributedTest extends AbstractAlfrescoDistributedTest
{
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    final String JETTY_SERVER_ID = this.getClass().getSimpleName();

    @Rule
    public JettyServerRule jetty = new JettyServerRule(JETTY_SERVER_ID, 0, null, null);

    @Test
    public void newCoreUsingAllDefaults() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

        //Now create the new core with
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);
        createSimpleCore(coreAdminHandler, null, null,null);

        //Get a reference to the new core
        SolrCore defaultCore = getCore(coreContainer, "alfresco");

        TimeUnit.SECONDS.sleep(3); //Wait a little for background threads to catchup
        assertNotNull(defaultCore);

        //Call custom actions
        SolrQueryResponse response = callHandler(coreAdminHandler, defaultCore, "SUMMARY");
        assertSummaryCorrect(response, defaultCore.getName());
    }

    @Test
    public void newCoreUsingArchiveStore() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

        //Now create the new core with
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);
        String coreName = "archive";

        createSimpleCore(coreAdminHandler, coreName, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.toString(),"rerankWithQueryLog/rerank",
                "property.alfresco.maxTotalBagels", "99", "property.alfresco.maxTotalConnections", "3456");
        //Get a reference to the new core
        SolrCore defaultCore = getCore(coreContainer, coreName);

        TimeUnit.SECONDS.sleep(3); //Wait a little for background threads to catchup
        assertNotNull(defaultCore);

        //Call custom actions
        SolrQueryResponse response = callHandler(coreAdminHandler, defaultCore, "SUMMARY");
        assertSummaryCorrect(response, defaultCore.getName());

        assertEquals("3456", defaultCore.getCoreDescriptor().getSubstitutableProperties().getProperty("alfresco.maxTotalConnections"));
        assertEquals("99", defaultCore.getCoreDescriptor().getSubstitutableProperties().getProperty("alfresco.maxTotalBagels"));


        //Test updating properties
        updateCore(coreAdminHandler,coreName, "property.alfresco.maxTotalBagels", "101", "property.alfresco.maxTotalConnections", "55");
        /**
         * See: https://issues.apache.org/jira/browse/SOLR-9533, this needs to be fixed first
        defaultCore = getCore(coreContainer, coreName);
        assertEquals("55", defaultCore.getCoreDescriptor().getSubstitutableProperties().getProperty("alfresco.maxTotalConnections"));
        assertEquals("101", defaultCore.getCoreDescriptor().getSubstitutableProperties().getProperty("alfresco.maxTotalBagels"));
         **/

    }

    public static void createSimpleCore(AlfrescoCoreAdminHandler coreAdminHandler,
                                        String coreName, String storeRef, String templateName,
                                        String... extraParams) throws InterruptedException {

        ModifiableSolrParams coreParams = params(CoreAdminParams.ACTION, "NEWUNSHARDEDCORE",
                "storeRef", storeRef,
                "coreName", coreName,
                "template", templateName);
        coreParams.add(params(extraParams));
        SolrQueryRequest request = new LocalSolrQueryRequest(null,coreParams);
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        TimeUnit.SECONDS.sleep(2);
    }

    public static void updateCore(AlfrescoCoreAdminHandler coreAdminHandler,
                                        String coreName,
                                        String... extraParams) throws InterruptedException {

        ModifiableSolrParams coreParams = params(CoreAdminParams.ACTION, "UPDATECORE", "coreName", coreName);
        coreParams.add(params(extraParams));
        SolrQueryRequest request = new LocalSolrQueryRequest(null,coreParams);
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        TimeUnit.SECONDS.sleep(2);
    }
}

