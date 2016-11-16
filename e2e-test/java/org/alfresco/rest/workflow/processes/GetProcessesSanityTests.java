package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestProcessModelsCollection;
import org.alfresco.utility.model.*;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/11/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class GetProcessesSanityTests extends RestWorkflowTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, anotherUser;
    private TaskModel task;
    private RestProcessModelsCollection allProcesses;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        anotherUser = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        task = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User gets all processes started by him using REST API and status code is OK (200)")
    public void getProcessesByUserWhoStartedProcess() throws Exception
    {
        allProcesses = restClient.authenticateUser(userWhoStartsTask).onWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        allProcesses.assertThat().entriesListContains("id", task.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User gets all processes assigned to him using REST API and status code is OK (200)")
    public void getProcessesByAssignedUser() throws Exception
    {
        allProcesses = restClient.authenticateUser(assignee).onWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        allProcesses.assertThat().entriesListContains("id", task.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User that is not involved in a process can not get that process using REST API and status code is OK (200)")
    public void getProcessesByAnotherUser() throws Exception
    {
        allProcesses = restClient.authenticateUser(anotherUser).onWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        allProcesses.assertThat().entriesListDoesNotContain("id", task.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify Admin gets all processes, even if he isn't involved in a process, using REST API and status code is OK (200)")
    public void getProcessesByAdmin() throws Exception
    {
        allProcesses = restClient.authenticateUser(dataUser.getAdminUser()).onWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        allProcesses.assertThat().entriesListContains("id", task.getNodeRef());
    }
}
