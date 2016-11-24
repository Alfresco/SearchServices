package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
public class DeleteSiteMemberCoreTests extends RestTest
{
    
    private UserModel userModel;
    private UserModel managerModel;
    private SiteModel publicSiteModel;
    private SiteModel moderatedSiteModel;
    private SiteModel privateSiteModel;
    private UserModel adminUserModel;
    private UserModel secondUserModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        secondUserModel = dataUser.createRandomTestUser();
        secondUserModel.setUserRole(UserRole.SiteCollaborator);
        userModel = dataUser.createRandomTestUser();
        userModel.setUserRole(UserRole.SiteCollaborator);
        managerModel = dataUser.createRandomTestUser();
        managerModel.setUserRole(UserRole.SiteManager);
        adminUserModel = dataUser.getAdminUser();
        publicSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        privateSiteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user removes a site member using '-me-' in place of personId and response is successful (204)")
    public void removeSiteMemberWithSuccessUsingMeAsPersonId() throws Exception
    {          
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(userModel);

        restClient.authenticateUser(userModel).withCoreAPI().usingMe().deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user can't remove a site membership if doesn't have access to personId and response is 422")
    public void notAbleToRemoveSiteMemberWithNoAccessToPersonId() throws Exception
    {  
        UserModel secondManager = dataUser.createRandomTestUser();
        dataSite.usingUser(secondManager).createPublicRandomSite();
         
        userModel.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(userModel);

        restClient.authenticateUser(secondManager).withCoreAPI().usingUser(userModel).deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, publicSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user can't remove a site membership of an inexistent uer and response is 404")
    public void userIsNotAbleToRemoveSiteMemberOfAnInexistentUser() throws Exception
    {           
        UserModel inexistentUser = new UserModel("inexistenUser", "password");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(inexistentUser).deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, inexistentUser.getUsername()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a membership of an inexistent site and response is 404")
    public void userIsNotAbleToRemoveMembershipOfInexistentSite() throws Exception
    {
        SiteModel inexistentSite = new SiteModel("inexistentSite");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(inexistentSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(ErrorModel.RELATIONSHIP_NOT_FOUND, userModel.getUsername(), inexistentSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a membership of an empty site id and response is 404")
    public void userIsNotAbleToRemoveMembershipOfEmptySiteId() throws Exception
    {
        SiteModel inexistentSite = new SiteModel("");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(inexistentSite);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(ErrorModel.DELETE_EMPTY_ARGUMENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a membership of another regular user from public site and response is 422")
    public void regularUserIsNotAbleToRemoveRegularUserSiteMembershipFromPublicSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(secondUserModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(userModel);
        restClient.authenticateUser(secondUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, publicSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a membership of another regular user from moderated site and response is 422")
    public void regularUserIsNotAbleToRemoveRegularUserSiteMembershipFromModeratedSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(secondUserModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(userModel);
        restClient.authenticateUser(secondUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(moderatedSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, moderatedSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a membership of another regular user from private site and response is 422")
    public void regularUserIsNotAbleToRemoveRegularUserSiteMembershipFromPrivateSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(userModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(secondUserModel);
        restClient.authenticateUser(secondUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(privateSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, privateSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is able to remove the membership of another regular user from public site and response is 204")
    public void adminIsAbleToRemoveRegularUserSiteMembershipFromPublicSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(userModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is able to remove the membership of another regular user from moderated site and response is 204")
    public void adminIsAbleToRemoveRegularUserSiteMembershipFromModeratedSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(userModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(moderatedSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is able to remove the membership of another regular user from private site and response is 204")
    public void adminIsAbleToRemoveRegularUserSiteMembershipFromPrivateSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(userModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(userModel).deleteSiteMember(privateSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove the membership of admin from public site and response is 422")
    public void regularUserIsNotAbleToRemoveAdminSiteMembershipFromPublicSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(managerModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(userModel);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(adminUserModel).deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, publicSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove the membership of admin from moderated site and response is 422")
    public void regularUserIsNotAbleToRemoveAdminSiteMembershipFromModeratedSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(managerModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(userModel);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(adminUserModel).deleteSiteMember(moderatedSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, moderatedSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove the membership of admin from private site and response is 422")
    public void regularUserIsNotAbleToRemoveAdminSiteMembershipFromPrivateSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(managerModel);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(userModel);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(adminUserModel).deleteSiteMember(privateSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, privateSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is able to remove the membership of manager of a public site and response is 204")
    public void adminIsAbleToRemoveManagerSiteMembershipOfPublicSite() throws Exception
    {
        UserModel secondManager = dataUser.createRandomTestUser();
        secondManager.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(secondManager);
        restClient.withCoreAPI().usingSite(publicSiteModel).addPerson(managerModel);
        restClient.withCoreAPI().usingUser(managerModel).deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is able to remove the membership of manager of a moderated site and response is 204")
    public void adminIsAbleToRemoveManagerSiteMembershipOfModeratedSite() throws Exception
    {
        UserModel secondManager = dataUser.createRandomTestUser();
        secondManager.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(secondManager);
        restClient.withCoreAPI().usingSite(moderatedSiteModel).addPerson(managerModel);
        restClient.withCoreAPI().usingUser(managerModel).deleteSiteMember(moderatedSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is able to remove the membership of manager of a private site and response is 204")
    public void adminIsAbleToRemoveManagerSiteMembershipOfPrivateSite() throws Exception
    {
        UserModel secondManager = dataUser.createRandomTestUser();
        secondManager.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(secondManager);
        restClient.withCoreAPI().usingSite(privateSiteModel).addPerson(managerModel);
        restClient.withCoreAPI().usingUser(managerModel).deleteSiteMember(privateSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify collaborator is not able to remove the membership of manager of a public site and response is 204")
    public void userIsNotAbleToRemoveManagerSiteMembershipOfPublicSite() throws Exception
    {
        UserModel secondManager = dataUser.createRandomTestUser();
        secondManager.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(secondManager);
        restClient.withCoreAPI().usingSite(publicSiteModel).addPerson(userModel);
        restClient.withCoreAPI().usingSite(publicSiteModel).addPerson(managerModel);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(managerModel).deleteSiteMember(publicSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, publicSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify collaborator is not able to remove the membership of manager of a moderated site and response is 204")
    public void userIsNotAbleToRemoveManagerSiteMembershipOfModeratedSite() throws Exception
    {
        UserModel secondManager = dataUser.createRandomTestUser();
        secondManager.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(secondManager);
        restClient.withCoreAPI().usingSite(publicSiteModel).addPerson(userModel);
        restClient.withCoreAPI().usingSite(moderatedSiteModel).addPerson(managerModel);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(managerModel).deleteSiteMember(moderatedSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, moderatedSiteModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify collaborator is not able to remove the membership of manager of a private site and response is 204")
    public void userIsNotAbleToRemoveManagerSiteMembershipOfPrivateSite() throws Exception
    {
        UserModel secondManager = dataUser.createRandomTestUser();
        secondManager.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(secondManager);
        restClient.withCoreAPI().usingSite(privateSiteModel).addPerson(userModel);
        restClient.withCoreAPI().usingSite(privateSiteModel).addPerson(managerModel);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(managerModel).deleteSiteMember(privateSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, privateSiteModel.getId()));
    }
}
