package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
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
public class PostCommentsSanityTests extends RestTest
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
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify admin user adds comments with Rest API and status code is 201")
    public void admiIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        commentsAPI.addComment(document, "This is a new comment added by " + adminUserModel.getUsername());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Manager user adds comments with Rest API and status code is 201")
    public void managerIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        commentsAPI.addComment(document, "This is a new comment added by user with role: " + UserRole.SiteManager);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user adds comments with Rest API and status code is 201")
    public void contributorIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        commentsAPI.addComment(document, "This is a new comment added by user with role" + UserRole.SiteContributor);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user adds comments with Rest API and status code is 201")
    public void collaboratorIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        commentsAPI.addComment(document, "This is a new comment added by user with role: " + UserRole.SiteCollaborator);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user adds comments with Rest API and status code is 201")
    public void consumerIsAbleToAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        commentsAPI.addComment(document, "This is a new comment added by user with role: " + UserRole.SiteConsumer);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "comments" }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user gets status code 401 on post comments call")
    @Bug(id="MNT-16904")
    public void unauthenticatedUserIsNotAbleToAddComment() throws JsonToModelConversionException, Exception
    {        
        restClient.authenticateUser(new UserModel("random user", "random password"));
        commentsAPI.addComment(document, "This is a new comment");
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

}
