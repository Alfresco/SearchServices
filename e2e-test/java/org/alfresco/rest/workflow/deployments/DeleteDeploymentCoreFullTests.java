package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DeleteDeploymentCoreFullTests extends RestTest
{
    private UserModel adminUser;
    private UserModel userModel;
    private RestDeploymentModel deployment;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
    }

    @Bug(id = "MNT-16996")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify deleteDeployment is unsupported for empty deployment id with REST API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.WORKFLOW })
    public void deleteDeploymentIsUnsupportedForEmptyId() throws Exception
    {
        deployment = restClient.authenticateUser(adminUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        deployment.setId("");
        restClient.withWorkflowAPI().usingDeployment(deployment).deleteDeployment();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @Bug(id = "MNT-16996")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify deleteDeployment is forbidden using non admin user or different user than creator with REST API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.WORKFLOW })
    public void deleteDeploymentUsingNonAdminUser() throws Exception
    {
        deployment = restClient.authenticateUser(adminUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        restClient.authenticateUser(userModel).withWorkflowAPI().usingDeployment(deployment).deleteDeployment();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
}
