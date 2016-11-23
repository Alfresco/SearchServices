package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
public class GetRatingsCoreTests extends RestTest {
	private SiteModel siteModel;
	private UserModel adminUserModel, userModel;
	private FileModel document;
	private RestRatingModel returnedRatingModel;

	@BeforeClass(alwaysRun = true)
	public void dataPreparation() throws DataPreparationException {
		adminUserModel = dataUser.getAdminUser();
		userModel = dataUser.createRandomTestUser();
		siteModel = dataSite.usingAdmin().createPublicRandomSite();
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp() throws DataPreparationException, Exception {
		document = dataContent.usingSite(siteModel).usingAdmin()
				.createContent(CMISUtil.DocumentType.TEXT_PLAIN);
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that rating for invalid maxItems status code is 400")
	public void checkInvalidMaxItemsStatusCode() throws Exception {

		restClient.authenticateUser(adminUserModel).withParams("maxItems=0").withCoreAPI().usingResource(document)
				.getLikeRating();
		restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
				.containsSummary(String.format(ErrorModel.INVALID_ARGUMENT, "argument"));
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that rating for invalid skipCount status code is 400")
	public void checkInvalidSkipCountStatusCode() throws Exception {

		restClient.authenticateUser(adminUserModel).withParams("skipCount=AB").withCoreAPI().usingResource(document)
				.getLikeRating();
		restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
				.containsSummary(String.format(ErrorModel.INVALID_ARGUMENT, "argument"));
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "If nodeId does not exist status code is 404 when a document is liked")
	public void addLikeUsingInvalidNodeId() throws Exception {

		document.setNodeRef(RandomStringUtils.randomAlphanumeric(20));
		returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document)
				.likeDocument();

		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
				.containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, document.getNodeRef()));
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that rating value is TRUE for a like rating")
	public void checkRatingValueIsTrueForLikedDoc() throws Exception {

		returnedRatingModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document)
				.likeDocument();
		restClient.assertStatusCodeIs(HttpStatus.CREATED);

		returnedRatingModel.assertThat().field("myRating").is("true").and().field("id").is("likes");
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, description = "Check that rating value is an INTEGER value for stars rating")
	public void checkRatingValueIsIntegerForStarsRating() throws Exception {

		returnedRatingModel = restClient.authenticateUser(userModel).withCoreAPI().usingResource(document)
				.rateStarsToDocument(5);
		restClient.assertStatusCodeIs(HttpStatus.CREATED);

		returnedRatingModel.assertThat().field("myRating").is("5").and().field("id").is("fiveStar")
 		 .and().field("aggregate").isNotEmpty();
	}

}
