package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
public class AddCommentSanityTests extends RestTest
{
    @Autowired
    RestCommentsApi commentsAPI;

    @Autowired
    DataUser dataUser;

    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        commentsAPI.useRestClient(restClient);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify admin user adds comments with Rest API and status code is 201")
    public void adminIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        String newContent = "This is a new comment added by " + adminUserModel.getUsername();
        commentsAPI.addComment(document, newContent)
                   .and().assertField("content").isNotEmpty()
                   .and().assertField("content").is(newContent);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Manager user adds comments with Rest API and status code is 201")
    public void managerIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        String contentSiteManger = "This is a new comment added by user with role: " + UserRole.SiteManager;
        commentsAPI.addComment(document, contentSiteManger)
                   .and().assertField("content").isNotEmpty()
                   .and().assertField("content").is(contentSiteManger);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Contributor user adds comments with Rest API and status code is 201")
    public void contributorIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        String contentSiteContributor = "This is a new comment added by user with role" + UserRole.SiteContributor;
        commentsAPI.addComment(document, contentSiteContributor)
                   .and().assertField("content").isNotEmpty()
                   .and().assertField("content").is(contentSiteContributor);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user adds comments with Rest API and status code is 201")
    public void collaboratorIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        String contentSiteCollaborator = "This is a new comment added by user with role: " + UserRole.SiteCollaborator;
        commentsAPI.addComment(document, contentSiteCollaborator)
                   .and().assertField("content").isNotEmpty()
                   .and().assertField("content").is(contentSiteCollaborator);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Consumer user can't add comments with Rest API and status code is 403")
    public void consumerIsNotAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        String contentSiteConsumer = "This is a new comment added by user with role: " + UserRole.SiteConsumer;
        commentsAPI.addComment(document, contentSiteConsumer);
        commentsAPI.usingRestWrapper()
                   .assertStatusCodeIs(HttpStatus.FORBIDDEN)
                   .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user gets status code 401 on post comments call")
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserIsNotAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password"));
        commentsAPI.addComment(document, "This is a new comment");
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

}
