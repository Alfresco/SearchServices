package org.alfresco.rest.tags.post;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.model.RestTagModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AddTagFullTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private String tagValue;
    private RestTagModel returnedModel;
    private RestCommentModel returnedModelComment;
    private RestTagModelsCollection returnedModelTags;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @BeforeMethod(alwaysRun = true)
    public void generateRandomTag() throws DataPreparationException, Exception
    {
        tagValue = RandomData.getRandomName("tag");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that contributor is able to tag a folder created by self")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void contributorIsAbleToTagAFolderCreatedBySelf() throws JsonToModelConversionException, Exception
    {
        FolderModel folderModel = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).usingSite(siteModel).createFolder();
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        returnedModel = restClient.withCoreAPI().usingResource(folderModel).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is(tagValue).and().field("id").isNotEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that collaborator is able to tag a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void collaboratorIsAbleToTagAFolder() throws JsonToModelConversionException, Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        returnedModel = restClient.withCoreAPI().usingResource(folderModel).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is(tagValue).and().field("id").isNotEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that consumer is not able to tag a folder. Check default error model schema.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void consumerIsNotAbleToTagAFolder() throws JsonToModelConversionException, Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
            .withCoreAPI().usingResource(folderModel).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that tagged folder can be tagged again")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void addTagToATaggedFolder() throws JsonToModelConversionException, Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        
        returnedModel = restClient.withCoreAPI().usingResource(folderModel).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is(tagValue).and().field("id").isNotEmpty();
        
        returnedModel = restClient.withCoreAPI().usingResource(folderModel).addTag("random_tag_value");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is("random_tag_value").and().field("id").isNotEmpty();
        
        restClient.withCoreAPI().usingResource(folderModel).getNodeTags().assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", "random_tag_value")
            .and().entriesListCountIs(2);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Using collaborator provide more than one tag element")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void provideMoreThanOneTagElement() throws JsonToModelConversionException, Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        String tagValue1 = RandomData.getRandomName("tag1");
        String tagValue2 = RandomData.getRandomName("tag2");

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        
        returnedModelTags = restClient.withCoreAPI().usingResource(folderModel).addTags(tagValue, tagValue1, tagValue2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModelTags.assertThat().entriesListContains("tag", tagValue)
        .and().entriesListContains("tag", tagValue1)
        .and().entriesListContains("tag", tagValue2)
        .and().entriesListCountIs(3);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager cannot add tag with special characters.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void addTagWithSpecialCharacters() throws JsonToModelConversionException, Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        String specialCharsTag = "!@#$%^&*()'\".,<>-_+=|\\";
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(folderModel).addTag(specialCharsTag);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_TAG, "|"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that you cannot tag a comment and it returns status code 405")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void addTagToAComment() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        String comment = "comment for a tag";
        
        restClient.authenticateUser(adminUserModel);
        returnedModelComment = restClient.withCoreAPI().usingResource(file).addComment(comment);
        file.setNodeRef(returnedModelComment.getId());
        restClient.withCoreAPI().usingResource(file).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.CANNOT_TAG);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that you cannot tag a tag and it returns status code 405")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void addTagToATag() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUserModel);
        returnedModel = restClient.withCoreAPI().usingResource(file).addTag(tagValue);
        file.setNodeRef(returnedModel.getId());
        restClient.withCoreAPI().usingResource(file).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.CANNOT_TAG);
    }
}
