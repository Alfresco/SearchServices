package org.alfresco.rest.sites;

import java.util.List;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestSiteContainerModel;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
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

/**
 * @author iulia.cojocea
 */
@Test(groups = { "rest-api", "sites", "sanity" })
public class GetSiteContainerSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private List<RestSiteContainerModel> listOfFoldersIds;

    @BeforeClass(alwaysRun=true)
    public void initTest() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site container and gets status code OK (200)")
    public void getSiteContainerWithManagerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        listOfFoldersIds = siteAPI.getSiteContainers(siteModel).getSiteContainersList();
        siteAPI.getSiteContainer(siteModel, listOfFoldersIds.get(0));
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site container and gets status code OK (200)")
    public void getSiteContainerWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        listOfFoldersIds = siteAPI.getSiteContainers(siteModel).getSiteContainersList();
        siteAPI.getSiteContainer(siteModel, listOfFoldersIds.get(0));
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site container and gets status code OK (200)")
    public void getSiteContainerWithContributorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        listOfFoldersIds = siteAPI.getSiteContainers(siteModel).getSiteContainersList();
        siteAPI.getSiteContainer(siteModel, listOfFoldersIds.get(0));
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
