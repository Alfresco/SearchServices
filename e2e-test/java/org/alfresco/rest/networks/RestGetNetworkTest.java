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
 * Created by Cristina Axinte on 9/26/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS })
public class RestGetNetworkTest extends RestTest
{
    UserModel adminTenantUser;
    UserModel adminAnotherTenantUser;
    SiteModel site;
    UserModel tenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        UserModel adminuser = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminuser);
        restClient.usingTenant().createTenant(adminTenantUser);
        adminAnotherTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminAnotherTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        site = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
    }

    @Bug(id = "MNT-16904")
    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify non existing user gets another exisiting network with Rest API and checks the forbidden status")
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser);
        restClient.usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.usingNetworks().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, description = "Verify tenant admin user gets specific network with Rest API and checks response parameters are correct")
    public void adminTenantChecksNetworkParamsAreCorrect() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.usingNetworks().getNetwork().assertNetworkHasName(adminTenantUser).assertNetworkIsEnabled();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, description = "Verify tenant admin user gets non exisiting network with Rest API and checks the not found status")
    public void adminTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.usingNetworks().getNetwork(UserModel.getRandomTenantUser());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, description = "Verify tenant admin user gets another exisiting network with Rest API and checks the forbidden status")
    public void adminTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.usingNetworks().getNetwork(adminAnotherTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, description = "Verify any tenant user gets its network with Rest API and response is not empty")
    public void userTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        restClient.usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, description = "Verify any tenant user gets specific network with Rest API and checks response parameters are correct")
    public void userTenantChecksNetworkParamsAreCorrect() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        restClient.usingNetworks().getNetwork(adminTenantUser).assertNetworkHasName(adminTenantUser).assertNetworkIsEnabled();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, description = "Verify any tenant user gets non exisiting network with Rest API and checks the not found status")
    public void userTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        restClient.usingNetworks().getNetwork(UserModel.getRandomTenantUser());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, description = "Verify any tenant user gets another exisiting network with Rest API and checks the forbidden status")
    public void userTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        restClient.usingNetworks().getNetwork(adminAnotherTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify manager tenant user gets its network with Rest API and response is not empty")
    public void tenantManagerUserChecksIfNetworkIsPresent() throws Exception
    {
        UserModel managerTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("manTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(managerTenantUser, site, UserRole.SiteManager);

        restClient.authenticateUser(managerTenantUser);
        restClient.usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify collaborator tenant user gets its network with Rest API and response is not empty")
    public void tenantCollaboratorUserChecksIfNetworkIsPresent() throws Exception
    {
        UserModel collaboratorTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("colTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(collaboratorTenantUser, site, UserRole.SiteCollaborator);

        restClient.authenticateUser(collaboratorTenantUser);
        restClient.usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify consumer tenant user gets its network with Rest API and response is not empty")
    public void tenantConsumerUserChecksIfNetworkIsPresent() throws Exception
    {
        UserModel consumerTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("consTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(consumerTenantUser, site, UserRole.SiteConsumer);

        restClient.authenticateUser(consumerTenantUser);
        restClient.usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify contributor tenant user gets its network with Rest API and response is not empty")
    public void tenantContributorUserChecksIfItsNetworkIsPresent() throws Exception
    {
        UserModel contributorTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("contTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(contributorTenantUser, site, UserRole.SiteContributor);

        restClient.authenticateUser(contributorTenantUser);
        restClient.usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
