package org.alfresco.rest.tags;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestTagsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "tags", "sanity" })
public class GetTagsSanityTests extends RestTest
{
    @Autowired
    private DataUser dataUser;
        
    @Autowired
    private RestTagsApi tagsAPI;
    
    private UserModel adminUserModel;
    private UserModel userModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
        tagsAPI.useRestClient(restClient);
    }
    
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role gets tags using REST API and status code is OK (200)")
    public void getTagsWithManagerRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        tagsAPI.getTags();
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role gets tags using REST API and status code is OK (200)")
    public void getTagsWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        tagsAPI.getTags();
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role gets tags using REST API and status code is OK (200)")
    public void getTagsWithContributorRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        tagsAPI.getTags();
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role gets tags using REST API and status code is OK (200)")
    public void getTagsWithConsumerRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        tagsAPI.getTags();
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Verify Admin user gets tags using REST API and status code is OK (200)")
    public void getTagsWithAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        tagsAPI.getTags();
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "tags" }, executionType = ExecutionType.SANITY, description = "Failed authentication get tags call returns status code 401 with Manager role")
    public void getTagsWithManagerRoleFailedAuth() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel);
        tagsAPI.getTags();
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
