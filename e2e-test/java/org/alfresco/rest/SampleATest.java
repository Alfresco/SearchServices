package org.alfresco.rest;

import org.alfresco.rest.v1.RestSites;
import org.alfresco.tester.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SampleATest extends BaseRestTest
{
    @Autowired
    RestSites onSites;

    @Test
    public void sampleATestMethod1()
    {
        UserModel admin = new UserModel("admin", "admin");
        Assert.assertFalse(onSites.withAuthUser(admin).getSites().getEntries().isEmpty(), "We have sites");
    }

    @Test
    public void sampleATestMethod2()
    {
        UserModel admin = new UserModel("admin", "a");
        Assert.assertTrue(onSites.withAuthUser(admin).getSites().getEntries().isEmpty(), "We have sites");

        Assert.assertEquals(onSites.usingRestWrapper().getLastStatus().getName(), HttpStatus.ACCEPTED.name(), "Status Code Name");
    }
}
