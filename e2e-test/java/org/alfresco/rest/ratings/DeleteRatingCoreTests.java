package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
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

public class DeleteRatingCoreTests extends RestTest
{

    private SiteModel siteModel;
    private UserModel adminUser;
    
    private ListUserWithRoles usersWithRoles;
    private RestRatingModel returnedRatingModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that if ratingId provided is unknown status code returned is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void deleteInvalidRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).deleteInvalidRating("random_rating");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError()
                    .containsSummary(String.format(RestErrorModel.INVALID_RATING, "random_rating"))
                    .containsErrorKey(String.format(RestErrorModel.INVALID_RATING, "random_rating"))
                    .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                    .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that if nodeId does not exist status code returned is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void deleteRatingUsingInvalidDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        document.setNodeRef("random_value");
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "random_value"));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete rating stars for a document that was not rated")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    @Bug(id = "MNT-17181")
    public void deleteStarsForANotRatedDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete like rating for a document that was not liked")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    @Bug(id = "MNT-17181")
    public void deleteLikeForANotLikedDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete like for a file then add it again")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void likeDocumentAfterLikeRatingIsDeleted() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        returnedRatingModel = restClient.withCoreAPI().usingResource(document).likeDocument();

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Delete stars for a file then add them again")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void addStarsToDocumentAfterRatingIsDeleted() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify site manager is not able to remove rating added by another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    @Bug(id = "ACE-5459")
    public void deleteDocumentRatingUsingManager() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.withCoreAPI().usingResource(document).likeDocument();

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);

        restClient.withCoreAPI().usingResource(document).getRatings()
                .assertNodeIsLiked()
                .assertNodeHasFiveStarRating();
    }
}