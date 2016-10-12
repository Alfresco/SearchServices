package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = {"rest-api", "people", "sanity" })
public class DeleteSiteMembershipRequestSanityTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;
    
    UserModel userModel;
    UserModel siteMember;
    SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception{
        String siteId = RandomData.getRandomName("site");
        siteModel = dataSite.usingUser(userModel).createSite(new SiteModel(Visibility.MODERATED, siteId, siteId, siteId, siteId));
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
        
        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify one user is able to delete his one site memebership request")
    public void userCanDeleteHisOwnSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.deleteSiteMembershipRequest(siteMember, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify site manager is able to delete site membership request")
    public void siteManagerCanDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.deleteSiteMembershipRequest(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to delete site memebership request")
    public void adminUserCanDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        peopleApi.deleteSiteMembershipRequest(adminUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify collaborator user is not able to delete site memebership request")
    public void collaboratorCannotDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        peopleApi.deleteSiteMembershipRequest(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify contributor user is not able to delete site memebership request")
    public void contributorCannotDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        peopleApi.deleteSiteMembershipRequest(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor), siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify consumer user is not able to delete site memebership request")
    public void consumerCannotDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        peopleApi.deleteSiteMembershipRequest(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer), siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify random user is not able to delete site memebership request")
    public void randomUserCanDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        UserModel randomUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(randomUser);
        peopleApi.deleteSiteMembershipRequest(randomUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site member call returns status code 401")
    public void unauthenticatedUserIsNotAuthorizedToDeleteSiteMmebershipRequest() throws JsonToModelConversionException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteModel);
        UserModel inexistentUser = new UserModel("inexistent user", "inexistent password");
        restClient.authenticateUser(inexistentUser);
        peopleApi.deleteSiteMembershipRequest(inexistentUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
