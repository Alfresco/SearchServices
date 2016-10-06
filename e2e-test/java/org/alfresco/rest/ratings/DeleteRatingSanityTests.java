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
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "ratings", "sanity" })
public class DeleteRatingSanityTests extends RestTest
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
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to remove its own rating of a document")
    public void managerIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));

        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);
        
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
    
    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role is able to remove its own rating of a document")
    public void collaboratorIsAbleToDeleteItsOwnRatings() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        ratingsApi.likeDocument(document, likeRating);
        ratingsApi.rateStarsToDocument(document, fiveStarRating);
        
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
}