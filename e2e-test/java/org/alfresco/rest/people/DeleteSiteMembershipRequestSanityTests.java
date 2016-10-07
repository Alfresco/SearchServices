package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.SiteMembershipRequest;
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
    SiteMembershipRequest siteMembershipRequest;
    private ListUserWithRoles usersWithRoles;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception{
        String siteId = RandomData.getRandomName("site");
        siteModel = dataSite.usingUser(userModel).createSite(new SiteModel(Visibility.MODERATED, siteId, siteId, siteId, siteId));
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
        siteMembershipRequest = new SiteMembershipRequest("Please accept me", siteModel.getId(), "New request");
        
        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = {"rest-api", "people" }, executionType = ExecutionType.SANITY, 
            description = "Verify one user is able to delete his one site memebership request")
    public void userCanDeleteHisOwnSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember);
        peopleApi.addSiteMembershipRequest(siteMember, siteMembershipRequest);
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
        peopleApi.addSiteMembershipRequest(siteMember, siteMembershipRequest);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        peopleApi.deleteSiteMembershipRequest(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
}
