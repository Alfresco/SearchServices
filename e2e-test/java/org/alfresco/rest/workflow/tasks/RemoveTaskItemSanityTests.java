package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class RemoveTaskItemSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;
        
    
    private UserModel adminUser, userModel, userWhoStartsTask, assigneeUser;
    private SiteModel siteModel; 
    private FileModel fileModel, document2;
    private TaskModel taskModel;

    private RestItemModel taskItem;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userWhoStartsTask = dataUser.createRandomTestUser();
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Delete existing task item")
    public void deleteTaskItem() throws Exception
    {
        restClient.authenticateUser(adminUser);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = tasksApi.addTaskItem(taskModel, document2);
        tasksApi.deleteTaskItem(taskModel, taskItem);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
        tasksApi.getTaskItems(taskModel).assertEntriesListDoesNotContain("name", document2.getName());
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Try to Delete existing task item using invalid taskId")
    public void deleteTaskItemUsingInvalidTaskId() throws Exception
    {
        restClient.authenticateUser(adminUser);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = tasksApi.addTaskItem(taskModel, document2);
        taskModel.setId("invalidTaskId");
        tasksApi.deleteTaskItem(taskModel, taskItem);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Try to Delete existing task item using invalid itemId")
    public void deleteTaskItemUsingInvalidItemId() throws Exception
    {
        restClient.authenticateUser(adminUser);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = tasksApi.addTaskItem(taskModel, document2);
        taskItem.setId("incorrectItemId");
        tasksApi.deleteTaskItem(taskModel, taskItem);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
