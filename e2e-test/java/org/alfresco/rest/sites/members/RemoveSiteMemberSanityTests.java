package org.alfresco.rest.sites.members;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
public class RemoveSiteMemberSanityTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUserModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Verify that site manager can delete site member and gets status code 204, 'No Content'")
    public void siteManagerIsAbleToDeleteSiteMemberWithConsumerRole() throws Exception
    {
        UserModel testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListDoesNotContain("id", testUserModel.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Verify that site collaborator cannot delete site member and gets status code 403, 'Forbidden'")
    public void siteCollaboratorIsNotAbleToDeleteSiteMemberWithConsumerRole() throws Exception
    {
        UserModel testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY);

        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListContains("id", testUserModel.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Verify that site contributor cannot delete site member and gets status code 403, 'Forbidden'")
    public void siteContributorIsNotAbleToDeleteSiteMemberWithConsumerRole() throws Exception
    {
        UserModel testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY);

        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListContains("id", testUserModel.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Verify that site consumer cannot delete site member and gets status code 403, 'Forbidden'")
    public void siteConsumerIsNotAbleToDeleteSiteMemberWithConsumerRole() throws Exception
    {
        UserModel testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingSite(siteModel)
                .deleteSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY);

        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListContains("id", testUserModel.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Verify that admin user can delete site member and gets status code 204, 'No Content'")
    public void adminUserIsAbleToDeleteSiteMember() throws Exception
    {
        UserModel testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).deleteSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListDoesNotContain("id", testUserModel.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Verify that unauthenticated user is not able to delete site member")
    @Bug(id = "MNT-16904", description = "It fails only on environment with tenants")
    public void unauthenticatedUserIsNotAuthorizedToDeleteSiteMember() throws Exception
    {
        UserModel testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);
        
        UserModel inexistentUser = new UserModel("inexistent user", "inexistent password");
        restClient.authenticateUser(inexistentUser).withCoreAPI().usingSite(siteModel).deleteSiteMember(testUserModel);

        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
}
