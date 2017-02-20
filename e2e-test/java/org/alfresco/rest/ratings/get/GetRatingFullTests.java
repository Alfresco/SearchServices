package org.alfresco.rest.ratings.get;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetRatingFullTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUserModel, userModel;
    private RestRatingModel returnedRatingModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingAdmin().createPublicRandomSite();
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check default error schema")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void checkDefaultErrorSchema() throws Exception
    {
        FileModel document = dataContent.usingSite(siteModel).usingAdmin().createContent(DocumentType.TEXT_PLAIN);
        String randomNodeRef = RandomStringUtils.randomAlphanumeric(10);
        document.setNodeRef(randomNodeRef);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, randomNodeRef))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get rating of a folder that has only stars")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getRatingOfFolderThatHasOnlyStars() throws Exception
    {
        FolderModel folder = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(folder).rateStarsToDocument(3);
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("id").is("fiveStar")
            .getAggregate().assertThat().field("numberOfRatings").is("1")
            .assertThat().field("average").is("3.0");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get rating of a folder that has both likes and stars")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getRatingOfFolderThatHasLikedAndStars() throws Exception
    {
        FolderModel folder = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(folder).rateStarsToDocument(3);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).likeDocument();
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("id").is("fiveStar")
            .getAggregate().assertThat().field("numberOfRatings").is("1")
            .assertThat().field("average").is("3.0");
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("id").is("likes")
            .assertThat().field("myRating").is("true");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get rating of a folder that has no ratings")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getRatingOfFolderThatHasNoRatings() throws Exception
    {
        FolderModel folder = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getFiveStarRating();
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("0");
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getLikeRating();
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get rating of a folder after rating was deleted")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getDeletedRatingOfAFolder() throws Exception
    {
        FolderModel folder = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(folder).rateStarsToDocument(3);
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(folder).deleteFiveStarRating();
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).likeDocument();
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).deleteLikeRating();
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getFiveStarRating();
        returnedRatingModel.assertThat().field("id").is("fiveStar").getAggregate().assertThat().field("numberOfRatings").is("0")
            .assertThat().field("average").isNull();
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getLikeRating();
        returnedRatingModel.assertThat().field("id").is("likes").assertThat().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get rating of a file after rating was deleted")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getDeletedRatingOfAFile() throws Exception
    {
        FileModel file = dataContent.usingUser(adminUserModel).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).rateStarsToDocument(3);
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).deleteFiveStarRating();
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).likeDocument();
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).deleteLikeRating();
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getFiveStarRating();
        returnedRatingModel.assertThat().field("id").is("fiveStar").getAggregate().assertThat().field("numberOfRatings").is("0")
            .assertThat().field("average").isNull();
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getLikeRating();
        returnedRatingModel.assertThat().field("id").is("likes").assertThat().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get rating of another user as admin")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getRatingOfAnotherUserAsAdmin() throws Exception
    {
        FileModel file = dataContent.usingUser(adminUserModel).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).rateStarsToDocument(3);
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).likeDocument();
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getFiveStarRating();
        returnedRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("1").assertThat().field("average").is("3.0");
        
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getLikeRating();
        returnedRatingModel.assertThat().field("id").is("likes").assertThat().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("1");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get rating of admin with another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getRatingOfAdminWithAnotherUser() throws Exception
    {
        FileModel file = dataContent.usingUser(userModel).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).rateStarsToDocument(3);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).likeDocument();
        
        returnedRatingModel = restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).getFiveStarRating();
        returnedRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("1").assertThat().field("average").is("3.0");
        
        returnedRatingModel = restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).getLikeRating();
        returnedRatingModel.assertThat().field("id").is("likes").assertThat().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("1");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get five star rating")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getFiveStarRating() throws Exception
    {
        FileModel file = dataContent.usingUser(userModel).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).rateStarsToDocument(5);
        
        returnedRatingModel = restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).getFiveStarRating();
        returnedRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("1").assertThat().field("average").is("5.0");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get one star rating")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getOneStarRating() throws Exception
    {
        FileModel file = dataContent.usingUser(userModel).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).rateStarsToDocument(1);
        
        returnedRatingModel = restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).getFiveStarRating();
        returnedRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").isNull()
            .getAggregate().assertThat().field("numberOfRatings").is("1").assertThat().field("average").is("1.0");
    }
}