

package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestTaskModelsCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.GroupModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class GetTasksSanityTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;
    RestTaskModelsCollection taskModels;    

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify admin user gets all existing tasks with Rest API and response is successfull (200)")
    public void adminUserGetsAllTasks() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
       
        taskModels = restClient.authenticateUser(adminUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify asignee user gets its existing tasks with Rest API and response is successfull (200)")
    public void asigneeUserGetsItsTasks() throws Exception
    {
        taskModels = restClient.authenticateUser(assigneeUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty().and().entriesListCountIs(1).and().entriesListContains("assignee", assigneeUser.getUsername());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify candidate user that claims the task gets its existing tasks with Rest API and response is successfull (200)")
    @Bug(id = "MNT-16967")    
    public void candidateUserGetsItsTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        
        taskModels = restClient.authenticateUser(userModel1).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify candidate user that claims the task gets its existing tasks with Rest API and response is successfull (200)")
    public void candidateUserThatClaimsTaskGetsItsTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        TaskModel taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        dataWorkflow.usingUser(userModel1).claimTask(taskModel);
        
        taskModels = restClient.authenticateUser(userModel1).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify candidate user without claim gets no tasks with Rest API and response is successfull (200)")
    public void candidateUserWithoutClaimTaskGetsNoTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        TaskModel taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        dataWorkflow.usingUser(userModel1).claimTask(taskModel);
        
        taskModels = restClient.authenticateUser(userModel2).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsEmpty();
    }
}