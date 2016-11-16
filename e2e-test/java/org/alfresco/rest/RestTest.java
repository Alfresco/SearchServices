package org.alfresco.rest;

import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.utility.TasProperties;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataGroup;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.network.ServerHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

@ContextConfiguration("classpath:alfresco-restapi-context.xml")
public abstract class RestTest extends AbstractTestNGSpringContextTests
{
    @Autowired
    protected RestProperties restProperties;

    @Autowired
    protected TasProperties properties;

    @Autowired
    protected ServerHealth serverHealth;

    @Autowired
    protected RestWrapper restClient;
    
    @Autowired
    protected DataUser dataUser;

    @Autowired
    protected DataSite dataSite;
    
    @Autowired
    protected DataContent dataContent;
    
    @Autowired
    protected DataGroup dataGroup;

    @BeforeClass(alwaysRun = true)
    public void checkServerHealth() throws Exception
    {
        serverHealth.assertServerIsOnline();
    }
}
