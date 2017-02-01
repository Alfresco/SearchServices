package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
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

public class GetTasksCoreTests extends RestTest
{
    UserModel userModel, userNotInvolved, candidateUser, assigneeUser, adminUser;
    UserModel adminTenantUser, tenantUser, tenantUserAssignee;
    SiteModel siteModel;
    FileModel fileModel;
    RestTaskModelsCollection taskModels, tenantTask;

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

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify the request in case of any user which is not involved.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void requestWithUserNotInvolved() throws Exception
    {
        taskModels = restClient.authenticateUser(userNotInvolved).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that orderBy parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void orderByParameterApplied() throws Exception
    { 
        taskModels = restClient.authenticateUser(dataUser.getAdminUser()).withParams("orderBy=id").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty().
            and().entriesListIsSortedAscBy("id");       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that orderBy parameter is applied and supports only one parameter.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void orderByParameterSupportsOnlyOneParameter() throws Exception
    {
        taskModels = restClient.authenticateUser(dataUser.getAdminUser()).withParams("orderBy=id,processDefinitionId").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(RestErrorModel.ONLY_ONE_ORDERBY);
        taskModels.assertThat().entriesListIsEmpty();   
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that where parameter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void whereParameterApplied() throws Exception
    {
        taskModels = restClient.authenticateUser(dataUser.getAdminUser()).withParams("where=(assignee='" + assigneeUser.getUsername() + "')").withWorkflowAPI()
                .getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty().and().entriesListContains("assignee", assigneeUser.getUsername()).and().entriesListCountIs(1);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that invalid orderBy parameter applied returns 400 status code.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    public void invalidOrderByParameterApplied() throws Exception
    {
        restClient.authenticateUser(dataUser.getAdminUser()).withParams("orderBy=invalidParameter").withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("invalidParameter is not supported");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that for network enabled only that network tasks are returned.")
    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS,  TestGroup.CORE })
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
