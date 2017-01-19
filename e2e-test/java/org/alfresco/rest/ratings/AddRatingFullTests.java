package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestCommentModel;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddRatingFullTests extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private UserModel adminUser;
    private ListUserWithRoles usersWithRoles;
    private RestRatingModel returnedRatingModel; 
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Contributor is able to like a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void contributorIsAbleToLikeAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI()
                                        .usingResource(document)
                                        .likeDocument();
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Collaborator is able to like a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void collaboratorIsAbleToLikeAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                                        .usingResource(document)
                                        .likeDocument();
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Consumer is able to like a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void consumerIsAbleToLikeAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                                        .usingResource(document)
                                        .likeDocument();
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Contributor is able to like a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void contributorIsAbleToLikeAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();

        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI()
                                        .usingResource(folderModel)
                                        .likeDocument();
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Collaborator is able to like a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void collaboratorIsAbleToLikeAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                                        .usingResource(folderModel)
                                        .likeDocument();
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Consumer is able to like a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void consumerIsAbleToLikeAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                                        .usingResource(folderModel)
                                        .likeDocument();
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Contributor is able to rate a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void contributorIsAbleToRateAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI()
                                        .usingResource(document)
                                        .rateStarsToDocument(5);
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Collaborator is able to rate a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void collaboratorIsAbleToRateAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                                        .usingResource(document)
                                        .rateStarsToDocument(5);
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Consumer is able to rate a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void consumerIsAbleToRateAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                                        .usingResource(document)
                                        .rateStarsToDocument(5);
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Contributor is able to rate a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void contributorIsAbleToRateAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI()
                             .usingResource(folderModel)
                             .rateStarsToDocument(5);
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION,
              description = "Verify that Collaborator is able to rate a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void collaboratorIsAbleToRateAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                                        .usingResource(folderModel)
                                        .rateStarsToDocument(5);
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Consumer is able to rate a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void consumerIsAbleToRateAFolder() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                                        .usingResource(folderModel)
                                        .rateStarsToDocument(5);
        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that adding like again has no effect on a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void folderCanBeLikedTwice() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(folderModel).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(folderModel).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that adding rate again has no effect on a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void folderCanBeRatedTwice() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(folderModel).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(folderModel).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("5")
                          .and().field("id").is("fiveStar")
                          .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that if invalid rate id is provided status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addRateUsingInvalidValueForId() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"like\"}");
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.INVALID_RATING, "like"))
                  .containsErrorKey(String.format(RestErrorModel.INVALID_RATING, "like"))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that rate id is case sensitive is provided status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addRateUsingInvalidValueForIdCaseSensitive() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"Likes\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.INVALID_RATING, "Likes"))
                  .containsErrorKey(String.format(RestErrorModel.INVALID_RATING, "Likes"))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                 .stackTraceIs(RestErrorModel.STACKTRACE);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"FiveStar\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.INVALID_RATING, "FiveStar"))
                  .containsErrorKey(String.format(RestErrorModel.INVALID_RATING, "FiveStar"))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                 .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that if invalid rating is provided status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addRateUsingInvalidValueForMyRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"likes\", \"myRating\":\"skiped\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NULL_LIKE_RATING));

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"fiveStar\", \"myRating\":\"string\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NULL_FIVESTAR_RATING));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
             description = "Like file created by a different user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addLikeToAnotherUserFile() throws Exception
    {
        UserModel user = dataUser.usingAdmin().createRandomTestUser();
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        returnedRatingModel = restClient.authenticateUser(user).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
             description = "Rate a file created by a different user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addRatingToAnotherUserFile() throws Exception
    {
        UserModel user = dataUser.usingAdmin().createRandomTestUser();
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        returnedRatingModel = restClient.authenticateUser(user).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Add Rate Using Boolean Value For 'myRating'")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addRateUsingBooleanValueForMyRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"fiveStar\", \"myRating\":\"true\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(RestErrorModel.NULL_FIVESTAR_RATING)
                  .containsErrorKey(RestErrorModel.NULL_FIVESTAR_RATING)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Add Like Using Integer Value For 'myRating'")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void addLikeUsingIntegerValueForMyRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"likes\", \"myRating\":\"2\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(RestErrorModel.NULL_LIKE_RATING)
                  .containsErrorKey(RestErrorModel.NULL_LIKE_RATING)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that user is not able to like his own comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void userCannotLikeHisOwnComment() throws Exception
    {
        UserModel user = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        FolderModel folderModel = dataContent.usingUser(user).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(user).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        RestCommentModel comment = restClient.authenticateUser(user).withCoreAPI().usingResource(document).addComment("This is a comment");
        document.setNodeRef(comment.getId());
    
        returnedRatingModel = restClient.authenticateUser(user).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.CANNOT_RATE))
                  .containsErrorKey(RestErrorModel.CANNOT_RATE)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER).stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that user is not able to rate his own comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void userCannotRateHisOwnComment() throws Exception
    {
        UserModel user = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        FolderModel folderModel = dataContent.usingUser(user).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(user).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        RestCommentModel comment = restClient.authenticateUser(user).withCoreAPI().usingResource(document).addComment("This is a comment");
        document.setNodeRef(comment.getId());

        returnedRatingModel = restClient.authenticateUser(user).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.CANNOT_RATE))
                  .containsErrorKey(RestErrorModel.CANNOT_RATE)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Collaborator is NOT able to add a negative rating to a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void collaboratorIsNotAbleToAddANegativeRatingToAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                                        .usingResource(document)
                                        .rateStarsToDocument(-5);        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(RestErrorModel.RATING_OUT_OF_BOUNDS)
                  .containsErrorKey(RestErrorModel.RATING_OUT_OF_BOUNDS)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that Collaborator is NOT able to add a high rating to a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void collaboratorIsNotAbleToAddAHighRatingToAFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                                        .usingResource(document)
                                        .rateStarsToDocument(10);        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(RestErrorModel.RATING_OUT_OF_BOUNDS)
                  .containsErrorKey(RestErrorModel.RATING_OUT_OF_BOUNDS)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Bug(id = "MNT-17375")
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Do not provide field - 'id'")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void notProvideIdLikeFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"myRating\":\"true\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(RestErrorModel.NULL_LIKE_RATING)
                  .containsErrorKey(RestErrorModel.NULL_LIKE_RATING)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
              description = "Do not provide field - 'myRating'")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void notProvideMyRatingRateFile() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"likes\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(RestErrorModel.NULL_LIKE_RATING)
                  .containsErrorKey(RestErrorModel.NULL_LIKE_RATING)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
