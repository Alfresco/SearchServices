package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
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
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
public class UpdateTaskVariableCoreTests extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private FileModel fileModel;
    private UserModel assigneeUser;
    private TaskModel taskModel;
    private RestVariableModel taskVariable;
    private RestVariableModel variableModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable by user who started the process")
    public void updateTaskVariableByUserWhoStartedProcess() throws Exception
    {
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.setValue("new-value");
        variableModel.setName("new-name");
        taskVariable = restClient.authenticateUser(userModel).
                withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskVariable.assertThat().field("value").is("new-value")
            .and().field("name").is("new-name");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with symbols in name")
    public void updateTaskVariableWithSymbolsInName() throws Exception
    {
        String symbolName = "<>.,;-'+=%|[]#*&-+";
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.setName(symbolName);
        taskVariable = restClient.authenticateUser(userModel).
                withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskVariable.assertThat().field("name").is(symbolName);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid task id")
    public void updateTaskVariableWithInvalidTaskId() throws Exception
    {
        TaskModel invalidTask = new TaskModel(userModel.getUsername());
        invalidTask.setId("invalid-task-id");
        taskVariable = restClient.authenticateUser(userModel).
                withWorkflowAPI().usingTask(invalidTask)
                .updateTaskVariable(RestVariableModel.getRandomTaskVariableModel("local", "d:text"));
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidTask.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid scope")
    public void updateTaskVariableWithInvalidScope() throws Exception
    {
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.setScope("invalid-scope");
        taskVariable = restClient.authenticateUser(userModel).
                withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.ILLEGAL_SCOPE, "invalid-scope"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid type")
    public void updateTaskVariableWithInvalidType() throws Exception
    {
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.setType("d:invalidType");
        taskVariable = restClient.authenticateUser(userModel).
                withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE, "d:invalidType"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with symbols in value")
    public void updateTaskVariableWithSymbolsInValue() throws Exception
    {
        String symbolValue = "<>.,;-'+=%|[]#*&-+/\\#!@";
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.setValue(symbolValue);
        taskVariable = restClient.authenticateUser(userModel).
                withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskVariable.assertThat().field("value").is(symbolValue);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable by non assigned user")
    public void updateTaskVariableByNonAssignedUser() throws Exception
    {
        UserModel nonAssigned = dataUser.createRandomTestUser();
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.setName("new-name");
        taskVariable = restClient.authenticateUser(nonAssigned)
                .withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable by inexistent user")
    public void updateTaskVariableByInexistentser() throws Exception
    {
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.setName("new-name");
        taskVariable = restClient.authenticateUser(UserModel.getRandomUserModel())
                .withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

}