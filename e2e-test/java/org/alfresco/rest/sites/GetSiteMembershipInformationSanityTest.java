package org.alfresco.rest.sites;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.rest.RestTest;
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

@Test(groups = { "rest-api", "sites", "sanity" })
public class GetSiteMembershipInformationSanityTest extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;
    private UserModel adminUser;
    private HashMap<UserRole, UserModel> usersWithRoles;

    @BeforeClass
    public void initTest() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersToSiteWithRoles(siteModel,
                Arrays.asList(UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor));

        siteAPI.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "sites" }, 
                executionType = ExecutionType.SANITY, 
                description = "Verify site manager is able to retrieve site membership information of another user")
    public void siteManagerCanRetrieveSiteMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        siteAPI.getSiteMembershipInformation(adminUser.getUsername());
        siteAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @TestRail(section = { "rest-api", "sites" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site collaborator is able to retrieve site membership information of another user")
    public void siteCollaboratorCanRetrieveSiteMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteCollaborator));
        siteAPI.getSiteMembershipInformation(adminUser.getUsername());
        siteAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site contributor is able to retrieve site membership information of another user")
    public void siteContributorCanRetrieveSiteMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteContributor));
        siteAPI.getSiteMembershipInformation(adminUser.getUsername());
        siteAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site consumer is able to retrieve site membership information of another user")
    public void siteConsumerCanRetrieveSiteMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteConsumer));
        siteAPI.getSiteMembershipInformation(adminUser.getUsername());
        siteAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK.toString());
    }

}