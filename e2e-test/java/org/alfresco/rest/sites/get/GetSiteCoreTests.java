package org.alfresco.rest.sites.get;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 11/23/2016.
 */
public class GetSiteCoreTests extends RestTest
{
    private UserModel userModel, privateSiteConsumer;
    private SiteModel publicSite, privateSite, moderatedSite;
    private RestSiteModel restSiteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        privateSiteConsumer = dataUser.createRandomTestUser();
        publicSite = dataSite.usingAdmin().createPublicRandomSite();
        privateSite = dataSite.usingAdmin().createPrivateRandomSite();
        moderatedSite = dataSite.usingAdmin().createModeratedRandomSite();
        dataUser.addUserToSite(privateSiteConsumer, privateSite, UserRole.SiteConsumer);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify invalid request returns status code 404 if siteId does not exist")
    public void checkStatusCodeForNonExistentSiteId() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite("NonExistentSiteId").getSite();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "NonExistentSiteId"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify user gets all public and moderated sites if an empty siteId is provided")
    public void checkStatusCodeForEmptySiteId() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI();
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "sites/{siteId}", "");
        RestSiteModelsCollection sites = restClient.processModels(RestSiteModelsCollection.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets public site details and status code is 200")
    public void getPublicSite() throws Exception
    {
        restSiteModel = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(publicSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("visibility").is(publicSite.getVisibility())
                .and().field("id").is(publicSite.getId())
                .and().field("description").is(publicSite.getDescription())
                .and().field("title").is(publicSite.getTitle())
                .and().field("guid").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets moderated site details and status code is 200")
    public void getModeratedSite() throws Exception
    {
        restSiteModel = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("visibility").is(moderatedSite.getVisibility())
                .and().field("id").is(moderatedSite.getId())
                .and().field("description").is(moderatedSite.getDescription())
                .and().field("title").is(moderatedSite.getTitle())
                .and().field("guid").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if member of a private site gets that site details and status code is 200")
    public void getPrivateSiteBySiteMember() throws Exception
    {
        restSiteModel = restClient.authenticateUser(privateSiteConsumer).withCoreAPI()
                .usingSite(privateSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("visibility").is(privateSite.getVisibility())
                .and().field("id").is(privateSite.getId())
                .and().field("description").is(privateSite.getDescription())
                .and().field("title").is(privateSite.getTitle())
                .and().field("guid").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if user that is not member of a private site does not get that site details and status code is 200")
    public void getPrivateSiteByNotASiteMember() throws Exception
    {
        restSiteModel = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(privateSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, privateSite.getTitle()));
    }

}
