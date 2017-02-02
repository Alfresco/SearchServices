package org.alfresco.rest;

import org.alfresco.rest.model.RestFavoriteSiteModel;
import org.alfresco.rest.model.RestSiteMemberModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FunctionalCasesTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel, moderatedSite;
    private RestSiteMemberModel updatedMember;
    private RestSiteMembershipRequestModelsCollection returnedCollection;
    private RestFavoriteSiteModel restFavoriteSiteModel;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        moderatedSite = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
    }
    
    /**
     * Scenario:
     * 1. Add a site member as Manager
     * 2. Update it's role to Collaborator
     * 3. Update it's role to Contributor
     * 4. Update it's role to Consumer
     */
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to update manager with different roles and gets status code CREATED (201)")
    public void managerIsAbleToUpdateManagerWithDifferentRoles() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).addPerson(testUser)
               .assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        testUser.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingSite(siteModel).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingSite(siteModel).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingSite(siteModel).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
    }
    
    /**
     * Scenario:
     * 1. Add a site member as Consumer
     * 2. Update it's role to Contributor
     * 3. Update it's role to Collaborator
     * 4. Update it's role to Manager
     */
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to update consumer with different roles and gets status code CREATED (201)")
    public void managerIsAbleToUpdateConsumerWithDifferentRoles() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).addPerson(testUser)
               .assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        testUser.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingSite(siteModel).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingSite(siteModel).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteManager);
        updatedMember = restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingSite(siteModel).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
    }
    
    /**
     * Scenario:
     * 1. Create site membership request
     * 2. Approve site membership request
     * 3. Add site to Favorites
     * 4. Delete site from Favorites
     */
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION, description = "Approve request, add site to favorites, then delete it from favorites")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void approveRequestAddAndDeleteSiteFromFavorites() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModel taskModel = restClient.authenticateUser(newMember).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(adminUserModel.getUsername(), adminUserModel.getPassword(), taskModel.getId(), true, "Approve");
        returnedCollection = restClient.authenticateUser(newMember).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListDoesNotContain("id", moderatedSite.getId());
        
        restFavoriteSiteModel = restClient.authenticateUser(newMember).withCoreAPI().usingUser(newMember).addFavoriteSite(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(moderatedSite.getId());
        
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().removeFavoriteSite(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    /**
     * Scenario:
     * 1. Create site membership request
     * 2. Reject site membership request
     * 3. Add moderated site to Favorites
     * 4. Create site membership request again
     * 5. Approve site membership request
     * 6. Delete member from site
     */
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION, description = "Reject request, add moderated site to favorites, create request again and approve it")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void rejectRequestAddModeratedSiteToFavorites() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModel taskModel = restClient.authenticateUser(newMember).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(adminUserModel.getUsername(), adminUserModel.getPassword(), taskModel.getId(), false, "Rejected");
        returnedCollection = restClient.authenticateUser(newMember).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListDoesNotContain("id", moderatedSite.getId());
        
        restFavoriteSiteModel = restClient.authenticateUser(newMember).withCoreAPI().usingUser(newMember).addFavoriteSite(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(moderatedSite.getId());
        
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        taskModel = restClient.authenticateUser(newMember).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(adminUserModel.getUsername(), adminUserModel.getPassword(), taskModel.getId(), true, "Accept");
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(newMember).deleteSiteMember(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingSite(moderatedSite).getSiteMembers().assertThat().entriesListDoesNotContain("id", newMember.getUsername());
    }
}
