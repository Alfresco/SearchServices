package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.model.RestDeploymentModelsCollection;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for REST API call DELETE "/deployments/{deploymentId}"
 * Class has priority 100 in order to be executed last in the list of tests classes
 * 
 * @author Cristina Axinte
 *
 */

@Test(groups = { TestGroup.REST_API, TestGroup.DEPLOYMENTS, TestGroup.SANITY, TestGroup.WORKFLOW }, priority = 100)
public class DeleteDeploymentSanityTests extends RestTest
{
    private UserModel adminUser;
    private RestDeploymentModel deployment;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
    }

    @Bug(id = "MNT-16996")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.SANITY, description = "Verify admin user deletes a specific deployment using REST API and status code is successful (204)")
    public void adminDeletesDeploymentWithSuccess() throws Exception
    {
        dataContent.assertExtensionAmpExists("alfresco-workflow-extension");
        // The deployment with name "customWorkflowExtentionForRest.bpmn" is created by Workflow Extention Point
        RestDeploymentModelsCollection allDeployments = restClient.authenticateUser(adminUser).withWorkflowAPI().getDeployments();
        allDeployments.assertThat().entriesListContains("name", "customWorkflowExtentionForRest.bpmn");
        deployment = allDeployments.getDeploymentByName("customWorkflowExtentionForRest.bpmn");

        restClient.withWorkflowAPI().usingDeployment(deployment).deleteDeployment();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @Bug(id = "MNT-16996")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.SANITY, description = "Verify admin user cannot delete an inexistent deployment using REST API and status code is successful (204)")
    public void adminCannotDeleteInexistentDeployment() throws Exception
    {
        deployment = restClient.authenticateUser(adminUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        deployment.setId(String.valueOf(1000));

        restClient.withWorkflowAPI().usingDeployment(deployment).deleteDeployment();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, "1000"));
    }
}
