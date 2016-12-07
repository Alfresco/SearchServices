package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
public class AddSiteMembershipRequestCoreTests extends RestTest
{
    private UserModel adminUser;
    private UserModel newMember;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createModeratedRandomSite();
    }

    @BeforeMethod
    public void setUp() throws DataPreparationException
    {
        newMember = dataUser.createRandomTestUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify addSiteMembershipRequest Rest API status code is 400 for a user that has already been invited")
    public void addSiteMembershipRequestStatusCodeIs400ReceivedForAUserThatIsAlreadyInvited() throws Exception
    {
        dataUser.addUserToSite(newMember, siteModel, UserRole.SiteContributor);
        restClient.authenticateUser(newMember)
                .withCoreAPI()
                .usingMe()
                .addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.ALREADY_Site_MEMBER, newMember.getUsername(), siteModel.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify addSiteMembershipRequest Rest API status code is 404 for a user that does not exist")
    public void addSiteMembershipRequestStatusCodeIs404ReceivedForAUserThatDoesNotExist() throws Exception
    {
        restClient.authenticateUser(newMember)
                .withCoreAPI()
                .usingUser(new UserModel("invalidUser", "password")).addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidUser"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify addSiteMembershipRequest Rest API status code is 404 for a site that does not exist")
    public void addSiteMembershipRequestStatusCodeIs404ReceivedForASiteThatDoesNotExist() throws Exception
    {
        restClient.authenticateUser(newMember)
                .withCoreAPI()
                .usingMe().addSiteMembershipRequest(new SiteModel("invalidSiteID"));
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, newMember.getUsername(), "invalidSiteID"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify addSiteMembershipRequest Rest API status code is 400 for empty request body")
    public void addSiteMembershipRequestStatusCodeIs400ForEmptyRequestBody() throws Exception
    {
        restClient.authenticateUser(newMember)
                .withCoreAPI()
                .usingMe().addSiteMembershipRequest("");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify addSiteMembershipRequest Rest API status code is 201 for request with empty message")
    public void addSiteMembershipRequestStatusCodeIs201ForRequestWithEmptyMessage() throws Exception
    {
        restClient.authenticateUser(newMember)
                .withCoreAPI()
                .usingMe().addSiteMembershipRequest("", siteModel, "New request");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify addSiteMembershipRequest Rest API status code is 201 for request with empty title")
    public void addSiteMembershipRequestStatusCodeIs201ForRequestWithEmptyTitle() throws Exception
    {
        restClient.authenticateUser(newMember)
                .withCoreAPI()
                .usingMe().addSiteMembershipRequest("Please accept me", siteModel, "");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }
}
