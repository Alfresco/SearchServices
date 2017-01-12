package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddCommentSanityTests extends RestTest
{    
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
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify admin user adds comments with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void adminIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        String newContent = "This is a new comment added by " + adminUserModel.getUsername();
        restClient.withCoreAPI().usingResource(document).addComment(newContent)
                   .assertThat().field("content").isNotEmpty()
                   .and().field("content").is(newContent);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Manager user adds comments with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void managerIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        String contentSiteManger = "This is a new comment added by user with role: " + UserRole.SiteManager;
        RestCommentModel createdComment = restClient.withCoreAPI().usingResource(document).addComment(contentSiteManger);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        createdComment.assertThat().field("content").isNotEmpty()
                   .and().field("content").is(contentSiteManger);
        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Contributor user adds comments with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    @Bug(id="ACE-4614")
    public void contributorIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        String contentSiteContributor = "This is a new comment added by user with role" + UserRole.SiteContributor;
        RestCommentModel createdComment = restClient.withCoreAPI().usingResource(document).addComment(contentSiteContributor);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        createdComment.assertThat().field("content").isNotEmpty()
                   .and().field("content").is(contentSiteContributor);
        
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user adds comments with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void collaboratorIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        String contentSiteCollaborator = "This is a new comment added by user with role: " + UserRole.SiteCollaborator;
        restClient.withCoreAPI().usingResource(document).addComment(contentSiteCollaborator)
                   .assertThat().field("content").isNotEmpty()
                   .and().field("content").is(contentSiteCollaborator);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Consumer user can't add comments with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void consumerIsNotAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        String contentSiteConsumer = "This is a new comment added by user with role: " + UserRole.SiteConsumer;
        restClient.withCoreAPI().usingResource(document).addComment(contentSiteConsumer);
        restClient
                   .assertStatusCodeIs(HttpStatus.FORBIDDEN)
                   .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user gets status code 401 on post comments call")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserIsNotAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password"));
        restClient.withCoreAPI().usingResource(document).addComment("This is a new comment");
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

}
