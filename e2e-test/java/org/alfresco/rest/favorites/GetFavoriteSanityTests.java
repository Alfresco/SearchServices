package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
import org.alfresco.rest.model.RestPersonFavoritesModelsCollection;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
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
        
        RestPersonFavoritesModel favoriteSite = restClient.withCoreAPI().usingUser(adminUserModel).getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteSite.assertThat().field("targetGuid").equals(siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets favorite folder with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsAbleToRetrieveFavoritesFolder() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingUser(adminUserModel).addFolderToFavorites(folderModel);
        
        RestPersonFavoritesModel favoriteFolder = restClient.withCoreAPI().usingUser(adminUserModel).getFavorite(folderModel.getNodeRef());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFolder.assertThat().field("targetGuid").equals(folderModel.getNodeRef());        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets favorite file with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsAbleToRetrieveFavoritesFile() throws JsonToModelConversionException, Exception
    {        
        restClient.withCoreAPI().usingUser(adminUserModel).addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.withCoreAPI().usingUser(adminUserModel).getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFile.assertThat().field("targetGuid").equals(fileModel.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Manager user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void managerIsAbleToRetrieveFavoriteSite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favSite = restClient.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favSite.assertThat().field("targetGuid").equals(siteModel.getGuid());

    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Manager user gets favorite folder with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void managerIsAbleToRetrieveFavoriteFolder() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(folderModel);
        
        RestPersonFavoritesModel favoriteFolder = restClient.withCoreAPI().usingAuthUser().getFavorite(folderModel.getNodeRef());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFolder.assertThat().field("targetGuid").equals(folderModel.getNodeRef());

    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Manager user gets favorite file with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void managerIsAbleToRetrieveFavoriteFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.withCoreAPI().usingAuthUser().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFile.assertThat().field("targetGuid").equals(fileModel.getNodeRef());
    }


    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Collaborator user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void collaboratorIsAbleToRetrieveFavoriteSite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favSite = restClient.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favSite.assertThat().field("targetGuid").equals(siteModel.getGuid());    
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify collaborator user gets favorite folder with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void collaboratorIsAbleToRetrieveFavoriteFolder() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(folderModel);
        
        RestPersonFavoritesModel favoriteFolder = restClient.withCoreAPI().usingAuthUser().getFavorite(folderModel.getNodeRef());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFolder.assertThat().field("targetGuid").equals(folderModel.getNodeRef());

    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify collaborator user gets favorite file with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void collaboratorIsAbleToRetrieveFavoriteFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.withCoreAPI().usingAuthUser().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFile.assertThat().field("targetGuid").equals(fileModel.getNodeRef());
    }

    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Contributor user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void contributorIsAbleToRetrieveFavoriteSite() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favSite = restClient.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favSite.assertThat().field("targetGuid").equals(siteModel.getGuid());   ;
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify contributor user gets favorite folder with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void contributorIsAbleToRetrieveFavoriteFolder() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(folderModel);
        
        RestPersonFavoritesModel favoriteFolder = restClient.withCoreAPI().usingAuthUser().getFavorite(folderModel.getNodeRef());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFolder.assertThat().field("targetGuid").equals(folderModel.getNodeRef());

    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify contributor user gets favorite file with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void contributorIsAbleToRetrieveFavoriteFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.withCoreAPI().usingAuthUser().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFile.assertThat().field("targetGuid").equals(fileModel.getNodeRef());
    }

    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Consumer user gets favorite site with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void consumerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favSite = restClient.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favSite.assertThat().field("targetGuid").equals(siteModel.getGuid());   
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify consumer user gets favorite folder with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void consumerIsAbleToRetrieveFavoriteFolder() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(folderModel);
        
        RestPersonFavoritesModel favoriteFolder = restClient.withCoreAPI().usingAuthUser().getFavorite(folderModel.getNodeRef());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFolder.assertThat().field("targetGuid").equals(folderModel.getNodeRef());

    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify consumer user gets favorite file with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void consumerIsAbleToRetrieveFavoriteFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.withCoreAPI().usingAuthUser().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFile.assertThat().field("targetGuid").equals(fileModel.getNodeRef());
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
    @Bug(id = "MNT-16904")
    public void userIsNotAbleToRetrieveFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager).withCoreAPI()
                  .usingUser(siteManager).getFavorite(siteModel.getGuid());        
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastExceptionContains(HttpStatus.UNAUTHORIZED.toString());
    }
    
}
