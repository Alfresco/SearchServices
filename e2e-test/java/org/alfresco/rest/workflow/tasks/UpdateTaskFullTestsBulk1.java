package org.alfresco.rest.workflow.tasks;

import javax.json.JsonObject;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
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
            description = "Verify task cannot be updated from status completed to delegated and response is 404")
    public void taskCannotBeUpdatedFromCompletedToDelegated() throws Exception
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
            description = "Verify task cannot be updated from status completed to resolved and response is 404")
    public void taskCannotBeUpdatedFromCompletedToResolved() throws Exception
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
            description = "Verify task cannot be updated from status completed to completed and response is 404")
    public void taskCannotBeUpdatedFromCompletedToCompleted() throws Exception
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
            description = "Verify task can be updated from status claimed to unclaimed and response is 200")
    public void taskCanBeUpdatedFromClaimedToUnclaimed() throws Exception
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
            description = "Verify task can be updated from status claimed to delegated and response is 200")
    public void taskCanBeUpdatedFromClaimedToDelegated() throws Exception
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
            description = "Verify task can be updated from status claimed to resolved and response is 200")
    public void taskCanBeUpdatedFromClaimedToResolved() throws Exception
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
            description = "Verify task can be updated from status claimed to claimed and response is 200")
    public void taskCanBeUpdatedFromClaimedToClaimed() throws Exception
    {       
        restTaskModel = restClient.authenticateUser(owner).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
            .and().field("state").is("claimed");
        
        restTaskModel = restClient.authenticateUser(owner)
            .withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT).assertLastError()
            .containsErrorKey(RestErrorModel.TAS_ALREADY_CLAIMED)
            .containsSummary(RestErrorModel.TAS_ALREADY_CLAIMED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
