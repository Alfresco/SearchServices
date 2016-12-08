package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/13/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY })
public class GetProcessDefinitionSanityTests extends RestTest
{
    private UserModel testUser;
    private RestProcessDefinitionModel randomProcessDefinition, returnedProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(dataUser.getAdminUser());
        randomProcessDefinition = restClient.withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets a specific process definition for non-network deployments using REST API and status code is OK (200)")
    public void adminGetsProcessDefinition() throws Exception
    {
        returnedProcessDefinition = restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat().field("name").is(randomProcessDefinition.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Any user gets a specific process definition for non-network deployments using REST API and status code is OK (200)")
    public void anyUserGetsProcessDefinition() throws Exception
    {
        restClient.authenticateUser(testUser);
        returnedProcessDefinition = restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat().field("name").is(randomProcessDefinition.getName());
    }
}
