package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.PreferenceName;
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
 * Tests for Get a Peference (/people/{personId}/preferences/{preferenceName}) RestAPI call
 * 
 * @author Cristina Axinte
 *
 */
@Test(groups = { "rest-api", "people", "preferences", "sanity" })
public class GetPeoplePreferenceTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;
    
    UserModel userModel;
    SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        peopleApi.useRestClient(restClient);
    }
    
    @TestRail(section = { "rest-api", "people", "preferences" }, executionType = ExecutionType.SANITY, description = "Verify manager user gets a specific preference with Rest API and response is successful (200)")
    public void managerUserGetsAPreferenceWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser);
        peopleApi.getPersonPreferenceInformation(managerUser, PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "preferences" }, executionType = ExecutionType.SANITY, description = "Verify collaborator user gets a specific preference with Rest API and response is successful (200)")
    public void collaboratorUserGetsAPreferenceWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataSite.usingUser(collaboratorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(collaboratorUser);
        peopleApi.getPersonPreferenceInformation(collaboratorUser, PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "preferences" }, executionType = ExecutionType.SANITY, description = "Verify contributor user gets a specific preference with Rest API and response is successful (200)")
    public void contributorUserGetsAPreferenceWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        dataSite.usingUser(contributorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(contributorUser);
        peopleApi.getPersonPreferenceInformation(contributorUser, PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "preferences" }, executionType = ExecutionType.SANITY, description = "Verify consumer user gets a specific preference with Rest API and response is successful (200)")
    public void consumerUserGetsAPreferenceWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        dataSite.usingUser(consumerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(consumerUser);
        peopleApi.getPersonPreferenceInformation(consumerUser, PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people", "preferences" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets a specific preference with Rest API and response is successful (200)")
    public void adminUserGetsAPreferenceWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        dataSite.usingUser(adminUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(adminUser);
        peopleApi.getPersonPreferenceInformation(adminUser, PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
