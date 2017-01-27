package org.alfresco.rest.workflow.tasks;

import java.util.HashMap;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.core.RestRequest;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdateTaskCoreTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    SiteModel tenantSiteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;
    TaskModel taskModel;
    TaskModel tenantTask;
    RestTaskModel restTaskModel;
    UserModel adminUser;

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

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can not update task from completed to claimed and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanNotUpdateTaskFromCompletedToClaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("completed");
        restTaskModel = restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
                .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can not update task from completed to unclaimed and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanNotUpdateTaskFromCompletedToUnclaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("completed");
        restTaskModel = restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
                .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can not update task from completed to completed and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanNotUpdateTaskWithCompleteStateTwice() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("completed");
        restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
                .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can update task from claimed to completed and response is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanUpdateTaskFromClaimedToCompleted() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("claimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("completed");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify user can update task from claimed to claimed and response is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void userCanUpdateTaskWithClaimedStateTwice() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("claimed");
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("claimed");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can update task from unclaimed to completed and response is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanUpdateTaskFromUnclaimedToCompleted() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("completed");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can not update task from unclaimed to unclaimed and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanNotUpdateTaskWithUnclaimedStateTwice() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
    }

    @Bug(id = "MNT-17407")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can update task from delegated to completed and response is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanUpdateTaskFromDelegatedToCompleted() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("state", "delegated");
        body.put("assignee", assigneeUser.getUsername());
        String postBody = JsonBodyGenerator.keyValueJson(body);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(),
                restClient.getParameters());
        restTaskModel = restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated").and().field("assignee")
                .is(assigneeUser.getUsername());

        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("completed");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can not update task from delegated to delegated and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanNotUpdateTaskWithDelegatedStateTwice() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("state", "delegated");
        body.put("assignee", assigneeUser.getUsername());
        String postBody = JsonBodyGenerator.keyValueJson(body);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(),
                restClient.getParameters());
        restTaskModel = restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated").and().field("assignee")
                .is(assigneeUser.getUsername());

        request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel = restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated").and().field("assignee")
                .is(assigneeUser.getUsername());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify owner can update task from resolved to completed and response is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskOwnerCanUpdateTaskFromResolvedToCompleted() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("resolved");

        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("completed");
    }
}
