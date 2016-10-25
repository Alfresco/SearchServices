package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestDeploymentsApi;
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
 * Created by Claudia Agache on 10/4/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.SANITY})
public class GetDeploymentsSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTenantApi tenantApi;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private RestDeploymentsApi deploymentsApi;

    private UserModel adminUserModel, adminTenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        deploymentsApi.useRestClient(restClient);
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.DEPLOYMENTS }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets non-network deployments using REST API and status code is OK (200)")
    public void getNonNetworkDeploymentsWithAdmin() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        deploymentsApi.getDeployments().assertThat().entriesListIsNotEmpty();
        deploymentsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.DEPLOYMENTS }, executionType = ExecutionType.SANITY, description = "Verify Tenant Admin user gets network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.NETWORKS })
    public void getNetworkDeploymentsWithAdmin() throws JsonToModelConversionException, Exception
    {
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser);
        restClient.authenticateUser(adminTenantUser);
        deploymentsApi.getDeployments().assertThat().entriesListIsNotEmpty();
        deploymentsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
