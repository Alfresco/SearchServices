package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModelsCollection;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.StatusModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetFavoriteSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private FolderModel folderModel;
    private FileModel fileModel;
    private ListUserWithRoles usersWithRoles;
    private RestSiteModelsCollection restSiteModelsCollection;
    private RestPersonFavoritesModelsCollection restPersonFavoritesModelsCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();

        folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        fileModel = dataContent.usingUser(adminUserModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        siteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).getSite().getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsAbleToRetrieveFavoritesSite() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
        
        restSiteModelsCollection = restClient.withCoreAPI().usingUser(adminUserModel).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.and().entriesListContains("guid",siteModel.getGuid());    
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets favorite folder with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsAbleToRetrieveFavoritesFolder() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingUser(adminUserModel).addFolderToFavorites(folderModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingUser(adminUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.and().entriesListContains("targetGuid", folderModel.getNodeRef());
        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets favorite file with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsAbleToRetrieveFavoritesFile() throws JsonToModelConversionException, Exception
    {        
        restClient.withCoreAPI().usingUser(adminUserModel).addFileToFavorites(fileModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingUser(adminUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.and().entriesListContains("targetGuid", fileModel.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Manager user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void managerIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restSiteModelsCollection = restClient.withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.and().entriesListContains("guid",siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Collaborator user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void collaboratorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restSiteModelsCollection = restClient.withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.and().entriesListContains("guid",siteModel.getGuid());      
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Contributor user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void contributorIsAbleToRetrieveFavorite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restSiteModelsCollection = restClient.withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.and().entriesListContains("guid",siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Consumer user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void consumerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restSiteModelsCollection = restClient.withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.and().entriesListContains("guid",siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify user doesn't get favorite site of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void userIsNotAbleToRetrieveFavoriteSiteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .getFavorite(siteModel.getGuid());

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify user doesn't get favorite site of admin user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void userIsNotAbleToRetrieveFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .withCoreAPI().usingUser(adminUserModel).getFavorite(siteModel.getGuid());
        
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, adminUserModel.getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify admin user doesn't get favorite site of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI()
                  .usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).getFavorite(siteModel.getGuid());
        
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify user gets status code 401 if authentication call fails")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void userIsNotAbleToRetrieveFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager).withCoreAPI()
                  .usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).getFavorite(siteModel.getGuid());        
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastStatus().hasName(StatusModel.UNAUTHORIZED);
    }
}
