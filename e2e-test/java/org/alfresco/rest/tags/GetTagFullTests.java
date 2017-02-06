package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
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

public class GetTagFullTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private FileModel document;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.REGRESSION, description = "Check that properties filter is applied when getting tag using Manager user.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void checkPropertiesFilterIsApplied() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
      
        RestTagModel returnedTag = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withParams("properties=id,tag").withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("id").is(tag.getId())
                    .assertThat().field("tag").is(tag.getTag().toLowerCase())
                    .assertThat().fieldsCount().is(2);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.REGRESSION, description = "Check that Manager user can get tag of a folder.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void getTagOfAFolder() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tagFolder");
        FolderModel folder = dataContent.usingAdmin().usingSite(siteModel).createFolder();
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        RestTagModel tag = restClient.withCoreAPI().usingResource(folder).addTag(tagValue);
      
        RestTagModel returnedTag = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.REGRESSION, description = "Check default error model schema. Use invalid skipCount parameter.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void checkDefaultErrorModelSchema() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
      
        restClient.withParams("skipCount=abc").withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"))
            .containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}