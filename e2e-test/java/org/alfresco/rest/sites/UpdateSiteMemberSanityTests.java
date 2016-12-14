package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateSiteMemberSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel testUserModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that manager is able to update site member and gets status code OK (200)")
    public void managerIsAbleToUpdateSiteMember() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        testUserModel.setUserRole(UserRole.SiteConsumer);
        restClient.withCoreAPI().usingSite(siteModel).updateSiteMember(testUserModel)
              .assertThat().field("id").is(testUserModel.getUsername())
              .and().field("role").is(testUserModel.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id="ACE-5444")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that collaborator is not able to update site member and gets status code FORBIDDEN (403)")
    public void collaboratorIsNotAbleToUpdateSiteMember() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        testUserModel.setUserRole(UserRole.SiteCollaborator);
        restClient.withCoreAPI().usingSite(siteModel).updateSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
        restClient.assertLastError()
            .containsSummary(String.format("The current user does not have permissions to modify the membership details of the site %s.", siteModel.getTitle()));        
    }
    
    @Bug(id="ACE-5444")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that contributor is not able to update site member and gets status code FORBIDDEN (403)")
    public void contributorIsNotAbleToUpdateSiteMember() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        testUserModel.setUserRole(UserRole.SiteCollaborator);
        restClient.withCoreAPI().usingSite(siteModel).updateSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
        restClient.assertLastError()
        .containsSummary(String.format("The current user does not have permissions to modify the membership details of the site %s.", siteModel.getTitle()));        
    }
    
    @Bug(id="ACE-5444")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that consumer is not able to update site member and gets status code FORBIDDEN (403)")
    public void consumerIsNotAbleToUpdateSiteMember() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        testUserModel.setUserRole(UserRole.SiteCollaborator);
        restClient.withCoreAPI().usingSite(siteModel).updateSiteMember(testUserModel);
        restClient.assertLastError()
        .containsSummary(String.format("The current user does not have permissions to modify the membership details of the site %s.", siteModel.getTitle()));
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that admin is able to update site member and gets status code OK (200)")
    public void adminIsAbleToUpdateSiteMember() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        testUserModel.setUserRole(UserRole.SiteCollaborator);
        restClient.withCoreAPI().usingSite(siteModel).updateSiteMember(testUserModel)
               .assertThat().field("id").is(testUserModel.getUsername())
               .and().field("role").is(testUserModel.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that unauthenticated user is not able to update site member")
    @Bug(id="MNT-16904")
    public void unauthenticatedUserIsNotAuthorizedToUpdateSiteMmeber() throws Exception{
        UserModel inexistentUser = new UserModel("inexistent user", "inexistent password");
        restClient.authenticateUser(inexistentUser);
        testUserModel.setUserRole(UserRole.SiteCollaborator);
        restClient.withCoreAPI().usingSite(siteModel).updateSiteMember(testUserModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastExceptionContains(HttpStatus.UNAUTHORIZED.toString());
    }
}
