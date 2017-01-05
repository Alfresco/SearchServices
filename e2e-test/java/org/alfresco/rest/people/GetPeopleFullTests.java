package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestPersonModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetPeopleFullTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;
    UserModel adminUser;
    private RestPersonModel personModel;
    private String domain = "@tas-automation.org";

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        searchedUser = dataUser.createRandomTestUser();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user gets person using '-me-' instead of personId with Rest API and response is successful")
    public void checkGetPersonIsSuccessfulForMe() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);

        personModel = restClient.authenticateUser(managerUser).withCoreAPI().usingMe().getPerson();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        personModel.assertThat().field("id").is(managerUser.getUsername())
                    .and().field("firstName").is(managerUser.getUsername() + " FirstName")
                    .and().field("email").is(managerUser.getUsername() + domain)
                    .and().field("emailNotificationsEnabled").is("true");

    }
}
