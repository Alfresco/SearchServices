package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestTaskModelsCollection;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetTasksTests extends RestTest
{
    UserModel userModel, userNotInvolved, assigneeUser, adminUser;
    UserModel adminTenantUser, tenantUser, tenantUserAssignee;
    SiteModel siteModel;
    FileModel fileModel;
    RestTaskModelsCollection taskModels, taskCollections, tenantTask;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        userNotInvolved = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        assigneeUser = dataUser.createRandomTestUser();
        dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify admin user gets all existing tasks with Rest API and response is successfull (200)")
    public void adminUserGetsAllTasks() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();

        taskModels = restClient.authenticateUser(adminUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify asignee user gets its existing tasks with Rest API and response is successfull (200)")
    public void assigneeUserGetsItsTasks() throws Exception
    {
        taskModels = restClient.authenticateUser(assigneeUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty().and().entriesListCountIs(1).and().entriesListContains("assignee", assigneeUser.getUsername());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify candidate user that claims the task gets its existing tasks with Rest API and response is successfull (200)")
    @Bug(id = "MNT-16967")
    public void candidateUserGetsItsTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();

        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);

        taskModels = restClient.authenticateUser(userModel1).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify candidate user that claims the task gets its existing tasks with Rest API and response is successfull (200)")
    public void candidateUserThatClaimsTaskGetsItsTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        TaskModel taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        dataWorkflow.usingUser(userModel1).claimTask(taskModel);

        taskModels = restClient.authenticateUser(userModel1).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify candidate user without claim gets no tasks with Rest API and response is successfull (200)")
    public void candidateUserWithoutClaimTaskGetsNoTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();

        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        TaskModel taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        dataWorkflow.usingUser(userModel1).claimTask(taskModel);

        taskModels = restClient.authenticateUser(userModel2).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that skipCount parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
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
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    public void maxItemsParameterApplied() throws Exception
    {
        restClient.authenticateUser(userModel).withWorkflowAPI().addProcess("activitiAdhoc", assigneeUser, false, Priority.Low);
        restClient.authenticateUser(userModel).withWorkflowAPI().addProcess("activitiAdhoc", adminUser, false, Priority.Low);
        taskCollections = restClient.authenticateUser(userModel).withParams("orderBy=assignee ASC").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        taskModels = restClient.withParams("orderBy=startedAt DESC&maxItems=2").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty()
                .assertThat().entriesListContains("assignee", assigneeUser.getUsername())
                .assertThat().entriesListDoesNotContain("assignee", userModel.getUsername());
        taskModels.assertThat().paginationField("maxItems").is("2");
        taskModels.assertThat().paginationField("count").is("2");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that properties parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
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
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    public void invalidWhereParameterCheckDefaultErrorModelSchema() throws Exception
    {
        restClient.authenticateUser(dataUser.getAdminUser()).where("assignee AND '" + assigneeUser.getUsername() + "'").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsErrorKey(RestErrorModel.INVALID_QUERY_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.INVALID_WHERE_QUERY, "(assignee AND '" + assigneeUser.getUsername() + "')"))
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
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

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify the request in case of any user which is not involved.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    public void requestWithUserNotInvolved() throws Exception
    {
        taskModels = restClient.authenticateUser(userNotInvolved).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that orderBy parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    public void orderByParameterApplied() throws Exception
    { 
        taskModels = restClient.authenticateUser(dataUser.getAdminUser()).withParams("orderBy=id").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty().
            and().entriesListIsSortedAscBy("id");       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that orderBy parameter is applied and supports only one parameter.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    public void orderByParameterSupportsOnlyOneParameter() throws Exception
    {
        taskModels = restClient.authenticateUser(dataUser.getAdminUser()).withParams("orderBy=id,processDefinitionId").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Only one order by parameter is supported");
        taskModels.assertThat().entriesListIsEmpty();   
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that where parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    public void whereParameterApplied() throws Exception
    {
        UserModel anotherAssignee = dataUser.createRandomTestUser();
        dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(anotherAssignee);
        taskModels = restClient.authenticateUser(dataUser.getAdminUser()).withParams("where=(assignee='" + anotherAssignee.getUsername() + "')").withWorkflowAPI()
                .getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty().and().entriesListContains("assignee", anotherAssignee.getUsername()).and().entriesListCountIs(1);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that invalid orderBy parameter applied returns 400 status code.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    public void invalidOrderByParameterApplied() throws Exception
    {
        restClient.authenticateUser(dataUser.getAdminUser()).withParams("orderBy=invalidParameter").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("invalidParameter is not supported");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that for network enabled only that network tasks are returned.")
    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS,  TestGroup.REGRESSION })
    public void networkEnabledTasksReturned() throws Exception
    {
        restClient.authenticateUser(adminUser);

        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("userTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("userTenantAssignee");

        restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, Priority.Normal);

        tenantTask = restClient.authenticateUser(tenantUserAssignee).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTask.assertThat().entriesListIsNotEmpty().and().entriesListCountIs(1).and().entriesListContains("assignee",
                String.format("userTenantAssignee@%s", tenantUserAssignee.getDomain().toLowerCase()));

    }
}
