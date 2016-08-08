package org.alfresco.rest.v1;

import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.tester.data.DataSite;
import org.alfresco.tester.data.DataUser;
import org.alfresco.tester.exception.DataPreparationException;
import org.alfresco.tester.model.SiteModel;
import org.alfresco.tester.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public void getSitesResponseNotNull() throws JsonToModelConversionException
    {
        Assert.assertNotNull(siteAPI.getSites().getEntries(), "Get sites response should not be null");
    }

    @Test
    public void getSitesCheckStatusCode()
    {
        Assert.assertEquals(siteAPI.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get sites response status code is not correct");
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
    public void collectionHasPagination() throws JsonToModelConversionException
    {
        siteAPI.getSites().assertPagination();
        Assert.assertEquals(siteAPI.getSites().getPagination().getCount(), 100);
    }
    
    @Autowired RestCommentsApi commentsAPI;
    @Test
    public void getComments() throws JsonToModelConversionException
    {
        commentsAPI.useRestClient(restClient);
        System.out.println(commentsAPI.getNodeComments("bc95c7bf-0593-422f-af7d-b92f9d8129de").getEntries().size());
    }
    
    
    @Test
    public void addComments() throws JsonToModelConversionException
    {
        commentsAPI.useRestClient(restClient);
        commentsAPI.addCommentToNode("bc95c7bf-0593-422f-af7d-b92f9d8129de");
    }

}