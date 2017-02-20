package org.alfresco.rest.sites.get;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
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

public class GetSiteMembershipRequestFullTests extends RestTest
{
    UserModel userModel, newMember, adminUser, userCollaborator, userConsumer, adminTenantUser, adminTenantUser2,
            tenantUser, tenantUserAssignee;
    SiteModel moderatedSite, secondModeratedSite;
    RestSiteMembershipRequestModel returnedModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        moderatedSite = dataSite.usingUser(userModel).createModeratedRandomSite();
        newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify site manager can get site membership requests on moderated site and response is successful (200)")
    public void siteManagerCanGetModeratedSiteMembershipRequests() throws Exception
    {
        UserModel moderatedUser = dataUser.createRandomTestUser();
        SiteModel moderatedSite = dataSite.usingUser(userModel).createModeratedRandomSite();
        restClient.authenticateUser(moderatedUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        returnedModel = restClient.authenticateUser(userModel).withCoreAPI().usingUser(moderatedUser).getSiteMembershipRequest(moderatedSite);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("id").is(moderatedSite.getId())
                     .and().field("message").is("Please accept me")
                     .and().field("site.title").is(moderatedSite.getTitle())
                     .and().field("site.visibility").is(Visibility.MODERATED.toString())
                     .and().field("site.guid").isNotEmpty()
                     .and().field("site.description").is(moderatedSite.getDescription())
                     .and().field("site.preset").is("site-dashboard");
    }

    @Bug(id = "ACE-2413")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify if person ID field is empty user can't get site membership requests on moderated site and response is not found (400)")
    public void emptyPersonIdCantGetModeratedSiteMembershipRequests() throws Exception
    {
        UserModel emptyUser = new UserModel("", "password");
        restClient.authenticateUser(userModel).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.authenticateUser(userModel).withCoreAPI().usingUser(emptyUser).getSiteMembershipRequest(moderatedSite);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .containsSummary(String.format("The entity with id: personId is null. was not found"))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Approve site membership request then verify get site membership requests - response is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void approveRequestThenGetSiteMembershipRequest() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel userWithApprovedRequests = dataUser.createRandomTestUser();
        restClient.authenticateUser(userWithApprovedRequests).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModel taskModel = restClient.authenticateUser(userWithApprovedRequests).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(userModel.getUsername(), userModel.getPassword(), taskModel.getId(), true, "Approve");

        returnedModel = restClient.authenticateUser(userWithApprovedRequests).withCoreAPI().usingMe().getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, userWithApprovedRequests.getUsername(), moderatedSite.getId()))
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Reject site membership request then verify get site membership requests - response is 404")
    public void rejectRequestThenGetSiteMembershipRequest() throws Exception
    {
        UserModel userWithRejectedRequests = dataUser.createRandomTestUser();
        restClient.authenticateUser(userWithRejectedRequests).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModel taskModel = restClient.authenticateUser(userWithRejectedRequests).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(userModel.getUsername(), userModel.getPassword(), taskModel.getId(), false, "Rejected");

        returnedModel = restClient.authenticateUser(userWithRejectedRequests).withCoreAPI().usingMe().getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, userWithRejectedRequests.getUsername(), moderatedSite.getId()))
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify entry details for get site favorite response with Rest API")
    public void checkResponseSchemaForGetSiteMembershipRequest() throws Exception
    {
        dataUser.usingUser(userModel).addUserToSite(newMember, moderatedSite, UserRole.SiteContributor);
        returnedModel = restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().getSiteMembershipRequest(moderatedSite);
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("id").is(moderatedSite.getId())
                     .and().field("message").is("Please accept me")
                     .and().field("site.title").is(moderatedSite.getTitle())
                     .and().field("site.visibility").is(Visibility.MODERATED.toString())
                     .and().field("site.guid").isNotEmpty()
                     .and().field("site.description").is(moderatedSite.getDescription())
                     .and().field("site.preset").is("site-dashboard")
                     .and().field("site.role").is("SiteContributor");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that getFavoriteSites request applies valid properties param")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void getSiteMembershipRequestWithValidPropertiesParam() throws Exception
    {
        RestSiteMembershipRequestModel returnedModel = restClient.authenticateUser(newMember).withParams("properties=message").withCoreAPI().usingAuthUser()
                .getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().fieldsCount().is(1).assertThat().field("message").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that getFavoriteSites request returns status 200 when using valid parameters")
    public void getSiteMembershipRequestUsingParameters() throws Exception
    {
        RestSiteMembershipRequestModel returnedModel = restClient.withParams("message=Please accept me")
                                                       .authenticateUser(newMember).withCoreAPI().usingMe()
                                                       .getSiteMembershipRequest(moderatedSite);
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("id").is(moderatedSite.getId()).and().field("site.title").is(moderatedSite.getTitle());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify user doesn't have permission to get site membership request of admin with membership request with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userIsNotAbleToGetSiteMembershipRequestOfAdminWithRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        returnedModel = restClient.authenticateUser(adminUser).withCoreAPI().usingUser(newMember).getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("id").is(moderatedSite.getId())
                     .and().field("message").is("Please accept me")
                     .and().field("site.title").is(moderatedSite.getTitle())
                     .and().field("site.visibility").is(Visibility.MODERATED.toString())
                     .and().field("site.guid").isNotEmpty()
                     .and().field("site.description").is(moderatedSite.getDescription())
                     .and().field("site.preset").is("site-dashboard");                    
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify user doesn't have permission to get site membership request of admin without membership request with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userIsNotAbleToGetSiteMembershipRequestOfAdminWithoutRequest() throws JsonToModelConversionException, Exception
    {
        SiteModel moderatedSite = dataSite.usingUser(userModel).createModeratedRandomSite();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        returnedModel = restClient.authenticateUser(newMember).withCoreAPI().usingUser(adminUser).getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUser.getUsername(), moderatedSite.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER).stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify admin user doesn't have permission to get site membership request of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void adminIsNotAbleToGetSiteMembershipRequestOfUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        returnedModel = restClient.authenticateUser(newMember).withCoreAPI().usingUser(adminUser).getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminUser.getUsername(), moderatedSite.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
              description = "Verify admin user doesn't have permission to get site membership request of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userIsNotAbleToGetSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        userCollaborator = dataUser.createRandomTestUser();
        userConsumer = dataUser.createRandomTestUser();
        restClient.authenticateUser(userConsumer).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        dataUser.usingUser(userModel).addUserToSite(userConsumer, moderatedSite, UserRole.SiteConsumer);
        restClient.authenticateUser(userCollaborator).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        dataUser.usingUser(userModel).addUserToSite(userCollaborator, moderatedSite, UserRole.SiteCollaborator);

        returnedModel = restClient.authenticateUser(userCollaborator).withCoreAPI().usingUser(userConsumer).getSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, userConsumer.getUsername(), moderatedSite.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, 
             description = "Add process item using by the admin in same network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL, TestGroup.NETWORKS })
    public void getSiteMembershipRequestByAdminSameNetwork() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");

        moderatedSite = dataSite.usingUser(adminTenantUser).createModeratedRandomSite();
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        returnedModel = restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getSiteMembershipRequest(moderatedSite);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("id").is(moderatedSite.getId())
                     .and().field("message").is("Please accept me")
                     .and().field("site.title").is(moderatedSite.getTitle())
                     .and().field("site.visibility").is(Visibility.MODERATED.toString())
                     .and().field("site.guid").isNotEmpty()
                     .and().field("site.description").is(moderatedSite.getDescription())
                     .and().field("site.preset").is("site-dashboard");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, 
              description = "Add process item using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL, TestGroup.NETWORKS })
    public void getSiteMembershipRequestByAdminInOtherNetwork() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser2);
        moderatedSite = dataSite.usingUser(adminTenantUser2).createModeratedRandomSite();

        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        returnedModel = restClient.authenticateUser(adminTenantUser2).withCoreAPI().usingAuthUser().getSiteMembershipRequest(moderatedSite);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminTenantUser2.getUsername().toLowerCase(), moderatedSite.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

}
