package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
public class GetFavoriteSitesSanityTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify manager user fails to get an user favorite sites with Rest API (403)")
    public void managerUserFailsToGetAnUserFavoriteSites() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        UserModel anotherUser = dataUser.usingAdmin().createRandomTestUser();    
        dataSite.usingUser(anotherUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser)                
                  .withCoreAPI()
                  .usingUser(anotherUser).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify collaborator user fails to get an user favorite sites with Rest API (403)")
    public void collaboratorUserFailsToGetAnUserFavoriteSites() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);    
        dataSite.usingUser(contributorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(collaboratorUser)
                  .withCoreAPI()
                  .usingUser(contributorUser).getFavoriteSites();        
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify contributor user fails to get an user favorite sites with Rest API (403)")
    public void contributorUserFailsToGetAnUserFavoriteSites() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        UserModel contributorUser2 = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        dataUser.usingUser(userModel).addUserToSite(contributorUser2, siteModel, UserRole.SiteContributor);
        dataSite.usingUser(contributorUser2).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(contributorUser)
                  .withCoreAPI()
                  .usingUser(contributorUser2).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify consumer user fails to get an user favorite sites with Rest API (403)")
    public void consumerUserFailsToGetAnUserFavoriteSites() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        dataUser.usingUser(collaboratorUser).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        dataSite.usingUser(collaboratorUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(consumerUser)
                  .withCoreAPI()
                  .usingUser(collaboratorUser).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify admin user gets its favorite sites with Rest API and response is successful (200)")
    public void adminUserGetsFavoriteSitesWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        UserModel anotherUser = dataUser.usingAdmin().createRandomTestUser();
        dataSite.usingUser(anotherUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(adminUser)
                  .withCoreAPI()
                  .usingUser(anotherUser).getFavoriteSites()
                  .assertThat().entriesListIsNotEmpty()
                  .and().paginationExist()
                  .and().entriesListContains("id", siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify any user gets its own user favorite sites with Rest API and response is successful (200)")
    public void anyUserGetsItsUserFavoriteSites() throws Exception
    {
        UserModel anyUser = dataUser.usingAdmin().createRandomTestUser();   
        dataSite.usingUser(anyUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(anyUser)
                  .withCoreAPI()
                  .usingAuthUser().getFavoriteSites()
                  .assertThat().entriesListIsNotEmpty()
                  .and().entriesListContains("id", siteModel.getId())
                  .and().paginationExist();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify any user is NOT Authorized to get its favorite sites with Rest API when authentication fails (401)")
    public void anyUserNotAuthenticatedIsNotAuthorizedToGetFavoriteSites() throws Exception
    {
        UserModel anyUser = dataUser.usingAdmin().createRandomTestUser();   
        dataSite.usingUser(anyUser).usingSite(siteModel).addSiteToFavorites();
        anyUser.setPassword("newpassword");

        restClient.authenticateUser(anyUser)
                  .withCoreAPI()
                  .usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
