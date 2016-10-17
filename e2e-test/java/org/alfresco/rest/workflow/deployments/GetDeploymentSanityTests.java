package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.requests.RestDeploymentsApi;
import org.alfresco.rest.requests.RestTenantApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for REST API call GET "/deployments/{deploymentId}"
 * 
 * @author Cristina Axinte
 *
 */
@Test(groups = { "rest-api", "workflow", "deployments", "sanity" })
public class GetDeploymentSanityTests extends RestWorkflowTest
{
    @Autowired
    private RestDeploymentsApi deploymentsApi;

    private UserModel adminUserModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        deploymentsApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "workflow", "deployments" }, 
            executionType = ExecutionType.SANITY, description = "Verify admin user gets a non-network deployment using REST API and status code is OK (200)")
    public void adminGetsNonNetworkDeploymentWithSuccess() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        deploymentsApi.getDeployment(deploymentsApi.getDeployments().getOneEntry());
        deploymentsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
