package org.alfresco.rest.people;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Cristina Axinte
 * 
 * Tests for getActivities (/people/{personId}/activities) RestAPI call
 * 
 */
@Test(groups = { "rest-api", "people", "activities", "sanity" })
public class GetPeopleActivitiesTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;

    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "people", "activities" }, executionType = ExecutionType.SANITY, description = "Verify manager user gets its activities with Rest API and response is successful")
    public void managerUserGetsPeopleActivitiesListSuccessful() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataContent.usingUser(managerUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(managerUser);
        peopleApi.getPersonActivities(managerUser).assertActivityListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "activities" }, executionType = ExecutionType.SANITY, description = "Verify collaborator user gets its activities with Rest API and response is successful")
    public void collaboratorUserGetsPeopleActivitiesListSuccessful() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataContent.usingUser(collaboratorUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(collaboratorUser);
        peopleApi.getPersonActivities(collaboratorUser).assertActivityListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "activities" }, executionType = ExecutionType.SANITY, description = "Verify contributor user gets its activities with Rest API and response is successful")
    public void contributorUserGetsPeopleActivitiesListSuccessful() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        dataContent.usingUser(contributorUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(contributorUser);
        peopleApi.getPersonActivities(contributorUser).assertActivityListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "activities" }, executionType = ExecutionType.SANITY, description = "Verify consumer user gets its activities with Rest API and response is successful")
    public void consumerUserGetsPeopleActivitiesListSuccessful() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);

        restClient.authenticateUser(consumerUser);
        peopleApi.getPersonActivities(consumerUser).assertActivityListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "activities" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets another user activities with Rest API and response is successful")
    public void adminUserGetsPeopleActivitiesListSuccessful() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();

        restClient.authenticateUser(adminUser);
        peopleApi.getPersonActivities(userModel).assertActivityListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
