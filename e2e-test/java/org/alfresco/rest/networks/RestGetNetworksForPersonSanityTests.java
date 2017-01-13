package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNetworkModelsCollection;
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

public class RestGetNetworksForPersonSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private UserModel adminTenantUser;
    private UserModel tenantUser;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        restClient.usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
    }
    
    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify non existing user gets another exisiting network with Rest API and checks the unauthorized status")
    @Test(groups = { TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingAuthUser().getNetworks(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant user is not authorized to check network of admin user with Rest API and checks the forbidden status")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void tenantUserIsNotAuthorizedToCheckNetworkOfAdminUser() throws Exception
    { 
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingAuthUser().getNetworks(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify admin tenant user is not authorized to check network of another user with Rest API and checks the forbidden status")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void adminTenantUserIsNotAuthorizedToCheckNetworkOfAnotherUser() throws Exception
    {
        UserModel secondAdminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(secondAdminTenantUser);
        UserModel secondTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("anotherTenant");
        restClient.authenticateUser(secondAdminTenantUser);
        restClient.withCoreAPI().usingAuthUser().getNetworks(secondTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant manager user gets specific network with Rest API and response is not empty")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void managerTenantChecksIfNetworkIsPresent() throws Exception
    {
        UserModel managerTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("managerTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(managerTenantUser, siteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(managerTenantUser);
        RestNetworkModelsCollection networks = restClient.withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled().and().field("id").is(managerTenantUser.getDomain().toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant collaborator user gets specific network with Rest API and response is not empty")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void collaboratorTenantChecksIfNetworkIsPresent() throws Exception
    {
        UserModel collaboratorTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("collaboratorTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(collaboratorTenantUser, siteModel, UserRole.SiteCollaborator);
        
        restClient.authenticateUser(collaboratorTenantUser);
        RestNetworkModelsCollection networks = restClient.withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled().and().field("id").is(collaboratorTenantUser.getDomain().toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant contributor user gets specific network with Rest API and response is not empty")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void contributorTenantChecksIfNetworkIsPresent() throws Exception
    {
        UserModel contributorTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("contributorTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(contributorTenantUser, siteModel, UserRole.SiteContributor);
        
        restClient.authenticateUser(contributorTenantUser);
        RestNetworkModelsCollection networks = restClient.withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled().and().field("id").is(contributorTenantUser.getDomain().toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant consumer user gets specific network with Rest API and response is not empty")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void consumerTenantChecksIfNetworkIsPresent() throws Exception
    {
        UserModel consumerTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("consumerTenant");
        dataUser.usingUser(adminTenantUser).addUserToSite(consumerTenantUser, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(consumerTenantUser);
        RestNetworkModelsCollection networks = restClient.withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled().and().field("id").is(consumerTenantUser.getDomain().toLowerCase());
    }
}