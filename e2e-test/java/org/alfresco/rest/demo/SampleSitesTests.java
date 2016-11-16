package org.alfresco.rest.demo;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "demo" })
public class SampleSitesTests extends RestTest
{  
    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.getAdminUser();
        restClient.authenticateUser(userModel);
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();        
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets site details with Rest API and response is not empty")
    public void adminShouldGetSiteDetails() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingSite(siteModel).getSite()
            .assertThat().field("id").isNotNull();
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets site information and gets status code OK (200)")
    public void adminShouldGetSites() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingSite(siteModel).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets sites with Rest API and the response is not empty")
    public void adminShouldAccessSites() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().getSites().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets sites with Rest API and status code is 200")
    public void adminShouldAccessResponsePagination() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().getSites().assertThat().paginationExist();
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user adds site member with Rest API and status code is 201")
    public void adminShouldAddNewSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.withCoreAPI().usingSite(siteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify that site exists from get all sites request")
    public void adminShouldGetSiteFromSitesList() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().getSites().assertThat().entriesListContains("id", siteModel.getId());    
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify site details: response not empty, description, title, visibility")
    public void adminShouldAccessSiteDetails() throws JsonToModelConversionException, Exception
    {
      restClient.withCoreAPI().usingSite(siteModel).getSite()
            .assertThat().field("id").isNotNull()
            .and().field("description").is(siteModel.getDescription())
            .and().field("title").is(siteModel.getTitle())
            .and().field("visibility").is(siteModel.getVisibility());            
    }

}