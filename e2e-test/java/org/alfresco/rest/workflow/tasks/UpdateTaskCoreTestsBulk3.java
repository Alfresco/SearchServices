package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 12/9/2016.
 */
public class UpdateTaskCoreTestsBulk3 extends RestTest
{
    UserModel owner;
    SiteModel siteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;
    TaskModel taskModel;
    RestTaskModel restTaskModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        owner = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(owner).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify task assignee can update task with status resolve")
    public void resolveTaskAsCurrentAssignee() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
        restTaskModel = restClient.authenticateUser(assigneeUser)
                .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
                .and().field("state").is("resolved");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify update task using invalid taskId")
    public void updateTaskUsingInvalidTaskId() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
        taskModel.setId("invalidId");

        restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidId"));
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify update task using another name")
    public void updateTaskUsingAnotherName() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        restClient.authenticateUser(owner).withParams("select=name").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"name\":\"newNameTask\"}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel = restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("name").is("newNameTask");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify update task using another description")
    public void updateTaskUsingAnotherDescription() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        restClient.authenticateUser(owner).withParams("select=description").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"description\":\"newDescription\"}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel = restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("description").is("newDescription");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify update task using another priority")
    public void updateTaskUsingAnotherPriority() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        restClient.authenticateUser(owner).withParams("select=priority").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"priority\":3}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel = restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("priority").is(CMISUtil.Priority.Low.getLevel());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify update task using another priority")
    public void updateTaskUsingAnotherOwner() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
        UserModel newOwner = dataUser.createRandomTestUser();

        restClient.authenticateUser(owner).withParams("select=owner").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"owner\":\""+newOwner.getUsername()+"\"}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel = restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("owner").is(newOwner.getUsername());
    }
}
