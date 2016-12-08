package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
public class AddTaskItemCoreTests extends RestTest
{
    private UserModel userModel, assigneeUser;
    private SiteModel siteModel;
    private FileModel fileModel, document2, document3, document4, document5;
    private TaskModel taskModel;
    private RestItemModel taskItem;

    private UserModel adminUser;
    private String taskId;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        adminUser = dataUser.getAdminUser();
        taskId = taskModel.getId();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Add task item using admin user from same network")
    public void addTaskItemByRandomUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(userModel);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);

        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        taskItem.assertThat().field("createdAt").is(taskItem.getCreatedAt()).assertThat().field("size").is(taskItem.getSize()).and().field("createdBy")
                .is(taskItem.getCreatedBy()).and().field("modifiedAt").is(taskItem.getModifiedAt()).and().field("name").is(taskItem.getName()).and()
                .field("modifiedBy").is(taskItem.getModifiedBy()).and().field("id").is(taskItem.getId()).and().field("mimeType").is(taskItem.getMimeType());

        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document2);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item, is falling in case invalid itemBody is provided")
    public void failedAddingTaskItemIfInvalidItemBodyIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);
        document3 = dataContent.usingSite(siteModel).createContent(DocumentType.HTML);
        document3.setNodeRef("invalidNodeRef");

        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document3);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary("The entity with id: item with id workspace://SpacesStore/invalidNodeRef not found was not found");

    }

    @Bug(id = "ACE-5675")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item is falling in case empty item body is provided")
    public void failedAddingTaskItemIfEmptyItemBodyIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);
        document4 = dataContent.usingSite(siteModel).createContent(DocumentType.PDF);
        document4.setNodeRef("");

        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document4);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item is falling in case invalid task id is provided")
    public void failedAddingTaskItemIfInvalidTaskIdIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);
        document5 = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        taskModel.setId("invalidTaskId");

        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document5);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary("The entity with id: invalidTaskId was not found");
    }

    @Bug(id = "ACE-5675")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item is falling in case incomplete body type is provided")
    public void failedAddingTaskVariableIfIncompleteBodyIsProvided() throws Exception
    {
        restClient.authenticateUser(adminUser);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{}", "tasks/{taskId}/items", taskId);
        restClient.processModel(RestVariableModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

}
