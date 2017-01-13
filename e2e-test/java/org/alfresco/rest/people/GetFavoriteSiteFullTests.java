package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Bogdan Bocancea
 */
public class GetFavoriteSiteFullTests extends RestTest
{
    UserModel userModel;
    SiteModel site1;
    private RestSiteModel restSiteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        site1 = dataSite.usingUser(userModel).createPublicRandomSite();
        dataSite.usingUser(userModel).usingSite(site1).addSiteToFavorites();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify invalid request returns status 404 when personId is invalid")
    public void getFavoriteSiteWithInvalidPersonId() throws Exception
    {
        UserModel userSpecialChars = dataUser.usingAdmin().createUser(RandomStringUtils.randomAlphabetic(2) + "~!@#$%^&*()_+[]{}|;'d", "password");
        userSpecialChars.setUsername(RandomStringUtils.randomAlphabetic(2) + "~!%40%23%24%25%5E%26*()_%2B%5B%5D%7B%7D%7C%5C%3B%27%3A%22%2C.%2F%3C%3E");

        restClient.authenticateUser(userModel).withCoreAPI().usingUser(userSpecialChars).getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, userSpecialChars.getUsername()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify request with empty site id")
    public void getFavoriteSiteWithEmptySiteId() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI();
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "people/{personId}/favorite-sites/{siteId}?{parameters}", 
                    userModel.getUsername(), "", restClient.getParameters());
        RestSiteModelsCollection sites = restClient.processModels(RestSiteModelsCollection.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListIsNotEmpty();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify invalid request returns status 401 when user is empty")
    public void getFavoriteSiteWithEmptyPersonId() throws Exception
    {
        UserModel emptyUser = new UserModel("", "password");
        restClient.authenticateUser(emptyUser).withCoreAPI().usingAuthUser().getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify invalid request returns status 404 when another user get favorite site of an user")
    public void getFavoriteSiteWithAnotherUser() throws Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        restClient.authenticateUser(user).withCoreAPI().usingAuthUser().getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, user.getUsername(), site1.getId()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify request returns status 200 when using -me-")
    public void getFavoriteSiteUsingMe() throws Exception
    {
        restSiteModel = restClient.authenticateUser(userModel).withCoreAPI().usingMe().getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("id").is(site1.getId())
            .and().field("title").is(site1.getTitle());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify entry details for get site favorite response with Rest API")
    public void checkResponseSchemaForGetSiteFavorite() throws Exception
    {
        restSiteModel = restClient.authenticateUser(userModel).withCoreAPI().usingAuthUser().getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("id").is(site1.getId())
                            .and().field("title").is(site1.getTitle())
                            .and().field("role").is(UserRole.SiteManager.toString())
                            .and().field("visibility").is(Visibility.PUBLIC.toString())
                            .and().field("guid").isNotEmpty()
                            .and().field("description").is(site1.getDescription())
                            .and().field("preset").is("site-dashboard");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify invalid request returns status 404 when providing folder name instead of site id")
    public void getFavoriteSiteUsingFolder() throws Exception
    {
        FolderModel folder = dataContent.usingUser(userModel).usingSite(site1).createFolder();
        SiteModel siteFolder = new SiteModel(folder.getName());
        restSiteModel = restClient.authenticateUser(userModel).withCoreAPI().usingMe().getFavoriteSite(siteFolder);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, userModel.getUsername(), folder.getName()));
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify request returns status 200 when using valid parameters")
    public void getFavoriteSiteUsingParameters() throws Exception
    {
        restSiteModel = restClient.withParams("maxItems=100").authenticateUser(userModel).withCoreAPI().usingMe().getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("id").is(site1.getId())
            .and().field("title").is(site1.getTitle());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify request returns status 400 when using maxItems=0")
    public void getFavoriteSiteUsingInvalidMaxItems() throws Exception
    {
        restSiteModel = restClient.withParams("maxItems=0").authenticateUser(userModel).withCoreAPI().usingMe().getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
        description = "Verify request returns status 401 when using invalid network")
    public void getFavoriteSiteUsingInvalidNetwork() throws Exception
    {
        UserModel invalidUserNetwork = dataUser.createRandomTestUser();
        SiteModel site = dataSite.usingUser(invalidUserNetwork).createPublicRandomSite();
        dataSite.usingUser(invalidUserNetwork).usingSite(site).addSiteToFavorites();
        invalidUserNetwork.setDomain("invalidNetwork");
        restSiteModel = restClient.authenticateUser(invalidUserNetwork).withCoreAPI().usingMe().getFavoriteSite(site);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
            .assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
}
