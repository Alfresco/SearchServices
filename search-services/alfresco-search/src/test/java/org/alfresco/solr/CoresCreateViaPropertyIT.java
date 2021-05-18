/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.alfresco.solr.AlfrescoSolrUtils.getCore;

/**
 * Tests creating the default alfresco cores with a property.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
public class CoresCreateViaPropertyIT extends AbstractAlfrescoDistributedIT
{
    static String testFolder;

    @BeforeClass
    public static void initData() throws Throwable
    {
        System.setProperty(AlfrescoCoreAdminHandler.ALFRESCO_DEFAULTS, "alfresco,archive");
        testFolder = initSolrServers(0, CoresCreateViaPropertyIT.class.getSimpleName(), null);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
        System.clearProperty(AlfrescoCoreAdminHandler.ALFRESCO_DEFAULTS);
    }

    @Test
    public void newCoreUsingAllDefaults() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(testFolder).getCoreContainer();

        //Now create the new core with
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);

        TimeUnit.SECONDS.sleep(15); //Wait a little for background threads to catchup

        //Get a reference to the new core
        SolrCore defaultCore = getCore(coreContainer, "alfresco");
        SolrCore archiveCore = getCore(coreContainer, "archive");

        assertNotNull(defaultCore);
        assertNotNull(archiveCore);
    }
}

