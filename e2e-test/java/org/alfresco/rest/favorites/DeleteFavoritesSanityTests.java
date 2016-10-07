package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestFavoritesApi;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "favorites", "sanity" })
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

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user delets site from favorites with Rest API and status code is 204")
    public void adminIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addSiteToFavorites(adminUserModel, siteModel);
        favoritesAPI.deleteSiteFromFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Manager user delets site from favorites with Rest API and status code is 204")
    public void managerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.deleteSiteFromFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user delets site from favorites with Rest API and status code is 204")
    public void collaboratorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.deleteSiteFromFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user delets site from favorites with Rest API and status code is 204")
    public void contributorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel);
        favoritesAPI.deleteSiteFromFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user delets site from favorites with Rest API and status code is 204")
    public void consumerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel);
        favoritesAPI.deleteSiteFromFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void userIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.deleteSiteFromFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to delete favorites of admin user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void userIsNotAbleToDeleteFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        favoritesAPI.addSiteToFavorites(adminUserModel, siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.deleteSiteFromFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify admin user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void adminIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        restClient.authenticateUser(adminUserModel);
        favoritesAPI.deleteSiteFromFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")
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
