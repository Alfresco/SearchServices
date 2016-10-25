package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
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
public class GetSiteMemberSanityTests extends RestTest
{

    @Autowired
    RestSitesApi restSitesApi;

    private UserModel adminUser;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel userModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        restSitesApi.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        userModel = dataUser.createRandomTestUser();
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteConsumer);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site member and status code is OK (200)")
    public void getSiteMemberWithManagerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restSitesApi.getSiteMember(siteModel, userModel)
                    .assertThat().field("id").is(userModel.getUsername())
                    .and().field("role").is(userModel.getUserRole());
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site member and gets status code OK (200)")
    public void getSiteMemberWithCollaboratorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        restSitesApi.getSiteMember(siteModel, userModel)
                    .and().field("id").is(userModel.getUsername())
                    .and().field("role").is(userModel.getUserRole());
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site member and gets status code OK (200)")
    public void getSiteMemberWithContributorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restSitesApi.getSiteMember(siteModel, userModel).and().field("id").is(userModel.getUsername())
        .and().field("role").is(userModel.getUserRole());
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site member and gets status code OK (200)")
    public void getSiteMemberWithConsumerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        restSitesApi.getSiteMember(siteModel, userModel)
                    .and().field("id").is(userModel.getUsername())
                    .and().field("role").is(userModel.getUserRole());
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with admin user gets site member and gets status code OK (200)")
    public void getSiteMemberWithAdminUser() throws Exception
    {
        restClient.authenticateUser(adminUser);
        restSitesApi.getSiteMember(siteModel, userModel)
                    .and().field("id").is(userModel.getUsername())
                    .and().field("role").is(userModel.getUserRole());
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site member call returns status code 401")
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserIsNotAuthorizedToRetrieveSiteMember() throws JsonToModelConversionException, Exception
    {
        UserModel inexistentUser = new UserModel("inexistent user", "inexistent password");
        restClient.authenticateUser(inexistentUser);
        restSitesApi.getSiteMember(siteModel, userModel);
        restSitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
