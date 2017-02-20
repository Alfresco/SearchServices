package org.alfresco.rest.tags.get;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.model.RestTagModelsCollection;
import org.alfresco.utility.Utility;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetTagsFullTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private RestTagModelsCollection returnedCollection;
    
    private String tagValue, tagValue2;
    private FileModel document;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
        document = dataContent.usingUser(adminUserModel).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);

        tagValue = RandomData.getRandomName("tag");
        tagValue2 = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTags(tagValue, tagValue2);
        
        Utility.waitToLoopTime(20);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify site Manager is able to get tags using properties parameter."
                    + "Check that properties filter is applied.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void siteManagerIsAbleToRetrieveTagsWithPropertiesParameter() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withParams("maxItems=5000&properties=tag").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase())
            .and().entriesListDoesNotContain("id");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "With admin get tags and use skipCount parameter. Check pagination")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void useSkipCountCheckPagination() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(adminUserModel).withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        RestTagModel firstTag = returnedCollection.getEntries().get(0).onModel();
        RestTagModel secondTag = returnedCollection.getEntries().get(1).onModel();
        RestTagModelsCollection tagsWithSkipCount = restClient.withParams("skipCount=2").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        tagsWithSkipCount.assertThat().entriesListDoesNotContain("tag", firstTag.getTag())
            .assertThat().entriesListDoesNotContain("tag", secondTag.getTag());
        tagsWithSkipCount.assertThat().paginationField("skipCount").is("2");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "With admin get tags and use maxItems parameter. Check pagination")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void useMaxItemsParameterCheckPagination() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(adminUserModel).withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        RestTagModel firstTag = returnedCollection.getEntries().get(0).onModel();
        RestTagModel secondTag = returnedCollection.getEntries().get(1).onModel();
        RestTagModelsCollection tagsWithMaxItems = restClient.withParams("maxItems=2").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        tagsWithMaxItems.assertThat().entriesListContains("tag", firstTag.getTag())
            .assertThat().entriesListContains("tag", secondTag.getTag())
            .assertThat().entriesListCountIs(2);
        tagsWithMaxItems.assertThat().paginationField("maxItems").is("2");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "With manager get tags and use high skipCount parameter. Check pagination")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void useHighSkipCountCheckPagination() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withParams("skipCount=20000").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsEmpty()
            .getPagination().assertThat().field("maxItems").is(100)
            .and().field("hasMoreItems").is("false")
            .and().field("count").is("0")
            .and().field("skipCount").is(20000)
            .and().field("totalItems").isNull();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "With Collaborator user get tags and use maxItems with value zero. Check default error model schema")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void useMaxItemsWithValueZeroCheckDefaultErrorModelSchema() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                .withParams("maxItems=0").withCoreAPI().getTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
            .containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);  
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "With Manager user delete tag. Check it is not retrieved anymore.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void checkThatDeletedTagIsNotRetrievedAnymore() throws JsonToModelConversionException, Exception
    {     
        String removedTag = RandomData.getRandomName("tag3");
        
        RestTagModel deletedTag = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(document).addTag(removedTag);

        restClient.withCoreAPI().usingResource(document).deleteTag(deletedTag);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        returnedCollection = restClient.withParams("maxItems=5000").withCoreAPI().getTags();
        returnedCollection.assertThat().entriesListIsNotEmpty()
            .and().entriesListDoesNotContain("tag", removedTag.toLowerCase());
    }
}
