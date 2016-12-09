package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSiteMembershipRequestModelsCollection;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for getSiteMembershipRequests (get
 * /people/{personId}/site-membership-requests) Core-RestAPI call
 * 
 * @author Andrei Rusu
 */

public class GetSiteMembershipRequestsCoreTests extends RestTest
{
    UserModel newMember, adminUser;
    SiteModel moderatedSite, publicSite;
    RestSiteMembershipRequestModelsCollection returnedCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        moderatedSite = dataSite.usingAdmin().createModeratedRandomSite();
        publicSite = dataSite.usingAdmin().createPublicRandomSite();
        newMember = dataUser.createRandomTestUser();

        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify that for invalid maxItems parameter status code returned is 400.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void checkInvalidMaxItemsStatusCode() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("maxItems=AB").withCoreAPI().usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Invalid paging parameter");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify that for invalid skipCount parameter status code returned is 400.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void checkInvalidSkipCountStatusCode() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("skipCount=AB").withCoreAPI().usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Invalid paging parameter");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify that if personId does not exist status code returned is 404.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void ifPersonIdDoesNotExist() throws Exception
    {
        UserModel nonexistentUser = dataUser.createRandomTestUser();
        nonexistentUser.setUsername("nonexistent");
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingUser(nonexistentUser).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, "nonexistent"));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Specify -me- string in place of <personid> for request.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void replacePersonIdWithMeRequest() throws Exception
    {
        returnedCollection = restClient.authenticateUser(newMember).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListContains("id", moderatedSite.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Verify that if empty personId is used status code returned is 404.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void useEmptyPersonId() throws Exception
    {
        UserModel emptyNameMember = dataUser.createRandomTestUser();
        emptyNameMember.setUsername(" ");
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingUser(emptyNameMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary("The entity with id: "  + emptyNameMember.getUsername() + " was not found");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Get site membership requests to a public site.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void getRequestsToPublicSite() throws Exception
    {
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        returnedCollection = restClient.withCoreAPI().usingAuthUser().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListDoesNotContain("id", publicSite.getId());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Get site membership requests to a moderated site.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void getRequestsToModeratedSite() throws Exception
    {
        returnedCollection = restClient.authenticateUser(newMember).withCoreAPI().usingUser(newMember).getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListContains("id", moderatedSite.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, description = "Approve request then get requests.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void approveRequestThenGetRequests() throws Exception
    {
        UserModel siteManager = dataUser.createRandomTestUser();

        moderatedSite = dataSite.usingUser(siteManager).createModeratedRandomSite();
        restClient.authenticateUser(newMember).withCoreAPI().usingMe().addSiteMembershipRequest(moderatedSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModel taskModel = restClient.authenticateUser(newMember).withWorkflowAPI().getTasks().getTaskModelByDescription(moderatedSite);
        workflow.approveSiteMembershipRequest(siteManager.getUsername(), siteManager.getPassword(), taskModel.getId(), true, "Approve");

        returnedCollection = restClient.authenticateUser(newMember).withCoreAPI().usingMe().getSiteMembershipRequests();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListDoesNotContain("id", moderatedSite.getId());
    }

}
