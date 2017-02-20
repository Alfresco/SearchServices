package org.alfresco.rest.comments.put;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestCommentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateCommentFullTests extends RestTest
{     
    private UserModel adminUserModel, networkUserModel;
    private SiteModel siteModel;
    private RestCommentModel commentModel, returnedCommentModel;
    private RestCommentModelsCollection comments;
    private DataUser.ListUserWithRoles usersWithRoles;
    private String firstComment = "This is a new comment";
    private String updatedComment = "This is the updated comment";

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        networkUserModel = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite(); 
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, 
                UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify Manager user can update a comment with a large string")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void managerIsAbleToUpdateACommentWithALargeString() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String longString = RandomStringUtils.randomAlphanumeric(10000);
        
        commentModel = restClient.authenticateUser(adminUserModel)
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedCommentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().usingResource(file).updateComment(commentModel, longString);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCommentModel.assertThat().field("content").is(longString);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify Manager user can update a comment with a short string")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void managerIsAbleToUpdateACommentWithAShortString() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String shortString = RandomStringUtils.randomAlphanumeric(2);
        
        commentModel = restClient.authenticateUser(adminUserModel)
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedCommentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().usingResource(file).updateComment(commentModel, shortString);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCommentModel.assertThat().field("content").is(shortString);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify Collaborator user can update a comment with special characters")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void collaboratorIsAbleToUpdateACommentThatContainsSpecialChars() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String specialChars = "!@#$%^&*()'\".,<>-_+=|\\";
        
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedCommentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingResource(file).updateComment(commentModel, specialChars);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCommentModel.assertThat().field("content").is(specialChars);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Check that you cannot update comment with Consumer then call getComments and check new comment is not listed")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void cannotUpdateCommentWithConsumerCallGetComments() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", firstComment)
            .and().entriesListDoesNotContain("content", updatedComment)
            .and().paginationField("totalItems").is("1");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Update comment with Contributor then call getComments and check new comment is listed")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    @Bug(id = "ACE-4614")
    public void updateCommentWithContributorCallGetComments() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", updatedComment)
            .and().entriesListDoesNotContain("content", firstComment)
            .and().paginationField("totalItems").is("1");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Update comment with Collaborator then call getComments and check new comment is listed")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void updateCommentWithCollaboratorCallGetComments() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", updatedComment)
            .and().entriesListDoesNotContain("content", firstComment)
            .and().paginationField("totalItems").is("1");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Update comment with Manager then check modified by information in response")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void updateCommentWithManagerCheckModifiedBy() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        UserModel manager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedCommentModel = restClient.authenticateUser(manager).withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCommentModel.assertThat().field("modifiedBy.id").is(manager.getUsername())
            .and().field("content").is(updatedComment); 
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Delete comment with Admin then try to update it")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void deleteCommentThenTryToUpdateIt() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(adminUserModel)
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify Manager user can update a comment with multi byte content")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void managerIsAbleToUpdateACommentWithMultiByteContent() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String multiByte = "\ufeff\u6768\u6728\u91d1";
        
        commentModel = restClient.authenticateUser(adminUserModel)
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedCommentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().usingResource(file).updateComment(commentModel, multiByte);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCommentModel.assertThat().field("content").is(multiByte);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify Admin user can update a comment with properties parameter")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void adminIsAbleToUpdateACommentWithPropertiesParameter() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(adminUserModel)
        .withCoreAPI().usingResource(file).addComment(firstComment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        UserModel manager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        
        returnedCommentModel = restClient.authenticateUser(manager)
        .withParams("properties=createdBy,modifiedBy,canEdit,canDelete").withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCommentModel.assertThat().field("createdBy.id").is(adminUserModel.getUsername())
            .assertThat().field("modifiedBy.id").is(manager.getUsername())
            .assertThat().fieldsCount().is(4);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Update comment with invalid node")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void updateCommentUsingInvalidNodeId() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingResource(file).addComment(firstComment);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        file.setNodeRef(RandomStringUtils.randomAlphanumeric(20));
        restClient.withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, file.getNodeRef()));
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify update comment from node with invalid network id returns status code 401")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void updateCommentWithInvalidNetworkId() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(firstComment);
        networkUserModel.setDomain("invalidNetwork");
        restClient.authenticateUser(networkUserModel).withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED); 
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
               description= "Verify User can not update comment to a not joined private site. Status code returned is 403")
        @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
        public void userCanNotUpdateCommentToANotJoinedPrivateSiteDefaultErrorModelSchema() throws Exception
        {
            UserModel newUser = dataUser.createRandomTestUser();
            FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
            
            commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
               .withCoreAPI().usingResource(file).addComment(firstComment);         
            restClient.assertStatusCodeIs(HttpStatus.CREATED);
            
            restClient.authenticateUser(newUser).withCoreAPI().usingResource(file).updateComment(commentModel, updatedComment);
            restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                    .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                    .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                    .stackTraceIs(RestErrorModel.STACKTRACE)
                    .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY);
        }
}