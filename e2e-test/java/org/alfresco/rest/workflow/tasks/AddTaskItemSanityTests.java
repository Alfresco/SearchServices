package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.rest.requests.RestTenantApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class AddTaskItemSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;
        
    @Autowired
    RestTenantApi tenantApi;
    
    private UserModel userModel, userWhoStartsTask, assigneeUser, adminTenantUser, tenantUser, tenantUserAssignee;;
    private SiteModel siteModel; 
    private FileModel fileModel, document2, document3, document4;
    private TaskModel taskModel;

    private RestItemModel taskItem;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userWhoStartsTask = dataUser.createRandomTestUser();
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Create non-existing task item")
    public void createTaskItem() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = tasksApi.addTaskItem(taskModel, document2);
        
        taskItem.assertThat().field("createdAt").is(taskItem.getCreatedAt())
            .assertThat().field("size").is(taskItem.getSize())
            .assertThat().field("createdBy").is(taskItem.getCreatedBy())
            .assertThat().field("modifiedAt").is(taskItem.getModifiedAt())
            .assertThat().field("name").is(taskItem.getName())
            .assertThat().field("modifiedBy").is(taskItem.getModifiedBy())
            .assertThat().field("id").is(taskItem.getId())
            .assertThat().field("mimeType").is(taskItem.getMimeType());
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @Bug(id = "MNT-16966")
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that in case task item exists request ")
    public void createTaskItemThatAlreadyExists() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        document3 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = tasksApi.addTaskItem(taskModel, document3);
        taskItem.assertThat().field("createdAt").is(taskItem.getCreatedAt())
            .assertThat().field("size").is(taskItem.getSize())
            .and().field("createdBy").is(taskItem.getCreatedBy())
            .and().field("modifiedAt").is(taskItem.getModifiedAt())
            .and().field("name").is(taskItem.getName())
            .and().field("modifiedBy").is(taskItem.getModifiedBy())
            .and().field("id").is(taskItem.getId())
            .and().field("mimeType").is(taskItem.getMimeType());
        taskItem = tasksApi.addTaskItem(taskModel, document3);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
    
    @Test(groups = { TestGroup.NETWORKS })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Add task item using admin user from same network")
    public void addTaskItemByAdminSameNetwork() throws JsonToModelConversionException, Exception
    {
        UserModel adminuser = dataUser.getAdminUser();
        restClient.authenticateUser(adminuser);
        
        adminTenantUser = UserModel.getAdminTenantUser();
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser);
        
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
        
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();   
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(tenantUserAssignee);
        
        document4 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = tasksApi.addTaskItem(taskModel, document4);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
}
