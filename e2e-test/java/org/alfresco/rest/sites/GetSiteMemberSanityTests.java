package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
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
public class GetSiteMemberSanityTests extends RestTest
{

    @Autowired
    RestSitesApi restSitesApi;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private UserModel adminUser;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel userModel;

    @BeforeClass(alwaysRun = true)
    public void initSetup() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        restSitesApi.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        userModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteConsumer);
    }

    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site member and gets status code OK (200)")
    public void getSiteMemberWithManagerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restSitesApi.getSiteMember(siteModel, userModel);
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site member and gets status code OK (200)")
    public void getSiteMemberWithCollaboratorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restSitesApi.getSiteMember(siteModel, userModel);
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site member and gets status code OK (200)")
    public void getSiteMemberWithContributorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restSitesApi.getSiteMember(siteModel, userModel);
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site member and gets status code OK (200)")
    public void getSiteMemberWithConsumerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restSitesApi.getSiteMember(siteModel, userModel);
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with admin user gets site member and gets status code OK (200)")
    public void getSiteMemberWithAdminUser() throws Exception
    {
        restClient.authenticateUser(adminUser);
        restSitesApi.getSiteMember(siteModel, userModel);
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
