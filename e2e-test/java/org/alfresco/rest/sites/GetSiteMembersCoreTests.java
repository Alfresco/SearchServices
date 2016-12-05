package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMemberModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 11/23/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
public class GetSiteMembersCoreTests extends RestTest
{
    private UserModel userModel, publicSiteContributor, privateSiteConsumer, moderatedSiteManager, admin;
    private SiteModel publicSite, privateSite, moderatedSite, moderatedSite2;
    private RestSiteMemberModelsCollection siteMembers;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        admin = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        publicSiteContributor = dataUser.createRandomTestUser();
        moderatedSiteManager = dataUser.createRandomTestUser();
        privateSiteConsumer = dataUser.createRandomTestUser();
        publicSite = dataSite.usingAdmin().createPublicRandomSite();
        privateSite = dataSite.usingAdmin().createPrivateRandomSite();
        moderatedSite = dataSite.usingAdmin().createModeratedRandomSite();
        moderatedSite2 = dataSite.usingAdmin().createModeratedRandomSite();
        dataUser.addUserToSite(publicSiteContributor, publicSite, UserRole.SiteContributor);
        dataUser.addUserToSite(moderatedSiteManager, moderatedSite, UserRole.SiteManager);
        dataUser.addUserToSite(privateSiteConsumer, privateSite, UserRole.SiteConsumer);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get site members call returns status code 404 if siteId does not exist")
    public void checkStatusCodeForNonExistentSiteId() throws Exception
    {
        restClient.authenticateUser(publicSiteContributor).withCoreAPI()
                .usingSite("NonExistentSiteId").getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "NonExistentSiteId"));
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get site members call returns status code 400 for invalid maxItems")
    public void checkStatusCodeForInvalidMaxItems() throws Exception
    {
        restClient.authenticateUser(publicSiteContributor).withParams("maxItems=0")
                .withCoreAPI().usingSite(publicSite).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary("Only positive values supported for maxItems");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get site members call returns status code 400 for invalid skipCount ")
    public void checkStatusCodeForInvalidSkipCount() throws Exception
    {
        restClient.authenticateUser(publicSiteContributor).withParams("skipCount=A")
                .withCoreAPI().usingSite(publicSite).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary("Invalid paging parameter skipCount:A");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets public site members and status code is 200")
    public void getPublicSiteMembers() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(publicSite).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListContains("id", admin.getUsername())
            .and().entriesListContains("id", publicSiteContributor.getUsername())
            .and().paginationField("count").is("2");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets moderated site members and status code is 200")
    public void getModeratedSiteMembers() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListContains("id", admin.getUsername())
            .and().entriesListContains("id", moderatedSiteManager.getUsername())
            .and().paginationField("count").is("2");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if user gets private site members if he is a member of that site and status code is 200")
    public void getPrivateSiteMembersByASiteMember() throws Exception
    {
        siteMembers = restClient.authenticateUser(privateSiteConsumer).withCoreAPI()
                .usingSite(privateSite).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListContains("id", admin.getUsername())
                .and().entriesListContains("id", privateSiteConsumer.getUsername())
                .and().paginationField("count").is("2");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if user doesn't get private site members if he is not a member of that site and status code is 404")
    public void getPrivateSiteMembersByNotASiteMember() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(privateSite).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, privateSite.getTitle()));
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if user gets moderated site members after the adding of a new member and status code is 200")
    public void getSiteMembersAfterAddingNewMember() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite2).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListContains("id", admin.getUsername())
                .and().paginationField("count").is("1");

        restClient.authenticateUser(admin).withCoreAPI().usingSite(moderatedSite2).addPerson(privateSiteConsumer);

        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite2).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListContains("id", admin.getUsername())
                .and().entriesListContains("id", privateSiteConsumer.getUsername())
                .and().paginationField("count").is("2");
    }

}
