package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModelsCollection;
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

public class GetNodeTagsFullTests extends RestTest
{
    private UserModel adminUserModel, userModel;
    private SiteModel siteModel;
    private FileModel document;
    private String tagValue;
    private String tagValue2;
    private RestTagModelsCollection returnedCollection;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);

        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        tagValue = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTag(tagValue);

        tagValue2 = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTag(tagValue2);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify site Manager is able to get node tags "
                    + "using properties parameter.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void siteManagerIsAbleToRetrieveNodeTagsWithPropertiesParameter() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withParams("properties=tag").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase())
            .and().entriesListDoesNotContain("id");
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify that Collaborator user is not able to get node tags "
                    + "using site id instead of node id.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void collaboratorGetNodeTagsUseSiteIdInsteadOfNodeId() throws JsonToModelConversionException, Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        file.setNodeRef(siteModel.getId());
        returnedCollection = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().usingResource(file).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, file.getNodeRef()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "With admin get node tags and use skipCount parameter. Check pagination and maxItems.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void useSkipCountCheckPaginationAndMaxItems() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(adminUserModel)
                .withParams("skipCount=1").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.getPagination().assertThat().field("maxItems").is(100)
            .and().field("hasMoreItems").is("false")
            .and().field("totalItems").is(2)
            .and().field("count").is("1");
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "With admin get node tags and use maxItems parameter. Check pagination.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void useMaxItemsParameterCheckPagination() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(adminUserModel)
                .withParams("maxItems=1").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.getPagination().assertThat().field("maxItems").is(1)
            .and().field("hasMoreItems").is("true")
            .and().field("totalItems").is("2")
            .and().field("count").is("1")
            .and().field("skipCount").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Get tags from a node to which user does not have access."
                    + " Check default error model schema")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void getTagsFromNodeWithUserWhoDoesNotHaveAccessCheckDefaultErrorModelSchema() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(userModel)
                .withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Using manager user get only one tag.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void usingManagerGetOnlyOneTag() throws JsonToModelConversionException, Exception
    {   
        FileModel file = dataContent.usingAdmin().usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.withCoreAPI().usingResource(file).addTag(tagValue);
        
        returnedCollection = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().usingResource(file).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.getPagination().assertThat().field("maxItems").is(100)
            .and().field("hasMoreItems").is("false")
            .and().field("totalItems").is("1")
            .and().field("count").is("1")
            .and().field("skipCount").is("0");
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Using admin get last 2 tags and skip first 2 tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void adminUserGetLast2TagsAndSkipFirst2Tags() throws JsonToModelConversionException, Exception
    {     
        String firstTag = "1st tag";
        String secondTag = "2nd tag";
        String thirdTag = "3rd tag";
        String fourthTag = "4th tag";
        FileModel file = dataContent.usingAdmin().usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addTag(firstTag);
        restClient.withCoreAPI().usingResource(file).addTag(secondTag);
        restClient.withCoreAPI().usingResource(file).addTag(thirdTag);
        restClient.withCoreAPI().usingResource(file).addTag(fourthTag);
        
        returnedCollection = restClient.withParams("skipCount=2").withCoreAPI().usingResource(file).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        returnedCollection.assertThat().entriesListContains("tag", thirdTag.toLowerCase())
            .and().entriesListContains("tag", fourthTag.toLowerCase());
        returnedCollection.getPagination().assertThat().field("maxItems").is(100)
            .and().field("hasMoreItems").is("false")
            .and().field("totalItems").is("4")
            .and().field("count").is("2")
            .and().field("skipCount").is("2");
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "With admin get node tags and use maxItems=0.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void getTagsWithZeroMaxItems() throws JsonToModelConversionException, Exception
    {     
        returnedCollection = restClient.authenticateUser(adminUserModel)
                .withParams("maxItems=0").withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that using high skipCount parameter returns status code 200.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void getTagsWithHighSkipCount() throws JsonToModelConversionException, Exception
    {        
        returnedCollection = restClient.authenticateUser(adminUserModel).withParams("skipCount=10000")
                .withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.getPagination().assertThat().field("maxItems").is(100)
            .and().field("hasMoreItems").is("false")
            .and().field("totalItems").is("2")
            .and().field("count").is("0")
            .and().field("skipCount").is("10000");
        returnedCollection.assertThat().entriesListCountIs(0);
    }
}
