package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class AddProcessVariableCoreTests extends RestTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, adminUser, adminTenantUser, secondAdminTenantUser, tenantUser;
    private RestProcessModel processModel;
    private RestProcessVariableModel variableModel, processVariable;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify addProcessVariable by admin in other network with REST API and status code is FORBIDDEN (403)")
    @Test(groups = { TestGroup.NETWORKS })
    public void addProcessVariableByAdminInOtherNetworkIsForbidden() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        secondAdminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        restClient.authenticateUser(adminUser).usingTenant().createTenant(secondAdminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");

        restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .addProcess("activitiAdhoc", tenantUser, false, CMISUtil.Priority.Normal);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();

        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify addProcessVariable by any user with REST API and status code is CREATED (201)")
    public void addProcessVariableByAnyUser() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();

        processVariable = restClient.authenticateUser(assignee).withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processVariable.assertThat().field("name").is(variableModel.getName())
                .and().field("type").is(variableModel.getType())
                .and().field("value").is(variableModel.getValue());

        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                .assertThat().entriesListContains("name", processVariable.getName());
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify addProcessVariable by any user for invalid processID with REST API and status code is NOT_FOUND (404)")
    public void addProcessVariableForInvalidProcessIdIsNotFound() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processModel.setId("invalidProcessID");

        processVariable = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidProcessID"));
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify addProcessVariable by any user for empty processID with REST API and status code is NOT_FOUND (404)")
    public void addProcessVariableForInvalidProcessIdIsEmpty() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processModel.setId("");

        processVariable = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }
}
