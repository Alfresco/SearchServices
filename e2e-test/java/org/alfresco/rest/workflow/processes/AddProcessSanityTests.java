package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.rest.requests.RestTenantApi;
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
 * Created by Claudia Agache on 10/18/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class AddProcessSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestProcessesApi processesApi;

    @Autowired
    private RestTenantApi tenantApi;
    
    private UserModel userWhoStartsProcess, assignee;
    private UserModel adminTenantUser, tenantUserWhoStartsProcess, tenantAssignee;
    private RestProcessModel addedProcess;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        processesApi.useRestClient(restClient);
        tenantApi.useRestClient(restClient);
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify non network user is able to start new process using REST API and status code is OK (200)")
    public void nonNetworkUserStartsNewProcess() throws JsonToModelConversionException, Exception
    {
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();

        restClient.authenticateUser(userWhoStartsProcess);
        addedProcess = processesApi.addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        addedProcess.and().assertField("startUserId").is(userWhoStartsProcess.getUsername());
        
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
        processesApi.getProcesses().assertEntriesListIsNotEmpty();
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify network user is able to start new process using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.NETWORKS })
    public void networkUserStartsNewProcess() throws JsonToModelConversionException, Exception
    {
        tenantApi.createTenant(adminTenantUser);
        restClient.authenticateUser(adminTenantUser);
        tenantUserWhoStartsProcess = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("u2Tenant");

        restClient.authenticateUser(tenantUserWhoStartsProcess);
        addedProcess = processesApi.addProcess("activitiAdhoc", tenantAssignee, false, Priority.Normal);
        addedProcess.and().assertField("startUserId").is(tenantUserWhoStartsProcess.getUsername());
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
        processesApi.getProcesses().assertEntriesListIsNotEmpty();
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
