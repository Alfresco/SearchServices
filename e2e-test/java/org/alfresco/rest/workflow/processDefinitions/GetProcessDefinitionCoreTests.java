package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 12/5/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.CORE })
public class GetProcessDefinitionCoreTests extends RestTest
{
    private UserModel adminUser, adminTenantUser;
    private RestProcessDefinitionModel randomProcessDefinition, returnedProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify if get process definition returns status code 404 when invalid processDefinitionId is used")
    public void getProcessDefinitionUsingInvalidProcessDefinitionId() throws Exception
    {
        restClient.authenticateUser(adminUser);
        randomProcessDefinition = restClient.withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        randomProcessDefinition.setId("invalidID");

        restClient.withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidID"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network admin is able to get a process definition using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.NETWORKS })
    public void networkAdminGetProcessDefinition() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);

        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        returnedProcessDefinition = restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat().field("name").is(randomProcessDefinition.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network user is able to get a process definition using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.NETWORKS })
    public void networkUserGetProcessDefinition() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);

        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");

        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        returnedProcessDefinition = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat().field("name").is(randomProcessDefinition.getName());
    }

}
