package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.ProcessModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for GET "/processes/{processId}/tasks" REST API call
 * 
 * @author Cristina Axinte
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class GetProcessTasksSanityTests extends RestWorkflowTest
{
    @Autowired
    private RestProcessesApi processesApi;

    private UserModel userModel;
    private FileModel document;
    private SiteModel siteModel;
    private UserModel assignee1, assignee2, assignee3;
    private ProcessModel process;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        assignee1 = dataUser.createRandomTestUser();
        assignee2 = dataUser.createRandomTestUser();
        assignee3 = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        process = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(document)
                .createMoreReviewersWorkflowAndAssignTo(assignee1, assignee2, assignee3);
        processesApi.useRestClient(restClient);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify user who started the process gets all tasks of started process with Rest API and response is successfull (200)")
    public void userWhoStartedProcessCanGetProcessTasks() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(userModel);
        processesApi.getProcessTasks(process)
            .assertEntriesListIsNotEmpty()
            .assertTaskWithAssigneeExists(assignee1)
            .assertTaskWithAssigneeExists(assignee2)
            .assertTaskWithAssigneeExists(assignee3);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify any assignee user of the process gets all tasks of the process with Rest API and response is successfull (200)")
    public void assigneeUserCanGetAllProcessTasks() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(assignee1);
        processesApi.getProcessTasks(process)
            .assertEntriesListIsNotEmpty()
            .assertTaskWithAssigneeExists(assignee1)
            .assertTaskWithAssigneeExists(assignee2)
            .assertTaskWithAssigneeExists(assignee3);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify any assignee user of the process gets all tasks of the process with Rest API and response is successfull (200)")
    public void involvedUserCanGetAllProcessTasks() throws JsonToModelConversionException, Exception
    {
        ProcessModel process = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(document)
                .createMoreReviewersWorkflowAndAssignTo(assignee1, assignee2, assignee3);
        dataWorkflow.usingUser(assignee1).approveTask(process);
        
        restClient.authenticateUser(assignee2);
        processesApi.getProcessTasks(process)
            .assertEntriesListIsNotEmpty()
            .assertTaskWithAssigneeExists(userModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
