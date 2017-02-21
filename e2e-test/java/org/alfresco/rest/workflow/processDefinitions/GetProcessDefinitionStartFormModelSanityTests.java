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
 * Created by Claudia Agache on 10/18/2016.
 */
public class GetProcessDefinitionStartFormModelSanityTests extends RestTest
{
    private UserModel adminUserModel, adminTenantUser;
    private RestProcessDefinitionModel randomProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Admin gets a model of the start form type definition for non-network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY })
    public void nonNetworkAdminGetsStartFormModel() throws Exception
    {
        randomProcessDefinition = restClient.withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Tenant Admin gets a model of the start form type definition for network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY, TestGroup.NETWORKS })
    public void networkAdminGetsStartFormModel() throws Exception
    {
        restClient.usingTenant().createTenant(adminTenantUser);
        restClient.authenticateUser(adminTenantUser);
        randomProcessDefinition = restClient.withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
