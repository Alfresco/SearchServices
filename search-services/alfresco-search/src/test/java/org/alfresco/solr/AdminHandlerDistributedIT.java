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
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Properties;

import static org.alfresco.repo.index.shard.ShardMethodEnum.*;
import static org.alfresco.solr.AlfrescoSolrUtils.*;

/**
 * Tests the custom Alfresco Handler.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AdminHandlerDistributedIT extends AbstractAlfrescoDistributedIT
{
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    final String JETTY_SERVER_ID = this.getClass().getSimpleName();
    static final String CORE_NAME = "newcoretesting";
    
    @BeforeClass
    private static void initData() throws Throwable
    {
        initSolrServers(2, "AdminHandlerDistributedIT", null);
    }

    @AfterClass
    private static void destroyData() throws Throwable
    {
        dismissSolrServers();
    }
    
    @Test
    public void newCoreUsingAdminHandler() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

        //Create the new core
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);

        SolrCore testingCore = createCoreUsingTemplate(coreContainer, coreAdminHandler, CORE_NAME, "rerank", 1, 1);
        Properties props = testingCore.getCoreDescriptor().getSubstitutableProperties();
        //The default sharding method is DB_ID
        assertEquals(DB_ID.toString(), props.get("shard.method"));

        //Call custom actions
        SolrQueryResponse response = callHandler(coreAdminHandler, testingCore, "check");
        assertNotNull(response);
        response = callHandler(coreAdminHandler, testingCore, "summary");
        assertSummaryCorrect(response, testingCore.getName());
        response = callHandler(coreAdminHandler, testingCore, "Report");
        assertNotNull(response);
        NamedList<Object> report = (NamedList<Object>) response.getValues().get("report");
        assertNotNull(report.get(CORE_NAME));

        //Create a core using ACL_ID sharding
        testingCore = createCoreUsingTemplate(coreContainer, coreAdminHandler, CORE_NAME+"aclId", "rerank", 1, 1,"property.shard.method",ACL_ID.toString());
        props = testingCore.getCoreDescriptor().getSubstitutableProperties();
        assertEquals(ACL_ID.toString(), props.get("shard.method"));
    }
}

