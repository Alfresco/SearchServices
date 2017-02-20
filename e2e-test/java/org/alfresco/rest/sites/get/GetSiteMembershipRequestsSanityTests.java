package org.alfresco.rest.sites.get;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for getSiteMembershipRequests (get /people/{personId}/site-membership-requests) RestAPI call
 * 
 * @author Cristina Axinte
 */

public class GetSiteMembershipRequestsSanityTests extends RestTest
{
    UserModel siteManager, newMember;
    DataUser.ListUserWithRoles usersWithRoles;
    SiteModel moderatedSite;
    RestSiteMembershipRequestModelsCollection siteMembershipRequests;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        siteManager = dataUser.createRandomTestUser();
        moderatedSite = dataSite.usingUser(siteManager).createModeratedRandomSite();
        newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        usersWithRoles = dataUser.addUsersWithRolesToSite(moderatedSite, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @Bug(id = "MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify manager user gets all site membership requests of a specific person with Rest API and response is successful (200)")
    public void managerUserGetsSiteMembershipRequestsWithSuccess() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI()
                .usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @Bug(id = "MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify collaborator user fails to get all site membership requests of another user with Rest API (403)")
    public void collaboratorUserFailsToGetSiteMembershipRequestsOfAnotherUser() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                .usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @Bug(id = "MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify contributor user fails to get all site membership requests of another user with Rest API (403)")
    public void contributorUserFailsToGetSiteMembershipRequestsOfAnotherUser() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI()
                .usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @Bug(id = "MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify consumer user fails to get all site membership requests of another user with Rest API (403)")
    public void consumerUserFailsToGetSiteMembershipRequestsOfAnotherUser() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @Bug(id = "MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify admin user gets all site membership requests of a specific person with Rest API and response is successful (200)")
    public void adminUserGetsSiteMembershipRequestsWithSuccess() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @Bug(id = "MNT-16904", description = "It fails only on environment with tenants")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify manager user fails to get all site membership requests of a specific person with Rest API when the authentication fails (401)")
    public void unauthorizedUserFailsToGetSiteMembershipRequests() throws Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password")).withCoreAPI()
                .usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                .assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify a user gets all its own site membership requests with Rest API and response is successful (200)")
    public void oneUserGetsItsOwnSiteMembershipRequestsWithSuccess() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(newMember).withCoreAPI().usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListContains("id", moderatedSite.getId())
                .assertThat().entriesListContains("message", "Please accept me")
                .assertThat().entriesListCountIs(1);
        siteMembershipRequests.getEntries().get(0).onModel().getSite()
                .assertThat().field("visibility").is(moderatedSite.getVisibility())
                .assertThat().field("guid").is(moderatedSite.getGuid())
                .assertThat().field("description").is(moderatedSite.getDescription())
                .assertThat().field("id").is(moderatedSite.getId())
                .assertThat().field("title").is(moderatedSite.getTitle());
    }
}
