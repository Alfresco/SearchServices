package org.alfresco.rest.tags.get;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetTagSanityTests extends RestTest
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
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify admin user gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void userWithAdminIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
      
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void userWithManagerRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void userWithCollaboratorRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void userWithContributorRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        RestTagModel returnedTag = restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tagValue.toLowerCase());
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role gets tag using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void userWithConsumerRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
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
    @Bug(id="MNT-16904", description = "It fails only on environment with tenants")
    public void managerIsNotAbleToGetTagIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(managerUser);
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        managerUser.setPassword("wrongPassword");
        restClient.authenticateUser(managerUser);
        restClient.withCoreAPI().getTag(tag);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
    
}