package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class RestGetNetworkCoreTests extends RestTest
{
    UserModel adminTenantUser;
    UserModel tenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        UserModel adminuser = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminuser);
        restClient.usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant admin user gets non existing network with Rest API and checks the not found status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.CORE })
    public void adminTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.withCoreAPI().usingNetworks().getNetwork(UserModel.getRandomTenantUser());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
