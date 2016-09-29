package org.alfresco.rest.people;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.SiteMember;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "people", "sanity" })
public class DeleteSiteMemberSanityTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;
    
    @Autowired
    RestSitesApi sitesApi;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;
    private UserModel adminUser;
    private UserModel newUser;
    private SiteMember siteMember;
    private HashMap<UserRole, UserModel> usersWithRoles;

    @BeforeClass
    public void initTest() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                Arrays.asList(UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor));

        peopleApi.useRestClient(restClient);
        sitesApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "people" }, 
                executionType = ExecutionType.SANITY, 
                description = "Verify site manager is able to delete another member of the site")
    public void siteManagerCanDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        newUser = dataUser.createRandomTestUser();
        siteMember = new SiteMember(Role.SiteCollaborator.toString(), newUser.getUsername());
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        sitesApi.addPerson(siteModel.getId(), siteMember);
        
        peopleApi.deleteSiteMember(newUser.getUsername(), siteModel.getId());
        sitesApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT.toString());
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site collaborator does not have permission to delete another member of the site")
    // TODO BUG ACE-5444
    public void siteCollaboratorIsNotAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        newUser = dataUser.createRandomTestUser();
        siteMember = new SiteMember(Role.SiteContributor.toString(), newUser.getUsername());
        restClient.authenticateUser(adminUser);
        sitesApi.addPerson(siteModel.getId(), siteMember);
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteCollaborator));

        peopleApi.deleteSiteMember(newUser.getUsername(), siteModel.getId());
        sitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site contributor does not have permission to delete another member of the site")
    // TODO BUG ACE-5444
    public void siteContributorIsNotAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        newUser = dataUser.createRandomTestUser();
        siteMember = new SiteMember(Role.SiteCollaborator.toString(), newUser.getUsername());
        restClient.authenticateUser(adminUser);
        sitesApi.addPerson(siteModel.getId(), siteMember);
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteContributor));

        peopleApi.deleteSiteMember(newUser.getUsername(), siteModel.getId());
        sitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify site consumer does not have permission to delete another member of the site")
    // TODO BUG ACE-5444
    public void siteConsumerIsNotAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        newUser = dataUser.createRandomTestUser();
        siteMember = new SiteMember(Role.SiteCollaborator.toString(), newUser.getUsername());
        restClient.authenticateUser(adminUser);
        sitesApi.addPerson(siteModel.getId(), siteMember);
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteConsumer));

        peopleApi.deleteSiteMember(newUser.getUsername(), siteModel.getId());
        sitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN.toString());
    }
    
    @TestRail(section = { "rest-api", "people" }, 
            executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to delete another member of the site")
    public void adminIsAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        newUser = dataUser.createRandomTestUser();
        siteMember = new SiteMember(Role.SiteCollaborator.toString(), newUser.getUsername());
        restClient.authenticateUser(adminUser);
        sitesApi.addPerson(siteModel.getId(), siteMember);
        
        peopleApi.deleteSiteMember(newUser.getUsername(), siteModel.getId());
        sitesApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT.toString());
    }

}