package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.StatusModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AddSiteMembershipRequestSanityTests extends RestTest
{
    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;

    private ListUserWithRoles usersWithRoles;

    private UserModel adminUser;
    private UserModel newMember;
    private RestSiteMembershipRequestModel requestModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @BeforeMethod
    public void setUp() throws DataPreparationException
    {
        newMember = dataUser.createRandomTestUser();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site manager is able to create new site membership request")
    @Bug(id = "MNT-16557")
    public void siteManagerIsAbleToCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").isNotEmpty().assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site collaborator is able to create new site membership request")
    @Bug(id = "MNT-16557")
    public void siteCollaboatorIsAbleToCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").isNotEmpty().assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site contributor is able to create new site membership request")
    @Bug(id = "MNT-16557")
    public void siteContributorIsAbleToCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").isNotEmpty().assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify site consumer is able to create new site membership request")
    @Bug(id = "MNT-16557")
    public void siteConsumerIsAbleToCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        requestModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingUser(newMember)
                .addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").isNotEmpty().assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify admin user is able to create new site membership request")
    @Bug(id = "MNT-16557")
    public void adminUserIsAbleToCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        requestModel = restClient.authenticateUser(adminUser).withCoreAPI().usingUser(newMember).addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        requestModel.assertThat().field("id").isNotEmpty().assertThat().field("site").isNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to create new site membership request")
    @Bug(id = "MNT-16557")
    public void unauthenticatedUserIsNotAbleToCreateSiteMembershipRequest() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password")).withCoreAPI().usingUser(newMember).addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastException().hasName(StatusModel.UNAUTHORIZED);
    }
}