package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestFormModelsCollection;
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
 * Created by Claudia Agache on 10/20/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class GetTaskFormModelSanityTests extends RestWorkflowTest
{
    @Autowired RestTasksApi tasksApi;

    UserModel userModel;
    SiteModel siteModel;
    FileModel fileModel;
    TaskModel taskModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(userModel);

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.SANITY, description = "Verify admin user gets all task form models with Rest API and response is successful (200)")
    public void adminGetsTaskFormModels() throws Exception
    {
        restClient.authenticateUser(dataUser.getAdminUser());
        RestFormModelsCollection returnedCollection = tasksApi.getTaskFormModel(taskModel);
        
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertEntriesListIsNotEmpty();
        
        String[] qualifiedNames = {
                               "{http://www.alfresco.org/model/bpm/1.0}percentComplete", 
                               "{http://www.alfresco.org/model/bpm/1.0}context",
                               "{http://www.alfresco.org/model/bpm/1.0}completedItems",
                               "{http://www.alfresco.org/model/content/1.0}name",
                               "{http://www.alfresco.org/model/bpm/1.0}packageActionGroup",
                               "{http://www.alfresco.org/model/bpm/1.0}reassignable",
                               "{http://www.alfresco.org/model/content/1.0}owner",
                               "{http://www.alfresco.org/model/bpm/1.0}outcome",
                               "{http://www.alfresco.org/model/bpm/1.0}taskId",
                               "{http://www.alfresco.org/model/bpm/1.0}packageItemActionGroup",
                               "{http://www.alfresco.org/model/bpm/1.0}completionDate"};
        
        for(String formQualifiedName :  qualifiedNames)
        {
          returnedCollection.assertEntriesListContains("qualifiedName", formQualifiedName);
        }
        
        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.SANITY, description = "Verify user involved in task gets all the task form models with Rest API and response is successful (200)")
    public void involvedUserGetsTaskFormModels() throws Exception
    {
        restClient.authenticateUser(userModel);
        RestFormModelsCollection returnedCollection = tasksApi.getTaskFormModel(taskModel).assertEntriesListIsNotEmpty();
        
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertEntriesListIsNotEmpty();
        
        String[] qualifiedNames = {
                               "{http://www.alfresco.org/model/bpm/1.0}percentComplete", 
                               "{http://www.alfresco.org/model/bpm/1.0}context",
                               "{http://www.alfresco.org/model/bpm/1.0}completedItems",
                               "{http://www.alfresco.org/model/content/1.0}name",
                               "{http://www.alfresco.org/model/bpm/1.0}packageActionGroup",
                               "{http://www.alfresco.org/model/bpm/1.0}reassignable",
                               "{http://www.alfresco.org/model/content/1.0}owner",
                               "{http://www.alfresco.org/model/bpm/1.0}outcome",
                               "{http://www.alfresco.org/model/bpm/1.0}taskId",
                               "{http://www.alfresco.org/model/bpm/1.0}packageItemActionGroup",
                               "{http://www.alfresco.org/model/bpm/1.0}completionDate"};
        
        for(String formQualifiedName :  qualifiedNames)
        {
          returnedCollection.assertEntriesListContains("qualifiedName", formQualifiedName);
        }
    }
}
