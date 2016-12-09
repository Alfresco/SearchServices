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

/**
 * 
 * @author Cristina Axinte
 *
 */
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
    
    @BeforeMethod(alwaysRun=true)
    public void createTask() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
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
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
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

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
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
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task cannot be update using a invalid state - Bad Request (400)")
    public void taskCanNotBeUpdatedWithInvalidState() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
                .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("continued");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_VALUE, "task state", "continued"));      
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
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
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task can be update using multiple select values successfully (200)")
    public void taskCanBeUpdatedWithMultipleSelectValues() throws Exception
    {             
        restClient.authenticateUser(assigneeUser)
                .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);
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

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
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

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
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
 
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
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
 
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @Bug(id = "TBD")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid name - (200))")
    public void taskOwnerCannotCompleteTaskWithInvalidName() throws Exception
    {              
        restClient.authenticateUser(owner)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"state\" : \"completed\", \"variables\" : [{\"name\" : \"bpmx_priorityx\",\"type\" : \"d:int\",\"value\" : 1,\"scope\" : \"global\"}]}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE,"bpm_priorityx"));
    }
     
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @Bug(id = "TBD")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid Type - Bad Request(400))")
    public void taskOwnerCannotCompleteTaskWithInvalidType() throws Exception
    {              
        restClient.authenticateUser(owner)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"state\" : \"completed\", \"variables\" : [{\"name\" : \"bpm_priority\",\"type\" : \"d_int\",\"value\" : 1,\"scope\" : \"global\"}]}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE,"d_int"));
    }
 
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @Bug(id = "TBD")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid value - Bad Request(400))")
    public void taskOwnerCannotCompleteTaskWithInvalidValue() throws Exception
    {              
        restClient.authenticateUser(owner)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"state\" : \"completed\", \"variables\" : [{\"name\" : \"bpm_priority\",\"type\" : \"d:int\",\"value\" : \"text\",\"scope\" : \"global\"}]}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE, "text"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid scope - Bad Request(400))")
    public void taskOwnerCannotCompleteTaskWithInvalidScope() throws Exception
    {              
        restClient.authenticateUser(owner)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel);
        String postBody = "{\"state\" : \"completed\", \"variables\" : [{\"name\" : \"bpm_priority\",\"type\" : \"d:int\",\"value\" : 1,\"scope\" : \"globalscope\"}]}";
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.ILLEGAL_SCOPE, "globalscope"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task assignee can claim task successfully (200)")
    public void taskAssigneeCanClaimTask() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");  
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task assignee can unclaim task successfully (200)")
    public void taskAssigneeCanUnclaimTask() throws Exception
    {        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("unclaimed");  
    }
 
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task assignee can delegate task successfully (200)")
    public void taskAssigneeCanDelegateTask() throws Exception
    {        
        UserModel delagateToUser = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(assigneeUser)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("state", "delegated");
        body.put("assignee", delagateToUser.getUsername());
        String postBody = JsonBodyGenerator.keyValueJson(body);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("delegated")
            .and().field("assignee").is(delagateToUser.getUsername());  
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner can delegate task to same assignee successfully (200)")
    public void taskOwnerCanDelegateTaskToSameAssignee() throws Exception
    {              
        restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("state", "delegated");
        body.put("assignee", assigneeUser.getUsername());
        String postBody = JsonBodyGenerator.keyValueJson(body);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("delegated")
            .and().field("assignee").is(assigneeUser.getUsername());  
    }
    
     
    @Bug(id = "TBD") //as per api-explorer: should not have OK status
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task assignee cannot delegate task to task owner -  (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskAssigneeCannotDelegateTaskToTaskOwner() throws Exception
    {              
        restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("state", "delegated");
        body.put("assignee", owner.getUsername());
        String postBody = JsonBodyGenerator.keyValueJson(body);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @Bug(id = "TBD")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot delegate task to invalid assignee - (200)")
    public void taskOwnerCannotDelegateTaskToInvalidAssignee() throws Exception
    {         
        UserModel invalidAssignee = new UserModel("invalidAssignee", "password");
        
        restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel);
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("state", "delegated");
        body.put("assignee", invalidAssignee.getUsername());
        String postBody = JsonBodyGenerator.keyValueJson(body);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, postBody, "tasks/{taskId}?{parameters}", taskModel.getId(), restClient.getParameters());
        restTaskModel =restClient.processModel(RestTaskModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
}
