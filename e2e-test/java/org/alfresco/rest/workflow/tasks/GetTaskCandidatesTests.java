package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.GroupModel;
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
 * 
 * @author Cristina Axinte
 *
 */
@Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class GetTaskCandidatesTests extends  RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;
    
    private UserModel userModel, user, userModel1, userModel2;
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
        user = dataUser.createRandomTestUser();
        userModel1 = dataUser.createRandomTestUser();
        userModel2 = dataUser.createRandomTestUser();
        group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        taskModel = dataWorkflow.usingUser(user).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that admin gets task candidates")
    public void getTaskCandidatesByAdmin() throws Exception
    {          
        restClient.authenticateUser(dataUser.getAdminUser());
        tasksApi.getTaskCandidates(taskModel)
            .assertThat().entriesListContains("candidateType", "group")
            .and().entriesListContains("candidateId", String.format("GROUP_%s", group.getGroupIdentifier()));
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that started the process gets task candidates")
    public void getTaskCandidatesByUserWhoStartedProcess() throws Exception
    {          
        restClient.authenticateUser(user);
        tasksApi.getTaskCandidates(taskModel)
            .assertThat().entriesListContains("candidateType", "group")
            .and().entriesListContains("candidateId", String.format("GROUP_%s", group.getGroupIdentifier()));
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Verify that user from the assighed group to the process gets task candidates")
    public void getTaskCandidatesByCandidateUser() throws Exception
    {      
        restClient.authenticateUser(userModel1);
        tasksApi.getTaskCandidates(taskModel)
            .assertThat().entriesListContains("candidateType", "group")
            .and().entriesListContains("candidateId", String.format("GROUP_%s", group.getGroupIdentifier()));
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
