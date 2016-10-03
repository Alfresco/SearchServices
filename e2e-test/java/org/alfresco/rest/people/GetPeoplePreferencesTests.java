package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for Get Peferences (/people/{personId}/preferences) RestAPI call
 * 
 * @author Cristina Axinte
 *
 */
@Test(groups = { "rest-api", "people", "preferences", "sanity" })
public class GetPeoplePreferencesTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;
    
    UserModel userModel;
    SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        //mark site as favorite (cum fac asta? fol data prep-care face call la rest API sau pot sa fol durect restAPI necesar?

        peopleApi.useRestClient(restClient);
    }
    
    @TestRail(section = { "rest-api", "people", "preferences" }, executionType = ExecutionType.SANITY, description = "Verify manager user gets its preferences with Rest API and response is successful (200)")
    public void managerUserGetsPeoplePreferencesWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser);
        peopleApi.getPersonPreferences(managerUser).assertPreferencesListIsNotEmpty();
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
