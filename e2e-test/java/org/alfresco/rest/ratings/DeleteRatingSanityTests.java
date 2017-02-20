package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModelsCollection;
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

public class DeleteRatingSanityTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUser;
    private ListUserWithRoles usersWithRoles;
    private RestRatingModelsCollection returnedRatingModelCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

 
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role is able to remove its own rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void managerIsAbleToDeleteItsOwnRatings() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked().assertNodeHasNoFiveStarRating().and().entriesListIsNotEmpty().and().paginationExist();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role is able to remove its own rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void collaboratorIsAbleToDeleteItsOwnRatings() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked().assertNodeHasNoFiveStarRating().and().entriesListIsNotEmpty().and().paginationExist();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role is able to remove its own rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void contributorIsAbleToDeleteItsOwnRatings() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked().assertNodeHasNoFiveStarRating().and().entriesListIsNotEmpty().and().paginationExist();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role is able to remove its own rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void consumerIsAbleToDeleteItsOwnRatings() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked().assertNodeHasNoFiveStarRating().and().entriesListIsNotEmpty().and().paginationExist();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify admin user is able to remove its own rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void adminIsAbleToDeleteItsOwnRatings() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        document = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).usingResource(folderModel)
                .createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser);

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        returnedRatingModelCollection = restClient.withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked().assertNodeHasNoFiveStarRating().and().entriesListIsNotEmpty().and().paginationExist();
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to remove its own rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    @Bug(id="MNT-16904", description = "It fails only on environment with tenants")
    public void unauthenticatedUserIsNotAbleToDeleteRatings() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        document = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).usingResource(folderModel)
                .createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUser);

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.authenticateUser(new UserModel("random user", "random password"));

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify one user is not able to remove rating added by another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    @Bug(id = "ACE-5459")
    public void oneUserIsNotAbleToDeleteRatingsOfAnotherUser() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);        
        UserModel userA = dataUser.createRandomTestUser();
        UserModel userB = dataUser.createRandomTestUser();

        restClient.authenticateUser(userA);

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.authenticateUser(userB);

        restClient.withCoreAPI().usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);

        restClient.withCoreAPI().usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        restClient.withCoreAPI().usingResource(document).getRatings()
                .assertNodeIsLiked()
                .assertNodeHasFiveStarRating();
    }
}