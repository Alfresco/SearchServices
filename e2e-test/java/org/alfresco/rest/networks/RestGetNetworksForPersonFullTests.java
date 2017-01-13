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

public class RestGetNetworksForPersonFullTests extends RestTest
{
    private UserModel adminUserModel;
    private UserModel adminTenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        restClient.usingTenant().createTenant(adminTenantUser);
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check default error schema for get networks for member")
    @Test(groups = { TestGroup.REST_API, TestGroup.FULL, TestGroup.NETWORKS })
    public void getNetworksAndCheckErrorSchema() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(new UserModel("invalidUser", "invalidPassword")).getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidUser"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check default error schema for get networks for member")
    @Test(groups = { TestGroup.REST_API, TestGroup.FULL, TestGroup.NETWORKS })
    public void getNetworksAndCheckPropertiesParameter() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("properties=isEnabled,id").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertThat().field("homeNetwork").is("false")
            .and().field("quotas").isNull()
            .and().field("isEnabled").is("true")
            .and().field("id").is(adminTenantUser.getDomain().toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check that skipCount parameter is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.FULL, TestGroup.NETWORKS })
    public void checkThatSkipCountParameterIsApplied() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("skipCount=1").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.assertThat().entriesListIsEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check that high skipCount parameter is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.FULL, TestGroup.NETWORKS })
    public void checkThatHighSkipCountParameterIsApplied() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("skipCount=100").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.assertThat().entriesListIsEmpty();
        networks.assertThat().paginationField("skipCount").is("100");
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check that maxItems parameter is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.FULL, TestGroup.NETWORKS })
    public void checkThatMaxItemsParameterIsApplied() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("maxItems=1").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertThat().field("homeNetwork").is("false")
            .and().field("quotas").is("[]")
            .and().field("isEnabled").is("true")
            .and().field("id").is(adminTenantUser.getDomain().toLowerCase());
        networks.assertThat().paginationField("maxItems").is("1");
    }

}