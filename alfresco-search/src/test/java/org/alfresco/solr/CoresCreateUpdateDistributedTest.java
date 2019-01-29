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
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.AfterClass;
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
 * Also tests updating properties with reload.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class CoresCreateUpdateDistributedTest extends AbstractAlfrescoDistributedTest
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
    public void newCoreWithUpdateSharedProperties() throws Exception {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

        //Now create the new core with
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler) coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);
        String coreName = "alfSharedCore";

        //AlfrescoSolrDataModel.getResourceDirectory() looks for solrhome in a system property or jndi.
        //In production there's always a solr.home but not in this test, so this is the workaround.
        //Set it here and clean up with a @AfterClass method.
        System.setProperty("solr.solr.home", coreContainer.getSolrHome());

        //First, we have no cores so we can update the shared properties, including disallowed
        updateShared(coreAdminHandler,"property.solr.host", "myhost", "property.my.property", "chocolate",
                "property.alfresco.identifier.property.0", "http://www.alfresco.org/model/content/1.0}userName");
        Properties props = AlfrescoSolrDataModel.getCommonConfig();
        assertEquals(props.getProperty("my.property"), "chocolate");
        assertEquals(props.getProperty("alfresco.identifier.property.0"), "http://www.alfresco.org/model/content/1.0}userName");

        createSimpleCore(coreAdminHandler, coreName, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.toString(), null);
        //Get a reference to the new core
        SolrCore defaultCore = getCore(coreContainer, coreName);

        TimeUnit.SECONDS.sleep(3); //Wait a little for background threads to catchup
        assertNotNull(defaultCore);

        String solrHost = props.getProperty("solr.host");
        assertFalse(props.containsKey("new.property"));
        try {
            updateShared(coreAdminHandler,"property.solr.host", "superhost", "property.new.property", "catchup", "property.alfresco.identifier.property.0", "not_this_time");
            assertFalse(true); //Should not get here
        } catch (SolrException se) {
            assertEquals(SolrException.ErrorCode.BAD_REQUEST.code, se.code());
        }
        updateShared(coreAdminHandler,"property.solr.host", "superhost", "property.new.property", "catchup");
        props = AlfrescoSolrDataModel.getCommonConfig();
        assertEquals(props.getProperty("new.property"), "catchup");
        assertNotEquals(props.getProperty("solr.host"), solrHost);
    }

    @Test
    public void newCoreUsingArchiveStore() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

        //Now create the new core with
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);
        String coreName = "archive";

        createSimpleCore(coreAdminHandler, coreName, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.toString(),AlfrescoCoreAdminHandler.DEFAULT_TEMPLATE,
                "property.alfresco.maxTotalBagels", "99", "property.alfresco.maxTotalConnections", "3456");
        //Get a reference to the new core
        SolrCore defaultCore = getCore(coreContainer, coreName);

        TimeUnit.SECONDS.sleep(3); //Wait a little for background threads to catchup
        assertNotNull(defaultCore);

        //Call custom actions
        SolrQueryResponse response = callHandler(coreAdminHandler, defaultCore, "SUMMARY");
        assertSummaryCorrect(response, defaultCore.getName());

        assertEquals("3456", defaultCore.getCoreDescriptor().getCoreProperty("alfresco.maxTotalConnections","notset"));
        assertEquals("99", defaultCore.getCoreDescriptor().getCoreProperty("alfresco.maxTotalBagels", "notset"));

        //Test updating properties
        updateCore(coreAdminHandler,coreName, "property.alfresco.maxTotalBagels", "101",
                "property.alfresco.maxTotalConnections", "55",
                "property.solr.is.great", "true");
        defaultCore = getCore(coreContainer, coreName);
        assertEquals("55", defaultCore.getCoreDescriptor().getCoreProperty("alfresco.maxTotalConnections","notset"));
        assertEquals("101", defaultCore.getCoreDescriptor().getCoreProperty("alfresco.maxTotalBagels", "notset"));
        assertEquals("true", defaultCore.getCoreDescriptor().getCoreProperty("solr.is.great", "notset"));
    }

    @AfterClass
    protected static void cleanupProp()
    {
        System.clearProperty("solr.solr.home");
    }

    public static void createSimpleCore(AlfrescoCoreAdminHandler coreAdminHandler,
                                        String coreName, String storeRef, String templateName,
                                        String... extraParams) throws InterruptedException {

        ModifiableSolrParams coreParams = params(CoreAdminParams.ACTION, "NEWDEFAULTINDEX",
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

    public static void updateShared(AlfrescoCoreAdminHandler coreAdminHandler,
                                    String... extraParams) throws InterruptedException {

        ModifiableSolrParams coreParams = params(CoreAdminParams.ACTION, "UPDATESHARED");
        coreParams.add(params(extraParams));
        SolrQueryRequest request = new LocalSolrQueryRequest(null,coreParams);
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        TimeUnit.SECONDS.sleep(2);
    }
}