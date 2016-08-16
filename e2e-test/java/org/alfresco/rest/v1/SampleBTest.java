package org.alfresco.rest.v1;

import org.alfresco.rest.RestSitesApi;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SampleBTest extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @BeforeClass
    public void initTest()
    {
        siteAPI.useRestClient(restClient);
    }

    @Test(groups = { "rest-api-v1", "sanity" })
    public void sampleBTestMethod1() throws JsonToModelConversionException
    {
        UserModel admin = new UserModel("admin", "admin");
        restClient.authenticateUser(admin);
        Assert.assertFalse(siteAPI.getSites().getEntries().isEmpty(), "We have sites");
    }

    @Test(groups = { "rest-api-v1", "sanity" })
    public void sampleBTestMethod2() throws JsonToModelConversionException
    {
        UserModel admin = new UserModel("admin", "a");
        restClient.authenticateUser(admin);
        Assert.assertTrue(siteAPI.getSites().getEntries().isEmpty(), "We have sites");

        Assert.assertEquals(siteAPI.usingRestWrapper().getLastStatus().getName().toLowerCase(), HttpStatus.UNAUTHORIZED.name().toLowerCase(),
                "Status Code Name");
    }

    @Test(groups = { "rest-api-v1" })
    public void getSite() throws JsonToModelConversionException
    {
        UserModel admin = new UserModel("admin", "admin");
        restClient.authenticateUser(admin);
        Assert.assertEquals(siteAPI.getSite("0-C2291-1470305685222").getId(), "no-id");
    }

}
