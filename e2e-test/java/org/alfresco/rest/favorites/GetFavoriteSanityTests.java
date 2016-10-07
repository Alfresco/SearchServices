package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestFavoritesApi;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetFavoriteSanityTests extends RestTest
{

    @Autowired
    RestFavoritesApi favoritesAPI;

    @Autowired
    RestSitesApi sitesApi;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private FolderModel folderModel;
    private FileModel fileModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();

        folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        fileModel = dataContent.usingUser(adminUserModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        favoritesAPI.useRestClient(restClient);
        sitesApi.useRestClient(restClient);
        siteModel.setGuid(sitesApi.getSite(siteModel).getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite site with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesSite() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addSiteToFavorites(adminUserModel, siteModel);
        favoritesAPI.getSiteFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite folder with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFolder() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addFolderToFavorites(adminUserModel, folderModel);
        favoritesAPI.getFolderFavorites(adminUserModel, folderModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite file with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFile() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addFileToFavorites(adminUserModel, fileModel);
        favoritesAPI.getFileFavorites(adminUserModel, fileModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets favorite site with Rest API and status code is 200")
    public void managerIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user gets favorite site with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user gets favorite site with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user gets favorite site with Rest API and status code is 200")
    public void consumerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user doesn't get favorite site of another user with Rest API and status code is 404")
    public void userIsNotAbleToRetrieveFavoriteSiteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user doesn't get favorite site of admin user with Rest API and status code is 404")
    public void userIsNotAbleToRetrieveFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.getSiteFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify admin user doesn't get favorite site of another user with Rest API and status code is 404")
    public void adminIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")
    public void userIsNotAbleToRetrieveFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

}
