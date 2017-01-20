package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateSiteMembershipRequestFullTests extends RestTest
{
    private SiteModel moderatedSite;
    private UserModel managerUser, adminUser, newMember, regularUser;
    private String updatedMessage;
    private RestSiteMembershipRequestModel requestUpdateModel, requestGetModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        managerUser = dataUser.createRandomTestUser();
        moderatedSite = dataSite.usingUser(managerUser).createModeratedRandomSite();
        updatedMessage = "Please review my request";

        newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is not able to update membership request of another user")
    // @Bug(id = "MNT-16919")
    public void userIsNotAbleToUpdateSiteMembershipRequestOfAdmin() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        restClient.authenticateUser(newMember).withCoreAPI().usingUser(adminUser).updateSiteMembershipRequest(moderatedSite, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format("The entity with id: " + RestErrorModel.ENTITY_NOT_FOUND, adminUser.getUsername()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER).stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is not able to update its own site membership request for private site")
    public void userCanUpdateSiteMembershipRequestForModeratedSite() throws Exception
    {
        SiteModel moderatedSite = dataSite.usingUser(managerUser).createModeratedRandomSite();
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        requestUpdateModel = restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(moderatedSite, updatedMessage);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId()).assertThat().field("message").is("Please review my request").assertThat()
                .field("site").isNotEmpty().assertThat().field("modifiedAt").isNotEmpty().assertThat().field("createdAt").isNotEmpty();
        requestUpdateModel.getSite().assertThat().field("visibility").is(moderatedSite.getVisibility()).assertThat().field("guid").is(moderatedSite.getGuid())
                .assertThat().field("description").is(moderatedSite.getDescription()).assertThat().field("id").is(moderatedSite.getId()).assertThat()
                .field("preset").is("site-dashboard").assertThat().field("title").is(moderatedSite.getTitle());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site collaborator is able to retrieve site membership request")
    public void siteCollaboratorIsAbleToUpdateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        dataUser.usingUser(managerUser).addUserToSite(newMember, moderatedSite, UserRole.SiteCollaborator);
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        requestUpdateModel = restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().updateSiteMembershipRequest(moderatedSite, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("message").is("Please review my request")
                    .assertThat().field("modifiedAt").isNotEmpty()
                    .assertThat().field("createdAt").isNotEmpty();
        requestUpdateModel.getSite()
                    .assertThat().field("role").is(UserRole.SiteCollaborator)
                    .assertThat().field("visibility").is(moderatedSite.getVisibility())
                    .assertThat().field("guid").is(moderatedSite.getGuid())
                    .assertThat().field("description").is(moderatedSite.getDescription())
                    .assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("preset").is("site-dashboard")
                    .assertThat().field("title").is(moderatedSite.getTitle());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site contributor is able to retrieve site membership request")
    public void siteContributorIsAbleToUpdateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        dataUser.usingUser(managerUser).addUserToSite(newMember, moderatedSite, UserRole.SiteContributor);
        requestUpdateModel = restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().updateSiteMembershipRequest(moderatedSite, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("message").is("Please review my request")
                    .assertThat().field("modifiedAt").isNotEmpty()
                    .assertThat().field("createdAt").isNotEmpty();
        requestUpdateModel.getSite()
                    .assertThat().field("role").is(UserRole.SiteContributor)
                    .assertThat().field("visibility").is(moderatedSite.getVisibility())
                    .assertThat().field("guid").is(moderatedSite.getGuid())
                    .assertThat().field("description").is(moderatedSite.getDescription())
                    .assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("preset").is("site-dashboard")
                    .assertThat().field("title").is(moderatedSite.getTitle());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site consumer is able to retrieve site membership request")
    public void siteConsumerIsAbleToUpdateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        dataUser.usingUser(managerUser).addUserToSite(newMember, moderatedSite, UserRole.SiteConsumer);
        requestUpdateModel = restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().updateSiteMembershipRequest(moderatedSite, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("message").is("Please review my request")
                    .assertThat().field("modifiedAt").isNotEmpty()
                    .assertThat().field("createdAt").isNotEmpty();
        requestUpdateModel.getSite()
                    .assertThat().field("role").is(UserRole.SiteConsumer)
                    .assertThat().field("visibility").is(moderatedSite.getVisibility())
                    .assertThat().field("guid").is(moderatedSite.getGuid())
                    .assertThat().field("description").is(moderatedSite.getDescription())
                    .assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("preset").is("site-dashboard")
                    .assertThat().field("title").is(moderatedSite.getTitle());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify site manager is able to retrieve site membership request")
    public void siteManagerIsAbleToUpdateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        newMember = dataUser.createRandomTestUser();
        dataUser.usingUser(managerUser).addUserToSite(newMember, moderatedSite, UserRole.SiteManager);
        requestUpdateModel = restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().updateSiteMembershipRequest(moderatedSite, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("message").is("Please review my request")
                    .assertThat().field("modifiedAt").isNotEmpty()
                    .assertThat().field("createdAt").isNotEmpty();
        requestUpdateModel.getSite()
                    .assertThat().field("role").is(UserRole.SiteManager)
                    .assertThat().field("visibility").is(moderatedSite.getVisibility())
                    .assertThat().field("guid").is(moderatedSite.getGuid())
                    .assertThat().field("description").is(moderatedSite.getDescription())
                    .assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("preset").is("site-dashboard")
                    .assertThat().field("title").is(moderatedSite.getTitle());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify admin is able to retrieve site membership request")
    public void adminIsAbleToRetrieveSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        regularUser = dataUser.createRandomTestUser();
        SiteModel anotherModeratedSite = dataSite.usingUser(regularUser).createModeratedRandomSite();
        requestUpdateModel = restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(anotherModeratedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
       
        requestUpdateModel = restClient.withCoreAPI().usingAuthUser().updateSiteMembershipRequest(anotherModeratedSite, updatedMessage);            
        restClient.assertStatusCodeIs(HttpStatus.OK);
        requestUpdateModel.assertThat().field("id").is(anotherModeratedSite.getId())
                    .assertThat().field("message").is("Please review my request")
                    .assertThat().field("modifiedAt").isNotEmpty()
                    .assertThat().field("createdAt").isNotEmpty();
        requestUpdateModel.getSite()
                    .assertThat().field("visibility").is(anotherModeratedSite.getVisibility())
                    .assertThat().field("guid").is(anotherModeratedSite.getGuid())
                    .assertThat().field("description").is(anotherModeratedSite.getDescription())
                    .assertThat().field("id").is(anotherModeratedSite.getId())
                    .assertThat().field("preset").is("site-dashboard")
                    .assertThat().field("title").is(anotherModeratedSite.getTitle());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is able to update its own site membership request with initial message")
    public void userUpdateWithNoMessageThenAddsNewMessage() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        requestUpdateModel = restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("message").is("Please accept me");
        
        requestUpdateModel = restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(moderatedSite, "");
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
                    .and().field("modifiedAt").isNotEmpty()
                    .assertThat().field("message").isNull();
        
        requestUpdateModel = restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(moderatedSite, updatedMessage);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
        .assertThat().field("message").is("Please review my request")
        .assertThat().field("modifiedAt").isNotEmpty()
        .assertThat().field("createdAt").isNotEmpty();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify user is able to update its own site membership request with initial message")
    public void userUpdateSiteMembershipRequestAndCheckModifiedAtWithGet() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        requestUpdateModel = restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestUpdateModel.assertThat().field("id").is(moderatedSite.getId())
                    .assertThat().field("message").is("Please accept me");
        
        requestUpdateModel = restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(moderatedSite, updatedMessage);
        requestGetModel = restClient.withCoreAPI().usingMe().getSiteMembershipRequest(moderatedSite);
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        requestGetModel.assertThat().field("id").is(requestUpdateModel.getId())
                    .and().field("modifiedAt").is(requestUpdateModel.getModifiedAt())
                    .assertThat().field("message").is(requestUpdateModel.getMessage()); 
    }

}
