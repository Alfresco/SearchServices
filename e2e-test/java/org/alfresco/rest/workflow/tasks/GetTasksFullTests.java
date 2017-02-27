package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestTaskModelsCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetTasksFullTests extends RestTest
{
    UserModel userModel, adminUser;
    SiteModel siteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;
    RestTaskModelsCollection taskModels, taskCollections;    

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that skipCount parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void skipCountParameterApplied() throws Exception
    { 
        taskCollections = restClient.authenticateUser(dataUser.getAdminUser()).withParams("orderBy=description ASC").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        RestTaskModel firstTask = taskCollections.getEntries().get(0).onModel();
        RestTaskModel secondTask = taskCollections.getEntries().get(1).onModel();
        
        taskModels = restClient.withParams("orderBy=description ASC&skipCount=2").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListDoesNotContain("id", firstTask.getId())
            .assertThat().entriesListDoesNotContain("id", secondTask.getId());    
        taskModels.assertThat().paginationField("skipCount").is("2");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that maxItems parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void maxItemsParameterApplied() throws Exception
    { 
        restClient.authenticateUser(userModel).withWorkflowAPI().addProcess("activitiAdhoc", assigneeUser, false, Priority.Low);
        restClient.authenticateUser(userModel).withWorkflowAPI().addProcess("activitiAdhoc", adminUser, false, Priority.Low);        
        taskCollections = restClient.authenticateUser(userModel).withParams("orderBy=assignee ASC").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);        
        
        taskModels = restClient.withParams("orderBy=assignee ASC&maxItems=2").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty()
            .assertThat().entriesListContains("assignee", assigneeUser.getUsername())
            .assertThat().entriesListDoesNotContain("assignee", adminUser.getUsername());   
        taskModels.assertThat().paginationField("maxItems").is("2");
        taskModels.assertThat().paginationField("count").is("2");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that properties parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void propertiesParameterApplied() throws Exception
    { 
        taskModels = restClient.authenticateUser(dataUser.getAdminUser()).withParams("properties=name,description").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty()
            .assertThat().entriesListContains("name")
            .assertThat().entriesListContains("description")
            .assertThat().entriesListDoesNotContain("processDefinitionId")
            .assertThat().entriesListDoesNotContain("processId")
            .assertThat().entriesListDoesNotContain("startedAt")
            .assertThat().entriesListDoesNotContain("id")
            .assertThat().entriesListDoesNotContain("state")
            .assertThat().entriesListDoesNotContain("activityDefinitionId")
            .assertThat().entriesListDoesNotContain("priority")
            .assertThat().entriesListDoesNotContain("formResourceKey");       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Use invalid where parameter. Check default error model schema.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void invalidWhereParameterCheckDefaultErrorModelSchema() throws Exception
    {     
        restClient.authenticateUser(dataUser.getAdminUser()).where("assignee AND '" + assigneeUser.getUsername() + "'").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(RestErrorModel.INVALID_QUERY_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.INVALID_WHERE_QUERY, "(assignee AND '" + assigneeUser.getUsername() + "')"))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);      
    }
    
    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, 
        executionType = ExecutionType.REGRESSION, description = "Check that for admin network gets tasks only from its network.")
    public void adminTenantGetsTasksOnlyFromItsNetwork() throws Exception
    {
        UserModel adminTenant1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant1);
        
        UserModel adminTenant2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant2);

        UserModel tenantUser1 = dataUser.usingUser(adminTenant1).createUserWithTenant("userTenant1");
        UserModel tenantUserAssignee1 = dataUser.usingUser(adminTenant1).createUserWithTenant("userTenantAssignee1");
        
        UserModel tenantUser2 = dataUser.usingUser(adminTenant2).createUserWithTenant("userTenant2");
        UserModel tenantUserAssignee2 = dataUser.usingUser(adminTenant2).createUserWithTenant("userTenantAssignee2");
        
        RestProcessModel processOnTenant1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee1, false, Priority.Low);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestProcessModel processOnTenant2 = restClient.authenticateUser(tenantUser2).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee2, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModelsCollection tenantTasks1 = restClient.authenticateUser(adminTenant1).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTasks1.assertThat().entriesListIsNotEmpty()
            .and().entriesListCountIs(1)
            .and().entriesListContains("assignee", String.format("userTenantAssignee1@%s", tenantUserAssignee1.getDomain().toLowerCase()))
            .and().entriesListContains("processId", processOnTenant1.getId())
            .and().entriesListDoesNotContain("assignee", String.format("userTenantAssignee2@%s", tenantUserAssignee2.getDomain().toLowerCase()))
            .and().entriesListDoesNotContain("processId", processOnTenant2.getId());
        
        RestTaskModelsCollection tenantTasks2 = restClient.authenticateUser(adminTenant2).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTasks2.assertThat().entriesListIsNotEmpty()
            .and().entriesListCountIs(1)
            .and().entriesListContains("assignee", String.format("userTenantAssignee2@%s", tenantUserAssignee2.getDomain().toLowerCase()))
            .and().entriesListContains("processId", processOnTenant2.getId())
            .and().entriesListDoesNotContain("assignee", String.format("userTenantAssignee1@%s", tenantUserAssignee1.getDomain().toLowerCase()))
            .and().entriesListDoesNotContain("processId", processOnTenant1.getId());
    }
}
