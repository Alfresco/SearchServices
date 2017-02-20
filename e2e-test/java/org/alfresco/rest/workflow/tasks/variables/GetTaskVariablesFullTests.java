package org.alfresco.rest.workflow.tasks.variables;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
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
import org.testng.annotations.Test;

public class GetTaskVariablesFullTests extends RestTest
{
    private UserModel userWhoStartsTask, assignee;
    private SiteModel siteModel;
    private FileModel fileModel;
    private TaskModel taskModel;
    private RestVariableModelsCollection variableModels;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify default error shema for get task variables")
    public void getTaskVariablesCheckDefaultErrorSchema() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask)
                   .usingSite(siteModel).usingResource(fileModel)
                       .createNewTaskAndAssignTo(assignee);
        taskModel.setId("invalidId");

        restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidId"))
                              .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                              .stackTraceIs(RestErrorModel.STACKTRACE)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @Bug(id="MNT-17438")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify user gets task with 'maxItems' parameter")
    public void getTaskVariablesWithMaxItems() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        variableModels = restClient.authenticateUser(userWhoStartsTask)
                .withWorkflowAPI().usingParams("maxItems=2").usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty()
                .and().paginationField("count").is("2");
    }
    
    @Bug(id="MNT-17438")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify user gets task with 'skipCount' parameter")
    public void getTaskVariablesWithSkipCount() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        variableModels = restClient.authenticateUser(userWhoStartsTask)
                .withWorkflowAPI().usingParams("skipCount=10").usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty()
                .and().paginationField("count").is("20");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify user cannot get task variables with invalid where clause.")
    public void getTaskVariablesWithInvalidWhereClauseAsParameter() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        variableModels = restClient.authenticateUser(userWhoStartsTask).where("scope='fake-where'")
                .withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_WHERE_QUERY, "Invalid value for 'scope' used in query: fake-where."))
                          .containsErrorKey(RestErrorModel.INVALID_QUERY_ERRORKEY)
                          .stackTraceIs(RestErrorModel.STACKTRACE)
                          .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify user gets tasks variables with 'properties' parameter")
    public void getTaskVariablesWithValidProperties() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        variableModels = restClient.authenticateUser(userWhoStartsTask).withParams("properties=scope,name")
                .withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty();
        variableModels.getOneRandomEntry().onModel().assertThat()
            .fieldsCount().is(2).and()
            .field("type").isNull().and()
            .field("value").isNull().and()
            .field("scope").isNotEmpty().and()
            .field("name").isNotEmpty();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify user cannot get tasks variables with invalid 'properties' parameter")
    public void getTaskVariablesWithInvalidProperties() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        variableModels = restClient.authenticateUser(userWhoStartsTask).withParams("properties=fake")
                .withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.getOneRandomEntry().onModel().assertThat()
            .fieldsCount().is(0).and()
            .field("type").isNull().and()
            .field("value").isNull().and()
            .field("scope").isNull().and()
            .field("name").isNull();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to retrieve network task variables")
    public void getTaskVariablesByTenantAdmin() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser1)
                        .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        
        variableModels = restClient.withWorkflowAPI().usingTask(task.onModel()).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to retrieve network task variables")
    public void getTaskVariablesByTenantFromAnotherNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        UserModel adminTenantUser2= UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser1);
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser2);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser1)
                        .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        
        variableModels = restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().usingTask(task.onModel()).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
}
