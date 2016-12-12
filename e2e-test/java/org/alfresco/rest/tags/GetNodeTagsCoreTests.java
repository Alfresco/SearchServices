package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
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
import org.testng.annotations.Test;

public class GetNodeTagsCoreTests extends RestTest
{
    private UserModel adminUserModel, userModel;
    private SiteModel siteModel;
    private FileModel document;
    private String tagValue;
    private String tagValue2;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        tagValue = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        tagValue2 = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTag(tagValue2);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that using invalid value for skipCount parameter returns status code 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void invalidSkipCountTest() throws JsonToModelConversionException, Exception
    {        
        restClient.withParams("skipCount=abc").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"));
    
        restClient.withParams("skipCount=-1").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that using invalid value for maxItems parameter returns status code 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void invalidMaxItemsTest() throws JsonToModelConversionException, Exception
    {        
        restClient.withParams("maxItems=abc").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "abc"));
    
        restClient.withParams("maxItems=-1").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that user without permissions returns status code 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void userWithoutPermissionsTest() throws JsonToModelConversionException, Exception
    {        
        SiteModel site = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        FileModel document = dataContent.usingSite(site).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).addTag(tagValue);

        restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if node does not exist returns status code 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void inexistentNodeTest() throws JsonToModelConversionException, Exception
    {        
        FileModel document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        String nodeRef = RandomStringUtils.randomAlphanumeric(10);
        document.setNodeRef(nodeRef);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, nodeRef));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if node id is empty returns status code 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void emptyNodeIdTest() throws JsonToModelConversionException, Exception
    {        
        FileModel document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document.setNodeRef("");
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify file tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void fileTagsTest() throws JsonToModelConversionException, Exception
    {        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getNodeTags()
            .assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase()); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify folder tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void folderTagsTest() throws JsonToModelConversionException, Exception
    {        
        FolderModel folder = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        
        tagValue = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(folder).addTag(tagValue);
        tagValue2 = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(folder).addTag(tagValue2);

        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(folder).getNodeTags()
            .assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase()); 
    }
}