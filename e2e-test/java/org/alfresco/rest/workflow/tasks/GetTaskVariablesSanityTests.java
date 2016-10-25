package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class GetTaskVariablesSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;

    private UserModel userModel, userWhoStartsTask, assignee;
    private SiteModel siteModel;
    private FileModel fileModel;
    private TaskModel taskModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that started the process gets task variables")
    public void getTaskVariablesByUserWhoStartedProcess() throws Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        tasksApi.getTaskVariables(taskModel).assertThat().entriesListIsNotEmpty();
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that is involved in the process gets task variables")
    public void getTaskVariablesByUserInvolvedInProcess() throws Exception
    {
        restClient.authenticateUser(assignee);
        tasksApi.getTaskVariables(taskModel).assertThat().entriesListIsNotEmpty();
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that is not involved in the process gets task variables")
    public void getTaskVariablesUsingAnyUser() throws Exception
    {
        UserModel randomUser = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(randomUser);
        tasksApi.getTaskVariables(taskModel);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary("Permission was denied");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that admin gets task variables")
    public void getTaskVariablesUsingAdmin() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        
        restClient.authenticateUser(adminUser);
        tasksApi.getTaskVariables(taskModel).assertThat().entriesListIsNotEmpty();
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
