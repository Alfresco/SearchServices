package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestFormModelsCollection;
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
 * 
 * @author bogdan.bocancea
 *
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
public class GetTaskFormModelCoreTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    FileModel fileModel;
    TaskModel taskModel;
    RestFormModelsCollection returnedCollection;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, 
                description = "Verify that non involved user in task cannot get form models with Rest API and response is FORBIDDEN (403)")
    public void nonInvolvedUserCannotGetTaskFormModels() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                    .usingResource(fileModel).createNewTaskAndAssignTo(userModel);
        UserModel nonInvolvedUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(nonInvolvedUser);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, 
                description = "Verify user involved in task cannot get task form models with invalid task id")
    public void getTaskFormModelsInvalidTaskId() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                    .usingResource(fileModel).createNewTaskAndAssignTo(userModel);
        taskModel.setId("0000");
        restClient.authenticateUser(userModel);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "0000"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, 
                description = "Verify user involved in task cannot get task form models with invalid task id")
    public void getTaskFormModelsEmptyTaskId() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                    .usingResource(fileModel).createNewTaskAndAssignTo(userModel);
        taskModel.setId("");
        restClient.authenticateUser(userModel);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, 
                description = "Verify user involved in task cannot get completed task form models")
    public void getTaskFormModelsForCompletedTask() throws Exception
    {
        UserModel assignedUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                    .usingResource(fileModel).createNewTaskAndAssignTo(assignedUser);
        dataWorkflow.usingUser(assignedUser).taskDone(taskModel);
        dataWorkflow.usingUser(userModel).taskDone(taskModel);
        restClient.authenticateUser(userModel);
        returnedCollection = restClient.withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsNotEmpty();
    }
}