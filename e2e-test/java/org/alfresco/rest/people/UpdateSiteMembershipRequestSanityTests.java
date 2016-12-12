package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateSiteMembershipRequestSanityTests extends RestTest
{
    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel adminUser;
    private String updatedMessage;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        String siteId = RandomData.getRandomName("site");
        siteModel = dataSite.usingUser(adminUser).createSite(new SiteModel(Visibility.MODERATED, siteId, siteId, siteId, siteId));

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        updatedMessage = "Please review my request";
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify user is able to update its own site membership request")
    public void userIsAbleToUpdateItsOwnSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);
        restClient.withCoreAPI().usingUser(newMember).updateSiteMembershipRequest(siteModel, updatedMessage).assertMembershipRequestMessageIs(updatedMessage)
                .and().field("id").is(siteModel.getId()).and().field("modifiedAt").isNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site manager is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteManagerIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingUser(newMember)
                .updateSiteMembershipRequest(siteModel, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site collaborator is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteCollaboratorIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingUser(newMember)
                .updateSiteMembershipRequest(siteModel, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site contributor is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteContributorIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingUser(newMember)
                .updateSiteMembershipRequest(siteModel, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site consumer is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void siteConsumerIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingUser(newMember)
                .updateSiteMembershipRequest(siteModel, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify one user is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void oneUserIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        UserModel otherMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

        restClient.authenticateUser(otherMember).withCoreAPI().usingUser(newMember).updateSiteMembershipRequest(siteModel, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify admin is not able to update membership request of another user")
    @Bug(id = "MNT-16919")
    public void adminIsNotAbleToUpdateSiteMembershipRequestOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);

        restClient.authenticateUser(adminUser).withCoreAPI().usingUser(newMember).updateSiteMembershipRequest(siteModel, updatedMessage);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
}