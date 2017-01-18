package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetFavoriteSitesFullTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    SiteModel testSite1;
    SiteModel testSite2;
    private RestSiteModelsCollection restSiteModelsCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();

        siteModel = new SiteModel("A-" + RandomData.getRandomAlphanumeric());
        testSite1 = new SiteModel("B-" + RandomData.getRandomAlphanumeric());
        testSite2 = new SiteModel("C-" + RandomData.getRandomAlphanumeric());

        dataSite.usingUser(userModel).createSite(siteModel);
        dataSite.usingUser(userModel).createSite(testSite1);
        dataSite.usingUser(userModel).createSite(testSite2);

        dataSite.usingUser(userModel).usingSite(testSite2).addSiteToFavorites();
        dataSite.usingUser(userModel).usingSite(testSite1).addSiteToFavorites();
        dataSite.usingUser(userModel).usingSite(siteModel).addSiteToFavorites();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request response status using -me- string in place of personId is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userGetsFavoriteSiteWithSuccessUsingMEForRequest() throws Exception
    {
        restSiteModelsCollection = restClient.authenticateUser(userModel).withCoreAPI().usingMe().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsNotEmpty()
                .and().entriesListContains("id", siteModel.getId())
                .and().paginationExist();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request response status for a user that has no favorite sites is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void getFavoriteSiteRequestForUserWithNoFavoriteSitesIsSuccessful() throws Exception
    {
        UserModel randomTestUser = dataUser.createRandomTestUser();

        restSiteModelsCollection = restClient.authenticateUser(randomTestUser).withCoreAPI().usingMe().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request response status for a user with several favorite sites is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void getFavoriteSiteRequestForUserWithSeveralFavoriteSitesIsSuccessful() throws Exception
    {
        restSiteModelsCollection = restClient.authenticateUser(userModel).withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsNotEmpty()
                .and().entriesListContains("id", siteModel.getId())
                .and().entriesListContains("id", testSite1.getId())
                .and().entriesListContains("id", testSite2.getId())
                .and().paginationExist();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request can only be called with a positive maxItems param")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userCanNotGetFavoriteSitesWith0MaxItems() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("maxItems=0").withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError()
                    .containsErrorKey(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                    .containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                    .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                    .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request response status code for a high skipCount param is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userCanGetFavoriteSitesWithHighSkipCount() throws Exception
    {
        restSiteModelsCollection = restClient.authenticateUser(userModel).withParams("skipCount=999999999").withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsEmpty()
                                .assertThat().paginationField("skipCount").is("999999999");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin gets favorite sites for username with special chars with Rest API and response status is NOT_FOUND (404)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void getFavoriteSitesRequestForPersonIDWithSpecialCharacters() throws Exception
    {
        String randomData = RandomData.getRandomAlphanumeric();
        UserModel userSpecialChars = dataUser.usingAdmin().createUser(randomData + "~!@#$%^&[]{}|\\;':\",./<>", "password");
        //setting the encoded text for username
        userSpecialChars.setUsername(randomData + "~!%40%23%24%25%5E%26%5B%5D%7B%7D%7C%5C%3B%27%3A%22%2C.%2F%3C%3E");

        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingUser(userSpecialChars).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, randomData + "~!%40%23%24%25%5E%26%5B%5D%7B%7D%7C%5C%3B%27%3A%22%2C.%2F%3C%3E"))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that user can retrieve the last 2 favorite sites")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userCanRetrieveLast2FavoriteSites() throws Exception
    {
        restSiteModelsCollection = restClient.authenticateUser(userModel).withParams("skipCount=1&maxCount=2").
                withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsNotEmpty()
                .assertThat().entriesListCountIs(2)
                .assertThat().entriesListContains("id", testSite1.getId())
                .assertThat().entriesListContains("id", testSite2.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request applies valid properties param")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void getFavoriteSitesRequestWithValidPropertiesParam() throws Exception
    {
        RestSiteModel restSiteModel = restClient.authenticateUser(userModel).withParams("properties=title")
                .withCoreAPI().usingAuthUser().getFavoriteSites().getOneRandomEntry().onModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().fieldsCount().is(1)
            .assertThat().field("title").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request applies invalid properties param")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void getFavoriteSitesRequestWithInvalidPropertiesParam() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("properties=tas").withCoreAPI().usingAuthUser()
                .getFavoriteSites().getOneRandomEntry().onModel().assertThat().fieldsCount().is(0);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
