package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestProcessDefinitionModel;
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
 * Created by Claudia Agache on 10/18/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY })
public class GetProcessDefinitionStartFormModelSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTenantApi tenantApi;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private RestProcessDefinitionsApi processDefinitionsApi;

    private UserModel adminUserModel, adminTenantUser;
    private RestProcessDefinitionModel randomProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        processDefinitionsApi.useRestClient(restClient);
        restClient.authenticateUser(adminUserModel);
    }

    // works on docker
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Admin gets a model of the start form type definition for non-network deployments using REST API and status code is OK (200)")
    public void nonNetworkAdminGetsStartFormModel() throws Exception
    {
        randomProcessDefinition = processDefinitionsApi.getProcessDefinitions().getOneRandomEntry();
        processDefinitionsApi.getProcessDefinitionStartFormModel(randomProcessDefinition).assertEntriesListIsNotEmpty();
        processDefinitionsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    // works on 215
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Tenant Admin gets a model of the start form type definition for network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.NETWORKS })
    public void networkAdminGetsStartFormModel() throws Exception
    {
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser);
        restClient.authenticateUser(adminTenantUser);
        randomProcessDefinition = processDefinitionsApi.getProcessDefinitions().getOneRandomEntry();
        processDefinitionsApi.getProcessDefinitionStartFormModel(randomProcessDefinition).assertEntriesListIsNotEmpty();
        processDefinitionsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
