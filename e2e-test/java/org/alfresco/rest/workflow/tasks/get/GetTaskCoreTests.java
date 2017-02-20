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
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetTaskCoreTests extends RestTest
{
    UserModel userModel, assigneeUser;
    UserModel adminTenantUser, tenantUser, tenantUserAssignee, adminUser;
    SiteModel siteModel;
    FileModel fileModel;
    TaskModel taskModel;
    RestTaskModel restTaskModel, tenantTask;
    RestTaskModelsCollection taskModels;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify if using invalid taskId status code 404 is returned.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void invalidTaskId() throws Exception
    {
        taskModel.setId(RandomStringUtils.randomAlphanumeric(20));
        restTaskModel = restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, taskModel.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify if using empty taskId status code 200 is returned.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void emptyTaskId() throws Exception
    {
        restClient.authenticateUser(userModel).withWorkflowAPI();
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "tasks/{taskId}", "");
        taskModels = restClient.processModels(RestTaskModelsCollection.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify that in a network only tasks inside network are returned.")
    @Test(groups = {TestGroup.NETWORKS,TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE  })
    public void tasksInsideNetworkReturned() throws Exception
    {
        restClient.authenticateUser(adminUser);

        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("userTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("userTenantAssignee");

        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false,
                Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        taskModel = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);
        tenantTask = restClient.authenticateUser(tenantUserAssignee).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTask.assertThat().field("assignee").is(String.format("userTenantAssignee@%s", tenantUserAssignee.getDomain().toLowerCase())).and()
                .field("processId").is(addedProcess.getId());
    }
}
