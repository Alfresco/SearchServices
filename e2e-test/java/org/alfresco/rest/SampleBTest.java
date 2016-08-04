package org.alfresco.rest;

import org.alfresco.rest.v1.RestSites;
import org.alfresco.tester.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SampleBTest extends BaseRestTest
{
    @Autowired
    RestSites onSites;

    @Test
    public void sampleBTestMethod1()
    {
        UserModel admin = new UserModel("admin", "admin");
        Assert.assertFalse(onSites.withAuthUser(admin).getSites().getEntries().isEmpty(), "We have sites");
    }

    @Test
    public void sampleBTestMethod2()
    {
        UserModel admin = new UserModel("admin", "a");
        Assert.assertTrue(onSites.withAuthUser(admin).getSites().getEntries().isEmpty(), "We have sites");

        Assert.assertEquals(onSites.usingRestWrapper().getLastStatus().getName().toLowerCase(), HttpStatus.UNAUTHORIZED.name().toLowerCase(), "Status Code Name");
    }
    
    
    @Test
    public void getSite()
    {
        UserModel admin = new UserModel("admin", "admin"); 
        
        Assert.assertEquals(onSites.withAuthUser(admin).getSite("0-C2291-1470305685222").getId(), "asd");
    }

}
