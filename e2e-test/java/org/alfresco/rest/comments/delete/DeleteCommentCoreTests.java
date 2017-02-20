package org.alfresco.rest.comments.delete;

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
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DeleteCommentCoreTests extends RestTest
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
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteConsumer);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify Consumer user can't delete comments created by admin user and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void consumerIsNotAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        comment = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify Manager user deletes comments created by admin and status code is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void managerIsAbleToDeleteComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);        
        comment = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify Admin user can't delete comments with inexistent ID and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void userIsNotAbleToDeleteInexistentComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        comment = new RestCommentModel();
        comment.setId("inexistent");
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);        
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify Admin user can't delete comments with inexistend NodeId and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void userIsNotAbleToDeleteCommentWithInexistentNodeId() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        comment = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment");
        FileModel inexistentDocument = new FileModel();
        inexistentDocument.setNodeRef("inexistent");
        restClient.withCoreAPI().usingResource(inexistentDocument).deleteComment(comment);        
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify Admin user can't delete deleted comments and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void userIsNotAbleToDeleteDeletedComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        comment = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment");
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);        
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingResource(document).deleteComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
