package org.alfresco.rest.people;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
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
@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, "activities", TestGroup.SANITY })
public class GetPeopleActivitiesSanityTests extends RestTest
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

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, "activities" }, executionType = ExecutionType.SANITY, description = "Verify manager user gets its activities with Rest API and response is successful")
    public void managerUserShouldGetPeopleActivitiesList() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataContent.usingUser(managerUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(managerUser);
        peopleApi.getPersonActivities(managerUser).assertEntriesListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, "activities" }, executionType = ExecutionType.SANITY, description = "Verify collaborator user gets its activities with Rest API and response is successful")
    public void collaboratorUserShouldGetPeopleActivitiesList() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataContent.usingUser(collaboratorUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(collaboratorUser);
        peopleApi.getPersonActivities(collaboratorUser).assertEntriesListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, "activities" }, executionType = ExecutionType.SANITY, description = "Verify contributor user gets its activities with Rest API and response is successful")
    public void contributorUserShouldGetPeopleActivitiesList() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        dataContent.usingUser(contributorUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(contributorUser);
        peopleApi.getPersonActivities(contributorUser).assertEntriesListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, "activities" }, executionType = ExecutionType.SANITY, description = "Verify consumer user gets its activities with Rest API and response is successful")
    public void consumerUserShouldGetPeopleActivitiesList() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(consumerUser);
        peopleApi.getPersonActivities(consumerUser).assertEntriesListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, "activities" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets another user activities with Rest API and response is successful")
    public void adminUserShouldGetPeopleActivitiesList() throws Exception
    {
        restClient.authenticateUser(dataUser.getAdminUser());
        peopleApi.getPersonActivities(userModel).assertEntriesListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, "activities" }, executionType = ExecutionType.SANITY, description = "Verify manager user is NOT Authorized to gets another user activities with Rest API")
    public void managerUserShouldGetPeopleActivitiesListIsNotAuthorized() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        managerUser.setPassword("newpassword");

        restClient.authenticateUser(managerUser);
        peopleApi.getPersonActivities(userModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
