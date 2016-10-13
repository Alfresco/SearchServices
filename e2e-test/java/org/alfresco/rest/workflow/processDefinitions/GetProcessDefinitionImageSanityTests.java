package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.requests.RestProcessDefinitionsApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/13/2016.
 */
@Test(groups = { "rest-api", "process-definitions", "sanity" })
public class GetProcessDefinitionImageSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;
    @Autowired
    private RestProcessDefinitionsApi processDefinitionsApi;

    private UserModel testUser;
    private RestProcessDefinitionModel randomProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        processDefinitionsApi.useRestClient(restClient);
        restClient.authenticateUser(dataUser.getAdminUser());
        randomProcessDefinition = processDefinitionsApi.getProcessDefinitions().getOneEntry();
    }

    @TestRail(section = { "rest-api", "process-definitions" },
            executionType = ExecutionType.SANITY, description = "Verify Any user gets a specific process definition image for non-network deployments using REST API and status code is OK (200)")
    public void anyUserGetsProcessDefinitionImage() throws Exception
    {
        restClient.authenticateUser(testUser);
        processDefinitionsApi.getProcessDefinitionImage(randomProcessDefinition).assertResponseContainsImage();
        processDefinitionsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}