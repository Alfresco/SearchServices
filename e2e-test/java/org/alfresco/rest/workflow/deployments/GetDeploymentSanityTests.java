package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for REST API call GET "/deployments/{deploymentId}"
 *
 * @author Cristina Axinte
 *
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.SANITY })
public class GetDeploymentSanityTests extends RestTest
{
    private UserModel adminUser;
    private UserModel anotherUser;
    private RestDeploymentModel expectedDeployment, actualDeployment;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        anotherUser = dataUser.createRandomTestUser();

        expectedDeployment = restClient.authenticateUser(adminUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.SANITY, description = "Verify admin user gets a non-network deployment using REST API and status code is OK (200)")
    public void adminGetsNonNetworkDeploymentWithSuccess() throws JsonToModelConversionException, Exception
    {
        actualDeployment = restClient.authenticateUser(adminUser).withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        actualDeployment.assertThat().field("deployedAt").isNotEmpty()
        	.and().field("name").is(expectedDeployment.getName())
        	.and().field("id").equals(expectedDeployment.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.SANITY, description = "Verify non admin user is forbidden to get a non-network deployment using REST API (403)")
    public void nonAdminIsForbiddenToGetNonNetworkDeployment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(anotherUser).withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
        	.assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
}
