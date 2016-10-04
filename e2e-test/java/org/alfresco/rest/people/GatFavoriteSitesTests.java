package org.alfresco.rest.people;

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
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser);
        peopleApi.getFavoriteSites(managerUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify collaborator user gets its favorite sites with Rest API and response is successful (200)")
    public void collaboratorUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataSite.usingUser(collaboratorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(collaboratorUser);
        peopleApi.getFavoriteSites(collaboratorUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify contributor user gets its favorite sites with Rest API and response is successful (200)")
    public void contributorUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        dataSite.usingUser(contributorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(contributorUser);
        peopleApi.getFavoriteSites(contributorUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify consumer user gets its favorite sites with Rest API and response is successful (200)")
    public void consumerUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        dataSite.usingUser(consumerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(consumerUser);
        peopleApi.getFavoriteSites(consumerUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets its favorite sites with Rest API and response is successful (200)")
    public void adminUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        dataSite.usingUser(adminUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(adminUser);
        peopleApi.getFavoriteSites(adminUser).assertResponseIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
