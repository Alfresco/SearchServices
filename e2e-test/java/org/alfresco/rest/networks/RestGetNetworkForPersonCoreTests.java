package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNetworkModel;
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
public class RestGetNetworkForPersonCoreTests extends RestTest
{
    UserModel adminUser;
    UserModel adminTenantUser;
    UserModel tenantUser;
    UserModel randomTestUser;
    RestNetworkModel restNetworkModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser);
        restClient.usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        randomTestUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("randomTestUser");
    }

    @Bug(id = "needs to be checked")
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify getNetwork request status code is 200 if a user tries to get network information of another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.CORE })
    public void verifyGetNetworkByAUserForAnotherUser() throws Exception
    {
        restNetworkModel = restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getNetwork(randomTestUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().field("id").is(tenantUser.getDomain());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getNetwork status code is 404 for a personId that does not exist")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.CORE })
    public void verifyThatGetNetworkStatusIs404ForAPersonIdThatDoesNotExist() throws Exception
    {
        UserModel invalidUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("validUsername");
        invalidUser.setUsername("invalidUsername");

        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(invalidUser).getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidUser.getUsername()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getNetwork status code is 404 for a networkId that does not exist")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.CORE })
    public void verifyThatGetNetworkStatusIs404ForANetworkIdThatDoesNotExist() throws Exception
    {
        UserModel invalidUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("invalidNetworkId");
        invalidUser.setDomain("invalidNetworkId");

        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(invalidUser).getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidUser.getUsername()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify getNetwork request that is made using -me- instead of personId")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.CORE })
    public void verifyGetNetworkRequestUsingMeInsteadOfPersonId() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingMe().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}