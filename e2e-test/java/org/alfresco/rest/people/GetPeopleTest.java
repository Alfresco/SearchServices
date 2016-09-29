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

@Test(groups = { "rest-api", "people" })
public class GetPeopleTest extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;

    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;

    @BeforeClass
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        searchedUser = dataUser.createRandomTestUser();

        peopleApi.useRestClient(restClient);
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify manager user gets a person with Rest API and response is successful")
    public void managerUserChecksIfPersonIsPresent() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);

        restClient.authenticateUser(managerUser);
        peopleApi.getPerson(searchedUser.getUsername());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify collaborator user gets a person with Rest API and response is successful")
    public void collaboratorUserChecksIfPersonIsPresent() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);

        restClient.authenticateUser(collaboratorUser);
        peopleApi.getPerson(searchedUser.getUsername());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test(groups = "sanity")
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify contributor user gets a person with Rest API and response is successful")
    public void contributorUserChecksIfPersonIsPresent() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);

        restClient.authenticateUser(contributorUser);
        peopleApi.getPerson(searchedUser.getUsername());
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
}
