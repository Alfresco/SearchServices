package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessItemModel;
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

    private RestProcessItemModel taskItem;

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
        
        taskItem.and().assertField("createdAt").is(taskItem.getCreatedAt())
            .and().assertField("size").is(taskItem.getSize())
            .and().assertField("createdBy").is(taskItem.getCreatedBy())
            .and().assertField("modifiedAt").is(taskItem.getModifiedAt())
            .and().assertField("name").is(taskItem.getName())
            .and().assertField("modifiedBy").is(taskItem.getModifiedBy())
            .and().assertField("id").is(taskItem.getId())
            .and().assertField("mimeType").is(taskItem.getMimeType());
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
        taskItem.and().assertField("createdAt").is(taskItem.getCreatedAt())
            .and().assertField("size").is(taskItem.getSize())
            .and().assertField("createdBy").is(taskItem.getCreatedBy())
            .and().assertField("modifiedAt").is(taskItem.getModifiedAt())
            .and().assertField("name").is(taskItem.getName())
            .and().assertField("modifiedBy").is(taskItem.getModifiedBy())
            .and().assertField("id").is(taskItem.getId())
            .and().assertField("mimeType").is(taskItem.getMimeType());
        taskItem = tasksApi.addTaskItem(taskModel, document3);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
}
