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
        favoritesAPI.addUserFavorites(adminUserModel, siteModel);
        favoritesAPI.getUserFavorite(adminUserModel, siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite folder with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFolder() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addUserFavorites(adminUserModel, folderModel);
        favoritesAPI.getUserFavorite(adminUserModel, folderModel.getNodeRef());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite file with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFile() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addUserFavorites(adminUserModel, fileModel);
        favoritesAPI.getUserFavorite(adminUserModel, fileModel.getNodeRef());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets favorite site with Rest API and status code is 200")
    public void managerIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.getUserFavorite(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user gets favorite site with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.getUserFavorite(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user gets favorite site with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel);
        favoritesAPI.getUserFavorite(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

}
