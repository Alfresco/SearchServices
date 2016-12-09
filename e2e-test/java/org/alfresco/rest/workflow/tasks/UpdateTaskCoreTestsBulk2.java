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
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
public class UpdateTaskCoreTestsBulk2 extends RestTest
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
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner can update twice task with status resolve successfully (200)")
    public void taskOwnerCanUpdateTaskWithResolveStateTwice() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
                .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
                .and().field("state").is("resolved");
        
        restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task asignee cannot update twice task with status resolve - Forbidden (403)")
    public void taskAssigneeCanNotUpdateTaskWithResolveStateTwice() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
                .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
                .and().field("state").is("resolved");
        
        restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task cannot be update using a invalid json body property - Bad Request (400)")
    public void taskCanNotBeUpdatedWithInvalidJsonBodyProperty() throws Exception
    {       
        String invalidJsonProperty= "states";
        restClient.authenticateUser(assigneeUser)
                .withParams("select=state").withWorkflowAPI().usingTask(taskModel);
        String postBody = JsonBodyGenerator.keyValueJson(invalidJsonProperty, "completed");
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.NO_CONTENT, "Unrecognized field \""+invalidJsonProperty+"\""));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task cannot be update using a invalid state - Bad Request (400)")
    public void taskCanNotBeUpdatedWithInvalidState() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
                .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("continued");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_VALUE, "task state", "continued"));      
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task cannot be update using a invalid property - Bad Request (400)")
    public void taskCanNotBeUpdatedWithInvalidSelectProperty() throws Exception
    {       
        String invalidProperty= "states";
        restClient.authenticateUser(assigneeUser)
                .withParams("select=" + invalidProperty).withWorkflowAPI().usingTask(taskModel);
        String postBody = JsonBodyGenerator.keyValueJson("state", "completed");
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.PROPERTY_DOES_NOT_EXIST, invalidProperty));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task can be update using multiple select values successfully (200)")
    public void taskCanBeUpdatedWithMultipleSelectValues() throws Exception
    {        
        restClient.authenticateUser(assigneeUser)
                .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);//.updateTask("completed");
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("state", "resolved");
        body.put("assignee", "admin");
        String postBody = JsonBodyGenerator.keyValueJson(body);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("resolved");   
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task assignee can complete task successfully (200)")
    public void taskAssigneeCanCompleteTask() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");  
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner can complete task successfully (200))")
    public void taskOwnerCanCompleteTask() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");
    }
 
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify any user cannot complete task - Forbidden (403))")
    public void anyUserCannotCompleteTask() throws Exception
    {        
        UserModel anyUser = dataUser.createRandomTestUser();
        
        restTaskModel = restClient.authenticateUser(anyUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
 
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid name - (200))")
    public void taskOwnerCannotCompleteTaskWithInvalidName() throws Exception
    {              
        restClient.authenticateUser(assigneeUser)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"state\" : \"completed\", \"variables\" : [{\"name\" : \"bpm_priorityx\",\"type\" : \"d_int\",\"value\" : 1,\"scope\" : \"global\"}]}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE,"bpm_priorityx"));
    }
//    provide invalid type(0.5) 
//    provide invalid value(0.5) 
//    provide invalid scope(0.5) 
//    claim a task as asignee - 200(0.5) 
//    unclaim a task as current asignee - 200(0.5) 
//    delegate a task as asignee - 200(0.5) 
//    "delegate a task as owner - provide the same user as asignee"(0.5) 
//    provide invalid asignee value(0.5) 
}
