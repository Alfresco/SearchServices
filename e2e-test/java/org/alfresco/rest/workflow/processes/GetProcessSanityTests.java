package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/19/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.SANITY })
public class GetProcessSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestProcessesApi processesApi;

    private UserModel userWhoStartsProcess, assignee;
    private RestProcessModel addedProcess;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        processesApi.useRestClient(restClient);
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        restClient.authenticateUser(userWhoStartsProcess);
        addedProcess = processesApi.addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.High);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify user is able to get the process started by him using REST API and status code is OK (200)")
    public void getProcessByOwner() throws Exception
    {
        restClient.authenticateUser(userWhoStartsProcess);
        processesApi.getProcess(addedProcess);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }


}
