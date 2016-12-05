package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestTagModelsCollection;
import org.alfresco.utility.Utility;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
public class GetTagsCoreTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    
    private String tagValue;
    private String tagValue2;
    private FileModel document;
    private RestTagModelsCollection returnedCollection;
    private FolderModel folder;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingUser(adminUserModel).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        folder = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
        
        tagValue = RandomData.getRandomName("tag");
        tagValue2 = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTags(tagValue);
        restClient.withCoreAPI().usingResource(folder).addTags(tagValue2);
        
        Utility.waitToLoopTime(60);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if maxItems is invalid status code returned is 400")
    public void maxItemsInvalidValueTest() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withParams("maxItems=abc").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(ErrorModel.INVALID_MAXITEMS, "abc"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that if skipCount is invalid status code returned is 400")
    public void skipCountInvalidValueTest() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withParams("skipCount=abc").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(ErrorModel.INVALID_SKIPCOUNT, "abc"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that file tag is retrieved")
    public void fileTagIsRetrieved() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        returnedCollection = restClient.withParams("maxItems=500").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("tag", tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that folder tag is retrieved")
    public void folderTagIsRetrieved() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        returnedCollection = restClient.withParams("maxItems=500").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("tag", tagValue2.toLowerCase());
    }
}