package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 12/7/2016.
 */
public class GetDeploymentCoreTests extends RestTest
{
    private UserModel adminUser, adminTenantUser;
    private RestDeploymentModel expectedDeployment, actualDeployment;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify if get deployment request returns status code 404 when invalid deploymentId is used.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.CORE })
    public void getNonNetworkDeploymentUsingInvalidDeploymentId() throws Exception
    {
        expectedDeployment = restClient.authenticateUser(adminUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        expectedDeployment.setId("invalidId");

        restClient.withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidId"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify if network admin user gets a network deployment using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.CORE, TestGroup.NETWORKS})
    public void adminGetsNetworkDeploymentWithSuccess() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);

        expectedDeployment = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        actualDeployment = restClient.withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        actualDeployment.assertThat().field("deployedAt").isNotEmpty()
                .and().field("name").is(expectedDeployment.getName())
                .and().field("id").is(expectedDeployment.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify non admin user is forbidden to get a network deployment using REST API (403)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.CORE, TestGroup.NETWORKS })
    public void nonAdminUserIsForbiddenToGetNetworkDeployment() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);
        expectedDeployment = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");

        restClient.authenticateUser(tenantUser).withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
}
