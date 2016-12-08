package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
public class AddTaskVariablesCoreTests extends RestTest
{
    private UserModel userModel, userWhoStartsTask;
    private SiteModel siteModel;
    private FileModel fileModel;
    private UserModel assigneeUser;
    private TaskModel taskModel;
    private RestVariableModel restVariablemodel;   

    private UserModel adminUser;
    private String taskId;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userWhoStartsTask = dataUser.createRandomTestUser();
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        adminUser = dataUser.getAdminUser();
        taskId = taskModel.getId();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case invalid variableBody is provided")
    public void failedAddingTaskVariableIfInvalidBodyIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestVariableModel invalidVariableModel = RestVariableModel.getRandomTaskVariableModel("instance", "d:char");
        restVariablemodel = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(invalidVariableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Illegal value for variable scope: 'instance'.");

    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case empty body type is provided")
    public void failedAddingTaskVariableIfEmptyBodyIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestVariableModel invalidVariableModel = RestVariableModel.getRandomTaskVariableModel("", "");
        restVariablemodel = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(invalidVariableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary("Variable scope is required and can only be 'local' or 'global'.");

    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case incomplete body type is provided")
    public void failedAddingTaskVariableIfIncompleteBodyIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{\"name\": \"missingVariableScope\",\"value\": \"test\",\"type\": \"d:text\"}",
                "tasks/{taskId}/variables", taskId);
        restClient.processModel(RestVariableModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary("Variable scope is required and can only be 'local' or 'global'.");

    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case incomplete body - missing required: name type is provided")
    public void failedAddingTaskVariableIfIncompleteRequiredBodyIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{\"scope\": \"local\",\"value\": \"missingVariableName\",\"type\": \"d:text\"}",
                "tasks/{taskId}/variables", taskId);
        restClient.processModel(RestVariableModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Variable name is required.");

    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case invalid type is provided")
    public void failedAddingTaskVariableIfInvalidTypeIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestVariableModel invalidVariableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:char");
        taskModel.setId(taskId);
        restVariablemodel = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(invalidVariableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Unsupported type of variable: 'd:char'.");

    }

    @Bug(id = "ACE-5674")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case invalid type prefix is provided")
    public void failedAddingTaskVariableIfInvalidTypePrefixIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestVariableModel invalidVariableModel = RestVariableModel.getRandomTaskVariableModel("local", "ddm:text");
        restVariablemodel = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(invalidVariableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Namespace prefix ddm is not mapped to a namespace URI");

    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case invalid scope is provided")
    public void failedAddingTaskVariableIfInvalidScopeIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestVariableModel invalidVariableModel = RestVariableModel.getRandomTaskVariableModel("instance", "d:text");
        restVariablemodel = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(invalidVariableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Illegal value for variable scope: 'instance'.");

    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case invalid task id is provided")
    public void failedAddingTaskVariableIfInvalidTaskIdIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskModel.setId(taskModel.getId() + "TEST");
        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary("The entity with id: " + taskModel.getId() + " was not found");

    }

    @Bug(id = "ACE-5673")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task variable is falling in case invalid task id is provided")
    public void failedAddingTaskVariableIfInvalidValueIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);

        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:int");
        variableModel.setValue("invalidValue");

        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("For input string: \"invalidValue\"");
    }
}
