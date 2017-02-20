package org.alfresco.rest.favorites.get;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 11/22/2016.
 */

public class GetFavoriteSiteCoreTests extends RestTest
{
    UserModel userModel;
    SiteModel site1;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        site1 = dataSite.usingUser(userModel).createPublicRandomSite();
        dataSite.usingUser(userModel).usingSite(site1).addSiteToFavorites();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify invalid request returns status 404 when personId does not exist")
    public void getFavoriteSiteWithNonExistentPersonId() throws Exception
    {
        UserModel someUser = new UserModel("someUser", DataUser.PASSWORD);

        restClient.authenticateUser(userModel).withCoreAPI().usingUser(someUser).getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "someUser"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify invalid request returns status 404 when siteId does not exist")
    public void getFavoriteSiteWithNonExistentSiteId() throws Exception
    {
        SiteModel nonExistentSite = new SiteModel("nonExistentSite");

        restClient.authenticateUser(userModel).withCoreAPI().usingAuthUser().getFavoriteSite(nonExistentSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, userModel.getUsername(), nonExistentSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify User fails to get specific favorite site of admin with Rest API and response is 403")
    public void userFailsToGetFavoriteSiteOfAdmin() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(dataUser.getAdminUser()).getFavoriteSite(site1);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
}
