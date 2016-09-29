package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
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
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Admin user updates comments with Rest API and status code is 200")
    public void adminIsAbleToUpdateComments() throws JsonToModelConversionException, Exception
    {
        commentsAPI.updateComment(document, commentModel, commentModel.getContent());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Manager user updates comments created by admin user with Rest API and status code is 200")
    public void managerIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        commentsAPI.updateComment(document, commentModel, commentModel.getContent());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user updates comments created by admin user with Rest API and status code is 200")
    public void contributorIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        commentsAPI.updateComment(document, commentModel, "This is the updated comment with Contributor user");
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user updates comments created by admin user with Rest API and status code is 200")
    public void consumerIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        commentsAPI.updateComment(document, commentModel, commentModel.getContent());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user updates comments created by admin user with Rest API and status code is 200")
    public void collaboratorIsAbleToUpdateComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        RestCommentModel commentEntry = commentsAPI.updateComment(document, commentModel,
                "This is the updated comment with Collaborator user");
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
        commentEntry.assertCommentContentIs("This is the updated comment with Collaborator user");
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets status code 401 if authentication call fails")
    public void managerIsNotAbleToUpdateCommentIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel incorrectUserModel = new UserModel("userName", "password");
        restClient.authenticateUser(incorrectUserModel);
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @TestRail(section = { "rest-api", "comments" }, executionType = ExecutionType.SANITY, description = "Verify if nodeId is not set the status code is 404")
    public void canNotUpdateCommentIfNodeIdIsNotSet() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        commentsAPI.updateComment(FolderModel.getRandomFolderModel(), commentModel, commentModel.getContent());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { "rest-api", "comments" }, executionType = ExecutionType.SANITY, description = "Verify if commentId is not set the status code is 404")
    public void canNotUpdateCommentIfCommentIdIsNotSet() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        commentsAPI.updateComment(document, new RestCommentModel(), commentModel.getContent());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

}
