package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
public class DeleteRatingSanityTests extends RestTest
{

    private SiteModel siteModel;
    private UserModel adminUser;
    private FolderModel folderModel;
    private FileModel document;
    private ListUserWithRoles usersWithRoles;
    private RestRatingModelsCollection returnedRatingModelCollection;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, 
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);                
    }
    
    @BeforeMethod()
    public void setUp() throws DataPreparationException, Exception {
        folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to remove its own rating of a document")
    public void managerIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.usingResource(document).likeDocument();
        restClient.usingResource(document).rateStarsToDocument(5);
        
        restClient.usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        returnedRatingModelCollection = restClient.usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating()
            .and().entriesListIsNotEmpty()
            .and().paginationExist();        
    }   
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role is able to remove its own rating of a document")
    public void collaboratorIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        restClient.usingResource(document).likeDocument();
        restClient.usingResource(document).rateStarsToDocument(5);
        
        restClient.usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        returnedRatingModelCollection = restClient.usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating()
            .and().entriesListIsNotEmpty()
            .and().paginationExist();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role is able to remove its own rating of a document")
    public void contributorIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        restClient.usingResource(document).likeDocument();
        restClient.usingResource(document).rateStarsToDocument(5);
        
        restClient.usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        returnedRatingModelCollection = restClient.usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating()
            .and().entriesListIsNotEmpty()
            .and().paginationExist();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role is able to remove its own rating of a document")
    public void consumerIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));

        restClient.usingResource(document).likeDocument();
        restClient.usingResource(document).rateStarsToDocument(5);
        
        restClient.usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        returnedRatingModelCollection = restClient.usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating()
            .and().entriesListIsNotEmpty()
            .and().paginationExist();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to remove its own rating of a document")
    public void adminIsAbleToDeleteItsOwnRatings() throws Exception
    {
    	document = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser);

        restClient.usingResource(document).likeDocument();
        restClient.usingResource(document).rateStarsToDocument(5);
        
        restClient.usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        returnedRatingModelCollection = restClient.usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModelCollection.assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating()
            .and().entriesListIsNotEmpty()
            .and().paginationExist();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify unauthenticated user is not able to remove its own rating of a document")
    public void unauthenticatedUserIsNotAbleToDeleteRatings() throws Exception
    {
    	document = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUser);
        
        restClient.usingResource(document).likeDocument();
        restClient.usingResource(document).rateStarsToDocument(5);
        
        restClient.authenticateUser(new UserModel("random user", "random password"));
        
        restClient.usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
        
        restClient.usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify one user is not able to remove rating added by another user")
    @Bug(id = "ACE-5459")
    public void oneUserIsNotAbleToDeleteRatingsOfAnotherUser() throws Exception
    {
        UserModel userA = dataUser.createRandomTestUser();
        UserModel userB = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(userA);
        
        restClient.usingResource(document).likeDocument();
        restClient.usingResource(document).rateStarsToDocument(5);
        
        restClient.authenticateUser(userB);
        
        restClient.authenticateUser(userB).usingResource(document).deleteLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
        
        restClient.usingResource(document).deleteFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }  
}