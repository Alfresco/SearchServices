package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.ErrorModel;
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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
public class GetRatingCoreTests extends RestTest {

	private SiteModel siteModel;
	private UserModel adminUserModel, userModel;
	private FileModel document;
	private RestRatingModel returnedRatingModel;
	private FolderModel firstFolderModel;

	@BeforeClass(alwaysRun = true)
	public void dataPreparation() throws DataPreparationException {
		adminUserModel = dataUser.getAdminUser();
		userModel = dataUser.createRandomTestUser();
		siteModel = dataSite.usingAdmin().createPublicRandomSite();
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that rating for invalid ratingId provided status code is 400.")
	public void checkInvalidRatingIdStatusCode() throws Exception {

		document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);

		restClient.authenticateUser(adminUserModel).withCoreAPI();
		RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "nodes/{nodeId}/ratings/{ratingId}",
				document.getNodeRef(), "invalid ratingId");
		restClient.processModel(RestRatingModel.class, request);
		restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
				.containsSummary("Invalid ratingSchemeId invalid ratingId");
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that rating for nodeId does not exist status code 404 is returned.")
	public void getRatingUsingInvalidNodeId() throws Exception {

		document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
		document.setNodeRef(RandomStringUtils.randomAlphanumeric(20));
		returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document)
				.getFiveStarRating();

		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
				.containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, document.getNodeRef()));
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has only likes.")
	public void getRatingOfFileThatHasOnlyLikes() throws Exception {

		document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
		restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).likeDocument();

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		returnedRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 1,
				"Node should have 1 like ratings");

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 0,
				"Node should have no five star ratings");
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has only stars.")
	public void getRatingOfFileThatHasOnlyStars() throws Exception {

		document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
		restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).rateStarsToDocument(5);

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and()
				.field("aggregate").isNotEmpty();
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 1,
				"Node should have 1 five star ratings");

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 0,
				"Node should have no like ratings");

	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has likes and stars.")
	public void getRatingOfFileThatHasLikesAndStars() throws Exception {

		document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
		restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).likeDocument();
		restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).rateStarsToDocument(5);

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		returnedRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 1,
				"Node should have 1 like ratings");

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar").and()
				.field("aggregate").isNotEmpty();
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 1,
				"Node should have 1 five stars ratings");
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a folder that has only likes.")
	public void getRatingOfFolderThatHasOnlyLikes() throws Exception {

		firstFolderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
		restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(firstFolderModel).likeDocument();

		returnedRatingModel = restClient.withCoreAPI().usingResource(firstFolderModel).getLikeRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		returnedRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 1,
				"Node should have 1 like rating");

	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Get rating of a file that has no ratings.")
	public void getRatingOfFileThatHasNoRatings() throws Exception {

		document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
		restClient.authenticateUser(adminUserModel).withCoreAPI();

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 0,
				"Node should have no likes ratings");

		returnedRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(returnedRatingModel.getAggregate().getNumberOfRatings(), 0,
				"Node should have no five star ratings");
	}

}
