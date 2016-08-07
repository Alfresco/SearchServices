package org.alfresco.rest.v1;

import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.v1.RestSitesApi;
import org.alfresco.tester.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SampleATest extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;
    
    @BeforeClass
    public void initTest()
    {
        siteAPI.useRestClient(restClient);
    }

    @Test(groups = {"rest-api-v1", "sanity"})
    public void sampleATestMethod1() throws JsonToModelConversionException
    {        
        UserModel admin = new UserModel("admin", "admin");
        restClient.authenticateUser(admin);
                
        Assert.assertFalse(siteAPI.getSites().getEntries().isEmpty(), "We have sites");
    }

    @Test(groups = {"rest-api-v1", "sanity"})
    public void sampleATestMethod2() throws JsonToModelConversionException
    {
        UserModel admin = new UserModel("admin", "a");
        restClient.authenticateUser(admin);
        Assert.assertTrue(siteAPI.getSites().getEntries().isEmpty(), "We have sites");
        Assert.assertEquals(siteAPI.usingRestWrapper().getLastStatus().getName().toLowerCase(), HttpStatus.UNAUTHORIZED.name().toLowerCase(), "Status Code Name");
    }
}
