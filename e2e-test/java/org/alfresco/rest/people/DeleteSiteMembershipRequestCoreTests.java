package org.alfresco.rest.people;

import org.alfresco.dataprep.WorkflowService;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DeleteSiteMembershipRequestCoreTests extends RestTest
{
    SiteModel moderatedSite;
    UserModel adminUserModel;
    UserModel siteMember;
    UserModel secondSiteMember;
    SiteModel secondModeratedSite;
    @Autowired WorkflowService workflow;

    @BeforeMethod(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        moderatedSite = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        adminUserModel = dataUser.getAdminUser();
        siteMember = dataUser.createRandomTestUser();
        secondSiteMember = dataUser.createRandomTestUser();
        secondModeratedSite = dataSite.usingUser(siteMember).createModeratedRandomSite();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is able to remove his own site memebership request using '-me-' in place of personId and response is successful (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void userCanDeleteHisOwnSiteMembershipRequestUsingMeAsPersonId() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        restClient.authenticateUser(siteMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.withCoreAPI().usingMe().deleteSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove site memebership request if doesn't have access to personI and response is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void userIsNotAbleToDeleteSiteMembershipRequestWithNoAccessToPersonId() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        restClient.authenticateUser(secondSiteMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.authenticateUser(siteMember).withCoreAPI().usingUser(secondSiteMember).deleteSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, secondSiteMember.getUsername(), moderatedSite.getId()));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a site memebership request of an inexistent user and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void userIsNotAbleToDeleteSiteMembershipRequestOfAnInexistentUser() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel inexistentUser = new UserModel("inexistenUser", "password");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(inexistentUser).deleteSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, inexistentUser.getUsername()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a site memebership request to an inexistent site and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void userIsNotAbleToDeleteSiteMembershipRequestToAnInexistentSite() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        SiteModel inexistentSite = new SiteModel("inexistentSite");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(adminUserModel).deleteSiteMembershipRequest(inexistentSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUserModel.getUsername(), inexistentSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a site memebership request to an empty site id and response is 405")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void userIsNotAbleToDeleteSiteMembershipRequestWithEmptySiteId() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        SiteModel inexistentSite = new SiteModel("");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(adminUserModel).deleteSiteMembershipRequest(inexistentSite);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.DELETE_EMPTY_ARGUMENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a site memebership request of another regular user from moderated site and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void regularUserIsNotAbleToDeleteSiteMembershipRequestOfAnotherRegularUserToAModeratedSite() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        siteMember.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSite).addPerson(siteMember);
        restClient.authenticateUser(secondSiteMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.authenticateUser(siteMember).withCoreAPI().usingUser(secondSiteMember).deleteSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, secondSiteMember.getUsername(), moderatedSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to remove a site memebership request of admin to a moderated site and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void regularUserIsNotAbleToDeleteSiteMembershipRequestOfAdminToAModeratedSite() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel collaboratorUser = dataUser.createRandomTestUser();
        collaboratorUser.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteMember).withCoreAPI().usingSite(secondModeratedSite).addPerson(collaboratorUser);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteMembershipRequest(secondModeratedSite);
        restClient.authenticateUser(collaboratorUser).withCoreAPI().usingUser(adminUserModel).deleteSiteMembershipRequest(secondModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUserModel.getUsername(), secondModeratedSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify manager is able to remove a site memebership request of admin to a moderated site and response is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void managerUserIsAbleToDeleteSiteMembershipRequestOfAdminToAModeratedSite() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteMembershipRequest(secondModeratedSite);
        restClient.authenticateUser(siteMember).withCoreAPI().usingUser(adminUserModel).deleteSiteMembershipRequest(secondModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify manager is not able to remove a site memebership request if it was already approved and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void managerUserIsNotAbleToDeleteASiteMembershipRequestIfItWasAlreadyApproved() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        secondSiteMember.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(secondSiteMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(secondModeratedSite);
        RestTaskModel taskModel = restClient.authenticateUser(secondSiteMember).withWorkflowAPI().getTasks().getTaskModelByDescription(secondModeratedSite);
        workflow.approveSiteMembershipRequest(siteMember.getUsername(), siteMember.getPassword(), taskModel.getId(), true, "Approve");
        
        restClient.authenticateUser(siteMember).withCoreAPI().usingUser(secondSiteMember).deleteSiteMembershipRequest(secondModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, secondSiteMember.getUsername(), secondModeratedSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify manager is not able to remove an inexitent site memebership request and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void managerUserIsNotAbleToDeleteAnInexistentSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        restClient.authenticateUser(siteMember).withCoreAPI().usingUser(adminUserModel).deleteSiteMembershipRequest(secondModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUserModel.getUsername(), secondModeratedSite.getId()));
    }
}
