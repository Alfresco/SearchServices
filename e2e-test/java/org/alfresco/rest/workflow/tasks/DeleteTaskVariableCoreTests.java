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
public class DeleteTaskVariableCoreTests extends RestTest
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
            description = "Delete task variable with any user")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void deleteTaskVarialbleByAnyUser() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        
        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables()
            .assertThat().entriesListDoesNotContain("name", variableModel.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable with invalid type")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void deleteTaskVariableInvalidType() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        variableModel.setType("invalid-type");
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables()
        .       assertThat().entriesListDoesNotContain("name", variableModel.getName());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable with invalid name")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void deleteTaskVariableInvalidName() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = new RestVariableModel("local", "<>.,;/|-+=%", "d:text", "invalid name");
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Create, update, delete task variable with any user")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void createUpdateDeleteTaskVarialbleByAnyUser() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        
        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        variableModel.setName("new-variable");
        restClient.withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables()
            .assertThat().entriesListDoesNotContain("name", variableModel.getName());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable by non assigned user")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void deleteTaskVarialbleByNonAssignedUser() throws Exception
    {
        UserModel nonAssigned = dataUser.createRandomTestUser();
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.authenticateUser(nonAssigned).withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task variable by inexistent user")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void deleteTaskVarialbleByInexistentUser() throws Exception
    {
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.authenticateUser(UserModel.getRandomUserModel())
            .withWorkflowAPI()
            .usingTask(taskModel)
            .deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
