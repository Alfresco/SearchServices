package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 11/21/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
public class GetFavoritesCoreTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel firstSiteModel, secondSiteModel;
    private FileModel firstFileModel, secondFileModel;
    private FolderModel firstFolderModel, secondFolderModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private RestPersonFavoritesModelsCollection userFavorites;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        firstSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        secondSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        firstFolderModel = dataContent.usingUser(adminUserModel).usingSite(firstSiteModel).createFolder();
        secondFolderModel = dataContent.usingUser(adminUserModel).usingSite(firstSiteModel).createFolder();
        firstFileModel = dataContent.usingUser(adminUserModel).usingResource(firstFolderModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        secondFileModel = dataContent.usingUser(adminUserModel).usingResource(firstFolderModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);

        firstSiteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(firstSiteModel).getSite().getGuid());
        secondSiteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(secondSiteModel).getSite().getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(firstSiteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets only favorites sites with Rest API and status code is 200")
    public void userIsAbleToRetrieveOnlyFavoritesSites() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser().where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", secondSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", firstFolderModel.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets only favorites files with Rest API and status code is 200")
    public void userIsAbleToRetrieveOnlyFavoritesFiles() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser().where().targetFileExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", secondFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", firstSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", firstFolderModel.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets only favorites folders with Rest API and status code is 200")
    public void userIsAbleToRetrieveOnlyFavoritesFolders() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser().where().targetFolderExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstFolderModel.getNodeRef())
                .and().entriesListDoesNotContain("targetGuid", secondFolderModel.getNodeRef())
                .and().entriesListDoesNotContain("targetGuid", firstSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", firstFileModel.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets only favorites files or folders with Rest API and status code is 200")
    public void userIsAbleToRetrieveFavoritesFilesOrFolders() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser()
                .where().targetFolderExist().or().targetFileExist()
                .getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstFolderModel.getNodeRef())
                .and().entriesListContains("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", secondFolderModel.getNodeRef())
                .and().entriesListDoesNotContain("targetGuid", secondFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", firstSiteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets only favorites files or sites with Rest API and status code is 200")
    public void userIsAbleToRetrieveFavoritesFilesOrSites() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser()
                .where().targetSiteExist().or().targetFileExist()
                .getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                .and().entriesListContains("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", secondSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", secondFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", firstFolderModel.getNodeRef());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets only favorites folders or sites with Rest API and status code is 200")
    public void userIsAbleToRetrieveFavoritesFoldersOrSites() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser()
                .where().targetSiteExist().or().targetFolderExist()
                .getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                .and().entriesListContains("targetGuid", firstFolderModel.getNodeRef())
                .and().entriesListDoesNotContain("targetGuid", secondSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", secondFolderModel.getNodeRef())
                .and().entriesListDoesNotContain("targetGuid", firstFileModel.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets all favorites with Rest API and status code is 200")
    public void userIsAbleToRetrieveAllFavorites() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser()
                .where().targetSiteExist().or().targetFolderExist().or().targetFileExist()
                .getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                .and().entriesListContains("targetGuid", firstFolderModel.getNodeRef())
                .and().entriesListContains("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", secondSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", secondFolderModel.getNodeRef())
                .and().entriesListDoesNotContain("targetGuid", secondFileModel.getNodeRefWithoutVersion());

        userFavorites = restClient.withCoreAPI().usingAuthUser().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
                .and().entriesListContains("targetGuid", firstFolderModel.getNodeRef())
                .and().entriesListContains("targetGuid", firstFileModel.getNodeRefWithoutVersion())
                .and().entriesListDoesNotContain("targetGuid", secondSiteModel.getGuid())
                .and().entriesListDoesNotContain("targetGuid", secondFolderModel.getNodeRef())
                .and().entriesListDoesNotContain("targetGuid", secondFileModel.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify request using invalid where parameter returns status 400")
    public void getFavoritesUsingInvalidWhereParameter() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser()
                .where().or().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_ARGUMENT, "WHERE query"));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify request for a user with no favorites returns status 200")
    public void userHasNoFavorites() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        userFavorites = restClient.withCoreAPI().usingAuthUser().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListIsEmpty().and().paginationField("totalItems").is("0");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify User gets correct favorites after deleting a favorite")
    public void checkFavoriteFolderIsRemoved() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(firstFileModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(firstFolderModel);

        restClient.withCoreAPI().usingAuthUser().deleteFolderFromFavorites(firstFolderModel);

        userFavorites = restClient.withCoreAPI().usingAuthUser().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListDoesNotContain("targetGuid", firstFolderModel.getNodeRef())
                .and().paginationField("totalItems").is("2");
    }

    @Bug(id="REPO-1642", description = "reproduced on 5.2.1 only, it works on 5.2.0")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify request using personId that does not exist returns status 404")
    public void checkFavoritesWhenPersonIdDoesNotExist() throws Exception
    {
        UserModel someUser = new UserModel("someUser", DataUser.PASSWORD);

        restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingUser(someUser).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "someUser"));
    }
}
