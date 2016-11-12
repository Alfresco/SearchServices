package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestFavoritesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
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
public class GetFavoriteSanityTests extends RestTest
{
    @Autowired
    RestFavoritesApi favoritesAPI;


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
        
        siteModel.setGuid(restClient.authenticateUser(adminUserModel).usingSite(siteModel).getSite().getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite site with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesSite() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addSiteToFavorites(adminUserModel, siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.getSiteFavorites(adminUserModel, siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite folder with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFolder() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addFolderToFavorites(adminUserModel, folderModel).and().field("targetGuid").is(folderModel.getNodeRef());
        favoritesAPI.getFolderFavorites(adminUserModel, folderModel).and().field("targetGuid").is(folderModel.getNodeRef());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorite file with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFile() throws JsonToModelConversionException, Exception
    {
        favoritesAPI.addFileToFavorites(adminUserModel, fileModel).and().field("targetGuid").is(fileModel.getNodeRef().replace(";1.0", ""));
        favoritesAPI.getFileFavorites(adminUserModel, fileModel).and().field("targetGuid").is(fileModel.getNodeRef().replace(";1.0", ""));
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets favorite site with Rest API and status code is 200")
    public void managerIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user gets favorite site with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Contributor user gets favorite site with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Consumer user gets favorite site with Rest API and status code is 200")
    public void consumerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.addSiteToFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel).and().field("targetGuid").is(siteModel.getGuid());
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't get favorite site of another user with Rest API and status code is 404")
    public void userIsNotAbleToRetrieveFavoriteSiteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't get favorite site of admin user with Rest API and status code is 404")
    public void userIsNotAbleToRetrieveFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        favoritesAPI.getSiteFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, adminUserModel.getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify admin user doesn't get favorite site of another user with Rest API and status code is 404")
    public void adminIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")
    @Bug(id="MNT-16904")
    public void userIsNotAbleToRetrieveFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager);
        favoritesAPI.getSiteFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

}
