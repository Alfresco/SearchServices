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

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets a specific process definition for non-network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY })
    public void adminGetsProcessDefinition() throws Exception
    {
        returnedProcessDefinition = restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat().field("name").is(randomProcessDefinition.getName());
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Any user gets a specific process definition for non-network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY })
    public void anyUserGetsProcessDefinition() throws Exception
    {
        restClient.authenticateUser(testUser);
        returnedProcessDefinition = restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat()
                .field("name").is(randomProcessDefinition.getName()).and()
                .field("deploymentId").is(randomProcessDefinition.getDeploymentId()).and()
                .field("description").is(randomProcessDefinition.getDescription()).and()
                .field("id").is(randomProcessDefinition.getId()).and()
                .field("startFormResourceKey").is(randomProcessDefinition.getStartFormResourceKey()).and()
                .field("category").is(randomProcessDefinition.getCategory()).and()
                .field("title").is(randomProcessDefinition.getTitle()).and()
                .field("version").is(randomProcessDefinition.getVersion()).and()
                .field("graphicNotationDefined").is(randomProcessDefinition.getGraphicNotationDefined()).and()
                .field("key").is(randomProcessDefinition.getKey());
    }
}
