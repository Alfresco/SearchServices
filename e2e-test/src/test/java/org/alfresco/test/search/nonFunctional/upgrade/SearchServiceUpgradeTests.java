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
