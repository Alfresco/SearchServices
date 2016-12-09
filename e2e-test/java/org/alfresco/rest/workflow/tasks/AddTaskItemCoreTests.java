package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
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

public class AddTaskItemCoreTests extends RestTest
{
    private UserModel userModel, assigneeUser, adminUser;
    private SiteModel siteModel;
    private FileModel fileModel, document;
    private TaskModel taskModel;
    private RestItemModel taskItem;

    private String taskId;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        taskId = taskModel.getId();
        restClient.authenticateUser(userModel);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Add task item using random user.")
    public void addTaskItemByRandomUser() throws JsonToModelConversionException, Exception
    {
        document = dataContent.usingSite(siteModel).createContent(DocumentType.XML);

        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        taskItem.assertThat().field("createdAt").isNotEmpty().and().field("size").isNotEmpty().and().field("createdBy").is(adminUser.getUsername()).and()
                .field("modifiedAt").isNotEmpty().and().field("name").is(document.getName()).and().field("modifiedBy").is(userModel.getUsername()).and()
                .field("id").is(document.getNodeRefWithoutVersion()).and().field("mimeType").is(document.getFileType().mimeType);
    }

    @Bug(id = "ACE-5683")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item, is falling in case invalid itemBody is provided")
    public void failedAddingTaskItemIfInvalidItemBodyIsProvided() throws Exception
    {
        document.setNodeRef("invalidNodeRef");
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidNodeRef"));
    }

    @Bug(id = "ACE-5675")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item is falling in case empty item body is provided")
    public void failedAddingTaskItemIfEmptyItemBodyIsProvided() throws Exception
    {
        document.setNodeRef("");
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document);

        // TODO - expected error message to be added
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item is falling in case invalid task id is provided")
    public void failedAddingTaskItemIfInvalidTaskIdIsProvided() throws Exception
    {
        taskModel.setId("invalidTaskId");
        taskItem = restClient.withWorkflowAPI().usingTask(taskModel).addTaskItem(document);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidTaskId"));
    }

    @Bug(id = "ACE-5675")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Adding task item is falling in case incomplete body type is provided")
    public void failedAddingTaskVariableIfIncompleteBodyIsProvided() throws Exception
    {
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{}", "tasks/{taskId}/items", taskId);
        restClient.processModel(RestVariableModel.class, request);

        // TODO - expected error message to be added
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("");
    }
}
