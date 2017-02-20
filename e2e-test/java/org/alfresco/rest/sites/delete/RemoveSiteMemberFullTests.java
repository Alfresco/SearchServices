package org.alfresco.rest.sites.delete;

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

public class RemoveSiteMemberFullTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUserModel;
    private ListUserWithRoles usersWithRoles;
    private SiteModel moderatedSiteModel;
    private SiteModel privateSiteModel;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        privateSiteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, 
                UserRole.SiteConsumer, UserRole.SiteContributor);        
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can delete a site member with Contributor role and gets status code 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void adminIsAbleToDeleteSiteMemberWithContributorRole() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel)
            .deleteSiteMember(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        dataUser.addUserToSite(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel, UserRole.SiteContributor);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can delete site member with Manager role and gets status code 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void aminIsAbleToDeleteSiteMemberWithManagerRole() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel)
            .deleteSiteMember(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        dataUser.addUserToSite(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel, UserRole.SiteManager);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can delete site member with Consumer role and gets status code 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void adminIsAbleToDeleteSiteMemberWithConsumerRole() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel)
            .deleteSiteMember(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        dataUser.addUserToSite(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel, UserRole.SiteConsumer);
    }    
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can delete a site member with Collaborator role and gets status code 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void adminIsAbleToDeleteSiteMemberWithCollaboratorRole() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel)
            .deleteSiteMember(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        dataUser.addUserToSite(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel, UserRole.SiteCollaborator);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can delete a site member of moderated site and gets status code 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void adminIsAbleToDeleteModeratedSiteMember() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        newMember.setUserRole(UserRole.SiteManager);
        dataUser.addUserToSite(newMember, moderatedSiteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).deleteSiteMember(newMember);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can delete a site member of private site and gets status code 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void adminIsAbleToDeletePrivateSiteMember() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        newMember.setUserRole(UserRole.SiteManager);
        dataUser.addUserToSite(newMember, privateSiteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).deleteSiteMember(newMember);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that regular user can not delete admin and gets status code 422")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void regularUserIsNotAbleToDeleteAdmin() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingSite(siteModel)
            .deleteSiteMember(adminUserModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError()
            .containsSummary(String.format(RestErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getTitle()))
            .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can not delete a site member twice and gets status code 404 for the second attempt")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @Bug(id="ACE-5447")
    public void adminIsNotAbleToRemoveSiteMemberTwice() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel)
            .deleteSiteMember(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel)
            .deleteSiteMember(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)); 
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
        .containsSummary(RestErrorModel.ENTITY_NOT_FOUND);
        dataUser.addUserToSite(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel, UserRole.SiteContributor);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify admin is not able to remove from site a user that created a member request that was not accepted yet")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @Bug(id="ACE-5447")
    public void adminIsNotAbleToRemoveFromSiteANonExistingMember() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).deleteSiteMember(newMember);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsSummary(RestErrorModel.ENTITY_NOT_FOUND);
    }
}