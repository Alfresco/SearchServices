package org.alfresco.rest.workflow.tasks.get;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestTaskModelsCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.ProcessModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
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
public class GetTaskFullTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;
    TaskModel taskModel;
    RestTaskModel restTaskModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();   
    }
    
    @BeforeMethod(alwaysRun=true)
    public void createTask() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, 
        executionType = ExecutionType.REGRESSION, description = "Verify user who started a task gets the task with empty taskId with Rest API")
    public void starterUserGetsTaskWithEmptyTaskId() throws Exception
    {
        restClient.authenticateUser(userModel).withWorkflowAPI();
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "tasks/{taskId}", "");
        RestTaskModelsCollection tasks = restClient.processModels(RestTaskModelsCollection.class, request);
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tasks.assertThat().entriesListIsNotEmpty()
            .and().entriesListCountIs(1)
            .and().entriesListContains("id", taskModel.getId());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, 
        executionType = ExecutionType.REGRESSION, description = "Verify user who started a task gets the task with properties parameter with Rest API")
    public void starterUserGetsTaskWithPropertiesParameter() throws Exception
    {
        restTaskModel = restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).usingParams("properties=id,assignee,formResourceKey").getTask();
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().fieldsCount().is(3)
            .and().field("id").is(taskModel.getId())
            .and().field("assignee").is(taskModel.getAssignee())
            .and().field("formResourceKey").is("wf:adhocTask")
            .and().field("processDefinitionId").isNull()
            .and().field("processId").isNull();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, 
        executionType = ExecutionType.REGRESSION, description = "Verify user who started a task gets the task after it is deleted with Rest API")
    public void starterUserGetsDeletedTask() throws Exception
    {
        restTaskModel = restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId());
        
        ProcessModel process = new ProcessModel();
        process.setId(taskModel.getProcessId());
        dataWorkflow.usingUser(userModel).deleteProcess(process);

        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, taskModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
   
    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, 
        executionType = ExecutionType.REGRESSION, description = "Check that for admin network gets task only from its network.")
    public void adminTenantGetsTaskOnlyFromItsNetwork() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);

        UserModel adminTenant1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant1);

        UserModel tenantUser1 = dataUser.usingUser(adminTenant1).createUserWithTenant("userTenant1");
        UserModel tenantUserAssignee1 = dataUser.usingUser(adminTenant1).createUserWithTenant("userTenantAssignee1");
        
        RestProcessModel processOnTenant1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee1, false, Priority.Low);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        UserModel adminTenant2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant2);

        UserModel tenantUser2 = dataUser.usingUser(adminTenant2).createUserWithTenant("userTenant2");
        UserModel tenantUserAssignee2 = dataUser.usingUser(adminTenant2).createUserWithTenant("userTenantAssignee2");

        RestProcessModel processOnTenant2 = restClient.authenticateUser(tenantUser2).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee2, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModelsCollection tenantTasks2 = restClient.authenticateUser(tenantUser2).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTasks2.assertThat().entriesListIsNotEmpty()
            .and().entriesListCountIs(1)
            .and().entriesListContains("assignee", String.format("userTenantAssignee2@%s", tenantUserAssignee2.getDomain().toLowerCase()))
            .and().entriesListContains("processId", processOnTenant2.getId())
            .and().entriesListDoesNotContain("assignee", String.format("userTenantAssignee1@%s", tenantUserAssignee1.getDomain().toLowerCase()))
            .and().entriesListDoesNotContain("processId", processOnTenant1.getId());

    }
}
