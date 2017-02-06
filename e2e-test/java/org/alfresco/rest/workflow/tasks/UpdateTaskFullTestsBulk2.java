package org.alfresco.rest.workflow.tasks;

import javax.json.JsonObject;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdateTaskFullTestsBulk2 extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private FileModel fileModel;
    private UserModel assigneeUser;
    private TaskModel taskModel;
    private RestTaskModel restTaskModel;
    private UserModel adminUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
    }

    @BeforeMethod(alwaysRun = true)
    public void createTask() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to claimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToClaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("claimed");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to delegated")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToDelegated() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
     
        JsonObject inputJson = JsonBodyGenerator.defineJSON().add("state", "delegated").add("assignee", assigneeUser.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to resolved")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToResolved() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("resolved");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to unclaimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToUnclaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
    }
    
    @Bug(id = "REPO-1982")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from delegated to unclaimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromDelegatedToUnclaimed() throws Exception
    {
        JsonObject inputJson = JsonBodyGenerator.defineJSON().add("state", "delegated").add("assignee", assigneeUser.getUsername()).build();
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated");
        
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task cannot be updated from delegated to claimed since it is already claimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCannotUpdateTaskFromDelegatedToClaimed() throws Exception
    {
        JsonObject inputJson = JsonBodyGenerator.defineJSON().add("state", "delegated").add("assignee", assigneeUser.getUsername()).build();
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated");
        
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT).assertLastError()
            .containsErrorKey(RestErrorModel.TASK_ALREADY_CLAIMED)
            .containsSummary(RestErrorModel.TASK_ALREADY_CLAIMED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Bug(id = "REPO-1924")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from delegated to completed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromDelegatedToCompleted() throws Exception
    {
        JsonObject inputJson = JsonBodyGenerator.defineJSON().add("state", "delegated").add("assignee", assigneeUser.getUsername()).build();
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated");
        
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError()
            .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
            .containsSummary(RestErrorModel.DELEGATED_TASK_CAN_NOT_BE_COMPLETED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from delegated to resolved")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromDelegatedToResolved() throws Exception
    {
        JsonObject inputJson = JsonBodyGenerator.defineJSON().add("state", "delegated").add("assignee", assigneeUser.getUsername()).build();
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated");
        
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("resolved");
    }
    
    @Bug(id = "REPO-1982")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from resolved to unclaimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromResolvedToUnclaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
     
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("resolved");
    }
    
    @Bug(id = "REPO-1982")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task cannot be updated from resolved to claimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskFromResolvedToClaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
     
        // assignee tries to claim the task 
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
        
        // owner tries to claim the task 
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("resolved");
    }
    
    @Bug(id = "REPO-1982")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from resolved to delegated")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCannotUpdateTaskFromResolvedToDelegated() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
     
        JsonObject inputJson = JsonBodyGenerator.defineJSON().add("state", "delegated").add("assignee", assigneeUser.getUsername()).build();
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated");
    }
    
    @Bug(id = "REPO-1982")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from resolved to resolved")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCannotUpdateTaskFromResolvedToResolved() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
     
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("resolved");
    }
    
    @Bug(id = "REPO-1982")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Update task by providing empty select value")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskByProvidingEmptySelectValue() throws Exception
    {
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(RestErrorModel.INVALID_SELECT_ERRORKEY)
            .containsSummary(RestErrorModel.INVALID_SELECT)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Update task by providing empty state value")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskByProvidingEmptyStateValue() throws Exception
    {
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask(" ");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.TASK_INVALID_STATE, " "))
            .containsSummary(String.format(RestErrorModel.TASK_INVALID_STATE, " "))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}