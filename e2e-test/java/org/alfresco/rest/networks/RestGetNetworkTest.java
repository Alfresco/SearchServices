package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestNetworksApi;
import org.alfresco.rest.requests.RestTenantApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Cristina Axinte on 9/26/2016.
 */
@Test(groups = { "rest-api", "networks" })
public class RestGetNetworkTest extends RestTest
{
    @Autowired
    RestNetworksApi networkApi;

    @Autowired
    RestTenantApi tenantApi;

    UserModel adminTenantUser;
    UserModel tenantUser;
    UserModel managerTenantUser;
    UserModel collaboratorTenantUser;
    UserModel consumerTenantUser;
    UserModel contributorTenantUser;
    String tenantName;
    String anotherTenantName;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        UserModel adminuser = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        tenantName = adminTenantUser.getDomain();

        restClient.authenticateUser(adminuser);
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser.getDomain());
        UserModel adminAnotherTenantUser = UserModel.getAdminTenantUser();
        anotherTenantName = adminAnotherTenantUser.getDomain();
        tenantApi.createTenant(anotherTenantName);

        tenantUser = dataUser.usingUser(adminTenantUser).createRandomTestUser("uTenant");
        managerTenantUser = dataUser.usingUser(adminTenantUser).createRandomTestUser("manTenant");
        collaboratorTenantUser = dataUser.usingUser(adminTenantUser).createRandomTestUser("colTenant");
        consumerTenantUser = dataUser.usingUser(adminTenantUser).createRandomTestUser("consTenant");
        contributorTenantUser = dataUser.usingUser(adminTenantUser).createRandomTestUser("contTenant");

        SiteModel site = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataUser.usingUser(adminTenantUser).addUserToSite(managerTenantUser, site, UserRole.SiteManager);
        dataUser.usingUser(adminTenantUser).addUserToSite(collaboratorTenantUser, site, UserRole.SiteCollaborator);
        dataUser.usingUser(adminTenantUser).addUserToSite(consumerTenantUser, site, UserRole.SiteConsumer);
        dataUser.usingUser(adminTenantUser).addUserToSite(contributorTenantUser, site, UserRole.SiteConsumer);

        networkApi.useRestClient(restClient);
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify non existing user gets another exisiting network with Rest API and checks the forbidden status")
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(tenantName);
        restClient.authenticateUser(tenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED.toString());
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        networkApi.getNetwork(adminTenantUser.getDomain());
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets specific network with Rest API and checks response parameters are correct")
    public void adminTenantChecksNetworkParamsAreCorrect() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        networkApi.getNetwork(tenantName).assertNetworkHasName(tenantName).assertNetworkIsEnabled(true);
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets non exisiting network with Rest API and checks the not found status")
    public void adminTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        String tenantName = "notenant";
        restClient.authenticateUser(adminTenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets another exisiting network with Rest API and checks the forbidden status")
    public void adminTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        networkApi.getNetwork(anotherTenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets its network with Rest API and response is not empty")
    public void userTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets specific network with Rest API and checks response parameters are correct")
    public void userTenantChecksNetworkParamsAreCorrect() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        networkApi.getNetwork(tenantName).assertNetworkHasName(tenantName).assertNetworkIsEnabled(true);
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets non exisiting network with Rest API and checks the not found status")
    public void userTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        String tenantName = "nontenant";
        restClient.authenticateUser(tenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets another exisiting network with Rest API and checks the forbidden status")
    public void userTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        networkApi.getNetwork(anotherTenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify manager tenant user gets its network with Rest API and response is not empty")
    public void tenantManagerUserChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(managerTenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify collaborator tenant user gets its network with Rest API and response is not empty")
    public void tenantCollaboratorUserChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(collaboratorTenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify consumer tenant user gets its network with Rest API and response is not empty")
    public void tenantConsumerUserChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(consumerTenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify contributor tenant user gets its network with Rest API and response is not empty")
    public void tenantContributorUserChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(contributorTenantUser);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
}
