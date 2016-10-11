package org.alfresco.rest;

import org.testng.annotations.BeforeClass;

import com.jayway.restassured.RestAssured;

public abstract class RestWorkflowTest extends RestTest
{
    @BeforeClass(alwaysRun = true)
    public void checkServerHealth() throws Exception
    {
        serverHealth.assertServerIsOnline();

        RestAssured.baseURI = restProperties.envProperty().getTestServerUrl();
        RestAssured.port = restProperties.envProperty().getPort();
        RestAssured.basePath = restProperties.getRestWorkflowPath();
    }
}
