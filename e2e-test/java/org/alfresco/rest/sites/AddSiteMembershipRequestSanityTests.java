package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.SiteMembership;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "sites", "sanity" })
public class AddSiteMembershipRequestSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;

    private ListUserWithRoles usersWithRoles;

    private UserModel adminUser;

    @BeforeClass(alwaysRun=true)
    public void initTest() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);

        siteAPI.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api","sites" }, executionType = ExecutionType.SANITY, description = "Verify site manager is able to create new site membership request")    
    @Bug(id="MNT-16557")
    @Test(enabled=false)
    public void siteManagerCanCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMembership siteMembership = new SiteMembership("Please accept me", siteModel.getId(), "New request");

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteAPI.addSiteMembershipRequest(newMember, siteMembership);
        siteAPI.getSite(siteModel).assertResponseIsNotEmpty();
    }

}