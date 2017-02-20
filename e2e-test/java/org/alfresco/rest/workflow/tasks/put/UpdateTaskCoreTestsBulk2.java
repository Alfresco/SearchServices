package org.alfresco.rest.workflow.tasks.put;

import javax.json.JsonObject;

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
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                                .add("state", "resolved")
                                .add("assignee", "admin").build();
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
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
    @Bug(id = "REPO-2062")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid name - Bad Request(400))")
    public void taskOwnerCannotCompleteTaskWithInvalidName() throws Exception
    {              
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "completed")
                .add("variables", JsonBodyGenerator.defineJSONArray()
                                    .add(JsonBodyGenerator.defineJSON()
                                            .add("name", "bpmx_priorityx")
                                            .add("type", "d:int")
                                            .add("value", 1)
                                            .add("scope", "global").build())
                                    ).build();
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE,"bpm_priorityx"));
    }
     
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid Type - Bad Request(400))")
    public void taskOwnerCannotCompleteTaskWithInvalidType() throws Exception
    {      
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "completed")
                .add("variables", JsonBodyGenerator.defineJSONArray()
                                    .add(JsonBodyGenerator.defineJSON()
                                            .add("name", "bpmx_priority")
                                            .add("type", "d_int")
                                            .add("value", 1)
                                            .add("scope", "global").build())
                                    ).build();
        
        restTaskModel = restClient.authenticateUser(owner)
                .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE,"d_int"));
    }
 
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @Bug(id = "REPO-2062")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid value - Bad Request(400))")
    public void taskOwnerCannotCompleteTaskWithInvalidValue() throws Exception
    {      
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "completed")
                .add("variables", JsonBodyGenerator.defineJSONArray()
                                    .add(JsonBodyGenerator.defineJSON()
                                            .add("name", "bpmx_priority")
                                            .add("type", "d:int")
                                            .add("value", "text")
                                            .add("scope", "global").build())
                                    ).build();
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.UNSUPPORTED_TYPE, "text"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with invalid scope - Bad Request(400))")
    public void taskOwnerCannotCompleteTaskWithInvalidScope() throws Exception
    {         
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "completed")
                .add("variables", JsonBodyGenerator.defineJSONArray()
                                    .add(JsonBodyGenerator.defineJSON()
                                            .add("name", "bpmx_priority")
                                            .add("type", "d:int")
                                            .add("value", 1)
                                            .add("scope", "globalscope").build())
                                    ).build();
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
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

        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "delegated")
                .add("assignee", delagateToUser.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
                .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
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
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "delegated")
                .add("assignee", assigneeUser.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("delegated")
            .and().field("assignee").is(assigneeUser.getUsername());  
    }
    
     
    @Bug(id = "REPO-2063")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task assignee cannot delegate task to task owner -  (400)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void taskAssigneeCannotDelegateTaskToTaskOwner() throws Exception
    {        
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "delegated")
                .add("assignee", owner.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @Bug(id = "REPO-2063")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task assignee cannot delegate task to invalid assignee - (400)")
    public void taskAssigneeCannotDelegateTaskToInvalidAssignee() throws Exception
    {         
        UserModel invalidAssignee = new UserModel("invalidAssignee", "password");
        
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "delegated")
                .add("assignee", invalidAssignee.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
}
