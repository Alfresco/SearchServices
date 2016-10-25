package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
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
public class GetSiteMembersSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;
    private UserModel adminUser;
    private ListUserWithRoles usersWithRoles;
    private UserModel userModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site members and gets status code OK (200)")
    public void getSiteMembersWithManagerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteAPI.getSiteMembers(siteModel).assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("id", usersWithRoles.getOneUserWithRole(UserRole.SiteManager).getUsername())
            .and().entriesListContains("role", usersWithRoles.getOneUserWithRole(UserRole.SiteManager).getUserRole().toString());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site members and gets status code OK (200)")
    public void getSiteMembersWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        siteAPI.getSiteMembers(siteModel).assertThat().entriesListIsNotEmpty().assertThat()
            .entriesListContains("id", usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUsername()).and()
            .entriesListContains("role", usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator).getUserRole().toString());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site members and gets status code OK (200)")
    public void getSiteMembersWithContributorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        siteAPI.getSiteMembers(siteModel).assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("id", usersWithRoles.getOneUserWithRole(UserRole.SiteContributor).getUsername())
            .and().entriesListContains("role", usersWithRoles.getOneUserWithRole(UserRole.SiteContributor).getUserRole().toString());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site members and gets status code OK (200)")
    public void getSiteMembersWithConsumerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        siteAPI.getSiteMembers(siteModel).assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("id", usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer).getUsername())
            .and().entriesListContains("role", usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer).getUserRole().toString());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with admin usere gets site members and gets status code OK (200)")
    public void getSiteMembersWithAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        siteAPI.getSiteMembers(siteModel).assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("id", adminUser.getUsername())
            .when().assertThat().entriesListContains("role", "SiteManager");
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id="MNT-16904")
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site members call returns status code 401")
    public void unauthenticatedUserIsNotAuthorizedToRetrieveSiteMembers() throws JsonToModelConversionException, Exception
    {
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel);
        siteAPI.getSiteMembers(siteModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
