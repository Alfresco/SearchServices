package org.alfresco.service.search.cmis;

import org.alfresco.service.search.CmisTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.exception.TestConfigurationException;
import org.alfresco.utility.model.QueryModel;
import org.alfresco.utility.model.TestGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SolrSearchByPropertyTests extends CmisTest
{
    /** Logger for the class. */
    private static Logger LOGGER = LoggerFactory.getLogger(SolrSearchByPropertyTests.class);
    private XMLTestData testData;

    @BeforeClass(alwaysRun = true)
    public void deployCustomModel() throws TestConfigurationException
    {
        dataContent.deployContentModel("model/tas-model.xml");
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
    @XMLDataConfig(file = "src/main/resources/testdata/search-by-property.xml")
    public void prepareDataForSearchWithProperty(XMLTestData testData) throws Exception
    {
        this.testData = testData;
        this.testData.createUsers(dataUser);
        this.testData.createSitesStructure(dataSite, dataContent, dataUser);
        // wait for solr index
        Utility.waitToLoopTime(getSolrWaitTimeInSeconds());
    }

    @Test(groups = { TestGroup.CMIS, TestGroup.QUERIES },
            dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData", dependsOnMethods = "prepareDataForSearchWithProperty")
    @XMLDataConfig(file = "src/main/resources/testdata/search-by-property.xml")
    public void executeSortedSearchByID(QueryModel query) throws Exception
    {
        cmisApi.withQuery(query.getValue())
            .applyNodeRefsFrom(testData).assertResultsCount().equals(query.getResults());
    }
}
