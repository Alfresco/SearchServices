package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.PreferenceName;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetPeoplePreferenceCoreTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        dataSite.usingUser(userModel).usingSite(siteModel).addSiteToFavorites();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.REGRESSION, description = "Verify user is Forbidden to get a specific preference for another user with Rest API and response is 403")
    public void userDoesNotHaveAccessToFavoriteSitesOfAnotherUser() throws Exception
    {
        UserModel secondUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(secondUser).withCoreAPI().usingUser(userModel)
                .getPersonPreferenceInformation(PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
        restClient.assertLastError().containsSummary("Permission was denied");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.REGRESSION, description = "Verify manager fails to get a specific preference for an invalid user with Rest API and response is 404")
    public void statusNotFoundIsReturnedForAPersonIDThatDoesNotExist() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(new UserModel("invalidPersonID", "password"))
                .getPersonPreferenceInformation(PreferenceName.SITES_FAVORITES_PREFIX + siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        restClient.assertLastError().containsSummary("The entity with id: invalidPersonID was not found");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.REGRESSION, description = "Verify manager fails to get a specific preference for an invalid preference with Rest API and response is 404")
    public void statusNotFoundIsReturnedForAPreferenceNameThatDoesNotExist() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI().usingAuthUser().getPersonPreferenceInformation("invalidPreferenceName");
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        restClient.assertLastError().containsSummary(
                String.format("The relationship resource was not found for the" + " entity with id: %s and a relationship id of invalidPreferenceName",
                        userModel.getUsername()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.PREFERENCES }, executionType = ExecutionType.REGRESSION, description = "Verify manager fails to get a specific preference for an removed preference with Rest API and response is 404")
    public void statusNotFoundIsReturnedForAPreferenceNameThatHasBeenRemoved() throws Exception
    {
        SiteModel preferenceSite = dataSite.usingUser(userModel).createPublicRandomSite();
        dataSite.usingUser(userModel).usingSite(preferenceSite).addSiteToFavorites();
        dataSite.usingUser(userModel).usingSite(preferenceSite).removeSiteFromFavorites();
        restClient.authenticateUser(userModel).withCoreAPI().usingAuthUser()
                .getPersonPreferenceInformation(PreferenceName.SITES_FAVORITES_PREFIX + preferenceSite.getId());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        restClient.assertLastError().containsSummary(
                String.format("The relationship resource was not found for the" + " entity with id: %s and a relationship id of %s", userModel.getUsername(),
                        PreferenceName.SITES_FAVORITES_PREFIX + preferenceSite.getId()));
    }
}
