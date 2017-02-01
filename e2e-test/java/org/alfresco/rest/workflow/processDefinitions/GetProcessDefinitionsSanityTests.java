package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
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
public class GetProcessDefinitionsSanityTests extends RestTest
{
    private UserModel adminUserModel, adminTenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY, description = "Verify Admin user gets process definitions for non-network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY })
    public void nonNetworkAdminGetsProcessDefinitions() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        restClient.withWorkflowAPI().getAllProcessDefinitions().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY, description = "Verify Tenant Admin user gets process definitions for network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY, TestGroup.NETWORKS})
    public void networkAdminGetsProcessDefinitions() throws Exception
    {
        restClient.usingTenant().createTenant(adminTenantUser);
        restClient.authenticateUser(adminTenantUser);
        restClient.withWorkflowAPI().getAllProcessDefinitions().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
