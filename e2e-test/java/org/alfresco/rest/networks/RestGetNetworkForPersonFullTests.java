package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNetworkModel;
import org.alfresco.rest.model.RestNetworkQuotaModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Tests for /people/{personId}/networks
 */
public class RestGetNetworkForPersonFullTests extends RestTest
{
    UserModel adminUser;
    UserModel adminTenantUser;
    UserModel secondAdminTenantUser;
    UserModel tenantUser;
    RestNetworkModel restNetworkModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        secondAdminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        restClient.usingTenant().createTenant(secondAdminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify getNetwork request response entry details")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.FULL })
    public void verifyGetNetworkRequestResponseEntryDetails() throws Exception
    {
        restNetworkModel = restClient.authenticateUser(adminTenantUser).withCoreAPI().usingMe().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().field("quotas").is(new ArrayList<RestNetworkQuotaModel>())
                .assertThat().field("isEnabled").is("true")
                .assertThat().field("homeNetwork").is("true")
                .assertThat().field("id").is(adminTenantUser.getDomain().toLowerCase());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that properties parameter is applied to getNetwork request")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.FULL })
    public void verifyPropertiesParameterIsAppliedToGetNetworkRequest() throws Exception
    {
        restNetworkModel = restClient.authenticateUser(adminTenantUser).withParams("properties=id").withCoreAPI().usingMe().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().field("id").is(adminTenantUser.getDomain().toLowerCase());
        // TODO add assert that entry has only 1 field
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify getNetwork request status code is 404 for a network to which user does not belong")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.FULL })
    public void verifyGetNetworkRequestStatusCodeIs404ForANetworkToWhichTheUserDoesNotBelong() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().getNetwork(secondAdminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
