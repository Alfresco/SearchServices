package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
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
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetTagTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private FileModel document;
    private String tagValue;
    private RestTagModel tag;
    
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

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify admin user gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void adminIsAbleToGetTag() throws Exception
    {
        RestTagModel returnedTag = restClient.authenticateUser(adminUserModel).withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void userWithManagerRoleIsAbleToGetTag() throws Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase())
                   .assertThat().field("id").is(tag.getId());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify user with Collaborator role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void userWithCollaboratorRoleIsAbleToGetTag() throws Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify user with Contributor role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void userWithContributorRoleIsAbleToGetTag() throws Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify user with Consumer role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void userWithConsumerRoleIsAbleToGetTag() throws Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets status code 401 if authentication call fails")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
//    @Bug(id="MNT-16904", description = "It fails only on environment with tenants")
    public void managerIsNotAbleToGetTagIfAuthenticationFails() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        managerUser.setPassword("wrongPassword");
        restClient.authenticateUser(managerUser).withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that if tag id is invalid status code returned is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void invalidTagIdTest() throws Exception
    {
        tag.setId("random_tag_value");
        restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "random_tag_value"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Check that properties filter is applied when getting tag using Manager user.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void checkPropertiesFilterIsApplied() throws Exception
    {
        RestTagModel returnedTag = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withParams("properties=id,tag").withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("id").is(tag.getId())
                .assertThat().field("tag").is(tag.getTag().toLowerCase())
                .assertThat().fieldsCount().is(2);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Check that Manager user can get tag of a folder.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void getTagOfAFolder() throws Exception
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
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION })
    public void checkDefaultErrorModelSchema() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withParams("skipCount=abc").withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsErrorKey(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"))
                .containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}