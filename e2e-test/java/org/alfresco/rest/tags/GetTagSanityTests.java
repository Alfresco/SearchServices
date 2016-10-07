package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.requests.RestTagsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "tags", "sanity" })
public class GetTagSanityTests extends RestTest
{

    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestTagsApi tagsAPI;

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
        tagsAPI.useRestClient(restClient);
    }
    
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets tag using REST API and status code is OK (200)")
    public void userWithAdminIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        RestTagModel tag = tagsAPI.addTag(document, tagValue);
      
        tagsAPI.getTag(tag).assertTagIs(tagValue.toLowerCase());
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role gets tag using REST API and status code is OK (200)")
    public void userWithManagerRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        RestTagModel tag = tagsAPI.addTag(document, tagValue);
       
        tagsAPI.getTag(tag).assertTagIs(tagValue.toLowerCase());
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role gets tag using REST API and status code is OK (200)")
    public void userWithCollaboratorRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        RestTagModel tag = tagsAPI.addTag(document, tagValue);
        
        tagsAPI.getTag(tag).assertTagIs(tagValue.toLowerCase());
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role gets tag using REST API and status code is OK (200)")
    public void userWithContributorRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        RestTagModel tag = tagsAPI.addTag(document, tagValue);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        tagsAPI.getTag(tag).assertTagIs(tagValue.toLowerCase());
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role gets tag using REST API and status code is OK (200)")
    public void userWithConsumerRoleIsAbletoGetTag() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        restClient.authenticateUser(adminUserModel);
        RestTagModel tag = tagsAPI.addTag(document, tagValue);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        tagsAPI.getTag(tag).assertTagIs(tagValue.toLowerCase());
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify Manager user gets status code 401 if authentication call fails")
    public void managerIsNotAbleToGetTagIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        String tagValue = RandomData.getRandomName("tag");
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(managerUser);
        RestTagModel tag = tagsAPI.addTag(document, tagValue);
        
        managerUser.setPassword("wrongPassword");
        restClient.authenticateUser(managerUser);
        tagsAPI.getTag(tag);
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
    
}