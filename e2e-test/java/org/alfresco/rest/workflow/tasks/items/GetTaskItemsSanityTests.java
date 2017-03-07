package org.alfresco.rest.workflow.tasks.items;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestItemModelsCollection;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class GetTaskItemsSanityTests extends RestTest
{
    private UserModel userModel, userWhoStartsTask, assignee;
    private SiteModel siteModel;
    private FileModel fileModel, document1;
    private TaskModel taskModel;
    private RestItemModel taskItem;
    private RestItemModelsCollection itemModels;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
    }

    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that user that started the process gets task items")
    public void getTaskItemsByUserWhoStartedProcess() throws Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        document1 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document1);
        Utility.checkObjectIsInitialized(taskItem, "taskItem");
        
        itemModels = restClient.withWorkflowAPI().usingTask(taskModel).getTaskItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        itemModels.assertThat()
            .entriesListIsNotEmpty().and()
            .entriesListContains("id", taskItem.getId()).and()
            .entriesListContains("name", document1.getName());
    }

    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that involved user in process gets task items")
    public void getTaskItemsByUserInvolvedInProcess() throws Exception
    {
        restClient.authenticateUser(assignee);
        document1 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document1);
        
        itemModels = restClient.withWorkflowAPI().usingTask(taskModel).getTaskItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        itemModels.assertThat()
            .entriesListIsNotEmpty().and()
            .entriesListContains("id", taskItem.getId()).and()
            .entriesListContains("name", document1.getName());
        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that user that started the process gets task items")
    @Test(groups = { TestGroup.NETWORKS,TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY})
    public void getTaskItemsByAdminInSameNetwork() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser1);
  
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant");
        UserModel tenantUserAssignee1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenantAssignee");

        siteModel = dataSite.usingUser(adminTenantUser1).createPublicRandomSite();
        document1 = dataContent.usingUser(adminTenantUser1).usingSite(siteModel).createContent(DocumentType.XML);
        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser1).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee1, false, Priority.Normal);
        RestTaskModel addedTask = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);  
        taskItem = restClient.withWorkflowAPI().usingTask(addedTask).addTaskItem(document1);
        
        itemModels = restClient.withWorkflowAPI().usingTask(addedTask).getTaskItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        itemModels.assertThat()
            .entriesListIsNotEmpty().and()
            .entriesListContains("id", taskItem.getId()).and()
            .entriesListContains("name", document1.getName());
    }
}