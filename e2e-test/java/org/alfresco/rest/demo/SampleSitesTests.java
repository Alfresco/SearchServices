package org.alfresco.rest.demo;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.SiteMember;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "demo" })
public class SampleSitesTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.getAdminUser();
        restClient.authenticateUser(userModel);
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        siteAPI.useRestClient(restClient);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets site details with Rest API and response is not empty")
    public void adminShouldGetSiteDetails() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSite(siteModel)
            .assertResponseIsNotEmpty();
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets site information and gets status code OK (200)")
    public void adminShouldGetSites() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSite(siteModel);
        siteAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets sites with Rest API and the response is not empty")
    public void adminShouldAccessSites() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSites()
            .assertThatResponseIsNotEmpty();
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets sites with Rest API and status code is 200")
    public void adminShouldRetrieveSites() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSites();
        siteAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets sites with Rest API and status code is 200")
    public void adminShouldAccessResponsePagination() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSites()
            .assertResponseHasPagination();
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user adds site member with Rest API and status code is 201")
    public void adminShouldAddNewSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMember siteMember = new SiteMember(Role.SiteCollaborator.toString(), newMember.getUsername());

        siteAPI.addPerson(siteModel, siteMember);
        siteAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify that site exists from get all sites request")
    public void adminShouldGetSiteFromSitesList() throws JsonToModelConversionException, Exception
    {
        siteAPI.getAllSites()
            .assertThatResponseHasSite(siteModel);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify site details: response not empty, description, title, visibility")
    public void adminShouldAccessSiteDetails() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSite(siteModel)
            .assertResponseIsNotEmpty()
            .assertSiteHasDescription(siteModel.getDescription())
            .assertSiteHasTitle(siteModel.getTitle())
            .assertSiteHasVisibility(siteModel.getVisibility());
    }

}