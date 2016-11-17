package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestPersonFavoritesModelsCollection;
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
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
public class GetFavoritesSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel firstSiteModel;
    private SiteModel secondSiteModel;
    private FileModel firstFileModel;
    private FileModel secondFileModel;
    private FolderModel firstFolderModel;
    private FolderModel secondFolderModel;
    private ListUserWithRoles usersWithRoles;
    private RestPersonFavoritesModelsCollection restPersonFavoritesModelsCollection;

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
  
        firstSiteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(firstSiteModel).getSite().getGuidWithoutVersion());
        secondSiteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(secondSiteModel).getSite().getGuidWithoutVersion());

        usersWithRoles = dataUser.addUsersWithRolesToSite(firstSiteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites sites with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesSites() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(secondSiteModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .and().entriesListContains("targetGuid", secondSiteModel.getGuid());    
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites folders with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFolders() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingUser(adminUserModel).addFolderToFavorites(firstFolderModel);
        restClient.withCoreAPI().usingUser(adminUserModel).addFolderToFavorites(secondFolderModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingAuthUser().where().targetFolderExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.assertThat().entriesListContains("targetGuid", firstFolderModel.getNodeRef())
                  .and().entriesListContains("targetGuid", secondFolderModel.getNodeRef());    
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites files with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFiles() throws JsonToModelConversionException, Exception
    {        
        restClient.withCoreAPI().usingUser(adminUserModel).addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingUser(adminUserModel).addFileToFavorites(secondFileModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingAuthUser().where().targetFileExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.assertThat().entriesListContains("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListContains("targetGuid", secondFileModel.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets favorites with Rest API and status code is 200")
    public void managerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .assertThat().entriesListContains("targetGuid", secondSiteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user gets favorites with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .and().entriesListContains("targetGuid", secondSiteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Contributor user gets favorites with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid()).and()
                  .entriesListContains("targetGuid", secondSiteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Consumer user gets favorites with Rest API and status code is 200")
    public void consumerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        
        restPersonFavoritesModelsCollection = restClient.withCoreAPI().usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restPersonFavoritesModelsCollection.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .assertThat().entriesListContains("targetGuid", secondSiteModel.getGuid());    
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to get favorites of another user with Rest API and status code is 404")
    public void userIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .withCoreAPI().usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).where()
                  .targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to get favorites of admin user with Rest API and status code is 200")
    public void userIsNotAbleToRetrieveFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                  .usingUser(adminUserModel).where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, adminUserModel.getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify admin user doesn't have permission to get favorites of another user with Rest API and status code is 200")
    public void adminIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")
    @Bug(id="MNT-16904")
    public void userIsNotAbleToRetrieveFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager).withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
