package org.alfresco.test.search.functional.searchServices.cmis;

import org.alfresco.utility.Utility;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.QueryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Test(dependsOnMethods = "prepareDataForSearchByPath", dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData")
    @XMLDataConfig(file = "src/test/resources/testdata/search-by-path.xml")
    public void executeSearchByPathQueries(QueryModel query)
    {
        cmisApi.withQuery(query.getValue()).assertResultsCount().equals(query.getResults());
    }
}
