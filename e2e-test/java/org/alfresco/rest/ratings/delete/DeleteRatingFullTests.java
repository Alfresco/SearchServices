package org.alfresco.rest.ratings.delete;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.rest.model.RestRatingModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
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

/**
 * Created by Claudia Agache on 1/25/2017.
 */
public class DeleteRatingFullTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUser;
    private DataUser.ListUserWithRoles usersWithRoles;
    private RestRatingModelsCollection returnedRatingModelCollection;
    private RestRatingModel returnedRatingModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify site contributor is not able to remove rating added by another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    @Bug(id = "ACE-5459")
    public void contributorIsNotAbleToDeleteRatingsOfAnotherUser() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.withCoreAPI().usingResource(document).likeDocument();

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);

        restClient.withCoreAPI().usingResource(document).getRatings()
                .assertNodeIsLiked()
                .assertNodeHasFiveStarRating();
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify site collaborator is not able to remove rating added by another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    @Bug(id = "ACE-5459")
    public void collaboratorIsNotAbleToDeleteRatingsOfAnotherUser() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.withCoreAPI().usingResource(document).likeDocument();

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);

        restClient.withCoreAPI().usingResource(document).getRatings()
                .assertNodeIsLiked()
                .assertNodeHasFiveStarRating();
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify site consumer is not able to remove rating added by another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    @Bug(id = "ACE-5459")
    public void consumerIsNotAbleToDeleteRatingsOfAnotherUser() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.withCoreAPI().usingResource(document).likeDocument();

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);

        restClient.withCoreAPI().usingResource(document).getRatings()
                .assertNodeIsLiked()
                .assertNodeHasFiveStarRating();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete rating stars twice")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    @Bug(id = "MNT-17181")
    public void deleteStarsTwice() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                  .withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeHasNoFiveStarRating();

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete like rating twice")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    @Bug(id = "MNT-17181")
    public void deleteLikeTwice() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingResource(document).likeDocument();

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked();

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete like for a folder then add it again")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void likeFolderAfterLikeRatingIsDeleted() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(folderModel).likeDocument();
        restClient.withCoreAPI().usingResource(folderModel).deleteLikeRating();
        returnedRatingModel = restClient.withCoreAPI().usingResource(folderModel).likeDocument();

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete stars for a folder then add them again")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addStarsToFolderAfterRatingIsDeleted() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(folderModel).rateStarsToDocument(5);
        restClient.withCoreAPI().usingResource(folderModel).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModel = restClient.withCoreAPI().usingResource(folderModel).rateStarsToDocument(5);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete like for a site then add it again")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void likeSiteAfterLikeRatingIsDeleted() throws Exception
    {
        FolderModel folderModel = new FolderModel();
        folderModel.setNodeRef(siteModel.getGuid());

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(folderModel).likeDocument();
        restClient.withCoreAPI().usingResource(folderModel).deleteLikeRating();
        returnedRatingModel = restClient.withCoreAPI().usingResource(folderModel).likeDocument();

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete stars for a site then add them again")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addStarsToSiteAfterRatingIsDeleted() throws Exception
    {
        FolderModel folderModel = new FolderModel();
        folderModel.setNodeRef(siteModel.getGuid());

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(folderModel).rateStarsToDocument(5);
        restClient.withCoreAPI().usingResource(folderModel).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModel = restClient.withCoreAPI().usingResource(folderModel).rateStarsToDocument(5);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify user with Manager role is able to remove one star rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void removeOneStarRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(1);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked().assertNodeHasNoFiveStarRating().and().entriesListIsNotEmpty().and().paginationExist();
    }

}
