package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for getSiteMembershipRequests (get
 * /people/{personId}/site-membership-requests) Core-RestAPI call
 * 
 * @author Andrei Rusu
 */

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
public class GetSiteMembershipRequestsCoreTests extends RestTest {

	UserModel userModel, newMember;
	SiteModel siteModel, siteModel2;
	RestWrapper restWrapper;

	@BeforeClass(alwaysRun = true)
	public void dataPreparation() throws Exception {

		String siteId = RandomData.getRandomName("site");
		siteModel = dataSite.usingUser(userModel)
				.createSite(new SiteModel(Visibility.MODERATED, siteId, siteId, siteId, siteId));
		newMember = dataUser.createRandomTestUser();

		restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

		restClient.assertStatusCodeIs(HttpStatus.CREATED);
	}

	@Bug(id = "MNT-16557")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify that for invalid maxItems parameter status code is 400.")
	public void checkInvalidMaxItemsStatusCode() throws Exception {

		UserModel adminUser = dataUser.getAdminUser();

		restClient.authenticateUser(adminUser).withParams("maxItems=AB").withCoreAPI().usingUser(newMember)
				.getSiteMembershipRequests().assertThat().entriesListIsNotEmpty();
		restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
				.containsSummary("Invalid paging parameter");
	}

	@Bug(id = "MNT-16557")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify that for invalid skipCount parameter status code is 400.")
	public void checkInvalidSkipCountStatusCode() throws Exception {

		UserModel adminUser = dataUser.getAdminUser();

		restClient.authenticateUser(adminUser).withParams("skipCount=AB").withCoreAPI().usingUser(newMember)
				.getSiteMembershipRequests().assertThat().entriesListIsNotEmpty();
		restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
				.containsSummary("Invalid paging parameter");
	}

	@Bug(id = "MNT-16557")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify that if personId does not exist status code is 404.")
	public void ifPersonIdDoesNotExist() throws Exception {

		UserModel adminUser = dataUser.getAdminUser();

		restClient.authenticateUser(adminUser).withCoreAPI();
		RestRequest request = RestRequest.simpleRequest(HttpMethod.GET,
				"people/{personId}/site-membership-requests?{parameters}", newMember.getUsername(),
				restClient.getParameters());
		restClient.processModels(RestSiteMembershipRequestModelsCollection.class, request).assertThat()
				.entriesListIsNotEmpty();
		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary("not found");
	}

	@Bug(id = "MNT-16557")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Specify -me- string in place of <personid> for request.")
	public void replacePersonIdWithMeRequest() throws Exception {

		UserModel adminUser = dataUser.getAdminUser();

		restClient.authenticateUser(adminUser).withCoreAPI();
		RestRequest request = RestRequest.simpleRequest(HttpMethod.GET,
				"people/{me}/site-membership-requests?{parameters}", newMember.getUsername(),
				restClient.getParameters());
		restClient.processModels(RestSiteMembershipRequestModelsCollection.class, request).assertThat()
				.entriesListIsNotEmpty();
		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary("not found");
	}

	@Bug(id = "MNT-16557")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Use empty personId.")
	public void useEmptyPersonId() throws Exception {

		UserModel adminUser = dataUser.getAdminUser();

		restClient.authenticateUser(adminUser).withCoreAPI();
		RestRequest request = RestRequest.simpleRequest(HttpMethod.GET,
				"people/{personId}/site-membership-requests?{parameters}", "", restClient.getParameters());
		restClient.processModels(RestSiteMembershipRequestModelsCollection.class, request);
		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary("not found");
	}

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Get site membership requests to a public site.")
	public void getRequestsToPublicSite() throws Exception {

		String siteId2 = RandomData.getRandomName("site");
		siteModel2 = dataSite.usingUser(userModel)
				.createSite(new SiteModel(Visibility.PUBLIC, siteId2, siteId2, siteId2, siteId2));
		newMember = dataUser.createRandomTestUser();
		restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel2);
		restClient.assertStatusCodeIs(HttpStatus.CREATED);

		UserModel adminUser = dataUser.getAdminUser();

		restClient.authenticateUser(adminUser).withCoreAPI().usingUser(newMember).getSiteMembershipRequests()
				.assertThat().entriesListIsEmpty();
		restClient.assertStatusCodeIs(HttpStatus.OK);

	}

	@Bug(id = "MNT-16557")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Get site membership requests to a moderated site.")
	public void getRequestsToModeratedSite() throws Exception {

		UserModel adminUser = dataUser.getAdminUser();

		restClient.authenticateUser(adminUser).withCoreAPI().usingUser(newMember).getSiteMembershipRequests()
				.assertThat().entriesListIsNotEmpty();
		restClient.assertStatusCodeIs(HttpStatus.OK);
	}
	
	@Bug(id = "MNT-16557")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Approve request then get requests.")
	public void approveRequestThenGetRequests() throws Exception {

		ListUserWithRoles usersWithRoles;

		UserModel adminUser = dataUser.getAdminUser();
		siteModel = dataSite.usingUser(adminUser).createModeratedRandomSite();
		usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator,
				UserRole.SiteConsumer, UserRole.SiteContributor);

		UserModel testUser = dataUser.createRandomTestUser("testUser");
		testUser.setUserRole(UserRole.SiteConsumer);
		restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
		restClient.withCoreAPI().usingSite(siteModel).addPerson(testUser).assertThat().field("id")
				.is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
		restClient.assertStatusCodeIs(HttpStatus.CREATED);

		restClient.authenticateUser(testUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

		restClient.authenticateUser(adminUser).withCoreAPI().usingUser(testUser).getSiteMembershipRequests()
				.assertThat().entriesListIsNotEmpty();
		restClient.assertStatusCodeIs(HttpStatus.OK);
	}

}
