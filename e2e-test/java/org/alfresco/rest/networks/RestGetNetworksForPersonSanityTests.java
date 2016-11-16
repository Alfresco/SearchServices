package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
public class RestGetNetworksForPersonSanityTests extends RestTest
{
    private UserModel adminUserModel;
    UserModel adminTenantUser;
    UserModel tenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        restClient.usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
    }
    
    @Bug(id = "MNT-16904")
    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify non existing user gets another exisiting network with Rest API and checks the forbidden status")
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser);
        restClient.onCoreAPI().usingUser(tenantUser).getNetworks(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.onCoreAPI().usingUser(adminTenantUser).getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify tenant user is not authorized to check network of admin user with Rest API and checks the forbidden status")
    public void tenantUserIsNotAuthorizedToCheckNetworkOfAdminUser() throws Exception
    { 
        restClient.authenticateUser(tenantUser);
        restClient.onCoreAPI().usingUser(tenantUser).getNetworks(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
    
    @Test(groups = TestGroup.COMMENTS)
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify admin tenant user is not authorized to check network of another user with Rest API and checks the forbidden status")
    public void adminTenantUserIsNotAuthorizedToCheckNetworkOfAnotherUser() throws Exception
    {
        UserModel secondAdminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(secondAdminTenantUser);
        UserModel secondTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("anotherTenant");
        restClient.authenticateUser(secondAdminTenantUser);
        restClient.onCoreAPI().usingUser(secondAdminTenantUser).getNetworks(secondTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
