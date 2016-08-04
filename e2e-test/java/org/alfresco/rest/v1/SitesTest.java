package org.alfresco.rest.v1;

import static org.alfresco.rest.RestWrapper.onRestAPI;

import java.io.IOException;

import org.alfresco.rest.BaseRestTest;
import org.alfresco.tester.model.UserModel;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class SitesTest extends BaseRestTest
{
    @Test
    public void getSite()
    {
        UserModel admin = new UserModel("admin", "admin");
        System.out.println(onRestAPI().withAuthUser(admin).onSites().getSite("SiteName1470151654495").getTitle());
    }

    @Test
    public void getSites() throws JsonParseException, JsonMappingException, IOException
    {
        UserModel admin = new UserModel("admin", "admin");

        Assert.assertEquals(onRestAPI().withAuthUser(admin).onSites().getSites().getPagination().getTotalItems(), 281);

        Assert.assertEquals(onRestAPI().withAuthUser(admin).onSites().getSites().getEntries().get(0).onModel().getTitle(), "0-C2291-1470255221170");        
    }
}
