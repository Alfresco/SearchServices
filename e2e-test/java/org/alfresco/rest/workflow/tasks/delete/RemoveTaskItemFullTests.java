package org.alfresco.rest.workflow.tasks.delete;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestItemModel;
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

/**
 * @author bogdan.bocancea
 */
public class RemoveTaskItemFullTests extends RestTest
{
    private UserModel userWhoStartsTask, assigneeUser;
    private SiteModel siteModel;
    private FileModel fileModel, document2;
    private TaskModel taskModel;
    private RestItemModel taskItem;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task item twice")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void deleteTaskItemTwice() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
        restClient.authenticateUser(userWhoStartsTask);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document2);

        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskItem(taskItem);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskItem(taskItem);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.PROCESS_ENTITY_NOT_FOUND, document2.getNodeRefWithoutVersion()))
                              .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task item with locked document")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void deleteTaskItemWithLockedDocument() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
        restClient.authenticateUser(userWhoStartsTask);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document2);

        dataContent.usingUser(userWhoStartsTask).usingResource(document2).checkOutDocument();
        
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskItem(taskItem);
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT)
            .assertLastError().containsSummary(String.format(RestErrorModel.LOCKED_NODE_OPERATION, document2.getNodeRefWithoutVersion()))
                              .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Delete task item with deleted document")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void deleteTaskItemWithDeletedDocument() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
        restClient.authenticateUser(userWhoStartsTask);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document2);

        dataContent.usingUser(userWhoStartsTask).usingResource(document2).deleteContent();
        
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskItem(taskItem);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.PROCESS_ENTITY_NOT_FOUND, document2.getNodeRefWithoutVersion()))
                              .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}