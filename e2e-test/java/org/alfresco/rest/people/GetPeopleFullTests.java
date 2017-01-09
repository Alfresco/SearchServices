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

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify entry details for get person response with Rest API")
    public void checkResponseSchemaForGetPerson() throws Exception
    {
        RestPersonModel newUser = RestPersonModel.getRandomPersonModel("aspectNames", "avatarId", "statusUpdatedAt");
        newUser = restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().createPerson(newUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        personModel = restClient.authenticateUser(userModel).withCoreAPI().usingUser(new UserModel(newUser.getId(), newUser.getPassword())).getPerson();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        personModel.assertThat().field("id").is(newUser.getId())
            .and().field("firstName").is(newUser.getFirstName())
            .and().field("lastName").is(newUser.getLastName())
            .and().field("description").is(newUser.getDescription())
            .and().field("email").is(newUser.getEmail())
            .and().field("skypeId").is(newUser.getSkypeId())
            .and().field("googleId").is(newUser.getGoogleId())
            .and().field("instantMessageId").is(newUser.getInstantMessageId())
            .and().field("jobTitle").is(newUser.getJobTitle())
            .and().field("location").is(newUser.getLocation())
            .and().field("mobile").is(newUser.getMobile())
            .and().field("telephone").is(newUser.getTelephone())
            .and().field("userStatus").is(newUser.getUserStatus())
            .and().field("enabled").is(newUser.getEnabled())
            .and().field("emailNotificationsEnabled").is(newUser.getEmailNotificationsEnabled());
    }
}
