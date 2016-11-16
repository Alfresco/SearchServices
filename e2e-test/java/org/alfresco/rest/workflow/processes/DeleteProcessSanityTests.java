package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/12/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class DeleteProcessSanityTests extends RestWorkflowTest
{
    private UserModel userWhoAddsProcess, assignee, anotherUser;
    private RestProcessModel process;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userWhoAddsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        anotherUser = dataUser.createRandomTestUser();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User is able to delete process started by him using REST API and status code is OK (204)")
    public void deleteProcessByUserWhoStartedProcess() throws Exception
    {
        process = restClient.authenticateUser(userWhoAddsProcess).addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.usingProcess(process).deleteProcess();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.getProcesses().assertThat().entriesListDoesNotContain("id", process.getId());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User is able to delete process assigned to him using REST API and status code is OK (204)")
    public void deleteProcessByAssignedUser() throws Exception
    {
        process = restClient.authenticateUser(userWhoAddsProcess).addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(assignee).usingProcess(process).deleteProcess();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.getProcesses().assertThat().entriesListDoesNotContain("id", process.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify User that is not involved in a process is not authorized to delete it using REST API and status code is 403")
    public void deleteProcessByAnotherUser() throws Exception
    {
        process = restClient.authenticateUser(userWhoAddsProcess).addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(anotherUser).usingProcess(process).deleteProcess();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary("user is not allowed to access information about process");
    }
}
