package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMemberModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 11/29/2016.
 */
public class UpdateSiteMemberCoreTests extends RestTest
{
    private UserModel adminUser, regularUser, anotherManager;
    private SiteModel publicSite;
    private ListUserWithRoles publicSiteUsers;
    private RestSiteMemberModel updatedMember;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        publicSite = dataSite.usingUser(adminUser).createPublicRandomSite();
        publicSiteUsers = dataUser.addUsersWithRolesToSite(publicSite, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        anotherManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(anotherManager, publicSite, UserRole.SiteManager);
        regularUser = dataUser.createRandomTestUser();
        regularUser.setUserRole(UserRole.SiteConsumer);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if update site member request returns status code 404 when nonexistent siteId is used")
    public void updateSiteMemberOfNonexistentSite() throws Exception
    {
        SiteModel deletedSite = dataSite.usingUser(adminUser).createPublicRandomSite();
        dataUser.addUserToSite(regularUser, deletedSite, UserRole.SiteConsumer);
        dataSite.deleteSite(deletedSite);

        restClient.authenticateUser(adminUser).withCoreAPI()
                .usingSite(deletedSite).updateSiteMember(regularUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, deletedSite.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if update site member request returns status code 400 when personId is not member of the site")
    public void updateNotASiteMember() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(regularUser);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary("User is not a member of the site");
    }

//    @Bug(id="REPO-1642", description = "reproduced on 5.2.1 only, it works on 5.2.0")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if update site member request returns status code 404 when personId does not exist")
    public void updateNonexistentSiteMember() throws Exception
    {
        UserModel nonexistentUser = new UserModel("nonexistentUser", DataUser.PASSWORD);
        nonexistentUser.setUserRole(UserRole.SiteContributor);
        restClient.authenticateUser(adminUser).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(nonexistentUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, nonexistentUser.getUsername()));
    }

    @Bug(id="REPO-1660")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if a manager is able to downgrade to contributor using -me- string in place of personId.")
    public void updateSiteManagerToSiteContributorUsingMe() throws Exception
    {
        UserModel meManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(meManager, publicSite, UserRole.SiteManager);
        UserModel meUser = new UserModel("-me-", "password");
        meUser.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.authenticateUser(meManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(meUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(meManager.getUsername())
            .and().field("role").is(meUser.getUserRole());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if update site member request returns status code 404 when empty siteId is used")
    public void updateSiteMemberUsingEmptySiteId() throws Exception
    {
        restClient.authenticateUser(adminUser).withCoreAPI()
                .usingSite("").updateSiteMember(regularUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if update site member request returns status code 405 when empty personId is used")
    public void updateSiteMemberUsingEmptyPersonId() throws Exception
    {
        UserModel emptyUser = new UserModel("", DataUser.PASSWORD);
        emptyUser.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(adminUser).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(emptyUser);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                .assertLastError().containsSummary(RestErrorModel.PUT_EMPTY_ARGUMENT);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if update site member request returns status code 400 when invalid role is used")
    public void updateSiteMemberUsingInvalidRole() throws Exception
    {
        restClient.authenticateUser(anotherManager).withCoreAPI();
        UserModel siteConsumer = publicSiteUsers.getOneUserWithRole(UserRole.SiteConsumer);
        String json = JsonBodyGenerator.keyValueJson("role","invalidRole");
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, json, "sites/{siteId}/members/{personId}", publicSite.getId(), siteConsumer.getUsername());
        restClient.processModel(RestSiteMemberModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.UNKNOWN_ROLE, "invalidRole"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update another site manager to site collaborator")
    public void managerUpdateSiteManagerToSiteCollaborator() throws Exception
    {
        UserModel siteManager = publicSiteUsers.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteManager.getUsername())
                .and().field("role").is(siteManager.getUserRole());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site contributor to site manager")
    public void managerUpdateSiteContributorToSiteManager() throws Exception
    {
        UserModel siteContributor = publicSiteUsers.getOneUserWithRole(UserRole.SiteContributor);
        siteContributor.setUserRole(UserRole.SiteManager);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteContributor);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteContributor.getUsername())
                .and().field("role").is(siteContributor.getUserRole());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site collaborator to site manager")
    public void managerUpdateSiteCollaboratorToSiteManager() throws Exception
    {
        UserModel siteCollaborator = publicSiteUsers.getOneUserWithRole(UserRole.SiteCollaborator);
        siteCollaborator.setUserRole(UserRole.SiteManager);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteCollaborator);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteCollaborator.getUsername())
                .and().field("role").is(siteCollaborator.getUserRole());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site consumer to site manager")
    public void managerUpdateSiteConsumerToSiteManager() throws Exception
    {
        UserModel siteConsumer = publicSiteUsers.getOneUserWithRole(UserRole.SiteConsumer);
        siteConsumer.setUserRole(UserRole.SiteManager);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteConsumer);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteConsumer.getUsername())
                .and().field("role").is(siteConsumer.getUserRole());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to downgrade himself to site consumer")
    public void managerDowngradeHimselfToSiteConsumer() throws Exception
    {
        UserModel siteManager = publicSiteUsers.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(siteManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteManager.getUsername())
                .and().field("role").is(siteManager.getUserRole());
    }
}
