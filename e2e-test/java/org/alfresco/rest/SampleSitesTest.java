package org.alfresco.rest;

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

    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass
    public void initTest() throws DataPreparationException
    {
        userModel = dataUser.getAdminUser();
        restClient.authenticateUser(userModel);
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        siteAPI.useRestClient(restClient);
    }

    @Test
    public void adminCanGetSiteDetails() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSite(siteModel.getId()).assertResponseIsNotEmpty();
    }

    @Test
    public void adminCanAccessSiteDetails() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSite(siteModel.getId());
        siteAPI.usingRestWrapper()
                .assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test
    public void adminCanAccessSites() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSites()
                .assertThatResponseIsNotEmpty();
    }

    @Test
    public void adminIsAbleToRetrieveSites() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSites();
        siteAPI.usingRestWrapper()
                .assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test
    public void adminIsAbleToAccessResponsePagination() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSites().assertResponseHasPagination();
    }

    @Test
    public void adminIsAbleToAddNewSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMember siteMember = new SiteMember(Role.SiteCollaborator.toString(), 
                                    newMember.getUsername());
        
        siteAPI.addPerson(siteModel.getId(), siteMember);
        siteAPI.usingRestWrapper()
                .assertStatusCodeIs(HttpStatus.CREATED.toString());
    }

    @Test
    public void adminIsAbleToGetSiteFromSitesList() throws JsonToModelConversionException, Exception
    {
        siteAPI.getAllSites()
                .assertThatResponseHasSite(siteModel.getId());
    }

    @Test
    public void adminIsAbleToAccessSiteDetails() throws JsonToModelConversionException, Exception
    {
        siteAPI.getSite(siteModel.getId())
                .assertResponseIsNotEmpty()
                .assertSiteHasDescription(siteModel.getDescription())
                .assertSiteHasTitle(siteModel.getTitle())
                .assertSiteHasVisibility(siteModel.getVisibility());
    }

}