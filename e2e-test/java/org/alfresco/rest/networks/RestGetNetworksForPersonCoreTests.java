package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNetworkModelsCollection;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RestGetNetworksForPersonCoreTests extends RestTest
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
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using invalid value for skipCount")
    @Test(groups = {TestGroup.REST_API, TestGroup.CORE, TestGroup.NETWORKS })
    public void invalidValueForSkipCountTest() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withParams("skipCount=abc").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"));
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using invalid value for maxItems")
    @Test(groups = {TestGroup.REST_API, TestGroup.CORE, TestGroup.NETWORKS })
    public void invalidValueForMaxItemsTest() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withParams("maxItems=abc").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "abc"));
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using personId that does not exist")
    @Test(groups = {TestGroup.REST_API, TestGroup.CORE, TestGroup.NETWORKS })
    public void inexistentTenantTest() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(new UserModel("invalidTenantUser", "password")).getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidTenantUser"));
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using -me- instead of personId")
    @Test(groups = {TestGroup.REST_API, TestGroup.CORE, TestGroup.NETWORKS })
    public void specifyMeInsteadOfPersonIdTest() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withCoreAPI().usingMe().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled().and().field("id").is(adminTenantUser.getDomain().toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks and validate network entry")
    @Test(groups = {TestGroup.REST_API, TestGroup.CORE, TestGroup.NETWORKS })
    public void checkNetworkEntryTest() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled()
            .and().field("id").is(adminTenantUser.getDomain().toLowerCase())
            .and().field("quotas").is("[]")
            .and().field("homeNetwork").is("false");
    }

}