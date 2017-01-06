package org.alfresco.rest.workflow.deployments;

import java.util.List;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.model.RestDeploymentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 12/7/2016.
 */
public class GetDeploymentsCoreTests extends RestTest
{
    private UserModel adminUser, adminTenantUser;
    private RestDeploymentModelsCollection deployments;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify non admin user is not able to get non-network deployments using REST API and status code is Forbidden")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.CORE})
    public void nonAdminUserCanNotGetNonNetworkDeployments() throws Exception
    {
        UserModel userModel = dataUser.createRandomTestUser();
        restClient.authenticateUser(userModel).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify non admin user is not able to get network deployments using REST API and status code is Forbidden")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.CORE, TestGroup.NETWORKS})
    public void nonAdminUserCanNotGetNetworkDeployments() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);

        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        restClient.authenticateUser(tenantUser).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Bug(id = "MNT-16996")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify get deployments returns an empty list after deleting all network deployments.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.CORE, TestGroup.NETWORKS})
    public void getNetworkDeploymentsAfterDeletingAllNetworkDeployments() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);
        List<RestDeploymentModel> networkDeployments = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getEntries();
        for (RestDeploymentModel networkDeployment: networkDeployments)
        {
            restClient.withWorkflowAPI().usingDeployment(networkDeployment.onModel()).deleteDeployment();
            restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        }
        deployments = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deployments.assertThat().entriesListIsEmpty();
    }

}
