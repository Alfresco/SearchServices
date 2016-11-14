package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = {TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
public class DeleteSiteMembershipRequestSanityTests extends RestTest
{    
    UserModel userModel;
    UserModel siteMember;
    SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception{
        String siteId = RandomData.getRandomName("site");
        siteModel = dataSite.usingUser(userModel).createSite(new SiteModel(Visibility.MODERATED, siteId, siteId, siteId, siteId));
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                                                          UserRole.SiteManager, 
                                                          UserRole.SiteCollaborator, 
                                                          UserRole.SiteConsumer, 
                                                          UserRole.SiteContributor);        
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, 
            description = "Verify one user is able to delete his one site memebership request")
    public void userCanDeleteHisOwnSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember)
                  .usingAuthUser().addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                  .usingUser(siteMember).deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, 
            description = "Verify site manager is able to delete site membership request")
    public void siteManagerCanDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember)
                  .usingUser(siteMember).addSiteMembershipRequest(siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                  .usingAuthUser().deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to delete site memebership request")
    public void adminUserCanDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember) 
                  .usingAuthUser().addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(dataUser.getAdminUser())
                  .usingAuthUser().deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, 
            description = "Verify collaborator user is not able to delete site memebership request")
    public void collaboratorCannotDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember)
                  .usingAuthUser().addSiteMembershipRequest(siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .usingAuthUser().deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, 
            description = "Verify contributor user is not able to delete site memebership request")
    public void contributorCannotDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember)
                  .usingAuthUser().addSiteMembershipRequest(siteModel);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                  .usingAuthUser().deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                                    .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, 
            description = "Verify consumer user is not able to delete site memebership request")
    public void consumerCannotDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(siteMember)
                  .usingAuthUser().addSiteMembershipRequest(siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .usingAuthUser().deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                                    .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @Bug(id="MNT-16916")
    @TestRail(section = {TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, 
            description = "Verify random user is not able to delete site memebership request")
    public void randomUserCanDeleteSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {     
        restClient.authenticateUser(dataUser.createRandomTestUser())
                  .usingAuthUser().addSiteMembershipRequest(siteModel);

        restClient.authenticateUser(dataUser.createRandomTestUser())
                  .usingAuthUser().deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                                    .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site member call returns status code 401")
    public void unauthenticatedUserIsNotAuthorizedToDeleteSiteMmebershipRequest() throws JsonToModelConversionException, Exception
    {        
        restClient.authenticateUser(dataUser.createRandomTestUser())
                  .usingAuthUser().addSiteMembershipRequest(siteModel);
        UserModel inexistentUser = new UserModel("inexistent user", "inexistent password");
        restClient.authenticateUser(inexistentUser)
                  .usingAuthUser().deleteSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
