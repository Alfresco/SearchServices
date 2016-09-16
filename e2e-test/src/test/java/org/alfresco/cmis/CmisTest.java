package org.alfresco.cmis;

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.utility.ServerHealth;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

@ContextConfiguration("classpath:alfresco-cmis-context.xml")
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
    public void setup() throws Exception
    {
        serverHealth.assertServerIsOnline();
    }
}
