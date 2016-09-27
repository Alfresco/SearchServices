package org.alfresco.rest;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.UserRole;
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
public class GetCommentsTest extends RestTest
{
    @Autowired
    RestCommentsApi commentsAPI;
    
    @Autowired
    DataUser dataUser;
	
    private UserModel adminUserModel;
    
    private FileModel document;
    private SiteModel siteModel;
    private HashMap<UserRole, UserModel> usersWithRoles;
	
    @BeforeClass
    public void initTest() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        commentsAPI.useRestClient(restClient);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        commentsAPI.addComment(document.getNodeRef(), "This is a new comment");
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, Arrays.asList(UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor));
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Admin user gets comments with Rest API and status code is 200")
    public void adminIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        commentsAPI.getNodeComments(document.getNodeRef());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user gets comments created by admin user with Rest API and status code is 200")
    public void managerIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        commentsAPI.getNodeComments(document.getNodeRef());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Contributor user gets comments created by admin user with Rest API and status code is 200")
    public void contributorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteContributor));
        commentsAPI.getNodeComments(document.getNodeRef());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Collaborator user gets comments created by admin user with Rest API and status code is 200")
    public void collaboratorIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteCollaborator));
        commentsAPI.getNodeComments(document.getNodeRef());
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
}
