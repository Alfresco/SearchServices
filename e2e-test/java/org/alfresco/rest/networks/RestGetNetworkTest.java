package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestNetworksApi;
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
@Test(groups = { "rest-api", "networks", "sanity" })
public class RestGetNetworkTest extends RestTest
{
    @Autowired
    RestNetworksApi networkApi;

    UserModel adminTenantUser;
    UserModel tenantUser;

    @BeforeClass
    public void setup()
    {
        // input data should be created for handle tenants:
        // create tenant "tenant1" with password "password"

        // with admin "admin@tenant1" create user "test1@tenant1" with password "password"
        adminTenantUser = new UserModel("admin@tenant1", "password");
        tenantUser = new UserModel("test1@tenant1", "password");
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify non existing user gets another exisiting network with Rest API and checks the forbidden status")
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        String tenantName = "tenant1";
        UserModel tenantUser = new UserModel("nonexisting@tenant1", "password");
        restClient.authenticateUser(tenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(adminTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets specific network with Rest API and checks response parameters are correct")
    public void adminTenantChecksNetworkParamsAreCorrect() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(adminTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName).assertNetworkHasName(tenantName).assertNetworkIsEnabled(true);
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets non exisiting network with Rest API and checks the not found status")
    public void adminTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        String tenantName = "netenant";
        restClient.authenticateUser(adminTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets another exisiting network with Rest API and checks the forbidden status")
    public void adminTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        String tenantName = "tenant2";
        restClient.authenticateUser(adminTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets its network with Rest API and response is not empty")
    public void userTenantChecksIfNetworkIsPresent() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(tenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets specific network with Rest API and checks response parameters are correct")
    public void userTenantChecksNetworkParamsAreCorrect() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(tenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName).assertNetworkHasName(tenantName).assertNetworkIsEnabled(true);
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets non exisiting network with Rest API and checks the not found status")
    public void userTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        String tenantName = "netenant";
        restClient.authenticateUser(tenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND.toString());
    }

    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify any tenant user gets another exisiting network with Rest API and checks the forbidden status")
    public void userTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        String tenantName = "tenant2";
        restClient.authenticateUser(tenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }
}
