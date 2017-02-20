package org.alfresco.rest.sites.get;

import java.util.List;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 11/22/2016.
 */
public class GetSitesCoreTests extends RestTest
{
    private UserModel regularUser, privateSiteManager, privateSiteConsumer;
    private SiteModel publicSite, privateSite, moderatedSite, deletedSite;
    private RestSiteModelsCollection sites;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        regularUser = dataUser.createRandomTestUser();
        privateSiteManager = dataUser.createRandomTestUser();
        privateSiteConsumer = dataUser.createRandomTestUser();
        siteModel = new SiteModel(RandomData.getRandomName("0-PublicSite"));
        publicSite = dataSite.usingAdmin().createSite(siteModel);
        siteModel = new SiteModel(RandomData.getRandomName("0-PrivateSite"), Visibility.PRIVATE);
        privateSite = dataSite.usingAdmin().createSite(siteModel);
        siteModel = new SiteModel(RandomData.getRandomName("0-ModeratedSite"), Visibility.MODERATED);
        moderatedSite = dataSite.usingAdmin().createSite(siteModel);
        dataUser.addUserToSite(privateSiteManager, privateSite, UserRole.SiteManager);
        dataUser.addUserToSite(privateSiteConsumer, privateSite, UserRole.SiteConsumer);
        deletedSite = dataSite.usingAdmin().createPublicRandomSite();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get sites request returns status code 400 when invalid maxItems parameter is used")
    public void getSitesWithInvalidMaxItems() throws Exception
    {
        restClient.authenticateUser(regularUser).withParams("maxItems=0=09")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "0=09"));

        restClient.withParams("maxItems=A")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "A"));

        restClient.withParams("maxItems=0")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get sites request returns status code 400 when invalid skipCount parameter is used")
    public void getSitesWithInvalidSkipCount() throws Exception
    {
        restClient.authenticateUser(regularUser).withParams("skipCount=A")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "A"));

        restClient.authenticateUser(regularUser).withParams("skipCount=-1")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if User gets sites ordered by title ascending and status code is 200")
    public void getSitesOrderedByNameASC() throws Exception
    {
        sites = restClient.authenticateUser(privateSiteManager).withParams("orderBy=title ASC")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListIsNotEmpty();
        List<RestSiteModel> sitesList = sites.getEntries();
        sitesList.get(0).onModel().assertThat().field("title").is(moderatedSite.getTitle());
        sitesList.get(1).onModel().assertThat().field("title").is(privateSite.getTitle());
        sitesList.get(2).onModel().assertThat().field("title").is(publicSite.getTitle());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if a regular user gets all public and moderated sites and status code is 200")
    public void regularUserGetsOnlyPublicAndModeratedSites() throws Exception
    {
        sites = restClient.authenticateUser(regularUser).withParams("maxItems=5000")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListContains("title", publicSite.getTitle())
                .and().entriesListContains("title", moderatedSite.getTitle())
                .and().entriesListDoesNotContain("title", privateSite.getTitle());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if a member of a private site gets the private site, all public sites and all moderated sites. Verify if status code is 200")
    public void privateSiteMemberGetsSitesVisibleForHim() throws Exception
    {
        sites = restClient.authenticateUser(privateSiteConsumer).withParams("maxItems=5000")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListContains("title", publicSite.getTitle())
                .and().entriesListContains("title", moderatedSite.getTitle())
                .and().entriesListContains("title", privateSite.getTitle());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if a site is not retrieved anymore after deletion and status code is 200")
    public void checkDeletedSiteIsNotRetrieved() throws Exception
    {
        sites = restClient.authenticateUser(regularUser).withParams("maxItems=5000").withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListContains("title", deletedSite.getTitle());

        dataSite.usingAdmin().deleteSite(deletedSite);

        sites = restClient.withParams("maxItems=5000").withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListDoesNotContain("title", deletedSite.getTitle());
    }

    @AfterClass(alwaysRun=true)
    public void cleanup() throws Exception
    {
        dataSite.usingAdmin().deleteSite(moderatedSite);
        dataSite.usingAdmin().deleteSite(privateSite);
        dataSite.usingAdmin().deleteSite(publicSite);
    }
}
