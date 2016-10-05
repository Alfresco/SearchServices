package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
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
public class GetSitesMembershipInformationSanityTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;

    private SiteModel siteModel;
    private UserModel adminUser;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);

        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "people" }, 
                executionType = ExecutionType.SANITY, 
                description = "Verify site manager is able to retrieve sites membership information of another user")
    public void siteManagerIsAbleToRetrieveSitesMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.getSitesMembershipInformation(adminUser);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site collaborator is able to retrieve sites membership information of another user")
    public void siteCollaboratorIsAbleToRetrieveSitesMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        peopleApi.getSitesMembershipInformation(adminUser);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site contributor is able to retrieve sites membership information of another user")
    public void siteContributorIsAbleToRetrieveSitesMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        peopleApi.getSitesMembershipInformation(adminUser);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site consumer is able to retrieve sites membership information of another user")
    public void siteConsumerIsAbleToRetrieveSitesMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        peopleApi.getSitesMembershipInformation(adminUser);
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify admin is able to retrieve sites membership information of another user")
    public void siteAdminIsAbleToRetrieveSitesMembershipInformation() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        peopleApi.getSitesMembershipInformation(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify that unauthenticated user is not able to retrieve sites membership information")
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserCannotRetrieveSitesMembershipInformation() throws JsonToModelConversionException, Exception
    {
        UserModel inexistentUser = new UserModel("inexistent user", "wrong password");
        restClient.authenticateUser(inexistentUser);
        peopleApi.getSitesMembershipInformation(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

}