package org.alfresco.rest.workflow.tasks.put;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestTaskModel;
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


public class UpdateTaskSanityTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;
    TaskModel taskModel;
    RestTaskModel restTaskModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify admin user updates task with Rest API and response is successfull (200)")
    public void adminUserUpdatesAnyTaskWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        
        restTaskModel = restClient.authenticateUser(adminUser).withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
                .and().field("description").is(taskModel.getMessage());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify assignee user updates its assigned task with Rest API and response is successfull (200)")
    public void assigneeUserUpdatesItsTaskWithSuccess() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId())
                .and().field("description").is(taskModel.getMessage());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify user that started the task updates the started task with Rest API and response is successfull (200)")
    public void starterUserUpdatesItsTaskWithSuccess() throws Exception
    {
        restTaskModel = restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify any user with no relation to task is forbidden to update other task with Rest API (403)")
    public void anyUserIsForbiddenToUpdateOtherTask() throws Exception
    {
        UserModel anyUser= dataUser.createRandomTestUser();

        restTaskModel = restClient.authenticateUser(anyUser).withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary("Permission was denied");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, description = "Verify candidate user updates its specific task and no other user claimed the task with Rest API and response is successfull (200)")
    public void candidateUserUpdatesItsTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        TaskModel taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);

        restTaskModel = restClient.authenticateUser(userModel1).withWorkflowAPI().usingTask(taskModel).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat()
            .field("dueAt").isNotEmpty()
            .and().field("processDefinitionId").is("activitiReviewPooled:1:12")
            .and().field("processId").is(taskModel.getProcessId())
            .and().field("name").is("Review Task")
            .and().field("description").is(taskModel.getMessage())
            .and().field("startedAt").isNotEmpty()
            .and().field("id").is(taskModel.getId())
            .and().field("state").is("unclaimed")
            .and().field("activityDefinitionId").is("reviewTask")
            .and().field("priority").is(taskModel.getPriority().getLevel())
            .and().field("formResourceKey").is("wf:activitiReviewTask");
    }
}