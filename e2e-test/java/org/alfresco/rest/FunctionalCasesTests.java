package org.alfresco.rest;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.model.*;
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
    private UserModel adminUser, manager;
    private SiteModel publicSite, moderatedSite;
    private RestSiteMemberModel updatedMember;
    private RestSiteMembershipRequestModelsCollection returnedCollection;
    private RestFavoriteSiteModel restFavoriteSiteModel;
    private RestActivityModelsCollection activities;
    private FileModel file;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        manager = dataUser.createRandomTestUser();
        publicSite = dataSite.usingUser(adminUser).createPublicRandomSite();
        dataUser.addUserToSite(manager, publicSite, UserRole.SiteManager);
        moderatedSite = dataSite.usingUser(adminUser).createModeratedRandomSite();
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
        restClient.authenticateUser(adminUser).withCoreAPI().usingSite(publicSite).addPerson(testUser)
               .assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        testUser.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.withCoreAPI()
                .usingSite(publicSite).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.withCoreAPI()
                .usingSite(publicSite).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteConsumer);
        updatedMember = restClient.withCoreAPI()
                .usingSite(publicSite).updateSiteMember(testUser);
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
        restClient.authenticateUser(adminUser).withCoreAPI().usingSite(publicSite).addPerson(testUser)
               .assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        testUser.setUserRole(UserRole.SiteContributor);
        updatedMember = restClient.withCoreAPI()
                .usingSite(publicSite).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteCollaborator);
        updatedMember = restClient.withCoreAPI()
                .usingSite(publicSite).updateSiteMember(testUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedMember.assertThat().field("id").is(testUser.getUsername()).and().field("role").is(testUser.getUserRole());
        
        testUser.setUserRole(UserRole.SiteManager);
        updatedMember = restClient.withCoreAPI()
                .usingSite(publicSite).updateSiteMember(testUser);
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
        workflow.approveSiteMembershipRequest(adminUser.getUsername(), adminUser.getPassword(), taskModel.getId(), true, "Approve");
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
        workflow.approveSiteMembershipRequest(adminUser.getUsername(), adminUser.getPassword(), taskModel.getId(), false, "Rejected");
        returnedCollection = restClient.authenticateUser(newMember).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListDoesNotContain("id", moderatedSite.getId());
        
        restFavoriteSiteModel = restClient.authenticateUser(newMember).withCoreAPI().usingUser(newMember).addFavoriteSite(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(moderatedSite.getId());
        
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        taskModel = restClient.authenticateUser(newMember).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(adminUser.getUsername(), adminUser.getPassword(), taskModel.getId(), true, "Accept");
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingUser(newMember).deleteSiteMember(moderatedSite);
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
        file = dataContent.usingUser(manager).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);
        activities = restClient.authenticateUser(manager).withCoreAPI().usingAuthUser().getPersonActivitiesUntilEntriesCountIs(2);
        activities.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("siteId", publicSite.getId())
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
        file = dataContent.usingUser(manager).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(manager).withCoreAPI().usingResource(file).addComment("new comment");
        activities = restClient.authenticateUser(manager).withCoreAPI().usingAuthUser().getPersonActivitiesUntilEntriesCountIs(3);
        activities.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("siteId", publicSite.getId())
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
        file = dataContent.usingUser(manager).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);
        dataContent.usingUser(manager).usingResource(file).deleteContent();
        activities = restClient.authenticateUser(manager).withCoreAPI().usingAuthUser().getPersonActivitiesUntilEntriesCountIs(2);
        activities.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("siteId", publicSite.getId())
            .and().entriesListContains("activityType", "org.alfresco.documentlibrary.file-deleted")
            .and().entriesListContains("activitySummary.objectId", file.getNodeRefWithoutVersion());
    }

    /**
     * 1. Post one comment
     * 2. Get comment details
     * 3. Update comment
     * 4. Get again comment details
     * 5. Delete comment
     */
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS },
            executionType = ExecutionType.REGRESSION,
            description = "Add comment to a file, then get comment details. Update it and check that get comment returns updated details. Delete comment then check that file has no comments.")
    public void addUpdateDeleteCommentThenGetCommentDetails() throws Exception
    {
        file = dataContent.usingUser(manager).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);
        RestCommentModel newComment = restClient.authenticateUser(manager).withCoreAPI().usingResource(file).addComment("new comment");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestCommentModelsCollection fileComments = restClient.withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        fileComments.assertThat().entriesListContains("content", newComment.getContent());

        RestCommentModel updatedComment = restClient.withCoreAPI().usingResource(file).updateComment(newComment, "updated comment");
        restClient.assertStatusCodeIs(HttpStatus.OK);

        fileComments = restClient.withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        fileComments.assertThat().entriesListContains("content", updatedComment.getContent()).assertThat().entriesListDoesNotContain("content", newComment.getContent());

        restClient.withCoreAPI().usingResource(file).deleteComment(updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        fileComments = restClient.withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        fileComments.assertThat().entriesListIsEmpty();
    }

    /**
     * 1. Post one comment
     * 2. Delete comment
     * 3. Post the same comment again
     */
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS },
            executionType = ExecutionType.REGRESSION,
            description = "Add a comment to a file, delete it, then added the same comment again.")
    public void checkThatADeletedCommentCanBePostedAgain() throws Exception
    {
        file = dataContent.usingUser(manager).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);
        RestCommentModel newComment = restClient.authenticateUser(manager).withCoreAPI().usingResource(file).addComment("new comment");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestCommentModelsCollection fileComments = restClient.withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        fileComments.assertThat().entriesListContains("content", newComment.getContent());

        restClient.withCoreAPI().usingResource(file).deleteComment(newComment);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        fileComments = restClient.withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        fileComments.assertThat().entriesListIsEmpty();

        restClient.authenticateUser(manager).withCoreAPI().usingResource(file).addComment("new comment");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        fileComments = restClient.withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        fileComments.assertThat().entriesListContains("content", newComment.getContent());
    }

    /**
     * Scenario:
     * 1. join an user to a site
     * 2. Check action is included in person activities list
     * 
     * @throws Exception
     */

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Create an user, join the user to a site and check that activity is included in person activities")
    public void joinUserToSiteThenGetPersonActivities() throws Exception
    {
        UserModel userJoinSite = dataUser.createRandomTestUser();

        restClient.authenticateUser(userJoinSite).withCoreAPI().usingMe().addSiteMembershipRequest(publicSite);
        activities = restClient.withCoreAPI().usingAuthUser().getPersonActivitiesUntilEntriesCountIs(2);
        activities.assertThat().entriesListIsNotEmpty().and()
                .entriesListContains("siteId", publicSite.getId()).and()
                .entriesListContains("activityType", "org.alfresco.site.user-joined").and()
                .entriesListContains("activitySummary.memberPersonId", userJoinSite.getUsername());
    }
}