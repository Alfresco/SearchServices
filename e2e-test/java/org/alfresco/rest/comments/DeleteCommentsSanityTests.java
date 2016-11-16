package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
public class DeleteCommentsSanityTests extends RestTest
{
    private UserModel adminUserModel;

    private FileModel document;
    private SiteModel siteModel;
    private RestCommentModel comment;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @BeforeMethod(alwaysRun=true)
    public void addCommentToDocument() throws Exception
    {
        restClient.authenticateUser(adminUserModel);        
        comment = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Admin user delete comments with Rest API and status code is 204")
    public void adminIsAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);        
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Manager user delete comments created by admin user with Rest API and status code is 204")
    public void managerIsAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user can't delete comments created by admin user with Rest API and status code is 403")
    public void collaboratorIsNotAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                   .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Contributor user can't delete comments created by admin user with Rest API and status code is 403")
    public void contributorIsNotAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Consumer user can't delete comments created by admin user with Rest API and status code is 403")
    public void consumerIsNotAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets status code 401 if authentication call fails")
    public void managerIsNotAbleToDeleteCommentIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel nonexistentModel = new UserModel("nonexistentUser", "nonexistentPassword");
        restClient.authenticateUser(nonexistentModel);
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
