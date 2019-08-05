package org.alfresco.test.search.nonFunctional.upgrade;

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.network.ServerHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

/**
 * We can use this class for both SearchService and InsightEngine.
 *
 * @author Paul Brodner
 */
@ContextConfiguration("classpath:alfresco-search-e2e-context.xml")
public abstract class AbstractUpgradeE2ETest extends AbstractTestNGSpringContextTests
{
    @Autowired
    protected ServerHealth serverHealth;

    @Autowired
    protected DataUser dataUser;
    
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;
    
    @Autowired
    protected CmisWrapper cmisAPI;
    
    protected XMLTestData testData;
        
    @BeforeClass(alwaysRun = true)
    public void checkServerHealth()
    {
        serverHealth.assertServerIsOnline();        
    }      
}