package org.alfresco.cmis;

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.network.ServerHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

@ContextConfiguration("classpath:alfresco-cmis-context.xml")
@Component
@Scope(value = "prototype")
public abstract class CmisTest extends AbstractTestNGSpringContextTests
{
    @Autowired
    CmisWrapper cmisApi;

    @Autowired
    protected DataUser dataUser;

    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    @Autowired
    ServerHealth serverHealth;

    @BeforeClass(alwaysRun = true)
    public void checkServerHealth() throws Exception
    {
        serverHealth.assertServerIsOnline();
    }
}
