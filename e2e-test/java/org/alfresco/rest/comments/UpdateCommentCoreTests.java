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
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.LinkModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateCommentCoreTests extends RestTest
{
    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private RestCommentModel commentModel;
    private ListUserWithRoles usersWithRoles;
    private RestCommentModelsCollection comments;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify can not update comment if NodeId is neither document or folder and returns status code 405")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void canNotUpdateCommentIfNodeIdIsNeitherDocumentOrFolder() throws JsonToModelConversionException, Exception
    {
        FileModel content = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        content = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUserModel);
        commentModel = restClient.withCoreAPI().usingResource(content).addComment("This is a new comment");
        
        LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
        content.setNodeRef(link.getNodeRef().replace("workspace://SpacesStore/", "workspace%3A%2F%2FSpacesStore%2F"));
        
        restClient.withCoreAPI().usingResource(content).updateComment(commentModel, "This is the updated comment.");                
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError()
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, content.getNodeRef()))
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify Admin user is not able to update with empty comment body and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void adminIsNotAbleToUpdateWithEmptyCommentBody() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        commentModel = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment added by admin");
        restClient.withCoreAPI().usingResource(document).updateComment(commentModel, "");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
        .assertLastError().containsSummary("An invalid argument was received");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify updated comment by Manager is listed when calling getComments and status code is 200")
//    @Bug(id="REPO-1011")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void updatedCommentByManagerIsListed() throws JsonToModelConversionException, Exception
    {
        commentModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                .withCoreAPI().usingResource(document).addComment("This is a new comment added by collaborator");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI()
        .usingResource(document).updateComment(commentModel, "This is the updated comment with Manager user"); 
        comments = restClient.withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);   
        comments.assertThat().entriesListContains("content", "This is the updated comment with Manager user");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify Collaborator user can not update comments of another user and status code is 200")
//    @Bug(id="MNT-2502",description="seems it's one old issue: also logged as MNT-2502, MNT-2346")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void collaboratorIsNotAbleToUpdateCommentOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        commentModel = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment added by admin");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingResource(document).updateComment(commentModel, "This is the updated comment with Collaborator user");        
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, description = "Verify entry content in response")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void checkEntryContentInResponse() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        commentModel = restClient.withCoreAPI().usingResource(document).addComment("This is a new comment added by admin");
        commentModel = restClient.withCoreAPI().usingResource(document).updateComment(commentModel, "This is the updated comment with admin user");        
        restClient.assertStatusCodeIs(HttpStatus.OK);   
        commentModel.assertThat().field("content").is("This is the updated comment with admin user");
    }
}
