package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetFavoriteSitesCoreTests extends RestTest
{
    UserModel userModel;
    UserModel testUser1;
    SiteModel siteModel;
    private RestSiteModelsCollection restSiteModelsCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        testUser1 = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        dataSite.usingUser(userModel).usingSite(siteModel).addSiteToFavorites();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request status code is 404 for a personId that does not exist")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void getFavoriteSitesReturns404ForAPersonIdThatDoesNotExist() throws Exception
    {
        UserModel invalidUser = new UserModel(RandomData.getRandomName("User"), DataUser.PASSWORD);

        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingUser(invalidUser).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidUser.getUsername()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that getFavoriteSites request status code is 403 if the user doesn't have access to the person favorite sites")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void userCannotGetFavoriteSitesForAnotherUser() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(testUser1).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
                .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin has access to regular user site favorites")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void adminCanGetFavoriteSitesForARegularUser() throws Exception
    {
        restSiteModelsCollection = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingUser(userModel).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsNotEmpty()
                .assertThat().entriesListContains("id", siteModel.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that regular user can not see admin user site favorites")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void regularUserCannotGetFavoriteSitesForAdminUser() throws Exception
    {
        restClient.authenticateUser(testUser1).withCoreAPI().usingUser(dataUser.getAdminUser()).getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
                .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that favorite site can be removed")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void verifyThatFavoriteSiteCanBeRemoved() throws Exception
    {
        UserModel randomTestUser = dataUser.createRandomTestUser();
        dataSite.usingUser(randomTestUser).usingSite(siteModel).addSiteToFavorites();

        restSiteModelsCollection = restClient.authenticateUser(randomTestUser).withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsNotEmpty()
                .assertThat().entriesListContains("id", siteModel.getId());

        dataSite.usingUser(randomTestUser).usingSite(siteModel).removeSiteFromFavorites();
        restSiteModelsCollection = restClient.withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModelsCollection.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that if maxItems param is invalid status code returned is BAD_REQUEST (400)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void getFavoriteSitesMaxItemsInvalidValueTest() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("maxItems=-1").withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);

        restClient.authenticateUser(userModel).withParams("maxItems=abc").withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "abc"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify that if skipCount param is invalid status code returned is BAD_REQUEST (400)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void getFavoriteSitesSkipCountInvalidValueTest() throws Exception
    {
        restClient.authenticateUser(userModel).withParams("skipCount=-1").withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT);

        restClient.authenticateUser(userModel).withParams("skipCount=abc").withCoreAPI().usingAuthUser().getFavoriteSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"));
    }
}
