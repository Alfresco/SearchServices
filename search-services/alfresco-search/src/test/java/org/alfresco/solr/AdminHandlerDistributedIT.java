/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Properties;

import static org.alfresco.repo.index.shard.ShardMethodEnum.*;
import static org.alfresco.solr.AlfrescoSolrUtils.*;

/**
 * Tests the custom Alfresco Handler.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
public class AdminHandlerDistributedIT extends AbstractAlfrescoDistributedIT
{
    static String testFolder;
    static final String CORE_NAME = "newcoretesting";
    
    @BeforeClass
    public static void initData() throws Throwable
    {
        testFolder = initSolrServers(2, AdminHandlerDistributedIT.class.getSimpleName(), null);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void newCoreUsingAdminHandler() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(testFolder).getCoreContainer();

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
