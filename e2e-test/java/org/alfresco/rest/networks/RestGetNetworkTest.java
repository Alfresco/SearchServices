package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestNetworksApi;
import org.alfresco.utility.data.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
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
@Test(groups = { "rest-api", "networks", "sanity" })
public class RestGetNetworkTest extends RestTest
{
    @Autowired
    RestNetworksApi networkApi;

    UserModel adminTenantUser;
    UserModel tenantUser;
    UserModel managerTenantUser;
    UserModel collaboratorTenantUser;
    UserModel consumerTenantUser;
    UserModel contributorTenantUser;

    @BeforeClass
    public void setup() throws DataPreparationException
    {
        // input data should be created for handle tenants:
        // create tenant "tenant1" with password "password"

        // with admin "admin@tenant1" create user "test1@tenant1" with password "password"
        adminTenantUser = new UserModel("admin@tenant1", "password");
        tenantUser = new UserModel("test1@tenant1", "password");
//        managerTenantUser = dataUser.usingUser(adminTenantUser).createUser("manTenant2@tenant1");
//        collaboratorTenantUser = dataUser.usingUser(adminTenantUser).createUser("manTenant2@tenant1");
//        consumerTenantUser = dataUser.usingUser(adminTenantUser).createUser("manTenant2@tenant1");
//        contributorTenantUser = dataUser.usingUser(adminTenantUser).createUser("manTenant2@tenant1");
        managerTenantUser = new UserModel("manTenant@tenant1", "password");
        collaboratorTenantUser = new UserModel("colTenant@tenant1", "password");
        consumerTenantUser = new UserModel("consTenant@tenant1", "password");
        contributorTenantUser = new UserModel("contTenant@tenant1", "password");      
        
        SiteModel site=dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataUser.usingUser(adminTenantUser).addUserToSite(managerTenantUser, site, UserRole.SiteManager);
        dataUser.usingUser(adminTenantUser).addUserToSite(collaboratorTenantUser, site, UserRole.SiteCollaborator);
        dataUser.usingUser(adminTenantUser).addUserToSite(consumerTenantUser, site, UserRole.SiteConsumer);
        dataUser.usingUser(adminTenantUser).addUserToSite(contributorTenantUser, site, UserRole.SiteConsumer);
        
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
    
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify manager tenant user gets its network with Rest API and response is not empty")
    public void tenantManagerUserChecksIfNetworkIsPresent() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(managerTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify collaborator tenant user gets its network with Rest API and response is not empty")
    public void tenantCollaboratorUserChecksIfNetworkIsPresent() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(collaboratorTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify consumer tenant user gets its network with Rest API and response is not empty")
    public void tenantConsumerUserChecksIfNetworkIsPresent() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(consumerTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "networks" }, executionType = ExecutionType.SANITY, description = "Verify contributor tenant user gets its network with Rest API and response is not empty")
    public void tenantContributorUserChecksIfNetworkIsPresent() throws Exception
    {
        String tenantName = "tenant1";
        restClient.authenticateUser(contributorTenantUser);
        networkApi.useRestClient(restClient);
        networkApi.getNetwork(tenantName);
        networkApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
}
