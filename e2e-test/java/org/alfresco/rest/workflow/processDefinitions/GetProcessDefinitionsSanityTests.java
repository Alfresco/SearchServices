package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.requests.RestProcessDefinitionsApi;
import org.alfresco.rest.requests.RestTenantApi;
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
public class GetProcessDefinitionsSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTenantApi tenantApi;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private RestProcessDefinitionsApi processDefinitionsApi;

    private UserModel adminUserModel, adminTenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        processDefinitionsApi.useRestClient(restClient);
    }

    // works on docker
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY, description = "Verify Admin user gets process definitions for non-network deployments using REST API and status code is OK (200)")
    public void nonNetworkAdminGetsProcessDefinitions() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        processDefinitionsApi.getProcessDefinitions().assertEntriesListIsNotEmpty();
        processDefinitionsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    // works on alfresco.server=172.29.100.215
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY, description = "Verify Tenant Admin user gets process definitions for network deployments using REST API and status code is OK (200)")

    @Test(groups = { TestGroup.NETWORKS })
    public void networkAdminGetsProcessDefinitions() throws Exception
    {
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser);
        restClient.authenticateUser(adminTenantUser);
        processDefinitionsApi.getProcessDefinitions().assertEntriesListIsNotEmpty();
        processDefinitionsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
