package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSiteContainerModel;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
public class GetSiteContainerSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private RestSiteContainerModel siteContainerModel;
    private UserModel userModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site container and gets status code OK (200)")
    public void getSiteContainerWithManagerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteContainerModel = siteAPI.getSiteContainers(siteModel).getOneRandomEntry();
        siteAPI.getSiteContainer(siteModel, siteContainerModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site container and gets status code OK (200)")
    public void getSiteContainerWithCollaboratorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        siteContainerModel = siteAPI.getSiteContainers(siteModel).getOneRandomEntry();
        siteAPI.getSiteContainer(siteModel, siteContainerModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site container and gets status code OK (200)")
    public void getSiteContainerWithContributorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        siteContainerModel = siteAPI.getSiteContainers(siteModel).getOneRandomEntry();
        siteAPI.getSiteContainer(siteModel, siteContainerModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site container and gets status code OK (200)")
    public void getSiteContainerWithConsumerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        siteContainerModel = siteAPI.getSiteContainers(siteModel).getOneRandomEntry();
        siteAPI.getSiteContainer(siteModel, siteContainerModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with admin user gets site container and gets status code OK (200)")
    public void getSiteContainerWithAdminUser() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        siteContainerModel = siteAPI.getSiteContainers(siteModel).getOneRandomEntry();
        siteAPI.getSiteContainer(siteModel, siteContainerModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site container call returns status code 401")
    @Bug(id="MNT-16904")
    public void unauthenticatedUserIsNotAuthorizedToRetrieveSiteContainer() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel);
        siteContainerModel = siteAPI.getSiteContainers(siteModel).getOneRandomEntry();
        siteAPI.getSiteContainer(siteModel, siteContainerModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
