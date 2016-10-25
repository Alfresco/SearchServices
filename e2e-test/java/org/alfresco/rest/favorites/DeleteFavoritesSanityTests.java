package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestFavoritesApi;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.ErrorModel;
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

@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
public class DeleteFavoritesSanityTests extends RestTest
{
    @Autowired
    RestFavoritesApi favoritesAPI;

    @Autowired
    RestSitesApi sitesApi;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();

        favoritesAPI.useRestClient(restClient);
        sitesApi.useRestClient(restClient);
        siteModel.setGuid(sitesApi.getSite(siteModel).getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user deletes site from favorites with Rest API and status code is 204")
    public void adminIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addSiteToFavorites(adminUserModel, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        favoritesAPI.deleteSiteFromFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
        favoritesAPI.getFavorites(adminUserModel).assertEntriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Manager user deletes site from favorites with Rest API and status code is 204")
    public void managerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        restClient.authenticateUser(siteManager);
        favoritesAPI.addSiteToFavorites(siteManager, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        favoritesAPI.deleteSiteFromFavorites(siteManager, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
        favoritesAPI.getFavorites(siteManager).assertEntriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user deletes site from favorites with Rest API and status code is 204")
    public void collaboratorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator);
        favoritesAPI.addSiteToFavorites(siteCollaborator, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        favoritesAPI.deleteSiteFromFavorites(siteCollaborator, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
        favoritesAPI.getFavorites(siteCollaborator).assertEntriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Contributor user deletes site from favorites with Rest API and status code is 204")
    public void contributorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteContributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        restClient.authenticateUser(siteContributor);
        favoritesAPI.addSiteToFavorites(siteContributor, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        favoritesAPI.deleteSiteFromFavorites(siteContributor, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
        favoritesAPI.getFavorites(siteContributor).assertEntriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Consumer user delets site from favorites with Rest API and status code is 204")
    public void consumerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteConsumer = usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer);
        restClient.authenticateUser(siteConsumer);
        favoritesAPI.addSiteToFavorites(siteConsumer, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        favoritesAPI.deleteSiteFromFavorites(siteConsumer, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
        favoritesAPI.getFavorites(siteConsumer).assertEntriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void userIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator);
        favoritesAPI.addSiteToFavorites(siteCollaborator, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.deleteSiteFromFavorites(siteCollaborator, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to delete favorites of admin user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void userIsNotAbleToDeleteFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        favoritesAPI.addSiteToFavorites(adminUserModel, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.deleteSiteFromFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify admin user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void adminIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator);
        favoritesAPI.addSiteToFavorites(siteCollaborator, siteModel).and().assertField("targetGuid").is(siteModel.getGuid());
        restClient.authenticateUser(adminUserModel);
        favoritesAPI.deleteSiteFromFavorites(siteCollaborator, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")
    @Bug(id = "MNT-16904")
    public void userIsNotAbleToDeleteFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager);
        favoritesAPI.deleteSiteFromFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
