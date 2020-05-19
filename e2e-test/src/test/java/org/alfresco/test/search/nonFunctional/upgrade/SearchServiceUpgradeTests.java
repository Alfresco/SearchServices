/*
 * #%L
 * Alfresco Search Services E2E Test
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

package org.alfresco.test.search.nonFunctional.upgrade;

import org.alfresco.utility.LogFactory;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.QueryModel;
import org.slf4j.Logger;
import org.testng.annotations.Test;

public class SearchServiceUpgradeTests extends AbstractUpgradeE2ETest
{
	private static Logger LOG = LogFactory.getLogger();
	
	@Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getAllData")
    @XMLDataConfig(file = "src/main/resources/testdata/for-upgrade.xml")
	public void prepareData(XMLTestData testData) throws Exception {
		LOG.info("Start Preparing data for Upgrade testing");
		this.testData = testData;
        this.testData.createUsers(dataUser);
        this.testData.createSitesStructure(dataSite, dataContent, dataUser);
        // wait for solr index
        Utility.waitToLoopTime(20);
	}
	
	/**
	 * This should be executed after prepareData
	 * I am not adding a dependency here, because I want to play with pre/post upgrade scenarios directly in xml suite files.
	 */
	@Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData")
	@XMLDataConfig(file = "src/main/resources/testdata/for-upgrade.xml")
    public void checkDataIsSearchable(QueryModel query) throws Exception
    {
		LOG.info("Start testing if data is searchable");
		cmisAPI.authenticateUser(dataUser.getAdminUser())
        	.withQuery(query.getValue())
            .applyNodeRefsFrom(testData).assertResultsCount().equals(query.getResults());
    }
}
