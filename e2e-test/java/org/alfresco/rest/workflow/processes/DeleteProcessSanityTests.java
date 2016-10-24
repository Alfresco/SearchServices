package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/12/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class DeleteProcessSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestProcessesApi processesApi;

    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, anotherUser;
    private TaskModel task;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        anotherUser = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        processesApi.useRestClient(restClient);
    }   

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User is able to delete process started by him using REST API and status code is OK (204)")
    public void deleteProcessByUserWhoStartedProcess() throws Exception
    {
        task = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);  
        restClient.authenticateUser(userWhoStartsTask);
        processesApi.deleteProcess(task);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);

        processesApi.getProcesses().entriesListDoesNotContain("id", task.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User is able to delete process assigned to him using REST API and status code is OK (204)")
    public void deleteProcessByAssignedUser() throws Exception
    {
        task = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);  
        restClient.authenticateUser(assignee);
        processesApi.deleteProcess(task);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);

        processesApi.getProcesses().entriesListDoesNotContain("id", task.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User that is not involved in a process is not authorized to delete it using REST API and status code is 403")
    public void deleteProcessByAnotherUser() throws Exception
    {
        task = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
        restClient.authenticateUser(anotherUser);
        processesApi.deleteProcess(task);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

}
