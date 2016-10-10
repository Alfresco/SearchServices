package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestNetworksApi;
import org.alfresco.rest.requests.RestTenantApi;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "networks", "sanity" })
public class RestGetNetworksForPersonSanityTests extends RestTest
{
    @Autowired
    RestTenantApi tenantApi;

    @Autowired
    RestNetworksApi networkApi;

    private UserModel adminUserModel;
    UserModel adminTenantUser;
    UserModel tenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        networkApi.useRestClient(restClient);
    }
    
    @Bug(id = "MNT-16904")
    @Test(groups = "sanity")
    @TestRail(section = { "rest-api",
            "networks" }, executionType = ExecutionType.SANITY, description = "Verify non existing user gets another exisiting network with Rest API and checks the forbidden status")
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser);
        networkApi.getNetworksForUser(adminTenantUser);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api",
            "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        networkApi.getNetworksForUser(adminTenantUser);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = "sanity")
    @TestRail(section = { "rest-api",
            "networks" }, executionType = ExecutionType.SANITY, description = "Verify tenant user is not authorized to check network of admin user with Rest API and checks the forbidden status")
    public void tenantUserIsNotAuthorizedToCheckNetworkOfAdminUser() throws Exception
    { 
        restClient.authenticateUser(tenantUser);
        networkApi.getNetworksForUser(adminTenantUser);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
    
    @Test(groups = "sanity")
    @TestRail(section = { "rest-api",
            "networks" }, executionType = ExecutionType.SANITY, description = "Verify admin tenant user is not authorized to check network of another user with Rest API and checks the forbidden status")
    public void adminTenantUserIsNotAuthorizedToCheckNetworkOfAnotherUser() throws Exception
    {
        UserModel secondAdminTenantUser = UserModel.getAdminTenantUser();
        tenantApi.createTenant(secondAdminTenantUser);
        UserModel secondTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("anotherTenant");
        restClient.authenticateUser(secondAdminTenantUser);
        networkApi.getNetworksForUser(secondTenantUser);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
