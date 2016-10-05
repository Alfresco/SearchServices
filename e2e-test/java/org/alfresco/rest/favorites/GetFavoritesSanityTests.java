package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestFavoritesApi;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.SpecificParametersForFavorites;
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

@Test(groups = { "rest-api", "favorites", "sanity" })
public class GetFavoritesSanityTests extends RestTest
{

    @Autowired
    RestFavoritesApi favoritesAPI;

    @Autowired
    RestSitesApi sitesApi;

    private UserModel adminUserModel;
    private SiteModel firstSiteModel;
    private SiteModel secondSiteModel;
    private FileModel firstFileModel;
    private FileModel secondFileModel;
    private FolderModel firstFolderModel;
    private FolderModel secondFolderModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        firstSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        secondSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        firstFolderModel = dataContent.usingUser(adminUserModel).usingSite(firstSiteModel).createFolder();
        secondFolderModel = dataContent.usingUser(adminUserModel).usingSite(firstSiteModel).createFolder();
        firstFileModel = dataContent.usingUser(adminUserModel).usingResource(firstFolderModel).createContent(DocumentType.TEXT_PLAIN);
        secondFileModel = dataContent.usingUser(adminUserModel).usingResource(firstFolderModel).createContent(DocumentType.TEXT_PLAIN);

        favoritesAPI.useRestClient(restClient);
        sitesApi.useRestClient(restClient);
        firstSiteModel.setGuid(sitesApi.getSite(firstSiteModel).getGuid());
        secondSiteModel.setGuid(sitesApi.getSite(secondSiteModel).getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(firstSiteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites sites with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesSites() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addUserFavorites(adminUserModel, firstSiteModel);
        favoritesAPI.addUserFavorites(adminUserModel, secondSiteModel);
        favoritesAPI.getUserFavorites(adminUserModel, SpecificParametersForFavorites.SITE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites folders with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFolders() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addUserFavorites(adminUserModel, firstFolderModel);
        favoritesAPI.addUserFavorites(adminUserModel, secondFolderModel);
        favoritesAPI.getUserFavorites(adminUserModel, SpecificParametersForFavorites.FOLDER.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites files with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFiles() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addUserFavorites(adminUserModel, firstFileModel);
        favoritesAPI.addUserFavorites(adminUserModel, secondFileModel);
        favoritesAPI.getUserFavorites(adminUserModel, SpecificParametersForFavorites.FILE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets favorites with Rest API and status code is 200")
    public void managerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), firstSiteModel);
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), secondSiteModel);
        favoritesAPI.getUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), SpecificParametersForFavorites.SITE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user gets favorites with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), firstSiteModel);
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), secondSiteModel);
        favoritesAPI.getUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), SpecificParametersForFavorites.SITE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user gets favorites with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), firstSiteModel);
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), secondSiteModel);
        favoritesAPI.getUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), SpecificParametersForFavorites.SITE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user gets favorites with Rest API and status code is 200")
    public void consumerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), firstSiteModel);
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), secondSiteModel);
        favoritesAPI.getUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), SpecificParametersForFavorites.SITE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to get favorites of another user with Rest API and status code is 404")
    public void userIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.getUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), SpecificParametersForFavorites.SITE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { "rest-api",
            "favorites" }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to get favorites of admin user with Rest API and status code is 200")
    public void userIsNotAbleToRetrieveFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.getUserFavorites(adminUserModel, SpecificParametersForFavorites.SITE.toString());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
