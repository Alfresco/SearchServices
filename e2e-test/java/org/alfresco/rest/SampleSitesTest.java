package org.alfresco.rest;

import org.alfresco.dataprep.ContentService;
import org.alfresco.rest.RestSitesApi;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.SiteMember;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SampleSitesTest extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    @Autowired
    ContentService content;

    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass
    public void initTest() throws DataPreparationException
    {
        userModel = dataUser.getAdminUser();
        restClient.authenticateUser(userModel);
        siteModel = dataSite.createPublicRandomSite();
        siteAPI.useRestClient(restClient);
    }

    @Test
    public void getSiteResponseNotNull() throws JsonToModelConversionException
    {
        Assert.assertNotNull(siteAPI.getSite(siteModel.getId()), "Get site response should not be null");
    }

    @Test
    public void getSiteCheckStatusCode() throws JsonToModelConversionException
    {
        siteAPI.getSite(siteModel.getId());
        Assert.assertEquals(siteAPI.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get site response status code is not correct");
    }

    @Test
    public void getSitesResponseNotNull() throws JsonToModelConversionException
    {
        Assert.assertNotNull(siteAPI.getSites().getEntries(), "Get sites response should not be null");
    }

    @Test
    public void getSitesCheckStatusCode() throws JsonToModelConversionException
    {
        siteAPI.getSites();
        Assert.assertEquals(siteAPI.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get sites response status code is not correct");
    }

    @Test
    public void sitesCollectionHasPagination() throws JsonToModelConversionException
    {
        siteAPI.getSites().assertResponseHasPagination();
        Assert.assertEquals(siteAPI.getSites().getPagination().getCount(), 100, "Sites collection should have pagination");
    }

    @Test
    public void addMemberToSiteCheckStatusCode() throws JsonToModelConversionException, DataPreparationException
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMember siteMember = new SiteMember(Role.SiteCollaborator.toString(), newMember.getUsername());
        siteAPI.addPerson(siteModel.getId(), siteMember);
        Assert.assertEquals(siteAPI.usingRestWrapper().getStatusCode(), HttpStatus.CREATED.toString(),
                "Add member to site response status code is not correct");
    }

    @Test
    public void isSiteReturned() throws JsonToModelConversionException
    {
        siteAPI.getAllSites().assertThatResponseHasSite(siteModel.getId());
    }

    @Test
    public void checkSiteDetails() throws JsonToModelConversionException
    {
        siteAPI.getSite(siteModel.getId());
    }

}