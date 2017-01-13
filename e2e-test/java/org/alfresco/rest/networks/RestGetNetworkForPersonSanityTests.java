package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
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
 * Tests for /people/{personId}/networks
 */
public class RestGetNetworkForPersonSanityTests extends RestTest
{
    UserModel adminUserModel;
    UserModel adminTenantUser;
    UserModel tenantUser;
    UserModel tenantSiteManager;
    UserModel tenantSiteCollaborator;
    UserModel tenantSiteContributor;
    UserModel tenantSiteConsumer;
    SiteModel tenantSite;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        restClient.usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantSiteManager = dataUser.usingUser(adminTenantUser).createUserWithTenant("tenantSiteManager");
        tenantSiteCollaborator = dataUser.usingUser(adminTenantUser).createUserWithTenant("tenantSiteCollaborator");
        tenantSiteContributor = dataUser.usingUser(adminTenantUser).createUserWithTenant("tenantSiteContributor");
        tenantSiteConsumer = dataUser.usingUser(adminTenantUser).createUserWithTenant("tenantSiteConsumer");
        tenantSite = dataSite.usingUser(tenantSiteManager).createPublicRandomSite();
        dataUser.usingUser(tenantSiteManager).addUserToSite(tenantSiteCollaborator, tenantSite, UserRole.SiteCollaborator);
        dataUser.usingUser(tenantSiteManager).addUserToSite(tenantSiteContributor, tenantSite, UserRole.SiteContributor);
        dataUser.usingUser(tenantSiteManager).addUserToSite(tenantSiteConsumer, tenantSite, UserRole.SiteConsumer);
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify non existing user gets another existing network with Rest API and checks the forbidden status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(adminTenantUser).getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Bug(id = "needs to be checked")
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant user check network of admin user with Rest API")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void tenantUserIsNotAuthorizedToCheckNetworkOfAdminUser() throws Exception
    { 
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify admin tenant user is not authorized to check network of another user with Rest API and checks the forbidden status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void adminTenantUserIsNotAuthorizedToCheckNetworkOfAnotherUser() throws Exception
    {
        UserModel secondAdminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(secondAdminTenantUser);
        UserModel secondTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("anotherTenant");
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().getNetwork(secondTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify site manager user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void siteManagerChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantSiteManager).withCoreAPI().usingAuthUser().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify site collaborator user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void siteCollaboratorChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantSiteCollaborator).withCoreAPI().usingAuthUser().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify site contributor user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void siteContributorChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantSiteContributor).withCoreAPI().usingAuthUser().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify site consumer user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void siteConsumerChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantSiteConsumer).withCoreAPI().usingAuthUser().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
