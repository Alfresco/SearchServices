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
import org.alfresco.utility.model.StatusModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups="DEV")
public class AddRatingSanityTests extends RestTest
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
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role is able to post like rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void managerIsAbleToLikeDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document)
                .likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role is able to post like rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void collaboratorIsAbleToLikeDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(document)
                .likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role is able to post like rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void contributorIsAbleToLikeDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingResource(document)
                .likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role is able to post like rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void consumerIsAbleToLikeDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingResource(document)
                .likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify admin user is able to post like rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void adminIsAbleToLikeDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to post like rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })    
    public void unauthenticatedUserIsNotAbleToLikeDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(new UserModel("random user", "random password")).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastExceptionContains(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role is able to post stars rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void managerIsAbleToAddStarsToDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document)
                .rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role is able to post stars rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void collaboratorIsAbleToAddStarsToDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(document)
                .rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();

    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role is able to post stars rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void contributorIsAbleToAddStarsToDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingResource(document)
                .rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role is able to post stars rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void consumerIsAbleToAddStarsToDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingResource(document)
                .rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify admin user is able to post stars rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void adminIsAbleToAddStarsToDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(3);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("3").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to post stars rating to a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })    
    public void unauthenticatedUserIsNotAbleToRateStarsToDocument() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(new UserModel("random user", "random password")).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastStatus().hasName(StatusModel.UNAUTHORIZED);
    }
}