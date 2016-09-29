package org.alfresco.rest.comments;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "comments", "sanity" })
public class DeleteCommentsSanityTests extends RestTest
{

    @Autowired
    RestCommentsApi commentsAPI;

    @Autowired
    DataUser dataUser;

    private UserModel adminUserModel;

    private FileModel document;
    private SiteModel siteModel;
    private String commentId;
    private HashMap<UserRole, UserModel> usersWithRoles;

    @BeforeClass
    public void initTest() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        commentsAPI.useRestClient(restClient);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                Arrays.asList(UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor));
    }

    @BeforeMethod
    public void initMethod() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        commentsAPI.useRestClient(restClient);
        commentId = commentsAPI.addComment(document.getNodeRef(), "This is a new comment").getId();
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Admin user delete comments with Rest API and status code is 204")
    public void adminIsAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        commentsAPI.deleteComment(document.getNodeRef(), commentId);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT.toString());
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Manager user delete comments created by admin user with Rest API and status code is 204")
    public void managerIsAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        commentsAPI.deleteComment(document.getNodeRef(), commentId);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT.toString());
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user can't delete comments created by admin user with Rest API and status code is 403")
    public void collaboratorIsNotAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteCollaborator));
        commentsAPI.deleteComment(document.getNodeRef(), commentId);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user can't delete comments created by admin user with Rest API and status code is 403")
    public void contributorIsNotAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteContributor));
        commentsAPI.deleteComment(document.getNodeRef(), commentId);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user can't delete comments created by admin user with Rest API and status code is 403")
    public void consumerIsNotAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteConsumer));
        commentsAPI.deleteComment(document.getNodeRef(), commentId);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }

}
