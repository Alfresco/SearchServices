package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.SiteMembershipRequest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
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

@Test(groups = { "rest-api", "people", "sanity" })
public class UpdateSiteMembershipRequestSanityTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel adminUser;
    private String initialMessage;
    private String updatedMessage;

    private SiteMembershipRequest initialSiteMembership;
    private SiteMembershipRequest updatedSiteMembership;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        String siteId = RandomData.getRandomName("site");
        siteModel = dataSite.usingUser(adminUser).createSite(new SiteModel(Visibility.MODERATED, siteId, siteId, siteId, siteId));
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);

        peopleApi.useRestClient(restClient);
        
        initialMessage = "Please accept me";
        updatedMessage = "Please review my request";
        
        initialSiteMembership = new SiteMembershipRequest(initialMessage, siteModel.getId(), "New request");
        updatedSiteMembership = new SiteMembershipRequest(updatedMessage, siteModel.getId(), "New request updated");
    }

    @TestRail(section = { "rest-api", "people" }, 
                executionType = ExecutionType.SANITY, description = "Verify user is able to update its own site membership request")
    public void userIsAbleToUpdateItsOwnSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember);
        peopleApi.addSiteMembershipRequest(newMember, initialSiteMembership);
        
        peopleApi.updateSiteMembershipRequest(newMember, updatedSiteMembership)
            .assertMembershipRequestMessageIs(updatedMessage);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify site manager is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteManagerIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember);
        peopleApi.addSiteMembershipRequest(newMember, initialSiteMembership);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.updateSiteMembershipRequest(newMember, updatedSiteMembership);            

        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify site collaborator is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteCollaboratorIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember);
        peopleApi.addSiteMembershipRequest(newMember, initialSiteMembership);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        peopleApi.updateSiteMembershipRequest(newMember, updatedSiteMembership);            

        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify site contributor is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteContributorIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember);
        peopleApi.addSiteMembershipRequest(newMember, initialSiteMembership);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        peopleApi.updateSiteMembershipRequest(newMember, updatedSiteMembership);            

        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify site consumer is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteConsumerIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember);
        peopleApi.addSiteMembershipRequest(newMember, initialSiteMembership);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        peopleApi.updateSiteMembershipRequest(newMember, updatedSiteMembership);            

        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify one user is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void oneUserIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        UserModel otherMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember);
        peopleApi.addSiteMembershipRequest(newMember, initialSiteMembership);

        restClient.authenticateUser(otherMember);
        peopleApi.updateSiteMembershipRequest(newMember, updatedSiteMembership);            

        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
}