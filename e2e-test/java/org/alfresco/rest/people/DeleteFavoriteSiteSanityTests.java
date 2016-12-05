package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Cristina Axinte
 */
@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
public class DeleteFavoriteSiteSanityTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel1;
    SiteModel siteModel2;
    UserModel searchedUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel1 = dataSite.usingUser(userModel).createPublicRandomSite();
        siteModel2 = dataSite.usingUser(userModel).createPublicRandomSite();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify manager user removes a site from its favorite sites list with Rest API and response is successful (204)")
    public void managerUserRemovesFavoriteSiteWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel1, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel1).addSiteToFavorites();
        dataSite.usingUser(managerUser).usingSite(siteModel2).addSiteToFavorites();

        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel1);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify collaborator user removes a site from its favorite sites list with Rest API and response is successful (204)")
    public void collaboratorUserRemovesFavoriteSiteWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel1, UserRole.SiteCollaborator);
        dataSite.usingUser(collaboratorUser).usingSite(siteModel1).addSiteToFavorites();
        dataSite.usingUser(collaboratorUser).usingSite(siteModel2).addSiteToFavorites();

        restClient.authenticateUser(collaboratorUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel1);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify contributor user removes a site from its favorite sites list with Rest API and response is successful (204)")
    public void contributorUserRemovesFavoriteSiteWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel1, UserRole.SiteContributor);
        dataSite.usingUser(contributorUser).usingSite(siteModel1).addSiteToFavorites();
        dataSite.usingUser(contributorUser).usingSite(siteModel2).addSiteToFavorites();

        restClient.authenticateUser(contributorUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel1);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify consumer user removes a site from its favorite sites list with Rest API and response is successful (204)")
    public void consumerUserRemovesFavoriteSiteWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel1, UserRole.SiteConsumer);
        dataSite.usingUser(consumerUser).usingSite(siteModel1).addSiteToFavorites();
        dataSite.usingUser(consumerUser).usingSite(siteModel2).addSiteToFavorites();

        restClient.authenticateUser(consumerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel1);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify admin user removes a site from any user's favorite sites list with Rest API and response is successful (204)")
    public void adminUserRemovesAnyFavoriteSiteWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        UserModel anyUser = dataUser.usingAdmin().createRandomTestUser();
        dataSite.usingUser(anyUser).usingSite(siteModel1).addSiteToFavorites();
        dataSite.usingUser(anyUser).usingSite(siteModel2).addSiteToFavorites();

        restClient.authenticateUser(adminUser).withCoreAPI().usingUser(anyUser).removeFavoriteSite(siteModel1);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify a user removes a site from another user's favorite sites list with Rest API and response is permission denied (403)")
    public void userUserRemovesAnotherUserFavoriteSiteWithSuccess() throws Exception
    {
        UserModel userAuth = dataUser.usingAdmin().createRandomTestUser();
        UserModel anotherUser = dataUser.usingAdmin().createRandomTestUser();
        dataSite.usingUser(anotherUser).usingSite(siteModel1).addSiteToFavorites();
        dataSite.usingUser(anotherUser).usingSite(siteModel2).addSiteToFavorites();

        restClient.authenticateUser(userAuth).withCoreAPI().usingUser(anotherUser).removeFavoriteSite(siteModel1);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify manager user is NOT Authorized to remove a site from its favorite sites list with Rest API when authentication fails (401)")
    public void managerUserNotAuthorizedFailsToRemoveFavoriteSite() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel1, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel1).addSiteToFavorites();
        dataSite.usingUser(managerUser).usingSite(siteModel2).addSiteToFavorites();
        managerUser.setPassword("newpassword");

        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel1);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastException().hasName(StatusModel.UNAUTHORIZED);
    }
}
