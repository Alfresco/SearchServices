package org.alfresco.rest.workflow.tasks;

import javax.json.JsonObject;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestVariableModelsCollection;
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

/**
 * 
 * @author Cristina Axinte
 *
 */
public class UpdateTaskFullTestsBulk1 extends RestTest
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
    }
    
    @BeforeMethod(alwaysRun=true)
    public void createTask() throws Exception
    {
        taskModel = dataWorkflow.usingUser(owner).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify tenant user that created the task can update task only in its network and response is 200, for other networks is 403")
    public void ownerTenantCanUpdatedTaskOnlyInItsNetwork() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);

        UserModel adminTenant1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant1);

        UserModel ownerTenant1 = dataUser.usingUser(adminTenant1).createUserWithTenant("ownerTenant1");
        UserModel assigneeTenant1 = dataUser.usingUser(adminTenant1).createUserWithTenant("assigneeTenant1"); 
        SiteModel siteModel1 = dataSite.usingUser(ownerTenant1).createPublicRandomSite();
        FileModel fileModel1 = dataContent.usingUser(ownerTenant1).usingSite(siteModel1).createContent(DocumentType.TEXT_PLAIN);  
        RestProcessDefinitionModel def1 = restClient.authenticateUser(ownerTenant1).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel1 = dataWorkflow.usingUser(ownerTenant1).usingSite(siteModel1).usingResource(fileModel1)
                .createTaskWithProcessDefAndAssignTo(def1.getId(), assigneeTenant1);
        
        UserModel adminTenant2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant2);

        UserModel ownerTenant2 = dataUser.usingUser(adminTenant2).createUserWithTenant("ownerTenant2");
        UserModel assigneeTenant2 = dataUser.usingUser(adminTenant2).createUserWithTenant("userTenantAssignee2");
        SiteModel siteModel2 = dataSite.usingUser(ownerTenant2).createPublicRandomSite();
        FileModel fileModel2 = dataContent.usingUser(ownerTenant2).usingSite(siteModel2).createContent(DocumentType.TEXT_PLAIN);
        RestProcessDefinitionModel def2 = restClient.authenticateUser(ownerTenant2).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel2 = dataWorkflow.usingUser(ownerTenant2).usingSite(siteModel2).usingResource(fileModel2)
                .createTaskWithProcessDefAndAssignTo(def2.getId(), assigneeTenant2);
        
        restTaskModel = restClient.authenticateUser(ownerTenant2).withParams("select=state").withWorkflowAPI().usingTask(taskModel2).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel2.getId())
            .and().field("state").is("completed");
        
        restTaskModel = restClient.authenticateUser(ownerTenant2).withParams("select=state").withWorkflowAPI().usingTask(taskModel1).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
        
    }
    
    @Bug(id = "REPO-1980")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify tenant admin can update only task from its network and response is 200")
    public void adminTenantCanUpdateOnlyTaskInItsNetwork() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);

        UserModel adminTenant1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant1);

        UserModel ownerTenant1 = dataUser.usingUser(adminTenant1).createUserWithTenant("ownerTenant1");
        UserModel assigneeTenant1 = dataUser.usingUser(adminTenant1).createUserWithTenant("assigneeTenant1"); 
        SiteModel siteModel1 = dataSite.usingUser(ownerTenant1).createPublicRandomSite();
        FileModel fileModel1 = dataContent.usingUser(ownerTenant1).usingSite(siteModel1).createContent(DocumentType.TEXT_PLAIN);  
        RestProcessDefinitionModel def1 = restClient.authenticateUser(ownerTenant1).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel1 = dataWorkflow.usingUser(ownerTenant1).usingSite(siteModel1).usingResource(fileModel1)
                .createTaskWithProcessDefAndAssignTo(def1.getId(), assigneeTenant1);
        
        UserModel adminTenant2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant2);

        UserModel ownerTenant2 = dataUser.usingUser(adminTenant2).createUserWithTenant("ownerTenant2");
        UserModel assigneeTenant2 = dataUser.usingUser(adminTenant2).createUserWithTenant("userTenantAssignee2");
        SiteModel siteModel2 = dataSite.usingUser(ownerTenant2).createPublicRandomSite();
        FileModel fileModel2 = dataContent.usingUser(ownerTenant2).usingSite(siteModel2).createContent(DocumentType.TEXT_PLAIN);
        RestProcessDefinitionModel def2 = restClient.authenticateUser(ownerTenant2).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel2 = dataWorkflow.usingUser(ownerTenant2).usingSite(siteModel2).usingResource(fileModel2)
                .createTaskWithProcessDefAndAssignTo(def2.getId(), assigneeTenant2);
        
        restTaskModel = restClient.authenticateUser(adminTenant2).withParams("select=state").withWorkflowAPI().usingTask(taskModel2).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel2.getId())
            .and().field("state").is("completed");
        
        restTaskModel = restClient.authenticateUser(adminTenant2).withParams("select=state").withWorkflowAPI().usingTask(taskModel1).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
        
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner cannot update task from status completed to delegated and response is 404")
    public void ownerCannotUpdateTaskFromCompletedToDelegated() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");
        
        restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("delegated");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify assignee cannot update task from status completed to delegated and response is 404")
    public void assigneeCannotUpdateTaskFromCompletedToDelegated() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");
        
        restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("delegated");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner cannot update task from status completed to resolved and response is 404")
    public void ownerCannotUpdateTaskFromCompletedToResolved() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");
        
        restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify assignee cannot update task from status completed to resolved and response is 404")
    public void assigneeCannotUpdateTaskFromCompletedToResolved() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");
        
        restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner cannot update task from status completed to completed and response is 404")
    public void ownerCannotUpdateTaskFromCompletedToCompleted() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");
        
        restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify assignee cannot update task from status completed to completed and response is 404")
    public void assigneeCannotUpdateTaskFromCompletedToCompleted() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("completed");
        
        restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .containsSummary(String.format(RestErrorModel.TASK_ALREADY_COMPLETED, taskModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner can update task from status claimed to unclaimed and response is 200")
    public void ownerCanUpdateTaskFromClaimedToUnclaimed() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("unclaimed");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify assignee can update task from status claimed to unclaimed and response is 200")
    public void assigneeCanUpdateTaskFromClaimedToUnclaimed() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("unclaimed");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner can update task from status claimed to delegated and response is 200")
    public void ownerCanUpdateTaskFromClaimedToDelegated() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        UserModel newAssignee = dataUser.createRandomTestUser();
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "delegated")
                .add("assignee", newAssignee.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("delegated")
            .and().field("assignee").is(newAssignee.getUsername());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify assignee can update task from status claimed to delegated and response is 200")
    public void assigneeCanUpdateTaskFromClaimedToDelegated() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        UserModel newAssignee = dataUser.createRandomTestUser();
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "delegated")
                .add("assignee", newAssignee.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("delegated")
            .and().field("assignee").is(newAssignee.getUsername());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner can update task from status claimed to resolved and response is 200")
    public void ownerCanUpdateTaskFromClaimedToResolved() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("resolved");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify assignee can update task from status claimed to resolved and response is 200")
    public void assigneeCanUpdateTaskFromClaimedToResolved() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("resolved");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner cannot update task from status claimed to claimed and response is 409")
    public void ownerCannotUpdateTaskFromClaimedToClaimed() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT).assertLastError()
            .containsErrorKey(RestErrorModel.TASK_ALREADY_CLAIMED)
            .containsSummary(RestErrorModel.TASK_ALREADY_CLAIMED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify assignee can update task from status claimed to claimed and response is 200")
    public void assigneeCanUpdateTaskFromClaimedToClaimed() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        restTaskModel = restClient.authenticateUser(assigneeUser)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner can complete task with valid variables and response is 200")
    public void taskOwnerCanCompleteTaskWithValidVariables() throws Exception
    {      
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "completed")
                .add("variables", JsonBodyGenerator.defineJSONArray()
                                    .add(JsonBodyGenerator.defineJSON()
                                            .add("name", "bpm_priority")
                                            .add("type", "d:int")
                                            .add("value", 3)
                                            .add("scope", "global").build())
                                    ).build();
        
        restTaskModel = restClient.authenticateUser(owner)
                .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("priority").is(1)
            .and().field("state").is("completed")
            .and().field("assignee").is(taskModel.getAssignee());
        RestVariableModelsCollection variables = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(restTaskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variables.getVariableByName("bpm_priority").assertThat().field("scope").is("global")
            .and().field("name").is("bpm_priority")
            .and().field("type").is("d:int")
            .and().field("value").is(3);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with empty variables array and response is 200")
    public void taskOwnerCannotUpdateTaskWithEmptyVariablesArray() throws Exception
    {      
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "completed")
                .add("variables", JsonBodyGenerator.defineJSONArray()
                                    ).build();
        
        restTaskModel = restClient.authenticateUser(owner)
                .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("priority").is(1)
            .and().field("state").is("completed")
            .and().field("assignee").is(taskModel.getAssignee());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify task owner cannot complete task with empty variables json body and response is 200")
    public void taskOwnerCannotUpdateTaskWithEmptyVariablesJsonBody() throws Exception
    {      
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "completed")
                .add("variables", JsonBodyGenerator.defineJSONArray()
                        .add(JsonBodyGenerator.defineJSON().build())
                                    ).build();
        
        restTaskModel = restClient.authenticateUser(owner)
                .withParams("select=state,variables").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(RestErrorModel.VARIABLE_NAME_REQUIRED)
            .containsSummary(RestErrorModel.VARIABLE_NAME_REQUIRED);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
        description = "Verify any user with no relation to task is forbidden to delegate other task with Rest API (403)")
    public void anyUserIsForbiddenToDelegateOtherTask() throws Exception
    {
        UserModel anyUser= dataUser.createRandomTestUser();

        restTaskModel = restClient.authenticateUser(anyUser).withWorkflowAPI().usingTask(taskModel).updateTask("delegated");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary("Permission was denied");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
        description = "Verify any user with no relation to task is forbidden to resolve other task with Rest API (403)")
    public void anyUserIsForbiddenToResolveOtherTask() throws Exception
    {
        UserModel anyUser= dataUser.createRandomTestUser();

        restTaskModel = restClient.authenticateUser(anyUser).withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary("Permission was denied");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
        description = "Verify any user with no relation to task is forbidden to claim other task with Rest API (403)")
    public void anyUserIsForbiddenToClaimOtherTask() throws Exception
    {
        UserModel anyUser= dataUser.createRandomTestUser();

        restTaskModel = restClient.authenticateUser(anyUser).withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary("Permission was denied");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
        description = "Verify any user with no relation to task is forbidden to unclaim other task with Rest API (403)")
    public void anyUserIsForbiddenToUnclaimOtherTask() throws Exception
    {
        UserModel anyUser= dataUser.createRandomTestUser();

        restTaskModel = restClient.authenticateUser(anyUser).withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary("Permission was denied");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user cannot delegate task with emty assignee name and response is 400")
    public void updateTaskWithEmptyAssigneeValue() throws Exception
    {      
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "delegated")
                .add("assignee", "")
                .build();
        
        restTaskModel = restClient.authenticateUser(owner)
                .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(RestErrorModel.DELEGATING_ASSIGNEE_PROVIDED)
            .containsSummary(RestErrorModel.DELEGATING_ASSIGNEE_PROVIDED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify owner can resolve task when assignee is the owner and response is 200")
    public void taskOwnerUpdateTaskResolveStateAndOwnerAssignee() throws Exception
    {      
        JsonObject inputJson = JsonBodyGenerator.defineJSON()
                .add("state", "resolved")
                .add("assignee", owner.getUsername())
                .build();
        
        restTaskModel = restClient.authenticateUser(owner)
                .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("state").is("resolved")
            .and().field("assignee").isNull();
    }
}
