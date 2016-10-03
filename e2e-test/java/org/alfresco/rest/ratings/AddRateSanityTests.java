package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.FiveStarRatingBody;
import org.alfresco.rest.body.LikeRatingBody;
import org.alfresco.rest.body.LikeRatingBody.ratingTypes;
import org.alfresco.rest.requests.RestRatingsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "ratings", "sanity" })
public class AddRateSanityTests extends RestTest
{
    @Autowired
    RestRatingsApi ratingsApi;

    private UserModel userModel;
    private SiteModel siteModel;
    private UserModel adminUser;
    private FolderModel folderModel;
    private FileModel document;
    private LikeRatingBody likeRating;
    private FiveStarRatingBody fiveStarRating;
    private ListUserWithRoles usersWithRoles;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        ratingsApi.useRestClient(restClient);
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @BeforeMethod
    public void setUp() throws Exception {
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to post like rating to a document")
    public void managerIsAbleToLikeDocument() throws Exception
    {
        likeRating = new LikeRatingBody(ratingTypes.fiveStar.toString(), true);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role is able to post like rating to a document")
    public void collaboratorIsAbleToLikeDocument() throws Exception
    {
        likeRating = new LikeRatingBody(ratingTypes.likes.toString(), true);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role is able to post like rating to a document")
    public void contributorIsAbleToLikeDocument() throws Exception
    {
        likeRating = new LikeRatingBody(ratingTypes.likes.toString(), true);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role is able to post like rating to a document")
    public void consumerIsAbleToLikeDocument() throws Exception
    {
        likeRating = new LikeRatingBody(ratingTypes.likes.toString(), true);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to post like rating to a document")
    public void adminIsAbleToLikeDocument() throws Exception
    {
        likeRating = new LikeRatingBody(ratingTypes.likes.toString(), true);
        
        restClient.authenticateUser(adminUser);
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify unauthenticated user is not able to post like rating to a document")
    public void unauthenticatedUserIsNotAbleToLikeDocument() throws Exception
    {
        likeRating = new LikeRatingBody(ratingTypes.likes.toString(), true);
        
        restClient.authenticateUser(new UserModel("random user", "random password"));
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to post stars rating to a document")
    public void managerIsAbleToAddStarsToDocument() throws Exception
    {
        fiveStarRating = new FiveStarRatingBody(ratingTypes.fiveStar.toString(), 5);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        ratingsApi.rateStarsToDocument(document, fiveStarRating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }
}