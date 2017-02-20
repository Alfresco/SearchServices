package org.alfresco.rest.sites.post;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddSiteMembershipRequestSanityTests extends RestTest
{
    private SiteModel publicSite, anotherPublicSite;
    private DataUser.ListUserWithRoles usersWithRoles;
    private UserModel adminUser, siteManager;
    private RestSiteMembershipRequestModel requestModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        publicSite = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(publicSite, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);

        siteManager = dataUser.createRandomTestUser();
        anotherPublicSite = dataSite.usingUser(siteManager).createPublicRandomSite();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site manager is able to create new site membership request for himself and status code is 201")
    public void managerCreatesNewSiteMembershipRequestForSelf() throws Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingAuthUser().addSiteMembershipRequest(anotherPublicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").is(anotherPublicSite.getId())
                    .assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site contributor is able to create new site membership request for himself and status code is 201")
    public void contributorCreatesNewSiteMembershipRequestForSelf() throws Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingAuthUser().addSiteMembershipRequest(anotherPublicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").is(anotherPublicSite.getId())
                    .assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site collaborator is able to create new site membership request for himself and status code is 201")
    public void collaboratorCreatesNewSiteMembershipRequestForSelf() throws Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingAuthUser().addSiteMembershipRequest(anotherPublicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").is(anotherPublicSite.getId())
                    .assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site consumer is able to create new site membership request for himself and status code is 201")
    public void consumerCreatesNewSiteMembershipRequestForSelf() throws Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingAuthUser().addSiteMembershipRequest(anotherPublicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").is(anotherPublicSite.getId())
                    .assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify admin is able to create new site membership request for himself and status code is 201")
    public void adminCreatesNewSiteMembershipRequestForSelf() throws Exception
    {
        requestModel = restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(anotherPublicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").is(anotherPublicSite.getId())
                    .assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to create new site membership request")
    public void unauthenticatedUserIsNotAbleToCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password")).withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
}