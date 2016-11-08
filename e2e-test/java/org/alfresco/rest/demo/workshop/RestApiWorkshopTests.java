package org.alfresco.rest.demo.workshop;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestSitesApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

/**
 * 
 * Demo workshop for RestAPI test
 *
 */
public class RestApiWorkshopTests extends RestTest
{
    @Autowired
    RestSitesApi sitesAPI;
    
    @Test
    public void verifyGetSitesRestApiCall() throws Exception
    {
        // creating a random user in repository
       
        // create a new random site using your UserModel from above

        // using "siteApi", call get "/sites" Rest API and verify created site is present
        
        // verify status is OK 

    }
    
    @Test
    public void verifyGetASiteRestApiCall() throws Exception
    {
        // creating a random user in repository
       
        // create a new random site using your UserModel from above

        // using "siteApi", call get "/sites/{siteId}" Rest API
        // using "siteApi", verify created site is present

        // verify status is OK 
    }
}
