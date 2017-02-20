package org.alfresco.rest.sites.get;

import java.util.List;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteContainerModelsCollection;
import org.alfresco.rest.model.RestSiteMemberModelsCollection;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.ContainerName;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetSitesFullTests extends RestTest
{
    private UserModel regularUser;
    private SiteModel publicSite, secondSite, privateSite, moderatedSite;
    private RestSiteModelsCollection sites;
    private String name;
    private UserModel adminUser;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        name = RandomData.getRandomName("ZZZZZZZZZ-PublicSite");
        regularUser = dataUser.createRandomTestUser();
        publicSite = dataSite.usingAdmin().createSite(new SiteModel(Visibility.PUBLIC, "guid", name, name, name));
        privateSite = dataSite.usingAdmin().createSite(new SiteModel(RandomData.getRandomName("ZZZZZZZZZ-PrivateSite"), Visibility.PRIVATE));
        moderatedSite = dataSite.usingAdmin().createSite(new SiteModel(RandomData.getRandomName("ZZZZZZZZZ-ModeratedSite"), Visibility.MODERATED));
        secondSite = dataSite.usingAdmin().createSite(new SiteModel(RandomData.getRandomName("000000000-PublicSite")));
        dataUser.addUserToSite(regularUser, privateSite, UserRole.SiteManager);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if User gets sites ordered by title descending and status code is 200")
    public void getSitesOrderedByTitleDESC() throws Exception
    {
        int totalItems = restClient.authenticateUser(regularUser).withCoreAPI().getSites().getPagination().getTotalItems();
        sites = restClient.authenticateUser(regularUser).withParams(String.format("maxItems=%s&orderBy=title DESC", totalItems))
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListIsNotEmpty();
        List<RestSiteModel> sitesList = sites.getEntries();
        sitesList.get(0).onModel().assertThat().field("title").is(publicSite.getTitle());
        sitesList.get(1).onModel().assertThat().field("title").is(privateSite.getTitle());
        sitesList.get(2).onModel().assertThat().field("title").is(moderatedSite.getTitle());
        sitesList.get(sitesList.size()-1).onModel().assertThat().field("title").is(secondSite.getTitle());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if User gets sites ordered by id ascending and status code is 200")
    public void getSitesOrderedByIdASC() throws Exception
    {
        int totalItems = restClient.authenticateUser(regularUser).withCoreAPI().getSites().getPagination().getTotalItems();
        sites = restClient.authenticateUser(regularUser).withParams(String.format("maxItems=%s&orderBy=id ASC", totalItems))
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListIsNotEmpty();
        List<RestSiteModel> sitesList = sites.getEntries();
        sitesList.get(sitesList.size()-1).onModel().assertThat().field("id").is(publicSite.getId());
        sitesList.get(sitesList.size()-2).onModel().assertThat().field("id").is(privateSite.getId());
        sitesList.get(sitesList.size()-3).onModel().assertThat().field("id").is(moderatedSite.getId());
        sitesList.get(0).onModel().assertThat().field("id").is(secondSite.getId());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify pagination")
    public void checkPagination() throws Exception
    {
        sites = restClient.authenticateUser(regularUser).withCoreAPI().getSites();
        sites.getPagination().assertThat()
            .field("totalItems").isNotEmpty().and()
            .field("maxItems").is("100").and()
            .field("hasMoreItems").is((sites.getPagination().getTotalItems() > sites.getPagination().getMaxItems()) ? "true" : "false").and()
            .field("skipCount").is("0").and()
            .field("count").is((sites.getPagination().isHasMoreItems()) ? sites.getPagination().getMaxItems() : sites.getPagination().getTotalItems());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify user can get only first two sites and status code is 200")
    public void getFirstTwoSites() throws Exception
    {
        sites = restClient.authenticateUser(regularUser).withParams(String.format("maxItems=%s&orderBy=id DESC", 2))
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.getPagination().assertThat().field("maxItems").is("2").and().field("count").is("2");
        sites.assertThat().entriesListCountIs(2).and().entriesListDoesNotContain(moderatedSite.getId());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify user can get sites using high skipCount parameter and status code is 200")
    public void getSitesUsingHighSkipCount() throws Exception
    {
        RestSiteModelsCollection allSites = restClient.authenticateUser(regularUser).withCoreAPI().getSites();
        sites = restClient.withParams("skipCount=100").withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().paginationField("skipCount").is("100");
        if(allSites.getPagination().getTotalItems() > 100)
            sites.assertThat().entriesListIsNotEmpty();
        else
            sites.assertThat().entriesListIsEmpty();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify user can not get sites using zero maxItems parameter and status code is 400")
    public void userCanNotGetSitesUsingZeroMaxItems() throws Exception
    {
        sites = restClient.authenticateUser(regularUser).withParams("maxItems=0").withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError()
            .containsErrorKey(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
            .containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify that getSites request applies valid properties param and status code is 200")
    public void getSitesRequestWithValidPropertiesParam() throws Exception
    {
        sites = restClient.authenticateUser(regularUser).withParams("properties=id").withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListDoesNotContain("description")
            .assertThat().entriesListDoesNotContain("title")
            .assertThat().entriesListDoesNotContain("visibility")
            .assertThat().entriesListDoesNotContain("guid")
            .assertThat().entriesListContains("id");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify pagination when skipCount and MaxItems are used")
    public void checkPaginationWithSkipCountAndMaxItems() throws Exception
    {
        sites = restClient.authenticateUser(regularUser).withParams("skipCount=10&maxItems=110").withCoreAPI().getSites();
        sites.getPagination().assertThat()
            .field("totalItems").isNotEmpty().and()
            .field("maxItems").is("110").and()
            .field("hasMoreItems").is((sites.getPagination().getTotalItems() > sites.getPagination().getMaxItems())?"true":"false").and()
            .field("skipCount").is("10").and()
                .field("count").is((sites.getPagination().isHasMoreItems()) ? sites.getPagination().getMaxItems()
                        : sites.getPagination().getTotalItems() - sites.getPagination().getSkipCount());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if User gets sites ordered by id ascending and status code is 200")
    public void getSitesOrderedByTitleASCAndVisibilityASC() throws Exception
    {
        SiteModel secondPublicSite = dataSite.usingAdmin().createSite(
                new SiteModel(Visibility.PUBLIC, "guid", name+"A", name+"A", name));   
        
        int totalItems = restClient.authenticateUser(regularUser).withCoreAPI().getSites().getPagination().getTotalItems();
        sites = restClient.authenticateUser(regularUser).withParams(String.format("maxItems=%s&orderBy=description ASC&orderBy=title ASC", totalItems))
                .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        sites.assertThat().entriesListIsNotEmpty();
        List<RestSiteModel> sitesList = sites.getEntries();
        sitesList.get(sitesList.size()-1).onModel().assertThat().field("title").is(secondPublicSite.getTitle());
        sitesList.get(sitesList.size()-2).onModel().assertThat().field("title").is(publicSite.getTitle());
        
        dataSite.deleteSite(secondPublicSite);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that relations parameter is applied for containers")
    public void checkThatRelationsParameterIsAppliedForContainers() throws Exception
    {
        List<List<Object>> jsonObjects = restClient.authenticateUser(adminUser)
                .withParams("relations=containers").withCoreAPI().usingSite(publicSite).getSitesWithRelations();
        
        List<Object> siteObjects = jsonObjects.get(0);
        for (int i = 0; i < siteObjects.size(); i++)
        {
            RestSiteModel siteModel = (RestSiteModel) siteObjects.get(i);
            siteModel.assertThat().field("visibility").isNotEmpty()
                .and().field("id").isNotEmpty()
                .and().field("title").isNotEmpty()
                .and().field("preset").is("site-dashboard")
                .and().field("guid").isNotEmpty();
        }
        
        List<Object> containerObjects = jsonObjects.get(1);
        for (int i = 0; i < containerObjects.size(); i++)
        {
            RestSiteContainerModelsCollection containers = (RestSiteContainerModelsCollection) containerObjects.get(i);
            containers.assertThat().entriesListIsNotEmpty().and().entriesListContains("folderId", ContainerName.documentLibrary.toString());
        }
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that relations parameter is applied for members")
    public void checkThatRelationsParameterIsAppliedForMembers() throws Exception
    {
        List<List<Object>> jsonObjects = restClient.authenticateUser(adminUser)
                .withParams("relations=members").withCoreAPI().usingSite(publicSite).getSitesWithRelations();
        
        List<Object> siteObjects = jsonObjects.get(0);
        for (int i = 0; i < siteObjects.size(); i++)
        {
            RestSiteModel siteModel = (RestSiteModel) siteObjects.get(i);
            siteModel.assertThat().field("visibility").isNotEmpty()
                .and().field("id").isNotEmpty()
                .and().field("title").isNotEmpty()
                .and().field("preset").is("site-dashboard")
                .and().field("guid").isNotEmpty();
        }
        
        List<Object> memberObjects = jsonObjects.get(1);
        for (int i = 0; i < memberObjects.size(); i++)
        {
            RestSiteMemberModelsCollection siteMembers = (RestSiteMemberModelsCollection) memberObjects.get(i);
            siteMembers.assertThat().entriesListIsNotEmpty().assertThat().entriesListContains("id").assertThat().entriesListContains("role", UserRole.SiteManager.toString());
            siteMembers.getOneRandomEntry().onModel().assertThat().field("person.firstName").isNotEmpty().and().field("person.id").isNotEmpty();
        }
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that relations parameter is applied for members and containers")
    public void checkThatRelationsParameterIsAppliedForMembersAndContainers() throws Exception
    {
        List<List<Object>> jsonObjects = restClient.authenticateUser(adminUser)
                .withParams("relations=containers,members").withCoreAPI().usingSite(publicSite).getSitesWithRelations();
        
        List<Object> siteObjects = jsonObjects.get(0);
        for (int i = 0; i < siteObjects.size(); i++)
        {
            RestSiteModel siteModel = (RestSiteModel) siteObjects.get(i);
            siteModel.assertThat().field("visibility").isNotEmpty()
                .and().field("id").isNotEmpty()
                .and().field("title").isNotEmpty()
                .and().field("preset").is("site-dashboard")
                .and().field("guid").isNotEmpty();
        }
        
        List<Object> containerObjects = jsonObjects.get(1);
        for (int i = 0; i < containerObjects.size(); i++)
        {
            RestSiteContainerModelsCollection containers = (RestSiteContainerModelsCollection) containerObjects.get(i);
            containers.assertThat().entriesListIsNotEmpty().and().entriesListContains("folderId", ContainerName.documentLibrary.toString());
        }
        
        List<Object> memberObjects = jsonObjects.get(2);
        for (int i = 0; i < memberObjects.size(); i++)
        {
            RestSiteMemberModelsCollection siteMembers = (RestSiteMemberModelsCollection) memberObjects.get(i);
            siteMembers.assertThat().entriesListIsNotEmpty().assertThat().entriesListContains("id").assertThat().entriesListContains("role", UserRole.SiteManager.toString());
            siteMembers.getOneRandomEntry().onModel().assertThat().field("person.firstName").isNotEmpty().and().field("person.id").isNotEmpty();
        }
    }
    
    @AfterClass(alwaysRun=true)
    public void tearDown() throws Exception
    {
        dataSite.deleteSite(publicSite);
        dataSite.deleteSite(privateSite);
        dataSite.deleteSite(moderatedSite);
        dataSite.deleteSite(secondSite);
    }
}