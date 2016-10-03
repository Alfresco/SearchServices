package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.CommentContent;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "comments", "sanity" })
public class UpdateCommentsSanityTests extends RestTest
{
    @Autowired
    RestCommentsApi commentsAPI;

    @Autowired
    DataUser dataUser;

    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private RestCommentModel commentModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        commentsAPI.useRestClient(restClient);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        commentModel = commentsAPI.addComment(document, "This is a new comment");

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Admin user updates comments and status code is 200")
    public void adminIsAbleToUpdateComments() throws JsonToModelConversionException, Exception
    {
        CommentContent commentContent = new CommentContent("This is the updated comment with admin user");
        
        commentsAPI.updateComment(document, commentModel, commentContent);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Manager user updates comments created by admin user and status code is 200")
    public void managerIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        
        CommentContent commentContent = new CommentContent("This is the updated comment with Manager user");
        
        commentsAPI.updateComment(document, commentModel, commentContent);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user updates comments created by admin user and status code is 200")
    public void contributorIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        
        CommentContent commentContent = new CommentContent("This is the updated comment with Contributor user");
        
        commentsAPI.updateComment(document, commentModel, commentContent);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user updates comments created by admin user and status code is 200")
    public void consumerIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        
        CommentContent commentContent = new CommentContent("This is the updated comment with Consumer user");
        
        commentsAPI.updateComment(document, commentModel, commentContent);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user updates comment created by admin user and status code is 200")
    @Bug(id="REPO-1011")
    public void collaboratorIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        
        CommentContent commentContent = new CommentContent("This is the updated comment with Collaborator user");
        
        RestCommentModel commentEntry = commentsAPI.updateComment(document, commentModel, commentContent);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
        commentEntry.assertCommentContentIs("This is the updated comment with Collaborator user");
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user gets status code 401 on update comment call")
    public void unauthenticatedUserIsNotAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        UserModel incorrectUserModel = new UserModel("userName", "password");
        restClient.authenticateUser(incorrectUserModel);
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @TestRail(section = { "rest-api", "comments" }, executionType = ExecutionType.SANITY, description = "Verify update comment with inexistent nodeId returns status code 404")
    public void canNotUpdateCommentIfNodeIdIsNotSet() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        
        FolderModel content = FolderModel.getRandomFolderModel();
        content.setNodeRef("node ref that does not exist");
        
        CommentContent comment = new CommentContent("This is the updated comment.");
        
        commentsAPI.updateComment(content, commentModel, comment);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { "rest-api", "comments" }, executionType = ExecutionType.SANITY, description = "Verify if commentId is not set the status code is 404")
    public void canNotUpdateCommentIfCommentIdIsNotSet() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        
        RestCommentModel comment = new RestCommentModel();
        comment.setId("comment id that does not exist");
        
        CommentContent commentContent = new CommentContent("This is the updated comment.");
        
        commentsAPI.updateComment(document, comment, commentContent);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

}