package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.GroupModel;
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
@Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.CORE })
public class GetTaskCandidatesCoreTests extends  RestTest
{
    private UserModel userModel, userModel1, userModel2;
    private SiteModel siteModel;
    private FileModel fileModel;
    private TaskModel taskModel;
    private GroupModel group;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userModel1 = dataUser.createRandomTestUser();
        userModel2 = dataUser.createRandomTestUser();
        group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify get task candidates with invalid task id")
    public void getTaskCandidatesWithInvalidTaskId() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                .usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        taskModel.setId("invalid-id");
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).getTaskCandidates();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalid-id"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify get task candidates with empty task id")
    public void getTaskCandidatesWithEmptyTaskId() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                .usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        taskModel.setId("");
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).getTaskCandidates();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify get task candidates for completed task")
    public void getTaskCandidatesForCompletedTask() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                .usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        
        restClient.authenticateUser(userModel1).withWorkflowAPI().usingTask(taskModel).getTaskCandidates();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        dataWorkflow.usingUser(userModel1).taskDone(taskModel);
        
        restClient.authenticateUser(userModel1).withWorkflowAPI().usingTask(taskModel).getTaskCandidates();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, taskModel.getId()));
        
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).getTaskCandidates();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, taskModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify get task candidates by non candidate user")
    public void getTaskCandidatesByNonCandidateUser() throws Exception
    {
        UserModel outsider = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel)
                .usingSite(siteModel)
                .usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        restClient.authenticateUser(outsider).withWorkflowAPI().usingTask(taskModel).getTaskCandidates();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
}