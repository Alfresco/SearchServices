package org.alfresco.rest;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
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

@Test(groups = { "rest-api", "sites", "sanity" })
public class GetSiteTests extends RestTest
{

    @Autowired
    DataUser dataUser;

    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataSite dataSite;

    private UserModel adminUserModel;
    private HashMap<UserRole, UserModel> usersWithRoles;
    private SiteModel siteModel;

    @BeforeClass
    public void initTest() throws DataPreparationException
    {
        adminUserModel = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUserModel);
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                Arrays.asList(UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor));
    }

    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role gets site information and gets status code OK (200)")
    public void getSiteWithManagerRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        siteAPI.getSite(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role gets site information and gets status code OK (200)")
    public void getSiteWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteCollaborator));
        siteAPI.getSite(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role gets site information and gets status code OK (200)")
    public void getSiteWithContributorRole() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteContributor));
        siteAPI.getSite(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
}
