package org.alfresco.rest.v1;

import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.utility.ServerHealth;
import org.alfresco.utility.TasProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

import com.jayway.restassured.RestAssured;

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

    @BeforeClass(alwaysRun = true)
    public void setupRestTest() throws Exception
    {
        serverHealth.assertIfServerOnline();

        RestAssured.baseURI = restProperties.envProperty().getTestServerUrl();
        RestAssured.port = restProperties.envProperty().getPort();
        RestAssured.basePath = restProperties.getRestBasePath();
    }
}
