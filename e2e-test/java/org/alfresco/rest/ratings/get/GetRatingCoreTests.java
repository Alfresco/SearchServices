package org.alfresco.rest.ratings.get;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetRatingCoreTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUserModel, userModel;
    private FileModel document;
    private RestRatingModel returnedRatingModel;
    private FolderModel firstFolderModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingAdmin().createPublicRandomSite();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws DataPreparationException, Exception
    {
        document = dataContent.usingSite(siteModel).usingAdmin().createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that using invalid ratingId for get rating call returns status code 400.")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void checkInvalidRatingIdStatusCode() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "nodes/{nodeId}/ratings/{ratingId}", document.getNodeRef(), "invalid ratingId");
        restClient.processModel(RestRatingModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_RATING, "invalid ratingId"));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that using invalid node ID for get rating call returns status code 404.")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void getRatingUsingInvalidNodeId() throws Exception
    {
        document.setNodeRef(RandomStringUtils.randomAlphanumeric(20));
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getFiveStarRating();

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, document.getNodeRef()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has only likes.")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void getRatingOfFileThatHasOnlyLikes() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).likeDocument();

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("1");

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("0");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has only stars.")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void getRatingOfFileThatHasOnlyStars() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).rateStarsToDocument(5);

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("1");

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("0");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has likes and stars.")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void getRatingOfFileThatHasLikesAndStars() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).likeDocument();
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).rateStarsToDocument(5);

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("1");

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and().field("aggregate").isNotEmpty();
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("1");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a folder that has only likes.")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void getRatingOfFolderThatHasOnlyLikes() throws Exception
    {
        firstFolderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(firstFolderModel).likeDocument();

        returnedRatingModel = restClient.withCoreAPI().usingResource(firstFolderModel).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("1");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has no ratings.")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void getRatingOfFileThatHasNoRatings() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("0");

        returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedRatingModel.getAggregate().assertThat().field("numberOfRatings").is("0");
    }

}