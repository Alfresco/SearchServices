package org.alfresco.rest.networks;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNetworkModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class RestGetNetworkFullTests extends RestTest
{
    UserModel adminTenantUser, adminuser;
    UserModel adminAnotherTenantUser;
    SiteModel site;
    UserModel tenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {        
        adminuser = dataUser.getAdminUser();
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminuser);
        restClient.usingTenant().createTenant(adminTenantUser);
        
        adminAnotherTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminAnotherTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("userTenant1");
        site = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
    }
    
    @Bug(id = "ACE-5738")
    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant admin user gets an invalid network with Rest API and checks the not found status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.FULL })
    public void adminTenantGetsInvalidNetwork() throws Exception
    {
        restClient.authenticateUser(adminAnotherTenantUser);
        adminAnotherTenantUser.setDomain("tenant.@%");
        
        restClient.withCoreAPI().usingNetworks().getNetwork(adminAnotherTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.UNEXPECTED_TENANT, adminTenantUser.getDomain(), "@"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Bug(id = "ACE-5745")
    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin user gets an existing network successfully with Rest API")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.FULL })
    public void adminUserGetsExistingNetwork() throws Exception
    {
        RestNetworkModel restNetworkModel = restClient.authenticateUser(adminuser).withCoreAPI().usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().fieldsCount().is("1");
        restNetworkModel.assertThat().field("id").is(adminTenantUser.getDomain())
                .and().field("homeNetwork").isNull()
                .and().field("isEnabled").is("true");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant admin user gets network using properties parameter successfully with Rest API")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.FULL })
    public void adminTenantGetsNetworkUsingPropertiesParameter() throws Exception
    {
        JSONObject entryResponse= restClient.authenticateUser(tenantUser).withCoreAPI().usingNetworks().usingParams("properties=id").getNetworkWithParams(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertTrue(entryResponse.get("id").equals(adminTenantUser.getDomain().toLowerCase()));
        assertJsonResponseDoesnotContainField(entryResponse, "homeNetwork");
        assertJsonResponseDoesnotContainField(entryResponse, "isEnabled");    
    }
    
    private void assertJsonResponseDoesnotContainField(JSONObject entryResponse, String field)
    {
        try{
            entryResponse.get(field);
        }
        catch(JSONException ex)
        {
            Assert.assertTrue(ex.getMessage().equals(String.format("JSONObject[\"%s\"] not found.", field)));
        }
    }
    
    
}
