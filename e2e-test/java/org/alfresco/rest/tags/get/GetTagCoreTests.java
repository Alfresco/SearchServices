package org.alfresco.rest.tags.get;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetTagCoreTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private FileModel document;
    private String tagValue;
    private RestTagModel tag;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
    }
    
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if tag id is invalid status code returned is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void invalidTagIdTest() throws JsonToModelConversionException, Exception
    {
        tag.setId("random_tag_value");
        restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "random_tag_value"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Retrieve and validate the id of a tag added to a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void validateTagIdTest() throws JsonToModelConversionException, Exception
    {
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("id").is(tag.getId());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Retrieve and validate the name of a tag added to a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void validateTagNameTest() throws JsonToModelConversionException, Exception
    {
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
}