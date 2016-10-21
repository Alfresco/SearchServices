package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.rest.requests.RestTenantApi;
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

@Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class GetTaskItemsSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;

    @Autowired
    RestTenantApi tenantApi;
    
    private UserModel userModel, userWhoStartsTask, assignee, adminTenantUser, tenantUser, tenantUserAssignee;
    private SiteModel siteModel;
    private FileModel fileModel, document1;
    private TaskModel taskModel;
    private RestItemModel taskItem;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that started the process gets task items")
    public void getTaskItemsByUserWhoStartedProcess() throws Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        document1 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = tasksApi.addTaskItem(taskModel, document1);
        tasksApi.getTaskItems(taskModel)
        .assertEntriesListIsNotEmpty()
        .assertEntriesListContains("id", taskItem.getId());

        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
