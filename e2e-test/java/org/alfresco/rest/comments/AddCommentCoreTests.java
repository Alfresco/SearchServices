package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestCommentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.LinkModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AddCommentCoreTests extends RestTest
{     
    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private String comment;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, 
                UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp()
    {
        comment = RandomData.getRandomName("comment1");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that invalid request returns status code 404 for nodeId that does not exist")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentUsingInvalidNodeId() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        file.setNodeRef(RandomStringUtils.randomAlphanumeric(20));
        
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingResource(file).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, file.getNodeRef()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that request using nodeId that is neither document or folder returns 405")
    @Bug(id = "MNT-16904")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentUsingResourceThatIsNotFileOrFolder() throws JsonToModelConversionException, Exception
    {
        LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
        FileModel fileWithNodeRefFromLink = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        fileWithNodeRefFromLink = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        fileWithNodeRefFromLink.setNodeRef(link.getNodeRef());

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().usingResource(fileWithNodeRefFromLink).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that adding comment using empty content returns 400 status code")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentUsingEmptyContent() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingResource(document).addComment("");                  
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NULL_ARGUMENT, "comment"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify adding comment with the same content as one existing comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentTwice() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingResource(document).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(document).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingResource(document).getNodeComments()
            .assertThat().entriesListContains("content", comment);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that comment can be retrieved after it is added")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentThenRetrieveComment() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingResource(document).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestCommentModelsCollection comments = restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(document).getNodeComments();                  
        restClient.assertStatusCodeIs(HttpStatus.OK);

        comments.assertThat().entriesListContains("content", comment);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify comment cannot be added if user is not member of private site")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentWithNonMemberOfPrivateSite() throws JsonToModelConversionException, Exception
    {
        UserModel member = dataUser.createRandomTestUser();
        SiteModel privateSite = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        FileModel file = dataContent.usingSite(privateSite).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(member)
                  .withCoreAPI().usingResource(file).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify comment cannot be added if empty network ID is provided")
    @Bug(id = "MNT-16904")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentUsingEmptyNetworkId() throws JsonToModelConversionException, Exception
    {
        UserModel member = dataUser.createRandomTestUser();
        member.setDomain("");
        
        restClient.authenticateUser(member)
                  .withCoreAPI().usingResource(document).addComment(comment);                  
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that comment cannot be added to another comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentToAComment() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        RestCommentModel commentEntry = restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingResource(file).addComment(comment);                  
        file.setNodeRef(commentEntry.getId());
        
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(file).addComment(comment);
        
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.CANNOT_COMMENT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.COMMENTS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that comment cannot be added to a tag")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
    public void addCommentToATag() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag("randomTag");
        
        file.setNodeRef(tag.getId());
        
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(file).addComment(comment);
        
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.CANNOT_COMMENT);
    }
}