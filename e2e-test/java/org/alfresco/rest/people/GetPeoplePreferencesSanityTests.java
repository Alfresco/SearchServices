package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.PreferenceName;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for Get Peferences (/people/{personId}/preferences) RestAPI call
 * 
 * @author Cristina Axinte
 *
 */
@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES, TestGroup.SANITY })
public class GetPeoplePreferencesSanityTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.SANITY, description = "Verify manager user gets its preferences with Rest API and response is successful (200)")
    public void managerUserGetsPeoplePreferencesWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser)
                  .usingAuthUser()
                  .getPersonPreferences().assertThat().entriesListIsNotEmpty()
                  .assertThat().paginationExist()
                  .and().entriesListContains("id", PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.SANITY, description = "Verify collaborator user gets its preferences with Rest API and response is successful (200)")
    public void collaboratorUserGetsPeoplePreferencesWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataSite.usingUser(collaboratorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(collaboratorUser)
                  .usingAuthUser().getPersonPreferences().assertThat().entriesListIsNotEmpty()
                	.assertThat().paginationExist()
                	.and().entriesListContains("id", PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.SANITY, description = "Verify contributor user gets its preferences with Rest API and response is successful (200)")
    public void contributorUserGetsPeoplePreferencesWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        dataSite.usingUser(contributorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(contributorUser)
                  .usingAuthUser().getPersonPreferences().assertThat().entriesListIsNotEmpty()
              		.assertThat().paginationExist()
              		.and().entriesListContains("id", PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.SANITY, description = "Verify consumer user gets its preferences with Rest API and response is successful (200)")
    public void consumerUserGetsPeoplePreferencesWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        dataSite.usingUser(consumerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(consumerUser)
                  .usingAuthUser().getPersonPreferences().assertThat().entriesListIsNotEmpty()
                  .assertThat().paginationExist()
                  .and().entriesListContains("id", PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.SANITY, description = "Verify admin user gets another user preferences with Rest API and response is successful (200)")
    public void adminUserGetsPeoplePreferencesWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();
        UserModel adminUser = dataUser.getAdminUser();

        restClient.authenticateUser(adminUser)
                  .usingUser(managerUser).getPersonPreferences().assertThat().entriesListIsNotEmpty()
                  .assertThat().paginationExist()
                  .and().entriesListContains("id", PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.SANITY, description = "Verify manager user is NOT Authorized to gets its preferences with Rest API when authentication fails(401)")
    public void managerUserGetsPeoplePreferencesIsNotAUthorized() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();
        managerUser.setPassword("newpassword");

        restClient.authenticateUser(managerUser)
                  .usingAuthUser()
                  .getPersonPreferences();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
