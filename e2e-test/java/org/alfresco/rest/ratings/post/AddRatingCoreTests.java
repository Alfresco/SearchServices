package org.alfresco.rest.ratings.post;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.LinkModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddRatingCoreTests extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private UserModel adminUser;    
    private ListUserWithRoles usersWithRoles;
    private RestRatingModel returnedRatingModel; // placeholder for returned model

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that if unknown rating scheme is provided status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void unknownRatingSchemeReturnsBadRequest() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"invalidRate\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.INVALID_RATING, "invalidRate"))
                  .containsErrorKey(String.format(RestErrorModel.INVALID_RATING, "invalidRate"))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that if nodeId does not exist status code 404 is returned")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void invalidNodeIdReturnsNotFound() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        document.setNodeRef(RandomStringUtils.randomAlphanumeric(10));
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, document.getNodeRef()))
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that if nodeId provided cannot be rated 405 status code is returned")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void likeResourceThatCannotBeRated() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
        document.setNodeRef(link.getNodeRef().replace("workspace://SpacesStore/", "workspace%3A%2F%2FSpacesStore%2F"));
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError()
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, document.getNodeRef()))
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that manager is able to like a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void managerIsAbleToLikeAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document)
                .likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that manager is able to like a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void managerIsAbleToLikeAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(folderModel)
                .likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that manager is able to like a site")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void managerIsAbleToLikeASite() throws Exception
    {
        FolderModel folderModel = new FolderModel();
        folderModel.setNodeRef(siteModel.getGuid());

        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(folderModel)
                .likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();

        restClient.withCoreAPI().usingResource(folderModel).getRatings()
                .assertNodeIsLiked();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that manager is able to rate a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void managerIsAbleToRateAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(folderModel)
                .rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that manager is able to rate a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void managerIsAbleToRateAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document)
                .rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that manager is able to rate a site")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void managerIsAbleToRateASite() throws Exception
    {
        FolderModel folderModel = new FolderModel();
        folderModel.setNodeRef(siteModel.getGuid());
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(folderModel)
                .rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();

        restClient.withCoreAPI().usingResource(folderModel).getRatings()
                .assertNodeHasFiveStarRating();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that adding like again has no effect on a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void fileCanBeLikedTwice() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that adding rate again has no effect on a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void fileCanBeRatedTwice() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that rate is not added if empty rating object is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void addRateUsingEmptyRatingObject() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.NO_CONTENT, "No content to map to Object due to end of input"));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that if empty rate id is provided status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void addRateUsingEmptyValueForId() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.NO_CONTENT, "N/A (through reference chain: org.alfresco.rest.api.model.NodeRating[\"id\"])"));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that if empty rating is provided status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void addRateUsingEmptyValueForMyRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"likes\", \"myRating\":\"\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NULL_LIKE_RATING));

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"fiveStar\", \"myRating\":\"\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NULL_FIVESTAR_RATING));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that user is not able to rate a comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void addRatingToAComment() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        RestCommentModel comment = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addComment("This is a comment");
        document.setNodeRef(comment.getId());

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.CANNOT_RATE))
                  .containsErrorKey(RestErrorModel.CANNOT_RATE)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(String.format(RestErrorModel.CANNOT_RATE));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Verify that user is not able to rate a tag")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void addRatingToATag() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        RestTagModel tag = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addTag("randomTag");
        document.setNodeRef(tag.getId());

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(String.format(RestErrorModel.CANNOT_RATE));

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(String.format(RestErrorModel.CANNOT_RATE));
    }

}