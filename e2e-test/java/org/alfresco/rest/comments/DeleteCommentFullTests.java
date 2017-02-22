package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
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

public class DeleteCommentFullTests extends RestTest
{
    private UserModel adminUserModel, networkUserModel;
    private SiteModel siteModel;
    private RestCommentModel commentModel;
    private RestCommentModelsCollection comments;
    private ListUserWithRoles usersWithRoles;
    private String comment = "This is a new comment";
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        networkUserModel = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteConsumer, 
                UserRole.SiteCollaborator, UserRole.SiteContributor);
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.COMMENTS }, 
            executionType = ExecutionType.REGRESSION, description = "Verify Manager user deletes comment created by admin"
            + " and status code is 204. Check with getComments for validation")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void managerIsAbleToDeleteCommentCreatedByOthers() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
         
        commentModel = restClient.authenticateUser(adminUserModel)
                .withCoreAPI().usingResource(file).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListDoesNotContain("content", comment);
        comments.getPagination().assertThat().field("totalItems").is("0").and().field("count").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.COMMENTS }, 
            executionType = ExecutionType.REGRESSION, description = "Verify Collaborator user can delete comment created by self"
            + " and status code is 204. Check with getComments for validation")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void collaboratorIsAbleToDeleteCommentCreatedBySelf() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingResource(file).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListDoesNotContain("content", comment);
        comments.getPagination().assertThat().field("totalItems").is("0").and().field("count").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.COMMENTS }, 
            executionType = ExecutionType.REGRESSION, description = "Verify Contributor user deletes comment created by self"
            + " and status code is 204. Check with getComments for validation")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    @Bug(id = "ACE-4614")
    public void contributorIsAbleToDeleteCommentCreatedBySelf() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
  
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
            .withCoreAPI().usingResource(file).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListDoesNotContain("content", comment);
        comments.getPagination().assertThat().field("totalItems").is("0").and().field("count").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.COMMENTS }, 
            executionType = ExecutionType.REGRESSION, description = "Verify Consumer user cannot delete comment created by admin"
            + " and status code is 403. Check with getComments for validation and check default error model schema.")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void consumerIsNotAbleToDeleteCommentCreatedByOthersDefaultErrorModelSchema() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
                    
        commentModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
            .withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                .statusCodeIs(HttpStatus.FORBIDDEN)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment);
        comments.getPagination().assertThat().field("totalItems").is("1").and().field("count").is("1");
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.COMMENTS }, 
            executionType = ExecutionType.REGRESSION, description = "Verify Manager can delete comment with version number")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void usingManagerDeleteCommentWithVersionNumber() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        dataContent.usingAdmin().usingResource(file).updateContent("updated content to increase version number");
    
        commentModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(comment);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.COMMENTS }, 
            executionType = ExecutionType.REGRESSION, description = "Verify Manager user cannot delete comment with invalid node "
                    + "and status code is 404. Check with getComments for validation")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void usingManagerDeleteCommentWithInvalidNode() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
      
        commentModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(comment);
        file.setNodeRef("invalid");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(file.getNodeRef() + " was not found");
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(file.getNodeRef() + " was not found");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS}, executionType = ExecutionType.REGRESSION,
            description = "Verify deleteComment from node with invalid network id returns status code 401")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void deleteCommentWithInvalidNetworkId() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(comment);
        networkUserModel.setDomain("invalidNetwork");
        restClient.authenticateUser(networkUserModel).withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);      
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.COMMENTS }, 
            executionType = ExecutionType.REGRESSION, description = "Verify deleteComment from node with empty network id returns status code 401")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void deleteCommentWithEmptyNetworkId() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        commentModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment(comment);
        networkUserModel.setDomain("");
        restClient.authenticateUser(networkUserModel).withCoreAPI().usingResource(file).deleteComment(commentModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

}
