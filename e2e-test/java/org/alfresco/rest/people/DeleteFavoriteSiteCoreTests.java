package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSiteModel;
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

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
public class DeleteFavoriteSiteCoreTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel adminUserModel;
    RestSiteModel restSiteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        adminUserModel = dataUser.getAdminUser();
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify user removes a site from favorites using '-me-' in place of personId with Rest API and response is successful (204)")
    public void removeFavoriteSiteWithSuccessUsingMeAsPersonId() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        dataSite.usingUser(adminUserModel).usingSite(siteModel).addSiteToFavorites();

        restClient.withCoreAPI().usingMe().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @Bug(id="REPO-1642", description = "reproduced on 5.2.1 only, it works on 5.2.0")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify inexistent user is not able to remove a site from favorites and response is 404")
    public void inexistentUserIsNotAbleToRemoveFavoriteSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        dataSite.usingUser(adminUserModel).usingSite(siteModel).addSiteToFavorites();

        UserModel inexistentUser = new UserModel("inexistenUser", "password");
        restClient.withCoreAPI().usingUser(inexistentUser).removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, "inexistenUser"));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify user is not able to remove from favorites a site with inexistent id and response is 404")
    public void userIsNotAbleToRemoveFavoriteSiteWithInexistentId() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        dataSite.usingUser(adminUserModel).usingSite(siteModel).addSiteToFavorites();

        SiteModel inexistentSite = new SiteModel("inexistentSite");
        restClient.withCoreAPI().usingUser(adminUserModel).removeFavoriteSite(inexistentSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(ErrorModel.RELATIONSHIP_NOT_FOUND, adminUserModel.getUsername(), inexistentSite.getTitle()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify manager user removes a site from its favorites abd add it again and response is successful (204)")
    public void managerUserRemovesFavoriteSiteAndAddItAgain() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingUser(managerUser).addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify manager user removes a site from its favorite sites list with Rest API and response is successful (204)")
    public void managerUserRemovesFavoriteSiteWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("id", siteModel.getId());
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify uninvited user can delete favorite public site and response is 204")
    public void uninvitedUserCanDeleteFavoritePublicSite() throws Exception
    {        
        SiteModel publicSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(userModel).addFavoriteSite(publicSiteModel);
        
        restClient.withCoreAPI().usingAuthUser().removeFavoriteSite(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavoriteSites().assertThat().entriesListDoesNotContain("id", publicSiteModel.getId());
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify uninvited user can delete favorite moderated site and response is 204")
    public void uninvitedUserCanDeleteFavoriteModeratedSite() throws Exception
    {        
        SiteModel moderatedSiteModel = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(userModel).addFavoriteSite(moderatedSiteModel);
        
        restClient.withCoreAPI().usingAuthUser().removeFavoriteSite(moderatedSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavoriteSites().assertThat().entriesListDoesNotContain("id", moderatedSiteModel.getId());
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify user can delete favorite private site and response is 204")
    public void userCanDeleteFavoritePrivateSite() throws Exception
    {
        SiteModel privateSiteModel = dataSite.usingUser(userModel).createPrivateRandomSite();
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(userModel).addFavoriteSite(privateSiteModel);
        
        restClient.withCoreAPI().usingAuthUser().removeFavoriteSite(privateSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavoriteSites().assertThat().entriesListDoesNotContain("id", privateSiteModel.getId());
    }
    
}
