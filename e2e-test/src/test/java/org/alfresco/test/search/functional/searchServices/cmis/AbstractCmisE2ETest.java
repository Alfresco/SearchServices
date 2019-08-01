package org.alfresco.test.search.functional.searchServices.cmis;

import java.lang.reflect.Method;

import org.alfresco.cmis.CmisProperties;
import org.alfresco.cmis.CmisWrapper;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Supertype layer for all InsightEngine E2E tests.
 *
 * @author agazzarini
 */
@ContextConfiguration("classpath:alfresco-search-e2e-context.xml")
@Component
@Scope(value = "prototype")
public abstract class AbstractCmisE2ETest extends AbstractE2EFunctionalTest
{
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractCmisE2ETest.class);

    @Autowired
    protected CmisWrapper cmisApi;
    
    @Autowired
    protected CmisProperties cmisProperties;

    public String documentContent = "CMIS document content";

    @BeforeMethod(alwaysRun = true)
    public void showStartTestInfo(Method method)
    {
        LOGGER.info(String.format("*** STARTING Test: [%s] ***", method.getName()));
    }

    @AfterMethod(alwaysRun = true)
    public void showEndTestInfo(Method method)
    {
        LOGGER.info(String.format("*** ENDING Test: [%s] ***", method.getName()));
    }
    
    public Integer getSolrWaitTimeInSeconds()
    {
        return cmisProperties.envProperty().getSolrWaitTimeInSeconds();
    }
    
    /**
     * Repeat SOLR Query till results count returns expectedCountResults
     * @param query CMIS Query to be executed
     * @param expectedCountResults Number of results expected
     * @return true when results count is equals to expectedCountResults 
     */
    protected boolean waitForIndexing(String query, long expectedCountResults)
    {

        for (int searchCount = 1; searchCount <= 3; searchCount++)
        {
            
            try
            {
                cmisApi.withQuery(query).assertResultsCount().equals(expectedCountResults);
                return true;
            } 
            catch (AssertionError ae)
            {
                LOGGER.debug(ae.toString());
            }
            
            
            Utility.waitToLoopTime(properties.getSolrWaitTimeInSeconds(), "Wait For Indexing");
            
        }

        return false;
    }

    
}
