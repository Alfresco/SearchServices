package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestVariableModel;
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
 * @author iulia.cojocea
 */
@Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class DeleteTaskVariableSanityTests extends RestTest
{
    private UserModel userModel, adminUser;
    private SiteModel siteModel;
    private FileModel fileModel;
    private UserModel assigneeUser;
    private TaskModel taskModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Delete existing task variable")
    public void deleteTaskVariable() throws Exception
    {
        restClient.authenticateUser(adminUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        
        restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables()
            .assertThat().entriesListDoesNotContain("name", variableModel.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Try to delete existing task variable using invalid task id")
    public void tryToDeleteTaskVariableUsingInvalidTaskId() throws Exception
    {
        restClient.authenticateUser(adminUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        
        restClient.withWorkflowAPI().usingTask(taskModel).updateTaskVariable(variableModel);
        taskModel.setId("incorrectTaskId");
        restClient.withWorkflowAPI().usingTask(taskModel).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
