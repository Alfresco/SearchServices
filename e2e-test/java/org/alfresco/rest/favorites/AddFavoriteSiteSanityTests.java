package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestFavoriteSiteModel;
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

public class AddFavoriteSiteSanityTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;
    private RestFavoriteSiteModel restFavoriteSiteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify manager user add a favorite site with Rest API and response is successful (201)")
    public void managerUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);

        restFavoriteSiteModel = restClient.authenticateUser(managerUser).withCoreAPI().usingUser(managerUser).addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(siteModel.getId());

        restClient.withCoreAPI().usingUser(managerUser).addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT);
        restClient.assertLastError().containsSummary(String.format("%s is already a favourite site", siteModel.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify collaborator user add a favorite site with Rest API and response is successful (201)")
    public void collaboratorUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);

        restFavoriteSiteModel = restClient.authenticateUser(collaboratorUser).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(siteModel.getId());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify contributor user add a favorite site with Rest API and response is successful (201)")
    public void contributorUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);

        restFavoriteSiteModel = restClient.authenticateUser(contributorUser).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(siteModel.getId());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify consumer user add a favorite site with Rest API and response is successful (201)")
    public void consumerUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);

        restFavoriteSiteModel = restClient.authenticateUser(consumerUser).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(siteModel.getId());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify admin user add a favorite site with Rest API and response is successful (201)")
    public void adminUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();

        restFavoriteSiteModel = restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(siteModel.getId());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify a manager user is NOT Authorized to add a favorite site with Rest API when authentication fails (401)")
    @Bug(id="MNT-16904", description = "It fails only on environment with tenants")
    public void managerUserNotAuthorizedFailsToAddFavoriteSite() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        managerUser.setPassword("newpassword");

        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
}
