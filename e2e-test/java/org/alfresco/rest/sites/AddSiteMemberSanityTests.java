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

@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
public class AddSiteMemberSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);

    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that manager is able to add site member and gets status code CREATED (201)")
    public void managerIsAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.usingSite(siteModel).addPerson(testUser)
               .assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);       
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that site collaborator is not able to add site member and gets status code FORBIDDEN (403)")
    public void collaboratorIsNotAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .usingSite(siteModel).addPerson(testUser);
        
        restClient.assertLastError().containsSummary("Permission was denied");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);       
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that site contributor is not able to add site member and gets status code FORBIDDEN (403)")
    public void contributorIsNotAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                  .usingSite(siteModel).addPerson(testUser);        
        restClient.assertLastError().containsSummary("Permission was denied");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);       
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that site consumer is not able to add site member and gets status code FORBIDDEN (403)")
    public void consumerIsNotAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .usingSite(siteModel).addPerson(testUser);
        restClient.assertLastError().containsSummary("Permission was denied");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);       
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that admin user is able to add site member and gets status code CREATED (201)")
    public void adminIsAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(adminUserModel)
                  .usingSite(siteModel).addPerson(testUser)
                  .and().field("id").is(testUser.getUsername())
                  .and().field("role").is(testUser.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);       
    }
    
    @Bug(id="MNT-16904")
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify that unauthenticated user is not able to add site member")
    public void unauthenticatedUserIsNotAuthorizedToAddSiteMmeber() throws Exception{
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        UserModel inexistentUser = new UserModel("inexistent user", "inexistent password");
        restClient.authenticateUser(inexistentUser)
                  .usingSite(siteModel).addPerson(testUser);        
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
