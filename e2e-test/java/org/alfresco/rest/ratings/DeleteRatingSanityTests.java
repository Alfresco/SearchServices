package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestRatingsApi;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.COMMENTS })
public class DeleteRatingSanityTests extends RestTest
{
    @Autowired
    RestRatingsApi ratingsApi;

    private UserModel userModel;
    private SiteModel siteModel;
    private UserModel adminUser;
    private FolderModel folderModel;
    private FileModel document;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        
        ratingsApi.useRestClient(restClient);
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, 
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);                
    }
    
    @BeforeMethod()
    public void setUp() throws DataPreparationException, Exception {
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to remove its own rating of a document")
    public void managerIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));

        ratingsApi.likeDocument(document);
        ratingsApi.rateStarsToDocument(document, 5);
        
        ratingsApi.deleteLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.deleteFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.getRatings(document)
            .assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating();        
    }   
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role is able to remove its own rating of a document")
    public void collaboratorIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        ratingsApi.likeDocument(document);
        ratingsApi.rateStarsToDocument(document, 5);
        
        ratingsApi.deleteLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.deleteFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.getRatings(document)
            .assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role is able to remove its own rating of a document")
    public void contributorIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        ratingsApi.likeDocument(document);
        ratingsApi.rateStarsToDocument(document, 5);
        
        ratingsApi.deleteLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.deleteFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.getRatings(document)
            .assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role is able to remove its own rating of a document")
    public void consumerIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));

        ratingsApi.likeDocument(document);
        ratingsApi.rateStarsToDocument(document, 5);
        
        ratingsApi.deleteLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.deleteFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.getRatings(document)
            .assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to remove its own rating of a document")
    public void adminIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(adminUser);

        ratingsApi.likeDocument(document);
        ratingsApi.rateStarsToDocument(document, 5);
        
        ratingsApi.deleteLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.deleteFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        ratingsApi.getRatings(document)
            .assertNodeIsNotLiked()
            .assertNodeHasNoFiveStarRating();        
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify unauthenticated user is not able to remove its own rating of a document")
    public void unauthenticatedUserIsNotAbleToDeleteRatings() throws Exception
    {
        restClient.authenticateUser(adminUser);
        
        ratingsApi.likeDocument(document);
        ratingsApi.rateStarsToDocument(document, 5);
        
        restClient.authenticateUser(new UserModel("random user", "random password"));
        
        ratingsApi.deleteLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
        
        ratingsApi.deleteFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }  
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify one user is not able to remove rating added by another user")
    @Bug(id = "ACE-5459")
    public void oneUserIsNotAbleToDeleteRatingsOfAnotherUser() throws Exception
    {
        UserModel userA = dataUser.createRandomTestUser();
        UserModel userB = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(userA);
        
        ratingsApi.likeDocument(document);
        ratingsApi.rateStarsToDocument(document, 5);
        
        restClient.authenticateUser(userB);
        
        ratingsApi.deleteLikeRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
        
        ratingsApi.deleteFiveStarRating(document);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }  
}