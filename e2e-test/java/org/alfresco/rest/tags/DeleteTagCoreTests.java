package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DeleteTagCoreTests extends RestTest
{
    private UserModel adminUserModel, userModel;
    private SiteModel siteModel;
    private RestTagModel tag;
    private FileModel document;
    private FolderModel folderModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
    }
    
    @BeforeMethod
    public void setUp() throws Exception {
        restClient.authenticateUser(adminUserModel);
        tag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag")); 
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if user has no permission to remove tag returned status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void deleteTagWithUserWithoutPermission() throws Exception
    {
        restClient.authenticateUser(userModel);
        restClient.withCoreAPI().usingResource(document).deleteTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if node does not exist returned status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void deleteTagForAnInexistentNode() throws Exception
    {
        FileModel document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        String nodeRef = RandomStringUtils.randomAlphanumeric(10);
        document.setNodeRef(nodeRef);
        restClient.withCoreAPI().usingResource(document).deleteTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, nodeRef));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if tag does not exist returned status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void deleteTagThatDoesNotExist() throws Exception
    {
        tag.setId("abc");
        restClient.withCoreAPI().usingResource(document).deleteTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "abc"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if tag id is empty returned status code is 405")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void deleteTagWithEmptyId() throws Exception
    {
        tag.setId("");
        restClient.withCoreAPI().usingResource(document).deleteTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.DELETE_EMPTY_ARGUMENT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that file tag can be deleted")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void deleteFileTag() throws Exception
    {
        restClient.withCoreAPI().usingResource(document).deleteTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingResource(document).getNodeTags()
            .assertThat().entriesListDoesNotContain("tag", tag.getTag());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that folder tag can be deleted")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void deleteFolderTag() throws Exception
    {
        tag = restClient.withCoreAPI().usingResource(folderModel).addTag(RandomData.getRandomName("tag")); 
        restClient.withCoreAPI().usingResource(folderModel).deleteTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingResource(folderModel).getNodeTags()
            .assertThat().entriesListDoesNotContain("tag", tag.getTag());
    }
}