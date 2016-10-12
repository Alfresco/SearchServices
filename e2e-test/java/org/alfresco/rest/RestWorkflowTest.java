package org.alfresco.rest;

import org.alfresco.utility.data.DataWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;

import com.jayway.restassured.RestAssured;

public abstract class RestWorkflowTest extends RestTest
{
    @Autowired
    protected DataWorkflow dataWorkflow;

    @BeforeClass(alwaysRun = true)
    public void checkServerHealth() throws Exception
    {
        serverHealth.assertServerIsOnline();

        RestAssured.baseURI = restProperties.envProperty().getTestServerUrl();
        RestAssured.port = restProperties.envProperty().getPort();
        RestAssured.basePath = restProperties.getRestWorkflowPath();
    }
}
