package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessModelsCollection;
import org.alfresco.rest.model.RestProcessVariableCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetProcessVariablesCoreTests extends RestTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, admin;
    private RestProcessModel processModel;
    private RestProcessVariableCollection variables;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        admin = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to retrieve network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void getProcessVariablesWithAdminFromSameNetwork() throws JsonToModelConversionException, Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(admin).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);   
        processModel = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().usingProcess(networkProcess1).getProcess();

        variables = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().usingProcess(processModel).getProcessVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variables.assertThat().entriesListIsNotEmpty();
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from different network is not able to retrieve network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void getProcessVariablesWithAdminFromDifferentNetwork() throws JsonToModelConversionException, Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(admin).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);   
        processModel = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().usingProcess(networkProcess1).getProcess();

        variables = restClient.authenticateUser(admin).withWorkflowAPI().usingProcess(processModel).getProcessVariables();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process variables using invalid process ID")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessVariablesUsingInvalidProcessId() throws JsonToModelConversionException, Exception
    {        
        restClient.authenticateUser(userWhoStartsTask).withParams("maxItems=2").withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        RestProcessModelsCollection processes = restClient.authenticateUser(userWhoStartsTask).withParams("maxItems=2").withWorkflowAPI().getProcesses();
        processModel  = processes.assertThat().entriesListIsNotEmpty().when().getOneRandomEntry().onModel();
        
        String id = RandomStringUtils.randomAlphanumeric(10);
        processModel.setId(id);
        variables = restClient.withParams("maxItems=2").withWorkflowAPI().usingProcess(processModel).getProcessVariables();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, id));
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process variables using empty process ID")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessVariablesUsingEmptyProcessId() throws JsonToModelConversionException, Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withParams("maxItems=2").withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processModel.setId("");
        variables = restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Delete process then get process variables, status OK should be returned")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessVariablesForADeletedProcess() throws JsonToModelConversionException, Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withParams("maxItems=2").withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.authenticateUser(admin).withWorkflowAPI().usingProcess(processModel).deleteProcess();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.authenticateUser(admin).withParams("maxItems=2").withWorkflowAPI().usingProcess(processModel).getProcess();

        variables = restClient.authenticateUser(admin).withWorkflowAPI().usingProcess(processModel).getProcessVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variables.assertThat().entriesListIsNotEmpty();
    }
}
