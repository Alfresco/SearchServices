package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddSiteMembershipRequestCoreTests extends RestTest
{
    private UserModel adminUser;    
    private SiteModel moderatedSite;
    private RestSiteMembershipRequestModel requestModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        moderatedSite = dataSite.usingUser(adminUser).createModeratedRandomSite();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify addSiteMembershipRequest Rest API status code is 400 for a user that has already been invited")
    public void addSiteMembershipRequestStatusCodeIs400ReceivedForAUserThatIsAlreadyInvited() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        dataUser.addUserToSite(newMember, moderatedSite, UserRole.SiteContributor);
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.ALREADY_Site_MEMBER, newMember.getUsername(), moderatedSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify addSiteMembershipRequest Rest API status code is 404 for a user that does not exist")
    public void addSiteMembershipRequestStatusCodeIs404ReceivedForAUserThatDoesNotExist() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingUser(new UserModel("invalidUser", "password")).addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidUser"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify addSiteMembershipRequest Rest API status code is 404 for a site that does not exist")
    public void addSiteMembershipRequestStatusCodeIs404ReceivedForASiteThatDoesNotExist() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(new SiteModel("invalidSiteID"));
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, newMember.getUsername(), "invalidSiteID"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify addSiteMembershipRequest Rest API status code is 400 for empty request body")
    public void addSiteMembershipRequestStatusCodeIs400ForEmptyRequestBody() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest("");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.NO_CONTENT, "No content to map to Object due to end of input"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify addSiteMembershipRequest Rest API status code is 201 for request with empty message")
    public void addSiteMembershipRequestStatusCodeIs201ForRequestWithEmptyMessage() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        requestModel = restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest("", moderatedSite, "New request");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").is(moderatedSite.getId()).assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify addSiteMembershipRequest Rest API status code is 201 for request with empty title")
    public void addSiteMembershipRequestStatusCodeIs201ForRequestWithEmptyTitle() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        requestModel = restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest("Please accept me", moderatedSite, "");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").is(moderatedSite.getId()).assertThat().field("site").isNotEmpty();
    }
}
