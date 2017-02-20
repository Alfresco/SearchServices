package org.alfresco.rest.sites.members;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSiteMemberModel;
import org.alfresco.rest.model.RestSiteMemberModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class GetSiteMembersFullTests extends RestTest
{
    private UserModel userModel, publicSiteContributor;
    private UserModel moderatedSiteContributor, moderatedSiteCollaborator, moderatedSiteConsumer;
    private SiteModel publicSite, moderatedSite, moderatedSite2;
    private RestSiteMemberModelsCollection siteMembers;
    private RestSiteMemberModel firstSiteMember, secondSiteMember, thirdSiteMember, fourthSiteMember;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        publicSiteContributor = dataUser.createRandomTestUser();
        moderatedSiteContributor = dataUser.createRandomTestUser();
        moderatedSiteCollaborator = dataUser.createRandomTestUser();
        moderatedSiteConsumer = dataUser.createRandomTestUser();
        publicSite = dataSite.usingUser(userModel).createPublicRandomSite();
        moderatedSite = dataSite.usingUser(userModel).createModeratedRandomSite();
        moderatedSite2 = dataSite.usingUser(userModel).createModeratedRandomSite();
        dataUser.addUserToSite(publicSiteContributor, publicSite, UserRole.SiteContributor);
        dataUser.addUserToSite(publicSiteContributor, publicSite, UserRole.SiteContributor);
        dataUser.addUserToSite(moderatedSiteContributor, moderatedSite, UserRole.SiteContributor);
        dataUser.addUserToSite(moderatedSiteCollaborator, moderatedSite, UserRole.SiteCollaborator);
        dataUser.addUserToSite(moderatedSiteConsumer, moderatedSite, UserRole.SiteConsumer);
        dataUser.addUserToSite(moderatedSiteContributor, moderatedSite2, UserRole.SiteContributor);
        
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite).usingParams("properties=role,id").getSiteMembers();
        firstSiteMember = siteMembers.getEntries().get(0).onModel();
        secondSiteMember = siteMembers.getEntries().get(1).onModel();
        thirdSiteMember = siteMembers.getEntries().get(2).onModel();
        fourthSiteMember = siteMembers.getEntries().get(3).onModel();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets moderated site members with properties parameter applied and status code is 200")
    public void getModeratedSiteMembersUsingPropertiesParameter() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite).usingParams("properties=role,id").getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListCountIs(4)
            .and().entriesListDoesNotContain("person")
            .and().entriesListContains("role", UserRole.SiteManager.toString())
            .and().entriesListContains("id", userModel.getUsername())
            .and().entriesListContains("role", UserRole.SiteContributor.toString())
            .and().entriesListContains("id", moderatedSiteContributor.getUsername())
            .and().entriesListContains("role", UserRole.SiteCollaborator.toString())
            .and().entriesListContains("id", moderatedSiteCollaborator.getUsername())
            .and().entriesListContains("role", UserRole.SiteConsumer.toString())
            .and().entriesListContains("id", moderatedSiteConsumer.getUsername());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets moderated site members with skipCount parameter applied")
    public void getModeratedSiteMembersUsingSkipCountParameter() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite).usingParams("skipCount=2").getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().paginationField("count").is("2");
        siteMembers.assertThat().paginationField("skipCount").is("2");
        siteMembers.assertThat().entriesListDoesNotContain("id", firstSiteMember.getId())
            .and().entriesListDoesNotContain("id", secondSiteMember.getId())
            .and().entriesListContains("role", thirdSiteMember.getRole().toString())
            .and().entriesListContains("id", thirdSiteMember.getId())
            .and().entriesListContains("role", fourthSiteMember.getRole().toString())
            .and().entriesListContains("id", fourthSiteMember.getId());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets moderated site members with high skipCount parameter applied")
    public void getModeratedSiteMembersUsingHighSkipCountParameter() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite).usingParams("skipCount=100").getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().paginationField("count").is("0");
        siteMembers.assertThat().paginationField("skipCount").is("100");
        siteMembers.assertThat().entriesListIsEmpty();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets moderated site members with maxItems parameter applied and check all pagination fields")
    public void getModeratedSiteMembersUsingMaxItemsParameter() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite).usingParams("maxItems=1").getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().paginationField("count").is("1");
        siteMembers.assertThat().paginationField("hasMoreItems").is("true");
        siteMembers.assertThat().paginationField("maxItems").is("1");
        siteMembers.assertThat().paginationField("totalItems").isNotPresent();
        siteMembers.assertThat().entriesListContains("id", firstSiteMember.getId())
            .and().entriesListContains("role", firstSiteMember.getRole().toString())
            .and().entriesListDoesNotContain("id", secondSiteMember.getId())
            .and().entriesListDoesNotContain("id", thirdSiteMember.getId());
    }
     
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if user gets moderated site members with member not joined yet and status code is 200")
    public void getSiteMembersFromNotJoinedModeratedSite() throws Exception
    {
        UserModel userNotJoined = dataUser.createRandomTestUser();
        restClient.authenticateUser(userNotJoined).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite2).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListContains("role", Role.SiteManager.toString())
            .and().entriesListContains("id", userModel.getUsername())
            .and().entriesListDoesNotContain("id", userNotJoined.getUsername());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if any user gets site members after the member was removed from site and status code is 200")
    public void getSiteMembersAfterRemoveTheSiteMember() throws Exception
    {
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite2).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListCountIs(2);
        
        restClient.authenticateUser(userModel).withCoreAPI().usingSite(moderatedSite2).deleteSiteMember(moderatedSiteContributor);
        
        siteMembers = restClient.authenticateUser(userModel).withCoreAPI()
                .usingSite(moderatedSite2).getSiteMembers();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        siteMembers.assertThat().entriesListCountIs(1)
            .and().entriesListContains("role", Role.SiteManager.toString())
            .and().entriesListContains("id", userModel.getUsername());
    }
}
