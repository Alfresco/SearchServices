package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
public class RemoveSiteMemberCoreTests extends RestTest
{    
    private SiteModel siteModel;
    private UserModel adminUserModel;
    private ListUserWithRoles usersWithRoles;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, 
                UserRole.SiteConsumer, UserRole.SiteContributor);        
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager can NOT delete site member for an inexistent user and gets status code 404, 'Not Found'")
    public void managerIsNotAbleToDeleteInexistentSiteMember() throws Exception
    {
        UserModel inexistentUser = new UserModel("inexistentUser", "password");
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(inexistentUser);        
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, inexistentUser.getUsername()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager can NOT delete site member for a non site member and gets status code 400, 'Bad Request'")
    public void managerIsNotAbleToDeleteNotSiteMember() throws Exception
    {
        UserModel nonMember = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(nonMember);        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(ErrorModel.INVALID_ARGUMENT, "argument"));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager can NOT delete site member for an invalid site and gets status code 404, 'Not Found'")
    public void managerIsNotAbleToDeleteSiteMemberOfInvalidSite() throws Exception
    {
        SiteModel invalidSite = new SiteModel("invalidSite");
        UserModel testUserModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUserModel, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(invalidSite).deleteSiteMember(testUserModel);        
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(ErrorModel.RELATIONSHIP_NOT_FOUND, testUserModel.getUsername(), invalidSite.getId()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that manager can delete a site member with manager role")
    public void siteManagerIsAbleToDeleteSiteMemberWithManagerRole() throws Exception
    {
        UserModel anothermanager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(anothermanager, siteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingSite(siteModel).deleteSiteMember(anothermanager);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListDoesNotContain("id", anothermanager.getUsername());
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager can delete site member using \"-me-\" in place of personId")
    public void managerIsAbleToDeleteSiteMemberUsingMe() throws Exception
    {
        UserModel manager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(manager, siteModel, UserRole.SiteManager);
        UserModel meUser = new UserModel("-me-", "password");
        
        restClient.authenticateUser(manager);
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(meUser);        
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListDoesNotContain("id", manager.getUsername());
    }
         
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site collaborator cannot delete a site member with Manager role and gets status code 422")
    public void siteCollaboratorIsNotAbleToDeleteSiteMemberWithManagerRole() throws Exception
    {
        UserModel managerForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(managerForDelete, siteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(managerForDelete);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));

    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site contributor cannot delete site member with Manager role and gets status code 422")
    public void siteContributorIsNotAbleToDeleteSiteMemberWithManagerRole() throws Exception
    {
        UserModel managerForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(managerForDelete, siteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));        
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(managerForDelete);        
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site consumer cannot delete site member with Manager role and gets status code 422")
    public void siteConsumerIsNotAbleToDeleteSiteMemberWithManagerRole() throws Exception
    {
        UserModel managerForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(managerForDelete, siteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));        
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(managerForDelete);        
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }    
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site collaborator cannot delete a site member with Contributor role and gets status code 422")
    public void siteCollaboratorIsNotAbleToDeleteSiteMemberWithContributorRole() throws Exception
    {
        UserModel contributorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(contributorForDelete, siteModel, UserRole.SiteContributor);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(contributorForDelete);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site contributor cannot delete site member with Contributor role and gets status code 422")
    public void siteContributorIsNotAbleToDeleteSiteMemberWithContributorRole() throws Exception
    {
        UserModel contributorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(contributorForDelete, siteModel, UserRole.SiteContributor);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));        
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(contributorForDelete);        
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }
      
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site consumer cannot delete site member with Contributor role and gets status code 422")
    public void siteConsumerIsNotAbleToDeleteSiteMemberWithContributorRole() throws Exception
    {
        UserModel contributorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(contributorForDelete, siteModel, UserRole.SiteContributor);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));        
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(contributorForDelete);        
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager can delete a site member with Contributor role")
    public void siteManagerIsAbleToDeleteSiteMemberWithContributorRole() throws Exception
    {
        UserModel contributorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(contributorForDelete, siteModel, UserRole.SiteContributor);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingSite(siteModel).deleteSiteMember(contributorForDelete);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListDoesNotContain("id", contributorForDelete.getUsername());
    }
    
//    delete collaborator user by consumer user(2.0) 
//    delete collaborator user by contributor user(0.5) 
//    delete collaborator user by collaborator user(0.5) 
//    delete collaborator user by manager user(2.0)
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site collaborator cannot delete a site member with Collaborator role and gets status code 422")
    public void siteCollaboratorIsNotAbleToDeleteSiteMemberWithCollaboratorRole() throws Exception
    {
        UserModel collaboratorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(collaboratorForDelete, siteModel, UserRole.SiteCollaborator);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(collaboratorForDelete);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site contributor cannot delete site member with Collaborator role and gets status code 422")
    public void siteContributorIsNotAbleToDeleteSiteMemberWithCollaboratorRole() throws Exception
    {
        UserModel collaboratorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(collaboratorForDelete, siteModel, UserRole.SiteCollaborator);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));        
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(collaboratorForDelete);        
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }
      
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that site consumer cannot delete site member with Collaborator role and gets status code 422")
    public void siteConsumerIsNotAbleToDeleteSiteMemberWithCollaboratorRole() throws Exception
    {
        UserModel collaboratorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(collaboratorForDelete, siteModel, UserRole.SiteCollaborator);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));        
        restClient.withCoreAPI().usingSite(siteModel).deleteSiteMember(collaboratorForDelete);        
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY)
            .assertLastError().containsSummary(String.format(ErrorModel.NOT_SUFFICIENT_PERMISSIONS, siteModel.getId()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager can delete a site member with Collaborator role")
    public void siteManagerIsAbleToDeleteSiteMemberWithCollaboratorRole() throws Exception
    {
        UserModel collaboratorForDelete = dataUser.createRandomTestUser();
        dataUser.addUserToSite(collaboratorForDelete, siteModel, UserRole.SiteCollaborator);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingSite(siteModel).deleteSiteMember(collaboratorForDelete);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingSite(siteModel).getSiteMembers()
            .assertThat().entriesListDoesNotContain("id", collaboratorForDelete.getUsername());
    }
}
