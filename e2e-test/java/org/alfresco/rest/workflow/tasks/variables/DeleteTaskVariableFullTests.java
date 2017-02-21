package org.alfresco.rest.workflow.tasks.variables;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author bogdan.bocancea
 */
public class DeleteTaskVariableFullTests extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private FileModel fileModel;
    private TaskModel taskModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(userModel);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable twice")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void deleteTaskVariableTwice() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        
        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables()
            .assertThat().entriesListDoesNotContain("name", variableModel.getName());
        
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
              .assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, variableModel.getName()))
                                .stackTraceIs(RestErrorModel.STACKTRACE)
                                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable with empty variable name")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void deleteTaskEmptyVariableName() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");

        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        variableModel.setName("");
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
              .assertLastError().containsErrorKey(RestErrorModel.DELETE_EMPTY_ARGUMENT)
                                .containsSummary(RestErrorModel.DELETE_EMPTY_ARGUMENT)
                                .stackTraceIs(RestErrorModel.STACKTRACE)
                                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable with empty variable scope")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void deleteTaskEmptyVariableScope() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");

        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        variableModel.setScope("");
        variableModel.setType("");
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables()
            .assertThat().entriesListDoesNotContain("name", variableModel.getName());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable with invalid variable name")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void deleteTaskInvalidVariableName() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");

        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        variableModel.setName("invalid-name");
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
              .assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalid-name"))
                                .stackTraceIs(RestErrorModel.STACKTRACE)
                                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to remove task variables")
    public void addTaskVariablesByTenantAdmin() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser1);
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", adminTenantUser1, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser1)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser1);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.withWorkflowAPI()
            .usingTask(task.onModel())
                .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withWorkflowAPI().usingTask(task.onModel()).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(task.onModel()).getTaskVariables()
            .assertThat().entriesListDoesNotContain("name", variableModel.getName());
    }
    
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the other network is not able to remove task variables")
    public void addTaskVariablesByTenantAdminOtherNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        UserModel adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser1);
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser2);

        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", adminTenantUser1, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser1)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser1);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.withWorkflowAPI()
            .usingTask(task.onModel())
                .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().usingTask(task.onModel()).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
