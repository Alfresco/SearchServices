/*
 * #%L
 * Alfresco Search Services E2E Test
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

package org.alfresco.test.search.functional.searchServices.cmis;

import org.alfresco.search.TestGroup;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.QueryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SolrSearchByPathTests extends AbstractCmisE2ETest
{
    /** Logger for the class. */
    private static Logger LOGGER = LoggerFactory.getLogger(SolrSearchByPathTests.class);
    private XMLTestData testData;

    @BeforeClass(alwaysRun = true)
    public void readTestDataFile()
    {
        cmisApi.authenticateUser(dataUser.getAdminUser());
    }

    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment()
    {
        if (testData != null)
        {
            testData.cleanup(dataContent);
        }
        else
        {
            LOGGER.warn("testData is inexplicably null - skipping clean up.");
        }
    }

    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getAllData")
    @XMLDataConfig(file = "src/test/resources/testdata/search-by-path.xml")
    public void prepareDataForSearchByPath(XMLTestData testData)
    {
        this.testData = testData;
        testData.createUsers(dataUser);
        testData.createSitesStructure(dataSite, dataContent, dataUser);
        // wait for solr index
        Utility.waitToLoopTime(getSolrWaitTimeInSeconds());
    }

    @Test(dependsOnMethods = "prepareDataForSearchByPath", dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData", groups={TestGroup.CONFIG_ENABLED_CASCADE_TRACKER})
    @XMLDataConfig(file = "src/test/resources/testdata/search-by-path.xml")
    public void executeSearchByPathQueries(QueryModel query)
    {
        cmisApi.authenticateUser(testUser);
        Assert.assertTrue(waitForIndexing(query.getValue(), query.getResults()), String.format("Result count not as expected for query: %s", query.getValue()));

    }
}
