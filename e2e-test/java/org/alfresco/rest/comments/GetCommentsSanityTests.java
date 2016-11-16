package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.Node;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
public class GetCommentsSanityTests extends RestTest
{
    @Autowired
    Node commentsAPI;
    
    @Autowired
    DataUser dataUser;
	
    private UserModel adminUserModel;
    
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userModel;
    private ListUserWithRoles usersWithRoles;
    private String content;
	
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        content = "This is a new comment";
        restClient.usingResource(document).addComment(content);
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify Admin user gets comments with Rest API and status code is 200")
    public void adminIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        restClient.usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets comments created by admin user with Rest API and status code is 200")
    public void managerIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.usingResource(document).getNodeComments()
                   .assertThat().entriesListContains("content", content);
                   
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify Contributor user gets comments created by admin user with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.usingResource(document).getNodeComments()
                   .assertThat().entriesListContains("content", content);
                   
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify Collaborator user gets comments created by admin user with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restClient.usingResource(document).getNodeComments()
                .assertThat().entriesListContains("content", content);                
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify Consumer user gets comments created by admin user with Rest API and status code is 200")
    public void consumerIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.usingResource(document).getNodeComments()
                   .assertThat().entriesListContains("content", content);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets status code 401 if authentication call fails")
    @Bug(id="MNT-16904")
    public void managerIsNotAbleToRetrieveCommentIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel nonexistentModel = new UserModel("nonexistentUser", "nonexistentPassword");
        restClient.authenticateUser(nonexistentModel);
        restClient.usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets comments created by another user and status code is 200")
    public void managerIsAbleToRetrieveCommentsCreatedByAnotherUser() throws JsonToModelConversionException, Exception
    {
        userModel = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(userModel);
        String contentManager = "This is a new comment added by " + userModel.getUsername();
        restClient.usingResource(document).addComment(contentManager);
        restClient.assertStatusCodeIs(HttpStatus.CREATED); 
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.usingResource(document).getNodeComments()
                   .assertThat().entriesListContains("content", contentManager);
                   
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.SANITY}, executionType= ExecutionType.SANITY,
            description= "Verify admin user gets comments created by another user and status code is 200")
    public void adminIsAbleToRetrieveCommentsCreatedByAnotherUser() throws JsonToModelConversionException, Exception
    {
        userModel = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(userModel);
        String contentCollaborator = "This is a new comment added by " + userModel.getUsername();
        restClient.usingResource(document).addComment(contentCollaborator);
        restClient.assertStatusCodeIs(HttpStatus.CREATED); 
        restClient.authenticateUser(adminUserModel);
        restClient.usingResource(document).getNodeComments()
                  .assertThat().entriesListContains("content", contentCollaborator);
                  
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
