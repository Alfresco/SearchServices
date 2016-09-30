package org.alfresco.rest.people;

import org.alfresco.dataprep.CMISUtil.DocumentType;
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
 * @author Cristina Axinte
 * 
 * Tests for getActivities (/people/{personId}/activities) RestAPI call
 * 
 */
@Test(groups = { "rest-api", "people", "activities" })
public class GetPeopleActivitiesTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;

    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify manager user gets a person with Rest API and response is successful")
    public void managerUserChecksIfPersonIsPresent() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataContent.usingUser(managerUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(managerUser);
        peopleApi.getPersonActivities(managerUser);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
