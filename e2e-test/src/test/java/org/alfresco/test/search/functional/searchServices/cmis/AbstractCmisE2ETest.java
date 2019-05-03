package org.alfresco.test.search.functional.searchServices.cmis;

import org.alfresco.cmis.CmisProperties;
import org.alfresco.cmis.CmisWrapper;

import java.lang.reflect.Method;

import org.alfresco.test.search.functional.searchServices.search.AbstractSearchServicesE2ETest;
import org.alfresco.utility.LogFactory;
import org.slf4j.Logger;
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
public abstract class AbstractCmisE2ETest extends AbstractSearchServicesE2ETest
{
    private static Logger LOG = LogFactory.getLogger();

    @Autowired
    protected CmisWrapper cmisApi;
    
    @Autowired
    protected CmisProperties cmisProperties;

    public String documentContent = "CMIS document content";

    @BeforeMethod(alwaysRun = true)
    public void showStartTestInfo(Method method)
    {
        LOG.info(String.format("*** STARTING Test: [%s] ***", method.getName()));
    }

    @AfterMethod(alwaysRun = true)
    public void showEndTestInfo(Method method)
    {
        LOG.info(String.format("*** ENDING Test: [%s] ***", method.getName()));
    }
    
    public Integer getSolrWaitTimeInSeconds()
    {
        return cmisProperties.envProperty().getSolrWaitTimeInSeconds();
    }
}
