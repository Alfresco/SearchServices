package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
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
 * Created by Claudia Agache on 10/19/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class GetProcessSanityTests extends RestWorkflowTest
{
    private UserModel userWhoStartsProcess, assignee;
    private RestProcessModel addedProcess, process;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        addedProcess = restClient.authenticateUser(userWhoStartsProcess).addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.High);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify user is able to get the process started by him using REST API and status code is OK (200)")
    public void getProcessByOwner() throws Exception
    {
        process = restClient.authenticateUser(userWhoStartsProcess).usingProcess(addedProcess).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        process.assertThat().field("id").is(addedProcess.getId())
               .and().field("startUserId").is(addedProcess.getStartUserId());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify user is able to get the process assigned to him using REST API and status code is OK (200)")
    public void getProcessByAssignee() throws Exception
    {
        process = restClient.authenticateUser(assignee).usingProcess(addedProcess).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        process.assertThat().field("id").is(addedProcess.getId())
                .and().field("startUserId").is(addedProcess.getStartUserId());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify admin is able to get any process using REST API and status code is OK (200)")
    public void getProcessByAdmin() throws Exception
    {
        process = restClient.authenticateUser(dataUser.getAdminUser()).usingProcess(addedProcess).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        process.assertThat().field("id").is(addedProcess.getId())
                .and().field("startUserId").is(addedProcess.getStartUserId());
    }
}
