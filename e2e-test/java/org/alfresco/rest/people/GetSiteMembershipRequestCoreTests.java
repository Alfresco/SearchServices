package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
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
 * @author Bogdan Bocancea
 */

public class GetSiteMembershipRequestCoreTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel newMember;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createModeratedRandomSite();
        newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify contributor user fails to get all site membership requests of a specific person with Rest API when the authentication fails (401)")
    @Bug(id = "MNT-16904")
    public void unauthorizedContributorUserFailsToGetSiteMembershipRequests() throws Exception
    {
        UserModel contributor = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributor, siteModel, UserRole.SiteContributor);
        contributor.setPassword("newpassword");
        restClient.authenticateUser(contributor).withCoreAPI().usingUser(newMember).getSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastExceptionContains(HttpStatus.UNAUTHORIZED.toString());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify collaborator user fails to get all site membership requests of a specific person with Rest API when the authentication fails (401)")
    @Bug(id="MNT-16904")
    public void unauthorizedCollaboratorUserFailsToGetSiteMembershipRequests() throws Exception
    {
        UserModel collaborator = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaborator, siteModel, UserRole.SiteContributor);
        collaborator.setPassword("newpassword");
        restClient.authenticateUser(collaborator).withCoreAPI().usingUser(newMember).getSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify consumer user fails to get all site membership requests of a specific person with Rest API when the authentication fails (401)")
    @Bug(id = "MNT-16904")
    public void unauthorizedConsumerUserFailsToGetSiteMembershipRequests() throws Exception
    {
        UserModel consumer = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumer, siteModel, UserRole.SiteContributor);
        consumer.setPassword("newpassword");
        restClient.authenticateUser(consumer).withCoreAPI().usingUser(newMember).getSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify a user gets all its own site membership requests using '-me-' with Rest API and response is successful (200)")
    public void usingMeGetSiteMembershipRequestsWithSuccess() throws Exception
    {
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().getSiteMembershipRequest(siteModel).assertThat().field("id").is(siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site manager can't get site membership requests for inexistent user and response is not found (404)")
    public void siteManagerCantGetSiteMembershipRequestsInexistentUser() throws Exception
    {
        restClient.authenticateUser(newMember).withCoreAPI().usingUser(UserModel.getRandomUserModel()).getSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user can get site membership requests on site with no requests and response is successful (200)")
    public void userCantGetSiteMembershipRequestsWithNoRequests() throws Exception
    {
        UserModel noRequestUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(noRequestUser).withCoreAPI().usingMe().getSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, noRequestUser.getUsername(), siteModel.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site manager can't get site membership requests on public site and response is not found (404)")
    public void siteManagerCantGetPublicSiteMembershipRequests() throws Exception
    {
        UserModel publicUser = dataUser.createRandomTestUser();
        SiteModel publicSite = dataSite.usingUser(userModel).createPublicRandomSite();
        restClient.authenticateUser(publicUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(publicUser).getSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, publicUser.getUsername(), publicSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site manager can't get site membership requests on private site and response is not found (404)")
    public void siteManagerCantGetPrivateSiteMembershipRequests() throws Exception
    {
        UserModel privateUser = dataUser.createRandomTestUser();
        SiteModel privateSite = dataSite.usingUser(userModel).createPrivateRandomSite();
        restClient.authenticateUser(privateUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(privateSite);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(privateUser).getSiteMembershipRequest(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, privateUser.getUsername(), privateSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site manager can't get site membership requests  for inexistent site and response is not found (404)")
    public void siteManagerCantGetSiteMembershipRequestsForInexistentSite() throws Exception
    {
        SiteModel inexistentSite = SiteModel.getRandomSiteModel();
        restClient.authenticateUser(userModel).withCoreAPI().usingMe().getSiteMembershipRequest(inexistentSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, userModel.getUsername(), inexistentSite.getId()));
    }
}
