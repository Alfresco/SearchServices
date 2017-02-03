package org.alfresco.rest;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.model.RestActivityModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestFavoriteSiteModel;
import org.alfresco.rest.model.RestSiteMemberModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FileModel;
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
    private SiteModel siteModel, moderatedSite, privateSite;
    private RestSiteMemberModel updatedMember;
    private RestSiteMembershipRequestModelsCollection returnedCollection;
    private RestFavoriteSiteModel restFavoriteSiteModel;
    private RestActivityModelsCollection activities;
    private UserModel user;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        user = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        dataUser.addUserToSite(user, siteModel, UserRole.SiteManager);
        moderatedSite = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        privateSite = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
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
    
    /**
     * Scenario:
     * 1. Add file
     * 2. Check file is included in person activities list
     */
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Add a file and check that activity is included in person activities")
    public void addFileThenGetPersonActivities() throws Exception
    {
        FileModel file = dataContent.usingUser(user).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        activities = restClient.authenticateUser(user).withCoreAPI().usingAuthUser().getPersonActivitiesUntilEntriesCountIs(2);
        activities.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("siteId", siteModel.getId())
            .and().entriesListContains("activityType", "org.alfresco.documentlibrary.file-added")
            .and().entriesListContains("activitySummary.objectId", file.getNodeRefWithoutVersion());
    }
    
    /**
     * Scenario:
     * 1. Add a comment to a file
     * 2. Check that comment is included in person activities list
     */
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Add a comment to a file and check that activity is included in person activities")
    public void addCommentThenGetPersonActivities() throws Exception
    {
        FileModel file = dataContent.usingUser(user).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(user).withCoreAPI().usingResource(file).addComment("new comment");
        activities = restClient.authenticateUser(user).withCoreAPI().usingAuthUser().getPersonActivitiesUntilEntriesCountIs(3);
        activities.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("siteId", siteModel.getId())
            .and().entriesListContains("activityType", "org.alfresco.comments.comment-created")
            .and().entriesListContains("activitySummary.objectId", file.getNodeRefWithoutVersion());
    }
    
    /**
     * Scenario:
     * 1. Add file then delete it
     * 2. Check action is included in person activities list
     */
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Add a file, delete it and check that activity is included in person activities")
    public void addFileDeleteItThenGetPersonActivities() throws Exception
    {
        FileModel file = dataContent.usingUser(user).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataContent.usingUser(user).usingResource(file).deleteContent();
        activities = restClient.authenticateUser(user).withCoreAPI().usingAuthUser().getPersonActivitiesUntilEntriesCountIs(2);
        activities.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("siteId", siteModel.getId())
            .and().entriesListContains("activityType", "org.alfresco.documentlibrary.file-deleted")
            .and().entriesListContains("activitySummary.objectId", file.getNodeRefWithoutVersion());
    }
    
    /**
     * Scenario:
     * 1. Add user to private site
     * 2. Remove user from private site
     * 3. User creates membership request to the same private site
     */
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION, description = "Verify membership request by user after it was removed from site gets status code 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userCanNotCreateMembershipRequestIfItWasRemovedFromPrivateSite() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        newMember.setUserRole(UserRole.SiteCollaborator);

        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSite).addPerson(newMember)
               .assertThat().field("id").is(newMember.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);   
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(newMember).deleteSiteMember(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
        .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, newMember.getUsername(), privateSite.getTitle()));
    }
    
    /**
     * Scenario:
     * 1. User creates membership request to moderated site
     * 2. Accept membership request
     * 3. Remove user from moderated site
     * 4. Add user on private site
     */
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION, description = "Verify user can be added back after if was removed from site")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userCanBeAddedAfterItWasRemovedFromSite() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        newMember.setUserRole(UserRole.SiteCollaborator);
        
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestTaskModel taskModel = restClient.authenticateUser(newMember).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(adminUserModel.getUsername(), adminUserModel.getPassword(), taskModel.getId(), true, "Accept");
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(newMember).deleteSiteMember(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingSite(moderatedSite).getSiteMembers().assertThat().entriesListDoesNotContain("id", newMember.getUsername());

        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSite).addPerson(newMember)
               .assertThat().field("id").is(newMember.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);   
        restClient.withCoreAPI().usingSite(moderatedSite).getSiteMembers().assertThat().entriesListContains("id", newMember.getUsername());
        
    }
}