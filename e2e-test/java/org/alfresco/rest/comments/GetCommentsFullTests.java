package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestCommentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetCommentsFullTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private String comment = "This is a new comment";
    private String comment2 = "This is the second comment";
    private RestCommentModelsCollection comments;
    private DataUser.ListUserWithRoles usersWithRoles;
    private FileModel file;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, 
                UserRole.SiteContributor, UserRole.SiteConsumer);
    }
    
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws DataPreparationException, Exception {
        file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify that if manager adds one comment, it will be returned in getComments response")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addCommentWithManagerAndCheckThatCommentIsReturned() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(file).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment)
            .getPagination().assertThat().field("totalItems").is("1")
            .assertThat().field("count").is("1");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify that if collaborator adds one comment, it will be returned in getComments response")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addCommentWithCollaboratorAndCheckThatCommentIsReturned() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingResource(file).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment)
            .getPagination().assertThat().field("totalItems").is("1")
            .assertThat().field("count").is("1");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify that if contributor adds one comment, it will be returned in getComments response")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    @Bug(id = "ACE-4614")
    public void addCommentWithContributorAndCheckThatCommentIsReturned() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
            .withCoreAPI().usingResource(file).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment)
            .getPagination().assertThat().field("totalItems").is("1")
            .assertThat().field("count").is("1");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify that consumer cannot add a comment and no comments will be returned in getComments response")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addCommentWithConsumerAndCheckThatCommentIsNotReturned() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
            .withCoreAPI().usingResource(file).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().paginationField("totalItems").is("0");
        comments.assertThat().paginationField("count").is("0");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Add one comment with Manager and check that returned person is the right one")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addCommentWithManagerCheckReturnedPersonIsTheRightOne() throws Exception
    {
        UserModel user1 = dataUser.createRandomTestUser();
        dataUser.addUserToSite(user1, siteModel, UserRole.SiteManager);
        
        restClient.authenticateUser(user1).withCoreAPI().usingResource(file).addComment(comment);    
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        comments.getOneRandomEntry().onModel().getCreatedBy().assertThat().field("firstName").is(user1.getUsername() + " FirstName")
            .assertThat().field("lastName").is("LN-" + user1.getUsername());        
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Add one comment with Collaborator and check that returned company details are correct")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addCommentWithCollaboratorCheckReturnedCompanyDetails() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(file).addComment(comment);    
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        comments.getOneRandomEntry().onModel().getCreatedBy().getCompany()
                .assertThat().field("organization").isNull()
                .assertThat().field("address1").isNull()
                .assertThat().field("address2").isNull()
                .assertThat().field("address3").isNull()
                .assertThat().field("postcode").isNull()
                .assertThat().field("telephone").isNull()
                .assertThat().field("fax").isNull()
                .assertThat().field("email").isNull();        
    }  
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Add 2 comments with Manager and Collaborator users and verify valid request using skipCount. Check that param is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addTwoCommentsWithManagerCollaboratorVerifySkipCountParamIsApplied() throws Exception
    {   
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(file).addComment(comment);    
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(file).addComment(comment2);    
        
        comments = restClient.authenticateUser(adminUserModel).withParams("skipCount=1")
                .withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment)
                .and().paginationField("count").is("1");
        comments.assertThat().paginationField("skipCount").is("1");
        comments.assertThat().paginationField("totalItems").is("2");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Add 2 comments with Admin and Collaborator users and verify valid request using maxItems. Check that param is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addTwoCommentsWithAdminCollaboratorVerifyMaxItemsParamIsApplied() throws Exception
    {   
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(comment);    
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(file).addComment(comment2);    
        
        comments = restClient.authenticateUser(adminUserModel).withParams("maxItems=1")
                .withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment2)
                .and().paginationField("count").is("1");
        comments.assertThat().paginationField("totalItems").is("2");
    }
  
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Add 2 comments with Manager and Admin users and verify valid request using properties. Check that param is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addTwoCommentsWithAdminManagerVerifyPropertiesParamIsApplied() throws Exception
    {   
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(comment);    
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingResource(file).addComment(comment2);    
        
        comments = restClient.authenticateUser(adminUserModel).withParams("properties=createdBy,modifiedBy")
                .withCoreAPI().usingResource(file).getNodeComments();
        comments.assertThat().entriesListIsNotEmpty()
                .and().paginationField("count").is("2");
        comments.assertThat().paginationField("totalItems").is("2");
        
        comments.getEntries().get(0).onModel().getCreatedBy()
            .assertThat().field("firstName").is(usersWithRoles.getOneUserWithRole(UserRole.SiteManager).getUsername() + " FirstName")
            .assertThat().field("lastName").is("LN-" + usersWithRoles.getOneUserWithRole(UserRole.SiteManager).getUsername());     
        
        comments.getEntries().get(1).onModel().getCreatedBy()
            .assertThat().field("firstName").is("Administrator")
            .assertThat().field("id").is("admin");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Check default error model schema")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addTwoCommentsWithManagerCheckDefaultErrorModelSchema() throws Exception
    {   
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(file).addComments(comment, comment2);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).usingParams("maxItems=0").getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);
        restClient.assertLastError().containsErrorKey(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);
        restClient.assertLastError().containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);
        restClient.assertLastError().descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
        restClient.assertLastError().stackTraceIs(RestErrorModel.STACKTRACE);
    }
}