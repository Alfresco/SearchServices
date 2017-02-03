package org.alfresco.rest.workflow.tasks;

import javax.json.JsonObject;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdateTaskFullTestsBulk2 extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private FileModel fileModel;
    private UserModel assigneeUser;
    private TaskModel taskModel;
    private RestTaskModel restTaskModel;
    private UserModel adminUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
    }

    @BeforeMethod(alwaysRun = true)
    public void createTask() throws Exception
    {
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to claimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToClaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("claimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("claimed");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to delegated")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToDelegated() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
     
        JsonObject inputJson = JsonBodyGenerator.defineJSON().add("state", "delegated").add("assignee", assigneeUser.getUsername()).build();
        
        restTaskModel = restClient.authenticateUser(adminUser).withParams("select=state,assignee").withWorkflowAPI().usingTask(taskModel).updateTask(inputJson);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("delegated");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to resolved")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToResolved() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("resolved");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("resolved");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that task can be updated from unclaimed to unclaimed")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void userCanUpdateTaskFromUnclaimedToUnclaimed() throws Exception
    {
        restTaskModel = restClient.authenticateUser(assigneeUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
        restTaskModel = restClient.authenticateUser(userModel).withParams("select=state").withWorkflowAPI().usingTask(taskModel).updateTask("unclaimed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel.getId()).and().field("state").is("unclaimed");
    }
}