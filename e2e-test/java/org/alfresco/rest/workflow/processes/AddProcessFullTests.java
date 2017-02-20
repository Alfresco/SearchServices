package org.alfresco.rest.workflow.processes;

import javax.json.JsonObject;

import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class AddProcessFullTests extends RestTest
{
    private UserModel userWhoStartsProcess, assignee;
    RestProcessModel addedProcess;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        assignee = dataUser.createRandomTestUser();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify network user cannot start new process with processDefinitionKey from another network using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL, TestGroup.NETWORKS})
    public void networkUserCannotStartNewProcessWithProcessDefinitionFromAnotherNetwork() throws JsonToModelConversionException, Exception
    { 
        UserModel adminTenant1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenant1);
        RestProcessDefinitionModel firstProcessDefTenant1 = restClient.authenticateUser(adminTenant1).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        
        UserModel adminTenant2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenant2);
        UserModel assigneeTenant2 = dataUser.usingUser(adminTenant2).createUserWithTenant("assigneeT2");

        String processDefinitionKey = firstProcessDefTenant1.getId().substring(0,firstProcessDefTenant1.getId().indexOf(":"));
        addedProcess = restClient.authenticateUser(adminTenant2).withWorkflowAPI().addProcess(processDefinitionKey, assigneeTenant2, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, processDefinitionKey))
            .containsSummary(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, processDefinitionKey))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify network user can start new process with processDefinitionKey from same network using REST API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL, TestGroup.NETWORKS})
    public void networkUserCanStartNewProcessWithProcessDefinitionFromSameNetwork() throws JsonToModelConversionException, Exception
    { 
        UserModel adminTenant1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenant1);
        RestProcessDefinitionModel firstProcessDefTenant1 = restClient.authenticateUser(adminTenant1).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        
        UserModel processStarterTenant1 = dataUser.usingUser(adminTenant1).createUserWithTenant("startert1");
        UserModel assigneeTenant1 = dataUser.usingUser(adminTenant1).createUserWithTenant("assigneet1");

        
        String processDefinitionKey = firstProcessDefTenant1.getKey();
        addedProcess = restClient.authenticateUser(processStarterTenant1).withWorkflowAPI().addProcess(processDefinitionKey, assigneeTenant1, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        addedProcess.assertThat().field("processDefinitionId").is(firstProcessDefTenant1.getId())
            .and().field("startUserId").is(processStarterTenant1.getEmailAddress().toLowerCase())
            .and().field("processDefinitionKey").is(processDefinitionKey);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid processDefinitionKey using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void userStartsNewProcessWithInvalidProcessDefinitionKey() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();

        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcess("activitiAdhocc", assignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, "activitiAdhocc"))
            .containsSummary(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, "activitiAdhocc"))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @Bug(id = "REPO-1970")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid processDefinitionKey using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void userStartsNewProcessWithEmptyVariablesBody() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();

        JsonObject postJson = JsonBodyGenerator.defineJSON()
                .add("processDefinitionKey", "activitiAdhoc")
                .add("variables", 
                        JsonBodyGenerator.defineJSON().build()
                        ).build();
        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcessWithBody(postJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, "activitiAdhoc"));
    }
    
    @Bug(id = "REPO-1970")
    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid processDefinitionKey using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void userStartsNewProcessWithNoAssigneeInVariablesBody() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();

        JsonObject postJson = JsonBodyGenerator.defineJSON()
                .add("processDefinitionKey", "activitiAdhoc")
                .add("variables", 
                        JsonBodyGenerator.defineJSON()
                                .add("bpm_sendEMailNotifications", false)
                                .add("bpm_workflowPriority", Priority.Low.getLevel()).build()
                     ).build();
        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcessWithBody(postJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, "activitiAdhoc"));
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid assignee using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void userStartsNewProcessWithInvalidAssignee() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();
        UserModel invalidAssignee = new UserModel("invalidAssignee", "password");

        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcess("activitiAdhoc", invalidAssignee, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.INVALID_USER_ID, invalidAssignee.getUsername()))
            .containsSummary(String.format(RestErrorModel.INVALID_USER_ID, invalidAssignee.getUsername()))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @Bug(id = "REPO-1970")
    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid sendEMailNotifications value using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void userStartsNewProcessWithInvalidEmailNotification() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();
        JsonObject postJson = JsonBodyGenerator.defineJSON()
                .add("processDefinitionKey", "activitiAdhoc")
                .add("variables", 
                        JsonBodyGenerator.defineJSON()
                                .add("bpm_assignee", assignee.getUsername())
                                .add("bpm_sendEMailNotifications", "111")
                                .add("bpm_workflowPriority", Priority.Low.getLevel()).build()
                     ).build();

        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcessWithBody(postJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.UNSUPPORTED_TYPE, "111"))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @Bug(id = "REPO-1970")
    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot start new process with invalid priority using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void userStartsNewProcessWithInvalidPriority() throws JsonToModelConversionException, Exception
    { 
        userWhoStartsProcess = dataUser.createRandomTestUser();
        JsonObject postJson = JsonBodyGenerator.defineJSON()
                .add("processDefinitionKey", "activitiAdhoc")
                .add("variables", 
                        JsonBodyGenerator.defineJSON()
                                .add("bpm_assignee", assignee.getUsername())
                                .add("bpm_sendEMailNotifications", false)
                                .add("bpm_workflowPriority", "test").build()
                     ).build();

        addedProcess = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcessWithBody(postJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.UNSUPPORTED_TYPE, "test"))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    } 
}
