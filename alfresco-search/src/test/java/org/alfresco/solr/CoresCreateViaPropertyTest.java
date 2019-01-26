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
package org.alfresco.solr;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import static org.alfresco.solr.AlfrescoSolrUtils.getCore;

/**
 * Tests creating the default alfresco cores with a property.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class CoresCreateViaPropertyTest extends AbstractAlfrescoDistributedTestStatic
{
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    final static String JETTY_SERVER_ID = "CoresCreateViaPropertyTest";

    @BeforeClass
    private static void initData() throws Throwable
    {
        System.setProperty(AlfrescoCoreAdminHandler.ALFRESCO_DEFAULTS, "alfresco,archive");
        initSolrServers(0, JETTY_SERVER_ID, null);
    }

    @AfterClass
    private static void destroyData() throws Throwable
    {
        dismissSolrServers();
        System.clearProperty(AlfrescoCoreAdminHandler.ALFRESCO_DEFAULTS);
    }

    @Test
    public void newCoreUsingAllDefaults() throws Exception
    {
        CoreContainer coreContainer = jettyContainers.get(JETTY_SERVER_ID).getCoreContainer();

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

