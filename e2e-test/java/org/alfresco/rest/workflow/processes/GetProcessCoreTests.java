package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetProcessCoreTests extends RestTest
{
    private UserModel userWhoStartsProcess, assignee, adminUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that using invalid process ID returns status code 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE})
    public void invalidProcessIdTest() throws Exception
    {
        RestProcessModel newProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.High);
        String processId = RandomStringUtils.randomAlphanumeric(10);
        newProcess.setId(processId);
        restClient.authenticateUser(dataUser.getAdminUser())
                .withWorkflowAPI().usingProcess(newProcess).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, processId));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES}, executionType = ExecutionType.REGRESSION, 
            description = "Verify that tenant user cannot get process from another network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    @Bug(id = "MNT-17238")
    public void tenantUserCannotGetProcessFromAnotherNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser1);
        
        UserModel adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser2);
        
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");
        UserModel tenantUser2 = dataUser.usingUser(adminTenantUser2).createUserWithTenant("uTenant2");
        
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);       
        
        restClient.authenticateUser(tenantUser2).withWorkflowAPI().usingProcess(networkProcess1).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that tenant user can get process from the same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void tenantUserCanGetProcessFromTheSameNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");
        
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);       
        
        restClient.authenticateUser(tenantUser1).withWorkflowAPI().usingProcess(networkProcess1).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networkProcess1.assertThat().field("id").is(networkProcess1.getId())
            .and().field("startUserId").is(networkProcess1.getStartUserId());;
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES}, executionType = ExecutionType.REGRESSION, 
            description = "Verify that non network user cannot get process from a network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    @Bug(id = "MNT-17238")
    public void nonNetworkUserCannotAccessNetworkprocess() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");
        
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);       
        
        restClient.authenticateUser(adminUser).withWorkflowAPI().usingProcess(networkProcess1).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

}