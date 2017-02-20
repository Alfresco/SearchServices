package org.alfresco.rest.sites.delete;

import org.alfresco.dataprep.WorkflowService;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.rest.model.RestTaskModel;
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

public class DeleteSiteMembershipRequestFullTests extends RestTest
{
    SiteModel moderatedSite, publicSite, secondModeratedSite;
    UserModel adminUserModel, siteMember, secondSiteMember;
    RestSiteMembershipRequestModelsCollection siteMembershipRequests;

    @Autowired
    WorkflowService workflow;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        moderatedSite = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        adminUserModel = dataUser.getAdminUser();
        siteMember = dataUser.createRandomTestUser();
        secondSiteMember = dataUser.createRandomTestUser();
        secondModeratedSite = dataSite.usingUser(siteMember).createModeratedRandomSite();
        publicSite = dataSite.usingUser(siteMember).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(moderatedSite, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify COLLABORATOR is able to remove/cancel a site memebership request of a user to a moderated site and response is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void collaboratorUserIsAbleToCancelSiteMembershipRequestOfAnotherUserToAModeratedSite() throws JsonToModelConversionException,
            DataPreparationException, Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        UserModel userCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        moderatedSite = dataSite.usingUser(userCollaborator).createModeratedRandomSite();

        restClient.authenticateUser(user).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.authenticateUser(userCollaborator).withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite).assertThat().field("id")
                .is(moderatedSite.getId());

        restClient.authenticateUser(userCollaborator).withCoreAPI().usingUser(user).deleteSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, user.getUsername(), moderatedSite.getTitle()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify CONTRIBUTOR is able to remove/cancel a site memebership request of a user to a moderated site and response is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void contributorUserIsAbleToCancelSiteMembershipRequestOfAnotherUserToAModeratedSite() throws JsonToModelConversionException,
            DataPreparationException, Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        UserModel userContributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        moderatedSite = dataSite.usingUser(userContributor).createModeratedRandomSite();

        restClient.authenticateUser(user).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.authenticateUser(userContributor).withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite).assertThat().field("id")
                .is(moderatedSite.getId());

        restClient.authenticateUser(userContributor).withCoreAPI().usingUser(user).deleteSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, user.getUsername(), moderatedSite.getTitle()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify CONSUMER is able to remove/cancel a site memebership request of a user to a moderated site and response is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void consumerUserIsAbleToCancelSiteMembershipRequestOfAnotherUserToAModeratedSite() throws JsonToModelConversionException, DataPreparationException,
            Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        UserModel userConsumer = usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer);
        moderatedSite = dataSite.usingUser(userConsumer).createModeratedRandomSite();

        restClient.authenticateUser(user).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.authenticateUser(userConsumer).withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite).assertThat().field("id")
                .is(moderatedSite.getId());

        restClient.authenticateUser(userConsumer).withCoreAPI().usingUser(user).deleteSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, user.getUsername(), moderatedSite.getTitle()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify user is not able to remove a site memebership request of admin to a public site and response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void regularUserIsNotAbleToDeleteSiteMembershipRequestOfAdminToAPublicSite() throws JsonToModelConversionException, DataPreparationException,
            Exception
    {
        UserModel userCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteMember).withCoreAPI().usingSite(publicSite).addPerson(userCollaborator);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);

        restClient.authenticateUser(userCollaborator).withCoreAPI().usingUser(adminUserModel).deleteSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUserModel.getUsername(), publicSite.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Delete a rejected site membership request.")
    public void deleteARejectedSiteMembershipRequest() throws Exception
    {
        UserModel userWithRejectedRequests = dataUser.createRandomTestUser();
        restClient.authenticateUser(userWithRejectedRequests).withCoreAPI().usingAuthUser().addSiteMembershipRequest(secondModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModel taskModel = restClient.authenticateUser(userWithRejectedRequests).withWorkflowAPI().getTasks()
                .getTaskModelByDescription(secondModeratedSite);
        workflow.approveSiteMembershipRequest(siteMember.getUsername(), siteMember.getPassword(), taskModel.getId(), false, "Rejected");

        siteMembershipRequests = restClient.authenticateUser(userWithRejectedRequests).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListDoesNotContain("id", secondModeratedSite.getId());

        restClient.authenticateUser(userWithRejectedRequests).withCoreAPI().usingUser(adminUserModel).deleteSiteMembershipRequest(secondModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUserModel.getUsername(), secondModeratedSite.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Bug(id="ACE-2413")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
             description = "Verify user is not able to remove a site memebership request-  empty person id and response is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userIsNotAbleToDeleteSiteMembershipRequestEmptyPersonID() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel emptyPersonId = new UserModel("", "password");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(emptyPersonId).deleteSiteMembershipRequest(moderatedSite);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .containsSummary(RestErrorModel.ENTITY_NOT_FOUND)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Verify COLLABORATOR removes/cancel twice same site memebership request and response is 404")
  @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
  public void userCancelsSiteMembershipRequestTwiceModeratedSite() throws JsonToModelConversionException,
          DataPreparationException, Exception
  {
      UserModel user = dataUser.createRandomTestUser();
      UserModel userCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
      moderatedSite = dataSite.usingUser(userCollaborator).createModeratedRandomSite();

      restClient.authenticateUser(user).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
      restClient.authenticateUser(userCollaborator).withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite).assertThat().field("id")
              .is(moderatedSite.getId());

      restClient.authenticateUser(userCollaborator).withCoreAPI().usingUser(user).deleteSiteMembershipRequest(moderatedSite);
      restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
      restClient.authenticateUser(userCollaborator).withCoreAPI().usingUser(user).deleteSiteMembershipRequest(moderatedSite);

      restClient.withCoreAPI().usingUser(user).getSiteMembershipRequest(moderatedSite);
      restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError()
                .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, user.getUsername(), moderatedSite.getTitle()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
  }    
}
