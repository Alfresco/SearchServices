package org.alfresco.rest.sites.members;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMemberModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateSiteMemberFullTests extends RestTest
{
    private UserModel adminUser, anotherManager;
    private SiteModel publicSite, moderatedSite, privateSite;
    private RestSiteMemberModel updatedMember;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        publicSite = dataSite.usingUser(adminUser).createPublicRandomSite();
        moderatedSite = dataSite.usingUser(adminUser).createModeratedRandomSite();
        privateSite = dataSite.usingUser(adminUser).createPrivateRandomSite();
        anotherManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(anotherManager, publicSite, UserRole.SiteManager);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update another site manager to site contributor")
    public void managerUpdateSiteManagerToSiteContributor() throws Exception
    {
        UserModel siteManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteManager, publicSite, UserRole.SiteManager);
        
        siteManager.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteManager.getUsername()).and().field("role").is(siteManager.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update another site manager to site consumer")
    public void managerUpdateSiteManagerToSiteConsumer() throws Exception
    {
        UserModel siteManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteManager, publicSite, UserRole.SiteManager);
        
        siteManager.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteManager.getUsername()).and().field("role").is(siteManager.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site contributor to site collaborator")
    public void managerUpdateSiteContributorToSiteCollaborator() throws Exception
    {
        UserModel siteContributor = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteContributor, publicSite, UserRole.SiteContributor);
        
        siteContributor.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteContributor);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteContributor.getUsername()).and().field("role").is(siteContributor.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site contributor to site consumer")
    public void managerUpdateSiteContributorToSiteConsumer() throws Exception
    {
        UserModel siteContributor = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteContributor, publicSite, UserRole.SiteContributor);
        
        siteContributor.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteContributor);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteContributor.getUsername()).and().field("role").is(siteContributor.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site collaborator to site contributor")
    public void managerUpdateSiteCollaboratorToSiteContributor() throws Exception
    {
        UserModel siteCollaborator = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteCollaborator, publicSite, UserRole.SiteCollaborator);
        
        siteCollaborator.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteCollaborator);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteCollaborator.getUsername()).and().field("role").is(siteCollaborator.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site collaborator to site consumer")
    public void managerUpdateSiteCollaboratorToSiteConsumer() throws Exception
    {
        UserModel siteCollaborator = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteCollaborator, publicSite, UserRole.SiteCollaborator);
        
        siteCollaborator.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteCollaborator);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteCollaborator.getUsername()).and().field("role").is(siteCollaborator.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site consumer to site collaborator")
    public void managerUpdateSiteConsumerToSiteCollaborator() throws Exception
    {
        UserModel siteConsumer = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteConsumer, publicSite, UserRole.SiteConsumer);
        
        siteConsumer.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteConsumer);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteConsumer.getUsername()).and().field("role").is(siteConsumer.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site consumer to site contributor")
    public void managerUpdateSiteConsumerToSiteContributor() throws Exception
    {
        UserModel siteConsumer = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteConsumer, publicSite, UserRole.SiteConsumer);
        
        siteConsumer.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteConsumer);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteConsumer.getUsername()).and().field("role").is(siteConsumer.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update another site manager to site manager")
    public void managerUpdateSiteManagerToSiteManager() throws Exception
    {
        UserModel siteManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteManager, publicSite, UserRole.SiteManager);
        
        siteManager.setUserRole(UserRole.SiteManager);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteManager.getUsername()).and().field("role").is(siteManager.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site collaborator to site collaborator")
    public void managerUpdateSiteCollaboratorToSiteCollaborator() throws Exception
    {
        UserModel siteCollaborator = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteCollaborator, publicSite, UserRole.SiteCollaborator);
        
        siteCollaborator.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteCollaborator);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteCollaborator.getUsername()).and().field("role").is(siteCollaborator.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site contributor to site contributor")
    public void managerUpdateSiteContributorToSiteContributor() throws Exception
    {
        UserModel siteContributor = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteContributor, publicSite, UserRole.SiteContributor);
        
        siteContributor.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteContributor);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteContributor.getUsername()).and().field("role").is(siteContributor.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update site consumer to site consumer")
    public void managerUpdateSiteConsumerToSiteConsumer() throws Exception
    {
        UserModel siteConsumer = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteConsumer, publicSite, UserRole.SiteConsumer);
        
        siteConsumer.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(publicSite).updateSiteMember(siteConsumer);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteConsumer.getUsername()).and().field("role").is(siteConsumer.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager of a private site is able to downgrade his role")
    public void privateSiteManagerDowngradesRole() throws Exception
    {
        UserModel siteManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteManager, privateSite, UserRole.SiteManager);
        
        siteManager.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(siteManager).withCoreAPI()
                .usingSite(privateSite).updateSiteMember(siteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteManager.getUsername()).and().field("role").is(siteManager.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager of a moderated site is able to downgrade his role")
    public void moderatedSiteManagerDowngradesRole() throws Exception
    {
        UserModel siteManager = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteManager, moderatedSite, UserRole.SiteManager);
        
        siteManager.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(siteManager).withCoreAPI()
                .usingSite(moderatedSite).updateSiteMember(siteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(siteManager.getUsername()).and().field("role").is(siteManager.getUserRole());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the response of updating a site member with empty body at request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyManagerCanNotUpdateSiteMemberWithEmptyBody() throws Exception
    {
        restClient.authenticateUser(anotherManager).withCoreAPI();
        UserModel siteCollaborator = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteCollaborator, publicSite, UserRole.SiteCollaborator);  
        siteCollaborator.setUserRole(UserRole.SiteConsumer);

        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "", "sites/{siteId}/members/{personId}", publicSite.getId(), siteCollaborator.getUsername());
        restClient.processModel(RestSiteMemberModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsSummary(String.format(RestErrorModel.NO_CONTENT, "No content to map to Object due to end of input"))
            .containsErrorKey(String.format(RestErrorModel.NO_CONTENT, "No content to map to Object due to end of input"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);  
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if manager is able to update a user that has created a site membership request, but it's not a member of the site yet")
    public void verifyManagerCanNotUpdateUserWithSiteMembershipRequest() throws Exception
    {
        UserModel siteConsumer = dataUser.createRandomTestUser();
        siteConsumer.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(siteConsumer).withCoreAPI().usingUser(siteConsumer).addSiteMembershipRequest(moderatedSite);
        
        updatedMember = restClient.authenticateUser(anotherManager).withCoreAPI()
                .usingSite(moderatedSite).updateSiteMember(siteConsumer);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
        .containsSummary(RestErrorModel.NOT_A_MEMBER);
    }
    
}