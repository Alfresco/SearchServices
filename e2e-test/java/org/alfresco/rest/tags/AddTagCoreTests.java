package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AddTagCoreTests extends RestTest
{
    private UserModel adminUserModel;
    private UserModel testUser;
    private FileModel document;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private String tagValue;
    private RestTagModel returnedModel;
    private FolderModel folderModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
    }

    @BeforeMethod(alwaysRun = true)
    public void generateRandomTag()
    {
        tagValue = RandomData.getRandomName("tag");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that adding empty tag returns status code 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void emptyTagTest() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag("");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.NULL_ARGUMENT, "tag"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that adding tag with user that has no permissions returns status code 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void addTagWithUserThatDoesNotHavePermissions() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(testUser);
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that adding tag to a node that does not exist returns status code 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void addTagToInexistentNode() throws JsonToModelConversionException, Exception
    {
        FileModel document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        String nodeRef = RandomStringUtils.randomAlphanumeric(10);
        document.setNodeRef(nodeRef);
        
        restClient.authenticateUser(adminUserModel);
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, nodeRef));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that adding tag to a node that does not accepts tags returns status code 405")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void addTagToANodeThatDoesNotAcceptsTags() throws JsonToModelConversionException, Exception
    {
        FileModel document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUserModel);
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        document.setNodeRef(returnedModel.getId());
        restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.CANNOT_TAG);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to tag a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void managerIsAbleToTagAFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is(tagValue).and().field("id").isNotEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that manager is able to tag a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void managerIsAbleToTagAFolder() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        returnedModel = restClient.withCoreAPI().usingResource(folderModel).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is(tagValue).and().field("id").isNotEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that tagged file can be tagged again")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void addTagToATaggedFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is(tagValue).and().field("id").isNotEmpty();
        
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is(tagValue).and().field("id").isNotEmpty();
        
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag("random_tag_value");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedModel.assertThat().field("tag").is("random_tag_value").and().field("id").isNotEmpty();
        
        restClient.withCoreAPI().usingResource(document).getNodeTags().assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", "random_tag_value");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that user cannot add invalid tag")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void addInvalidTag() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag("-1~!|@#$%^&*()_=");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_TAG, "|"));
    }
}