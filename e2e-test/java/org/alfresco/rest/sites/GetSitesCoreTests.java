package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ErrorModel;
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

import java.util.List;

/**
 * Created by Claudia Agache on 11/22/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
public class GetSitesCoreTests extends RestTest
{
    private UserModel userModel, privateSiteManager, privateSiteConsumer;
    private SiteModel publicSite, privateSite, moderatedSite, deletedSite;
    private RestSiteModelsCollection sites;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
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

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify invalid request returns status code 400 for invalid maxItems")
    public void checkStatusCodeForInvalidMaxItems() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("maxItems=0")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(ErrorModel.INVALID_ARGUMENT, "argument"));
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify invalid request returns status code 400 for invalid skipCount")
    public void checkStatusCodeForInvalidSkipCount() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("skipCount=A")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(ErrorModel.INVALID_ARGUMENT, "argument"));
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify User gets sites ordered by name ascendant and status code is 200")
    public void getSitesOrderedByNameASC() throws Exception
    {
        sites = restClient.authenticateUser(privateSiteManager).withParams("orderBy=name ASC")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListIsNotEmpty();
        List<RestSiteModel> sitesList = sites.getEntries();
        sitesList.get(0).onModel().assertThat().field("title").is(moderatedSite.getTitle());
        sitesList.get(1).onModel().assertThat().field("title").is(privateSite.getTitle());
        sitesList.get(2).onModel().assertThat().field("title").is(publicSite.getTitle());
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets all public and moderated sites and status code is 200")
    public void userGetsOnlyPublicAndModeratedSites() throws Exception
    {
        sites = restClient.authenticateUser(userModel).withParams("maxItems=5000")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListContains("title", publicSite.getTitle())
                .and().entriesListContains("title", moderatedSite.getTitle())
                .and().entriesListDoesNotContain("title", privateSite.getTitle());
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if a member of a private site gets the private site, all public sites and all moderated sites. Verify if status code is 200")
    public void userGetsSitesVisibleForHim() throws Exception
    {
        sites = restClient.authenticateUser(privateSiteConsumer).withParams("maxItems=5000")
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListContains("title", publicSite.getTitle())
                .and().entriesListContains("title", moderatedSite.getTitle())
                .and().entriesListContains("title", privateSite.getTitle());
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if a site is not retrieved anymore after deletion and status code is 200")
    public void checkDeletedSiteIsNotRetrieved() throws Exception
    {
        sites = restClient.authenticateUser(userModel).withParams("maxItems=5000").withCoreAPI().getSites();
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
