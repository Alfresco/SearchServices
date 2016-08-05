package sites;

import java.util.List;

import org.alfresco.rest.BaseRestTest;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.rest.v1.RestSites;
import org.alfresco.tester.data.DataSite;
import org.alfresco.tester.data.DataUser;
import org.alfresco.tester.exception.DataPreparationException;
import org.alfresco.tester.model.SiteModel;
import org.alfresco.tester.model.UserModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SitesTest extends BaseRestTest
{
    @Autowired
    RestSites onSites;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass
    public void setUp() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        siteModel = new SiteModel(Role.SiteManager.toString(), Visibility.PUBLIC, "", RandomStringUtils.randomAlphanumeric(20),
                RandomStringUtils.randomAlphanumeric(20), RandomStringUtils.randomAlphanumeric(20));

        siteModel = dataSite.createSite(siteModel);
    }

    @Test
    public void getSitesResponseNotNull()
    {
        Assert.assertNotNull(onSites.withAuthUser(userModel).getSites().getEntries(), "Get sites response should not be null");
    }

    @Test
    public void getSitesCheckStatusCode()
    {
        onSites.withAuthUser(userModel).getSites();
        Assert.assertEquals(onSites.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get sites response status code is not correct");
    }

    @Test
    public void getSiteResponseNotNull()
    {
        Assert.assertNotNull(onSites.withAuthUser(userModel).getSite(siteModel.getId()), "Get site response should not be null");
    }

    @Test
    public void getSiteCheckStatusCode()
    {
        onSites.withAuthUser(userModel).getSite(siteModel.getId());
        Assert.assertEquals(onSites.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get site response status code is not correct");
    }

    @Test
    public void isSitePresent()
    {
        onSites.withAuthUser(userModel).getSites().hasSite(siteModel.getId());
    }
}