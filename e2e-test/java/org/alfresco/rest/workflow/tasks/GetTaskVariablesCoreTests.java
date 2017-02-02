package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestVariableModelsCollection;
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
 * Created by Claudia Agache on 12/7/2016.
 */

public class GetTaskVariablesCoreTests extends RestTest
{
    private UserModel userWhoStartsTask, assignee;
    private SiteModel siteModel;
    private FileModel fileModel;
    private TaskModel taskModel;
    private RestVariableModelsCollection variableModels;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify if get task variables request returns status code 404 when invalid taskId is used")
    public void getTaskVariablesUsingInvalidTaskId() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        taskModel.setId("invalidId");

        restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidId"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify if get task variables request returns status code 404 when empty taskId is used")
    public void getTaskVariablesUsingEmptyTaskId() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        taskModel.setId("");

        restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify if get task variables request returns status code 200 after the task is finished.")
    public void getTaskVariablesAfterFinishingTask() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        dataWorkflow.usingUser(assignee).taskDone(taskModel);

        variableModels = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify if get task variables request returns status code 200 after the process is deleted (Task state is now completed.)")
    public void getTaskVariablesAfterDeletingProcess() throws Exception
    {
        RestProcessModel addedProcess = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        RestTaskModel addedTask = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);
        restClient.withWorkflowAPI().usingProcess(addedProcess).deleteProcess();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        variableModels = restClient.withWorkflowAPI().usingTask(addedTask).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty();
    }


    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify user gets task variables that matches a where clause.")
    public void getTaskVariablesWithWhereClauseAsParameter() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
        variableModels = restClient.authenticateUser(userWhoStartsTask).where("scope='local'")
                .withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty()
                .and().entriesListDoesNotContain("scope", "global")
                .and().paginationField("totalItems").is("8");

        variableModels = restClient.authenticateUser(userWhoStartsTask)
                .withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListContains("scope", "global")
                .and().paginationField("totalItems").is("30");
    }

}
