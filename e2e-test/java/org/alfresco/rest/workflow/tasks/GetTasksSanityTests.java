package org.alfresco.rest.workflow.tasks;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "workflow", "tasks", "sanity" })
public class GetTasksSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;

    UserModel userModel;
    SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify manager user fails to get an user favorite sites with Rest API (403)")
    public void managerUserFailsToGetAnUserFavoriteSites() throws Exception
    {
        UserModel adminUser=dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        tasksApi.getTasks().assertEntriesListIsNotEmpty();
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
