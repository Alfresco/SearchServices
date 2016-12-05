package org.alfresco.rest.ratings;

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
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
public class AddRatingCoreTests extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private UserModel adminUser;
    private FolderModel folderModel;
    private FileModel document;
    private ListUserWithRoles usersWithRoles;
    private RestRatingModel returnedRatingModel; //placeholder for returned model

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @BeforeMethod
    public void setUp() throws Exception
    {
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if unknown rating scheme is provided status code is 400")
    public void unknownRatingSchemeReturnsBadRequest() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"invalidRate\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_RATING, "invalidRate"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if nodeId does not exist status code 404 is returned")
    public void invalidNodeIdReturnsNotFound() throws Exception
    {
        document.setNodeRef(RandomStringUtils.randomAlphanumeric(10));
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, document.getNodeRef()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if nodeId provided cannot be rated 405 status code is returned")
    @Bug(id = "MNT-16904")
    public void likeResourceThatCannotBeRated() throws Exception
    {
        LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
        document.setNodeRef(link.getNodeRef());
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to like a file")
    public void managerIsAbleToLikeAFile() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
            .and().field("id").is("likes")
            .and().field("aggregate").isNotEmpty();       
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to like a folder")
    public void managerIsAbleToLikeAFolder() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(folderModel).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
            .and().field("id").is("likes")
            .and().field("aggregate").isNotEmpty();       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to rate a folder")
    public void managerIsAbleToRateAFolder() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(folderModel).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("5")
            .and().field("id").is("fiveStar")
            .and().field("aggregate").isNotEmpty();       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to rate a file")
    public void managerIsAbleToRateAFile() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("5")
            .and().field("id").is("fiveStar")
            .and().field("aggregate").isNotEmpty();       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that adding like again has no effect on a file")
    public void fileCanBeLikedTwice() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
            .and().field("id").is("likes")
            .and().field("aggregate").isNotEmpty();       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that adding rate again has no effect on a file")
    public void fileCanBeRatedTwice() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("5")
            .and().field("id").is("fiveStar")
            .and().field("aggregate").isNotEmpty();       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that rate is not added if empty rating object is provided")
    public void addRateUsingEmptyRatingObject() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NO_CONTENT,
                "No content to map to Object due to end of input"));       
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if empty rate id is provided status code is 400")
    public void addRateUsingEmptyValueForId() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NO_CONTENT,
                "N/A (through reference chain: org.alfresco.rest.api.model.NodeRating[\"id\"])")); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if empty rating is provided status code is 400")
    public void addRateUsingEmptyValueForMyRating() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"likes\", \"myRating\":\"\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NULL_LIKE_RATING));
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addInvalidRating("{\"id\":\"fiveStar\", \"myRating\":\"\"}");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NULL_FIVESTAR_RATING));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that user is not able to rate a comment")
    public void addRatingToAComment() throws Exception
    {
        RestCommentModel comment = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addComment("This is a comment");
        document.setNodeRef(comment.getId());
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(String.format(RestErrorModel.CANNOT_RATE));
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(String.format(RestErrorModel.CANNOT_RATE));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that user is not able to rate a tag")
    public void addRatingToATag() throws Exception
    {
        RestTagModel tag = restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).addTag("randomTag");
        document.setNodeRef(tag.getId());
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(String.format(RestErrorModel.CANNOT_RATE));
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(String.format(RestErrorModel.CANNOT_RATE));
    }
    
}