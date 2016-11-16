package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
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

  
        firstSiteModel.setGuid(restClient.authenticateUser(adminUserModel).onCoreAPI().usingSite(firstSiteModel).getSite().getGuidWithoutVersion());
        secondSiteModel.setGuid(restClient.authenticateUser(adminUserModel).onCoreAPI().usingSite(secondSiteModel).getSite().getGuidWithoutVersion());

        usersWithRoles = dataUser.addUsersWithRolesToSite(firstSiteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites sites with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesSites() throws JsonToModelConversionException, Exception
    {
        restClient.onCoreAPI().usingUser(adminUserModel).addSiteToFavorites(firstSiteModel);
        restClient.onCoreAPI().usingUser(adminUserModel).addSiteToFavorites(secondSiteModel);
        restClient.onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites()
                  .assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .and().entriesListContains("targetGuid", secondSiteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites folders with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFolders() throws JsonToModelConversionException, Exception
    {
        restClient.onCoreAPI().usingUser(adminUserModel).addFolderToFavorites(firstFolderModel);
        restClient.onCoreAPI().usingUser(adminUserModel).addFolderToFavorites(secondFolderModel);
        restClient.onCoreAPI()
                  .usingAuthUser().where().targetFolderExist().getFavorites()
                  .assertThat().entriesListContains("targetGuid", firstFolderModel.getNodeRef())
                  .and().entriesListContains("targetGuid", secondFolderModel.getNodeRef());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets favorites files with Rest API and status code is 200")
    public void adminIsAbleToRetrieveFavoritesFiles() throws JsonToModelConversionException, Exception
    {        
        restClient.onCoreAPI().usingUser(adminUserModel).addFileToFavorites(firstFileModel);
        restClient.onCoreAPI().usingUser(adminUserModel).addFileToFavorites(secondFileModel);
        restClient.onCoreAPI()
                .usingAuthUser().where().targetFileExist().getFavorites()
                .assertThat().entriesListContains("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListContains("targetGuid", secondFileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets favorites with Rest API and status code is 200")
    public void managerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.onCoreAPI()
                  .usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.onCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        restClient.onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites()
                  .assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .assertThat().entriesListContains("targetGuid", secondSiteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user gets favorites with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .onCoreAPI()
                  .usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.onCoreAPI()
                  .usingAuthUser().addSiteToFavorites(secondSiteModel);
        restClient.onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites()
                  .assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .and().entriesListContains("targetGuid", secondSiteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Contributor user gets favorites with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                  .onCoreAPI()
                  .usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.onCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        restClient.onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites()
                  .assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid()).and()
                  .entriesListContains("targetGuid", secondSiteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Consumer user gets favorites with Rest API and status code is 200")
    public void consumerIsAbleToRetrieveFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .onCoreAPI()
                  .usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.onCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        restClient.onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites()
                  .assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                  .assertThat().entriesListContains("targetGuid", secondSiteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to get favorites of another user with Rest API and status code is 404")
    public void userIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to get favorites of admin user with Rest API and status code is 200")
    public void userIsNotAbleToRetrieveFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, adminUserModel.getUsername()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify admin user doesn't have permission to get favorites of another user with Rest API and status code is 200")
    public void adminIsNotAbleToRetrieveFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
                  .onCoreAPI()
                  .usingAuthUser().where().targetSiteExist().getFavorites();
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
        restClient.authenticateUser(siteManager)
                  .onCoreAPI()
                  .usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.onCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
