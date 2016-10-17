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

    private UserModel adminUser;
    private UserModel anotherUser;
    private RestDeploymentModel deployment;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        anotherUser = dataUser.createRandomTestUser();
        
        deploymentsApi.useRestClient(restClient);
        restClient.authenticateUser(adminUser);
        deployment = deploymentsApi.getDeployments().getOneEntry();
    }

    @TestRail(section = { "rest-api", "workflow", "deployments" }, 
            executionType = ExecutionType.SANITY, description = "Verify admin user gets a non-network deployment using REST API and status code is OK (200)")
    public void adminGetsNonNetworkDeploymentWithSuccess() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        deploymentsApi.getDeployment(deployment);
        deploymentsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "workflow", "deployments" }, 
            executionType = ExecutionType.SANITY, description = "Verify non admin user is forbidden to get a non-network deployment using REST API (403)")
    public void nonAdminIsForbiddenToGetNonNetworkDeployment() throws JsonToModelConversionException, Exception
    {        
        restClient.authenticateUser(anotherUser);
        deploymentsApi.getDeployment(deployment);
        deploymentsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
}
