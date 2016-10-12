package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "sites", "sanity" })
public class AddSiteMemberSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);

    }

    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify that manager is able to add site member and gets status code CREATED (201)")
    public void managerIsAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteAPI.addPerson(siteModel, testUser);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);       
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify that site collaborator is not able to add site member and gets status code FORBIDDEN (403)")
    public void collaboratorIsNotAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        siteAPI.addPerson(siteModel, testUser);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);       
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify that site contributor is not able to add site member and gets status code FORBIDDEN (403)")
    public void contributorIsNotAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        siteAPI.addPerson(siteModel, testUser);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);       
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify that site consumer is not able to add site member and gets status code FORBIDDEN (403)")
    public void consumerIsNotAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        siteAPI.addPerson(siteModel, testUser);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);       
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify that admin user is able to add site member and gets status code CREATED (201)")
    public void adminIsAbleToAddSiteMember() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(adminUserModel);
        siteAPI.addPerson(siteModel, testUser);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);       
    }
    
    @Bug(id="MNT-16904")
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify that unauthenticated user is not able to add site member")
    public void unauthenticatedUserIsNotAuthorizedToAddSiteMmeber() throws Exception{
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        UserModel inexistentUser = new UserModel("inexistent user", "inexistent password");
        restClient.authenticateUser(inexistentUser);
        siteAPI.addPerson(siteModel, testUser);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
