package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "people", "sanity" })
public class GatFavoriteSitesTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;

    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify manager user gets its favorite sites with Rest API and response is successful (200)")
    public void managerUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(collaboratorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser);
        peopleApi.getFavoriteSites(collaboratorUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify collaborator user gets its favorite sites with Rest API and response is successful (200)")
    public void collaboratorUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        
        dataSite.usingUser(collaboratorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(collaboratorUser);
        peopleApi.getFavoriteSites(contributorUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify contributor user gets its favorite sites with Rest API and response is successful (200)")
    public void contributorUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(contributorUser);
        peopleApi.getFavoriteSites(managerUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify consumer user gets its favorite sites with Rest API and response is successful (200)")
    public void consumerUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        dataUser.usingUser(collaboratorUser).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataSite.usingUser(consumerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(consumerUser);
        peopleApi.getFavoriteSites(collaboratorUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets its favorite sites with Rest API and response is successful (200)")
    public void adminUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        dataSite.usingUser(consumerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(adminUser);
        peopleApi.getFavoriteSites(consumerUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id = "")
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify manager user is NOT Authorized to get its favorite sites with Rest API when authentication fails (401)")
    public void managerUserGetsFavoriteSitesIsNotAuthorized() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        dataSite.usingUser(consumerUser).usingSite(siteModel).addSiteToFavorites();
        
        managerUser.setPassword("newpassword");

        restClient.authenticateUser(managerUser);
        peopleApi.getFavoriteSites(consumerUser);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
