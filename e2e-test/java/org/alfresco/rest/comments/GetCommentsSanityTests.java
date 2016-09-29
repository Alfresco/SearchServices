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
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "comments", "sanity" })
public class GetCommentsSanityTests extends RestTest
{
    @Autowired
    RestCommentsApi commentsAPI;
    
    @Autowired
    DataUser dataUser;
	
    private UserModel adminUserModel;
    
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userModel;
    private ListUserWithRoles usersWithRoles;
	
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        commentsAPI.useRestClient(restClient);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        commentsAPI.addComment(document, "This is a new comment");
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Admin user gets comments with Rest API and status code is 200")
    public void adminIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets comments created by admin user with Rest API and status code is 200")
    public void managerIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Contributor user gets comments created by admin user with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Collaborator user gets comments created by admin user with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Consumer user gets comments created by admin user with Rest API and status code is 200")
    public void consumerIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets status code 401 if authentication call fails")
    public void managerIsNotAbleToRetrieveCommentIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel nonexistentModel = new UserModel("nonexistentUser", "nonexistentPassword");
        restClient.authenticateUser(nonexistentModel);
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets comments created by another user and status code is 200")
    public void managerIsAbleToRetrieveCommentsCreatedByAnotherUser() throws JsonToModelConversionException, Exception
    {
        userModel = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(userModel);
        commentsAPI.addComment(document, "This is a new comment added by " + userModel.getUsername());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED); 
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify admin user gets comments created by another user and status code is 200")
    public void adminIsAbleToRetrieveCommentsCreatedByAnotherUser() throws JsonToModelConversionException, Exception
    {
        userModel = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(userModel);
        commentsAPI.addComment(document, "This is a new comment added by " + userModel.getUsername());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED); 
        restClient.authenticateUser(adminUserModel);
        commentsAPI.getNodeComments(document);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
