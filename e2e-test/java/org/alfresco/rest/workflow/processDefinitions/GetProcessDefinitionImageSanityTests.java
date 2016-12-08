package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.TestGroup;
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
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY })
public class GetProcessDefinitionImageSanityTests extends RestTest
{
    private UserModel testUser;
    private RestProcessDefinitionModel randomProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(dataUser.getAdminUser());
        randomProcessDefinition = restClient.withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION }, executionType = ExecutionType.SANITY, description = "Verify Any user gets a specific process definition image for non-network deployments using REST API and status code is OK (200)")
    public void anyUserGetsProcessDefinitionImage() throws Exception
    {
        restClient.authenticateUser(testUser);
        restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage()
            .assertResponseContainsImage();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets a specific process definition image for non-network deployments using REST API and status code is OK (200)")
    public void adminGetsProcessDefinitionImage() throws Exception
    {
        restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage()
            .assertResponseContainsImage();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
