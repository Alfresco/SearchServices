package org.alfresco.rest.comments.get;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModelsCollection;
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

public class GetCommentsSanityTests extends RestTest
{
   private UserModel adminUserModel;
    
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userModel;
    private ListUserWithRoles usersWithRoles;
    private String content = "This is a new comment";
    private RestCommentModelsCollection comments;
	
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingResource(document).addComment(content);
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify Admin user gets comments with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void adminIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI()
                    .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", content);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets comments created by admin user with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void managerIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        comments = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI()
                    .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", content);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify Contributor user gets comments created by admin user with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void contributorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        comments = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI()
                    .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", content);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify Collaborator user gets comments created by admin user with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void collaboratorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        comments = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                    .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", content);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify Consumer user gets comments created by admin user with Rest API and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void consumerIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        comments = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", content);
    }
    

    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets status code 401 if authentication call fails")    
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    @Bug(id="MNT-16904", description = "It fails only on environment with tenants")
    public void managerIsNotAbleToRetrieveCommentIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel nonexistentModel = new UserModel("nonexistentUser", "nonexistentPassword");
        restClient.authenticateUser(nonexistentModel).withCoreAPI()
                .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets comments created by another user and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void managerIsAbleToRetrieveCommentsCreatedByAnotherUser() throws JsonToModelConversionException, Exception
    {
        userModel = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        String contentManager = "This is a new comment added by " + userModel.getUsername();
        restClient.authenticateUser(userModel).withCoreAPI()
                .usingResource(document).addComment(contentManager);
        restClient.assertStatusCodeIs(HttpStatus.CREATED); 
        comments = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI()
                .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", contentManager)
                .and().entriesListContains("content", content);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.SANITY,
            description= "Verify admin user gets comments created by another user and status code is 200")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
    public void adminIsAbleToRetrieveCommentsCreatedByAnotherUser() throws JsonToModelConversionException, Exception
    {
        userModel = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        String contentCollaborator = "This is a new comment added by " + userModel.getUsername();
        restClient.authenticateUser(userModel).withCoreAPI()
                .usingResource(document).addComment(contentCollaborator);
        restClient.assertStatusCodeIs(HttpStatus.CREATED); 
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", contentCollaborator)
                .and().entriesListContains("content", content);
    }
}
