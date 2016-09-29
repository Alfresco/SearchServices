package org.alfresco.rest.demo;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "demo" })
public class SamplePeopleTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleAPI;

    @Autowired
    DataUser dataUser;

    private UserModel userModel;
    private UserModel adminUser;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        peopleAPI.useRestClient(restClient);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Verify admin user gets person with Rest API and response is not empty")
    public void adminShouldRetrievePerson() throws Exception
    {
        peopleAPI.getPerson(userModel).assertResponseIsNotEmpty();

        peopleAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description = "Admin user gets own person information with Rest Api and assert that name is correct")
    public void adminShouldRetrieveItself() throws Exception
    {
        peopleAPI.getPerson(adminUser).assertPersonHasName(adminUser);
    }

}