package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessModelsCollection;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/18/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class AddProcessSanityTests extends RestWorkflowTest
{
    private UserModel userWhoStartsProcess, assignee;
    private UserModel adminTenantUser, tenantUserWhoStartsProcess, tenantAssignee;
    private RestProcessModel addedProcess;
    private RestProcessModelsCollection processes;

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify non network user is able to start new process using REST API and status code is OK (200)")
    public void nonNetworkUserStartsNewProcess() throws JsonToModelConversionException, Exception
    {
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();

        addedProcess = restClient.authenticateUser(userWhoStartsProcess).onWorkflowAPI().addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        addedProcess.assertThat().field("id").is(addedProcess.getId())
                    .and().field("startUserId").is(addedProcess.getStartUserId());

        processes = restClient.onWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processes.assertThat().entriesListContains("id", addedProcess.getId());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, description = "Verify network user is able to start new process using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.NETWORKS })
    public void networkUserStartsNewProcess() throws JsonToModelConversionException, Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser())
                .usingTenant().createTenant(adminTenantUser);
        tenantUserWhoStartsProcess = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("u2Tenant");

        addedProcess = restClient.authenticateUser(tenantUserWhoStartsProcess).onWorkflowAPI().addProcess("activitiAdhoc", tenantAssignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        addedProcess.assertThat().field("id").is(addedProcess.getId())
                    .and().field("startUserId").is(addedProcess.getStartUserId());

        processes = restClient.onWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processes.assertThat().entriesListContains("id", addedProcess.getId());
    }
}
