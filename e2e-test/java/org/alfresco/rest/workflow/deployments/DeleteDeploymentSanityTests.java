package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.requests.RestDeploymentsApi;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DeleteDeploymentSanityTests extends RestWorkflowTest
{
    @Autowired
    private RestDeploymentsApi deploymentsApi;

    private UserModel adminUser;
    private RestDeploymentModel deployment;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        deploymentsApi.useRestClient(restClient);
    }

    @Bug(id = "MNT-16996")    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, 
            executionType = ExecutionType.SANITY, description = "Verify admin user deletes a specific deployment using REST API and status code is successful (204)")
    public void adminDeletesDeploymentWithSuccess() throws JsonToModelConversionException, Exception
    {
        dataContent.assertExtensionAmpExists("alfresco-workflow-extension");
        restClient.authenticateUser(adminUser);
        // The deployment with name "customWorkflowExtentionForRest.bpmn" is created by Workflow Extention Point
        deploymentsApi.getDeployments().assertThat()
                .entriesListContains("name", "customWorkflowExtentionForRest.bpmn");
        
        deployment = deploymentsApi.getDeployments().getDeploymentByName("customWorkflowExtentionForRest.bpmn");
        deploymentsApi.deleteDeployment(deployment);
        deploymentsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id = "MNT-16996")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, 
            executionType = ExecutionType.SANITY, description = "Verify admin user cannot delete an inexistent deployment using REST API and status code is successful (204)")
    public void adminCannotDeleteInexistentDeployment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        deployment = deploymentsApi.getDeployments().getOneRandomEntry();
        deployment.onModel().setId(String.valueOf(1000));

        deploymentsApi.deleteDeployment(deployment);
        deploymentsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
