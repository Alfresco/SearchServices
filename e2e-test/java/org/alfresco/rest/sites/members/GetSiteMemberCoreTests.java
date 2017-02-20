package org.alfresco.rest.sites.members;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
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
public class GetSiteMemberCoreTests extends RestTest
{
    private UserModel adminUser;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel manager, consumer, collaborator, contributor;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        consumer = dataUser.createRandomTestUser();
        dataUser.addUserToSite(consumer, siteModel, UserRole.SiteConsumer);
        
        manager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        collaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        contributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Manager role doesn't get a site member of inexistent site and status code is Not Found (404)")
    public void getSiteMemberOfInexistentSite() throws Exception
    {
        SiteModel invalidSite = new SiteModel("invalidSite");
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(invalidSite).getSiteMember(consumer);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, consumer.getUsername(), invalidSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Manager role doesn't get non site member of inexistent site and status code is Not Found (404)")
    public void getSiteMemberForNonSiteMember() throws Exception
    {
        UserModel nonMember = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(nonMember);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, nonMember.getUsername(), siteModel.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Manager role doesn't get not existing site member and status code is Not Found (404)")
    public void getSiteMemberForInexistentSiteMember() throws Exception
    {
        UserModel inexistentUser = new UserModel("inexistentUser", "password");
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(inexistentUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, inexistentUser.getUsername()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Manager role can get site member using \"-me-\" in place of personId")
    public void getSiteMemberUsingMeForPersonId() throws Exception
    {
        UserModel manager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(manager, siteModel, UserRole.SiteManager);
        UserModel meUser = new UserModel("-me-", "password");
        
        restClient.authenticateUser(manager);
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(meUser)
            .assertThat().field("id").is(manager.getUsername())
            .and().field("role").is(manager.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Manager role can get site member for empty siteId")
    public void getSiteMemberForEmptySiteId() throws Exception
    {
        SiteModel emptySite = new SiteModel("");
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingSite(emptySite).getSiteMember(consumer);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, consumer.getUsername(), emptySite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Manager role gets site member with Manager role and status code is OK (200)")
    public void getSiteManagerMemberWithManagerRole() throws Exception
    {
        UserModel anotherManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(anotherManager, siteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(manager);
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(anotherManager)
                    .assertThat().field("id").is(anotherManager.getUsername())
                    .and().field("role").is(anotherManager.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Collaborator role gets site member with Manager role and status code is OK (200)")
    public void getSiteManagerMemberWithCollaboratorRole() throws Exception
    {        
        restClient.authenticateUser(collaborator);
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(manager)
                    .assertThat().field("id").is(manager.getUsername())
                    .and().field("role").is(manager.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Consumer role gets site member with Manager role and status code is OK (200)")
    public void getSiteManagerMemberWithConsumerRole() throws Exception
    {        
        restClient.authenticateUser(consumer);
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(manager)
                    .assertThat().field("id").is(manager.getUsername())
                    .and().field("role").is(manager.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Contributor role gets site member with Manager role and status code is OK (200)")
    public void getSiteManagerMemberWithContributorRole() throws Exception
    {        
        restClient.authenticateUser(contributor);
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(manager)
                    .assertThat().field("id").is(manager.getUsername())
                    .and().field("role").is(manager.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Contributor role gets admin site member and status code is OK (200)")
    public void getSiteAdminManagerMember() throws Exception
    {        
        restClient.authenticateUser(contributor);
        restClient.withCoreAPI().usingSite(siteModel).getSiteMember(adminUser)
                    .assertThat().field("id").is(adminUser.getUsername())
                    .and().field("role").is(UserRole.SiteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
}
