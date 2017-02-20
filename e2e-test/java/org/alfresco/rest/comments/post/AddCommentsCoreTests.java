package org.alfresco.rest.comments.post;

import java.util.List;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestCommentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.LinkModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddCommentsCoreTests extends RestTest
{     
    private UserModel adminUserModel, networkUserModel;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private RestCommentModelsCollection comments;
    private String comment = "This is a new comment";
    private String comment2 = "This is the second comment";

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        networkUserModel = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify User can't add comments to a node with ID that does not exist and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void userCanNotAddCommentsOnNonexistentFile() throws Exception
    {       
        FileModel nonexistentFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        
        nonexistentFile.setNodeRef("ABC");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(nonexistentFile).addComments(comment,comment2);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
        .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, nonexistentFile.getNodeRef()));
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify User can't add comments to a node that exists but is not a document or a folder and status code is 405")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    @Bug(id = "MNT-16904")
    public void userCanNotAddCommentsOnLink() throws Exception
    { 
        LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
        FileModel fileWithNodeRefFromLink = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        fileWithNodeRefFromLink.setNodeRef(link.getNodeRef());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingResource(fileWithNodeRefFromLink).addComments(comment,comment2);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(RestErrorModel.UNABLE_TO_LOCATE);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify User can add comments with the same content as one existing comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void userCanAddCommentWithTheSameContentAsExistingOne() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        String sameComment = comment;
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(file).addComments(comment, sameComment)
            .assertThat().paginationExist().and().entriesListIsNotEmpty()
            .and().entriesListContains("content", comment)
            .and().entriesListContains("content", sameComment);          
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI()
            .usingResource(file).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListIsNotEmpty();
        List<RestCommentModel> commentsList = comments.getEntries();
        commentsList.get(0).onModel().assertThat().field("content").is(comment);
        commentsList.get(1).onModel().assertThat().field("content").is(sameComment);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify User can not add comments to a not joined private site. Status code returned is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void userCanNotAddCommentsToANotJoinedPrivateSite() throws Exception
    {
        SiteModel sitePrivateNotJoined = dataSite.createPrivateRandomSite();
        FileModel file = dataContent.usingSite(sitePrivateNotJoined).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(file).addComments(comment, comment2);         
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify add comments from node with invalid network id returns status code 401")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentsWithInvalidNetworkId() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        networkUserModel.setDomain("invalidNetwork");
        restClient.authenticateUser(networkUserModel).withCoreAPI().usingResource(file).addComments(comment,comment2);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify add comments from node with empty network id returns status code 401")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentsWithEmptyNetworkId() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
      
        networkUserModel.setDomain("");
        restClient.authenticateUser(networkUserModel).withCoreAPI().usingResource(file).addComments(comment,comment2);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that comments cannot be added to another comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentsToAComment() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        RestCommentModel commentEntry = restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingResource(file).addComment(comment);                  
        file.setNodeRef(commentEntry.getId());
        
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(file).addComments(comment, comment2);
        
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.CANNOT_COMMENT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that comments cannot be added to a tag")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentsToATag() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        RestTagModel tag = restClient.withCoreAPI().usingResource(file).addTag("randomTag");
        
        file.setNodeRef(tag.getId());
        
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(file).addComments(comment, comment2);
        
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.CANNOT_COMMENT);
    }
}
