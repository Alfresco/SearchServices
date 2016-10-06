package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.FiveStarRatingBody;
import org.alfresco.rest.body.LikeRatingBody;
import org.alfresco.rest.requests.RestRatingsApi;
import org.alfresco.utility.constants.Rating;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "ratings", "sanity" })
public class GetRatingSanityTests extends RestTest
{
    @Autowired
    RestRatingsApi ratingsApi;

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
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        
        ratingsApi.useRestClient(restClient);
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, 
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
        
        likeRating = new LikeRatingBody(Rating.likes, true);
        fiveStarRating = new FiveStarRatingBody(Rating.fiveStar, 5);
    }
    
    @BeforeMethod()
    public void setUp() throws DataPreparationException, Exception {
        folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to retrieve rating of a document")
    public void managerIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));

        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);
        
        ratingsApi.getLikeRating(document)
            .assertLikeRatingExists()
            .assertMyLikeRatingIs(Boolean.TRUE);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
        
        ratingsApi.getFiveStarRating(document)
            .assertFiveStarRatingExists()
            .assertMyFiveStarRatingIs(5);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }   
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role is able to retrieve rating of a document")
    public void collaboratorIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);
        
        ratingsApi.getLikeRating(document)
            .assertLikeRatingExists()
            .assertMyLikeRatingIs(Boolean.TRUE);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
        
        ratingsApi.getFiveStarRating(document)
            .assertFiveStarRatingExists()
            .assertMyFiveStarRatingIs(5);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }   
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role is able to retrieve rating of a document")
    public void contributorIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);
        
        ratingsApi.getLikeRating(document)
            .assertLikeRatingExists()
            .assertMyLikeRatingIs(Boolean.TRUE);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
        
        ratingsApi.getFiveStarRating(document)
            .assertFiveStarRatingExists()
            .assertMyFiveStarRatingIs(5);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }   
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role is able to retrieve rating of a document")
    public void consumerIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));

        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);
        
        ratingsApi.getLikeRating(document)
            .assertLikeRatingExists()
            .assertMyLikeRatingIs(Boolean.TRUE);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
        
        ratingsApi.getFiveStarRating(document)
            .assertFiveStarRatingExists()
            .assertMyFiveStarRatingIs(5);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }   
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to retrieve rating of a document")
    public void adminIsAbleToRetrieveRating() throws Exception
    {
        document = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser);
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);

        ratingsApi.getLikeRating(document)
            .assertLikeRatingExists()
            .assertMyLikeRatingIs(Boolean.TRUE);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
        
        ratingsApi.getFiveStarRating(document)
            .assertFiveStarRatingExists()
            .assertMyFiveStarRatingIs(5);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }   
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify unauthenticated user is not able to retrieve rating of a document")
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserIsNotAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(adminUser);
        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);

        restClient.authenticateUser(new UserModel("random user", "random password"));
        
        ratingsApi.getLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
        
        ratingsApi.getFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }   
}