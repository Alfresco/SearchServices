package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.SiteMembership;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestPeopleApi;
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
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "people", "sanity" })
public class GetSiteMembershipRequestSanityTests extends RestTest
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

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);

        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api","people" }, 
                executionType = ExecutionType.SANITY, description = "Verify site manager is able to retrieve site membership request")    
    @Bug(id="MNT-16557")  
    public void siteManagerIsAbleToRetrieveSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMembership siteMembership = new SiteMembership("Please accept me", siteModel.getId(), "New request");

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.addSiteMembershipRequest(newMember, siteMembership);
        
        peopleApi.getSiteMembershipRequest(newMember, siteModel);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
                executionType = ExecutionType.SANITY, description = "Verify site collaborator is able to retrieve site membership request")
    @Bug(id = "MNT-16557")
    public void siteCollaboratorIsAbleToRetrieveSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMembership siteMembership = new SiteMembership("Please accept me", siteModel.getId(), "New request");

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        peopleApi.addSiteMembershipRequest(newMember, siteMembership);

        peopleApi.getSiteMembershipRequest(newMember, siteModel);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
                executionType = ExecutionType.SANITY, description = "Verify site contributor is able to retrieve site membership request")
    @Bug(id = "MNT-16557")
    public void siteContributorIsAbleToRetrieveSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMembership siteMembership = new SiteMembership("Please accept me", siteModel.getId(), "New request");

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        peopleApi.addSiteMembershipRequest(newMember, siteMembership);

        peopleApi.getSiteMembershipRequest(newMember, siteModel);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify site consumer is able to retrieve site membership request")
    @Bug(id = "MNT-16557")
    public void siteConsumerIsAbleToRetrieveSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMembership siteMembership = new SiteMembership("Please accept me", siteModel.getId(), "New request");

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        peopleApi.addSiteMembershipRequest(newMember, siteMembership);

        peopleApi.getSiteMembershipRequest(newMember, siteModel);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify admin user is able to retrieve site membership request")
    @Bug(id = "MNT-16557")
    public void adminIsAbleToRetrieveSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMembership siteMembership = new SiteMembership("Please accept me", siteModel.getId(), "New request");

        restClient.authenticateUser(adminUser);
        peopleApi.addSiteMembershipRequest(newMember, siteMembership);

        peopleApi.getSiteMembershipRequest(newMember, siteModel);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to retrieve site membership request")
    @Bug(id = "MNT-16557")
    public void unauthenticatedUserIsNotAbleToRetrieveSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        SiteMembership siteMembership = new SiteMembership("Please accept me", siteModel.getId(), "New request");

        restClient.authenticateUser(new UserModel("random user", "random password"));
        peopleApi.addSiteMembershipRequest(newMember, siteMembership);

        peopleApi.getSiteMembershipRequest(newMember, siteModel);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
    
}