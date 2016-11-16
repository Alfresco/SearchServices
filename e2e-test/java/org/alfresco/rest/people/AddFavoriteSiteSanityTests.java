package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
public class AddFavoriteSiteSanityTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify manager user add a favorite site with Rest API and response is successful (201)")
    public void managerUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);

        restClient.authenticateUser(managerUser)
                  .onCoreAPI()                  
                  .usingUser(managerUser)
                  .addFavoriteSite(siteModel)
                  .assertThat().field("id").is(siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.onCoreAPI().usingUser(managerUser).addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT);        
        restClient.assertLastError().containsSummary(String.format("%s is already a favourite site", siteModel.getId()));        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify collaborator user add a favorite site with Rest API and response is successful (201)")
    public void collaboratorUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);

        restClient.authenticateUser(collaboratorUser)
                  .onCoreAPI()
                  .usingAuthUser()
                  .addFavoriteSite(siteModel)
                  .assertThat().field("id").is(siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify contributor user add a favorite site with Rest API and response is successful (201)")
    public void contributorUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);

        restClient.authenticateUser(contributorUser)
                  .onCoreAPI()
                  .usingAuthUser()
                  .addFavoriteSite(siteModel)
                  .assertThat().field("id").is(siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify consumer user add a favorite site with Rest API and response is successful (201)")
    public void consumerUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);

        restClient.authenticateUser(consumerUser)
                  .onCoreAPI()
                  .usingAuthUser()
                  .addFavoriteSite(siteModel)
                  .assertThat().field("id").is(siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify admin user add a favorite site with Rest API and response is successful (201)")
    public void adminUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();

        restClient.authenticateUser(adminUser)
                  .onCoreAPI()
                  .usingAuthUser()
                  .addFavoriteSite(siteModel)
                  .assertThat().field("id").is(siteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify a manager user is NOT Authorized to add a favorite site with Rest API when authentication fails (401)")
    public void managerUserNotAuthorizedFailsToAddFavoriteSite() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        managerUser.setPassword("newpassword");

        restClient.authenticateUser(managerUser)
                  .onCoreAPI()
                  .usingAuthUser()
                  .addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
