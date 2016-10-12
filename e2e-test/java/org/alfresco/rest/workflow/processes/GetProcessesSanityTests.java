package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/11/2016.
 */
@Test(groups = { "rest-api", "processes", "sanity" })
public class GetProcessesSanityTests extends RestWorkflowTest
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
        task = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
        processesApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api",
            "processes" }, executionType = ExecutionType.SANITY, description = "Verify User gets all processes started by him using REST API and status code is OK (200)")
    public void getProcessesByUserWhoStartedProcess() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        processesApi.getProcesses().assertProcessExists(task.getNodeRef());
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "processes" }, executionType = ExecutionType.SANITY, description = "Verify User gets all processes assigned to him using REST API and status code is OK (200)")
    public void getProcessesByAssignedUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(assignee);
        processesApi.getProcesses().assertProcessExists(task.getNodeRef());
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "processes" }, executionType = ExecutionType.SANITY, description = "Verify User that is not involved in a process can not get that process using REST API and status code is OK (200)")
    public void getProcessesByAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(anotherUser);
        processesApi.getProcesses().assertProcessDoesNotExist(task.getNodeRef());
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "processes" }, executionType = ExecutionType.SANITY, description = "Verify Admin gets all processes, even if he isn't involved in a process, using REST API and status code is OK (200)")
    public void getProcessesByAdmin() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(dataUser.getAdminUser());
        processesApi.getProcesses().assertProcessExists(task.getNodeRef());
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
