package org.alfresco.rest;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { "rest-api", "comments", "sanity" })
public class GetSitesTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private UserModel adminUserModel;
    private UserModel userModel;
    private HashMap<UserRole, UserModel> usersWithRoles;
    private SiteModel siteModel;

    @BeforeClass
    public void initTest() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                Arrays.asList(UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor));

    }

    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role gets sites information and gets status code OK (200)")
    public void getSitesWithManagerRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        siteAPI.getAllSites();
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role gets sites information and gets status code OK (200)")
    public void getSitesWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteCollaborator));
        siteAPI.getAllSites();
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role gets sites information and gets status code OK (200)")
    public void getSitesWithContributorRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteContributor));
        siteAPI.getAllSites();
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role gets sites information and gets status code OK (200)")
    public void getSitesWithConsumerRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteConsumer));
        siteAPI.getAllSites();
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Admin user gets sites information and gets status code OK (200)")
    public void getSitesWithAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        siteAPI.getAllSites();
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Failed authentication get sites call returns status code 401 with Manager role")
    public void getSitesWithManagerRoleFailedAuth() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel);
        siteAPI.getAllSites();
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED.toString());
    }
}
