package org.alfresco.rest.sites.membershipRequests;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 1/10/2017.
 */
public class GetSiteMembershipRequestsFullTests extends RestTest
{
    UserModel siteManager, newMember, adminUser, regularUser;
    SiteModel moderatedSite1, moderatedSite2, publicSite, privateSite;
    RestSiteMembershipRequestModelsCollection siteMembershipRequests;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        siteManager = dataUser.createRandomTestUser();
        publicSite = dataSite.usingUser(siteManager).createPublicRandomSite();
        moderatedSite1 = dataSite.usingUser(siteManager).createModeratedRandomSite();
        moderatedSite2 = dataSite.usingUser(siteManager).createModeratedRandomSite();
        privateSite = dataSite.usingUser(siteManager).createPrivateRandomSite();

        adminUser = dataUser.getAdminUser();
        newMember = dataUser.createRandomTestUser();
        regularUser = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Verify admin user gets all its own site membership requests with Rest API and response is successful (200)")
    public void adminGetsItsOwnSiteMembershipRequestsWithSuccess() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(adminUser).withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListContains("id", moderatedSite1.getId())
                .assertThat().entriesListContains("id", moderatedSite2.getId());
    }

    @Bug(id = "MNT-16557")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Verify regular user fails to get all site membership requests of another user with Rest API (403)")
    public void regularUserFailsToGetSiteMembershipRequestsOfAnotherUser() throws Exception
    {
        restClient.authenticateUser(regularUser).withCoreAPI()
                .usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Bug(id = "MNT-16557")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Verify regular user fails to get all site membership requests of admin with Rest API (403)")
    public void regularUserFailsToGetSiteMembershipRequestsOfAdmin() throws Exception
    {
        restClient.authenticateUser(regularUser).withCoreAPI()
                .usingUser(adminUser).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Verify regular user is able to get his site membership requests with, but skip the first one")
    public void getSiteMembershipRequestsButSkipTheFirstOne() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(newMember).withParams("orderBy=id ASC").withCoreAPI()
                .usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListContains("id", moderatedSite1.getId())
                .assertThat().entriesListContains("id", moderatedSite2.getId());

        RestSiteMembershipRequestModel firstSiteMembershipRequest = siteMembershipRequests.getEntries().get(0).onModel();
        RestSiteMembershipRequestModel secondSiteMembershipRequest = siteMembershipRequests.getEntries().get(1).onModel();

        siteMembershipRequests = restClient.authenticateUser(newMember).withParams("orderBy=id ASC&skipCount=1").withCoreAPI()
                .usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListDoesNotContain("id", firstSiteMembershipRequest.getId())
                .assertThat().entriesListContains("id", secondSiteMembershipRequest.getId())
                .assertThat().entriesListContains("id", secondSiteMembershipRequest.getSite().getId());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Verify regular user is able to get his site membership requests with higher skipCount than number of requests")
    public void getSiteMembershipRequestsWithHighSkipCount() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(newMember).withParams("skipCount=3").withCoreAPI()
                .usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListIsEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Verify get site membership requests returns an empty list where there are no requests")
    public void getSiteMembershipRequestsWhereThereAreNoRequests() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(regularUser).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListIsEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Reject request then get site membership requests.")
    public void rejectRequestThenGetSiteMembershipRequests() throws Exception
    {
        UserModel userWithRejectedRequests = dataUser.createRandomTestUser();

        restClient.authenticateUser(userWithRejectedRequests).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModel taskModel = restClient.authenticateUser(userWithRejectedRequests).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite1);
        workflow.approveSiteMembershipRequest(siteManager.getUsername(), siteManager.getPassword(), taskModel.getId(), false, "Rejected");

        siteMembershipRequests = restClient.authenticateUser(userWithRejectedRequests).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListDoesNotContain("id", moderatedSite1.getId())
                .assertThat().entriesListContains("id", moderatedSite2.getId());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE },
            executionType = ExecutionType.REGRESSION,
            description = "Check get site membership requests when properties parameter is used")
    public void getSiteMembershipRequestsWithProperties() throws Exception
    {
        siteMembershipRequests = restClient.authenticateUser(newMember).withParams("properties=id")
                .withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembershipRequests.assertThat().entriesListContains("id", moderatedSite1.getId())
                .assertThat().entriesListContains("id", moderatedSite2.getId());
        RestSiteMembershipRequestModel siteMembershipRequest = siteMembershipRequests.getOneRandomEntry().onModel();
        siteMembershipRequest.assertThat().fieldsCount().is(1);
    }

}
