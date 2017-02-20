package org.alfresco.rest.sites.membershipRequests;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteEntry;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 1/9/2017.
 */
public class AddSiteMembershipRequestFullTests extends RestTest
{
    private SiteModel publicSite, privateSite, moderatedSite;
    private DataUser.ListUserWithRoles usersWithRoles;
    private UserModel adminUser, newMember, regularUser;
    private RestSiteMembershipRequestModel siteMembershipRequest;
    private RestSiteMembershipRequestModelsCollection siteMembershipRequests;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        publicSite = dataSite.usingUser(adminUser).createPublicRandomSite();
        privateSite = dataSite.usingUser(adminUser).createPrivateRandomSite();
        moderatedSite = dataSite.usingUser(adminUser).createModeratedRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(publicSite, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        newMember = dataUser.createRandomTestUser();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site manager is not able to create new site membership request for other user")
    public void managerIsNotAbleToCreateSiteMembershipRequestForOtherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()))
                .statusCodeIs(HttpStatus.NOT_FOUND)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site collaborator is not able to create new site membership request for other user")
    public void collaboratorIsNotAbleToCreateSiteMembershipRequestForOtherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site contributor is not able to create new site membership request for other user")
    public void contributorIsNotAbleToCreateSiteMembershipRequestForOtherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site consumer is not able to create new site membership request for other user")
    public void consumerIsNotAbleToCreateSiteMembershipRequestForOtherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin user is not able to create new site membership request for other user")
    public void adminIsNotAbleToCreateSiteMembershipRequestForOtherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingUser(newMember).addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));

        restClient.withCoreAPI().usingUser(newMember).addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));

        restClient.withCoreAPI().usingUser(newMember).addSiteMembershipRequest(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));
    }

    @Bug(id="ACE-2413")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify addSiteMembershipRequest Rest API status code is 400 for an invalid user id")
    public void addSiteMembershipRequestReturns400ForEmptyUserId() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI()
                .usingUser(new UserModel("", "password")).addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify regular user is able to create new public site membership request for himself. Check that user joins immediately as consumer.")
    public void userIsAbleToRequestMembershipOfPublicSite() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        siteMembershipRequest = restClient.authenticateUser(regularUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(publicSite.getId())
                .assertThat().field("site").isNotEmpty();
        RestSiteEntry siteEntry = restClient.withCoreAPI().usingAuthUser().getSiteMembership(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteEntry.assertThat().field("role").is(UserRole.SiteConsumer)
                 .and().field("id").is(publicSite.getId());
        siteMembershipRequests = restClient.withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListIsEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify regular user is able to create new moderated site membership request for himself. Check that the request is added to the site membership request list.")
    public void userIsAbleToRequestMembershipOfModeratedSite() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        siteMembershipRequest = restClient.authenticateUser(regularUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(moderatedSite.getId())
                .assertThat().field("message").is("Please accept me")
                .assertThat().field("site").isNotEmpty();
        siteMembershipRequest.getSite()
                .assertThat().field("visibility").is(moderatedSite.getVisibility())
                .assertThat().field("guid").is(moderatedSite.getGuid())
                .assertThat().field("description").is(moderatedSite.getDescription())
                .assertThat().field("id").is(moderatedSite.getId())
                .assertThat().field("title").is(moderatedSite.getTitle());

        siteMembershipRequests = restClient.withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListContains("id", moderatedSite.getId())
                .assertThat().entriesListCountIs(1);

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify regular user is not able to request membership of a private site.")
    public void userIsNotAbleToRequestMembershipOfPrivateSite() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(regularUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, regularUser.getUsername(), privateSite.getId()));
        siteMembershipRequests = restClient.withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListIsEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify regular user is not able to create new site membership request for other user")
    public void regularUserIsNotAbleToCreateSiteMembershipRequestForOtherUser() throws JsonToModelConversionException, Exception
    {
        regularUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(regularUser).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));

        restClient.withCoreAPI().usingUser(newMember).addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));

        restClient.withCoreAPI().usingUser(newMember).addSiteMembershipRequest(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newMember.getUsername()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create public site membership request returns status code 400 when the request is made twice")
    public void userRequestsTwiceMembershipOfPublicSite() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        siteMembershipRequest = restClient.authenticateUser(regularUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(publicSite.getId())
                .assertThat().field("site").isNotEmpty();
        restClient.withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.ALREADY_Site_MEMBER, regularUser.getUsername(), publicSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create moderated site membership request returns status code 400 when the request is made twice")
    public void userRequestsTwiceMembershipOfModeratedSite() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        siteMembershipRequest = restClient.authenticateUser(regularUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(moderatedSite.getId())
                .assertThat().field("site").isNotEmpty();
        restClient.withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.assertLastError().containsSummary(String.format(RestErrorModel.ALREADY_INVITED, regularUser.getUsername(), moderatedSite.getId()));

        siteMembershipRequests = restClient.withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListContains("id", moderatedSite.getId())
                .assertThat().entriesListCountIs(1);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create private site membership request returns status code 404 when request is made with the user who created the site")
    public void siteCreatorRequestsMembershipOfHisPrivateSite() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUser.getUsername(), privateSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create moderated site membership request returns status code 400 when request is made with the user who created the site")
    public void siteCreatorRequestsMembershipOfHisModeratedSite() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.ALREADY_Site_MEMBER, adminUser.getUsername(), moderatedSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin is able to create new moderated site membership request for himself. Check that the request is added to the site membership request list.")
    public void adminIsAbleToRequestMembershipOfModeratedSite() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        SiteModel anotherModeratedSite = dataSite.usingUser(regularUser).createModeratedRandomSite();
        siteMembershipRequest = restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(anotherModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(anotherModeratedSite.getId())
                .assertThat().field("site").isNotEmpty();
        siteMembershipRequests = restClient.withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListContains("id", anotherModeratedSite.getId());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create site membership request returns status code 404 when personId is not member of the domain.")
    public void addSiteMembershipRequestWhenPersonIdIsNotInTheDomain() throws Exception
    {
        UserModel adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminTenantUser.getUsername().toLowerCase(), publicSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create site membership request returns status code 200 with tenant.")
    public void addSiteMembershipRequestWithTenant() throws Exception
    {
        UserModel adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);
        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");

        SiteModel tenantPublicSite = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        siteMembershipRequest = restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(tenantPublicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(tenantPublicSite.getId())
                .assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify site membership request is automatically rejected when a site is switched from moderated to private")
    public void siteMembershipRequestIsRejectedWhenSiteIsSwitchedFromModeratedToPrivate() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        SiteModel moderatedThenPrivateSite = dataSite.usingUser(adminUser).createModeratedRandomSite();
        siteMembershipRequest = restClient.authenticateUser(regularUser).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedThenPrivateSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(moderatedThenPrivateSite.getId())
                .assertThat().field("site").isNotEmpty();

        dataSite.usingUser(adminUser).updateSiteVisibility(moderatedThenPrivateSite, Site.Visibility.PRIVATE);

        siteMembershipRequests = restClient.withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListIsEmpty();
    }
}
