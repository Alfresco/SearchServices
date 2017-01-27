package org.alfresco.rest.sites;

import java.util.List;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteContainerModelsCollection;
import org.alfresco.rest.model.RestSiteMemberModelsCollection;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.utility.constants.ContainerName;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetSiteFullTests extends RestTest
{
    private UserModel adminUserModel, testUser;
    private SiteModel publicSite;
    private RestSiteModel restSiteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        testUser = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUserModel);        
        publicSite = dataSite.usingUser(adminUserModel).createPublicRandomSite();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Perform invalid request and check default error schema")
    public void checkDefaultErrorSchema() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite("NonExistentSiteId").getSite();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "NonExistentSiteId"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL }, expectedExceptions = java.lang.AssertionError.class)
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that properties parameter is applied (guid field is mandatory, thus assertion error is expected)")
    public void checkThatPropertiesParameterIsApplied() throws Exception
    {
        restSiteModel = restClient.authenticateUser(adminUserModel).withParams("properties=id, visibility").withCoreAPI().usingSite(publicSite).getSite();
        restSiteModel.assertThat().field("id").is(publicSite.getId()).and().field("visibility").is(Visibility.PUBLIC.toString());        
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that properties parameter is applied")
    public void checkThatPropertiesParameterIsAppliedPositiveTest() throws Exception
    {
        restSiteModel = restClient.authenticateUser(adminUserModel).withParams("properties=id,guid,title,visibility").withCoreAPI().usingSite(publicSite).getSite();
        restSiteModel.assertThat().field("id").is(publicSite.getId()).and().field("visibility").is(Visibility.PUBLIC.toString())
            .and().field("description").isNull()
            .and().field("role").isNull();
    }
 
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Delete site then get site details")
    public void deleteSiteThenGetSiteDetails() throws Exception
    {
        SiteModel newSite = dataSite.usingAdmin().createPublicRandomSite();
        dataSite.deleteSite(newSite);
        restSiteModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(newSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newSite.getId()))
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);      
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Delete site then get site details")
    public void updateSiteVisibilityToPrivateThenGetSite() throws Exception
    {
        SiteModel newSite = dataSite.usingAdmin().createPublicRandomSite();
        restSiteModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(newSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        dataSite.updateSiteVisibility(newSite, Visibility.PRIVATE);
        restSiteModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(newSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("id").is(newSite.getId())
            .and().field("visibility").is(Visibility.PRIVATE.toString());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION, description= "Get private site with a non member")
    public void getPrivateSiteWithNonMemberUser() throws Exception
    {
        SiteModel newSite = dataSite.usingAdmin().createPrivateRandomSite();
        restSiteModel = restClient.authenticateUser(testUser).withCoreAPI().usingSite(newSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, newSite.getId()))
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);      
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION, description= "Get moderated site with a non member")
    public void getModeratedSiteWithNonMemberUser() throws Exception
    {
        SiteModel newSite = dataSite.usingAdmin().createModeratedRandomSite();
        restSiteModel = restClient.authenticateUser(testUser).withCoreAPI().usingSite(newSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("id").is(newSite.getId())
            .and().field("visibility").is(Visibility.MODERATED.toString());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION, description= "Get public site with a non member")
    public void getPublicSiteWithNonMemberUser() throws Exception
    {
        SiteModel newSite = dataSite.usingAdmin().createPublicRandomSite();
        restSiteModel = restClient.authenticateUser(testUser).withCoreAPI().usingSite(newSite).getSite();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restSiteModel.assertThat().field("id").is(newSite.getId())
            .and().field("visibility").is(Visibility.PUBLIC.toString());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that relations parameter is applied for containers")
    public void checkThatRelationsParameterIsAppliedForContainers() throws Exception
    {
        List<Object> jsonObjects = restClient.authenticateUser(adminUserModel)
                .withParams("relations=containers").withCoreAPI().usingSite(publicSite).getSiteWithRelations();
        
        RestSiteModel siteModel = (RestSiteModel) jsonObjects.get(0);
        RestSiteContainerModelsCollection containers = (RestSiteContainerModelsCollection) jsonObjects.get(1);
        
        siteModel.assertThat().field("visibility").is(publicSite.getVisibility())
            .and().field("id").is(publicSite.getId())
            .and().field("description").is(publicSite.getDescription())
            .and().field("title").is(publicSite.getTitle())
            .and().field("preset").is("site-dashboard")
            .and().field("guid").isNotEmpty();
        
        containers.assertThat().entriesListCountIs(1)
            .and().entriesListContains("folderId", ContainerName.documentLibrary.toString());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that relations parameter is applied for members")
    public void checkThatRelationsParameterIsAppliedForMembers() throws Exception
    {
        List<Object> jsonObjects = restClient.authenticateUser(adminUserModel)
                .withParams("relations=members").withCoreAPI().usingSite(publicSite).getSiteWithRelations();
        
        RestSiteModel siteModel = (RestSiteModel) jsonObjects.get(0);
        RestSiteMemberModelsCollection siteMembers = (RestSiteMemberModelsCollection) jsonObjects.get(1);
        
        siteModel.assertThat().field("visibility").is(publicSite.getVisibility())
            .and().field("id").is(publicSite.getId())
            .and().field("description").is(publicSite.getDescription())
            .and().field("title").is(publicSite.getTitle())
            .and().field("preset").is("site-dashboard")
            .and().field("guid").isNotEmpty();
        
        siteMembers.assertThat().entriesListCountIs(1)
            .assertThat().entriesListContains("id", adminUserModel.getUsername())
            .assertThat().entriesListContains("role", UserRole.SiteManager.toString());
        siteMembers.getOneRandomEntry().onModel().assertThat().field("person.firstName").is("Administrator")
            .and().field("person.id").is("admin");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL } )
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Check that relations parameter is applied for containers and members")
    public void checkThatRelationsParameterIsAppliedForContainersAndMembers() throws Exception
    {
        List<Object> jsonObjects = restClient.authenticateUser(adminUserModel)
                .withParams("relations=containers,members").withCoreAPI().usingSite(publicSite).getSiteWithRelations();
        
        RestSiteModel siteModel = (RestSiteModel) jsonObjects.get(0);
        RestSiteContainerModelsCollection containers = (RestSiteContainerModelsCollection) jsonObjects.get(1);
        RestSiteMemberModelsCollection siteMembers = (RestSiteMemberModelsCollection) jsonObjects.get(2);
        
        siteModel.assertThat().field("visibility").is(publicSite.getVisibility())
            .and().field("id").is(publicSite.getId())
            .and().field("description").is(publicSite.getDescription())
            .and().field("title").is(publicSite.getTitle())
            .and().field("preset").is("site-dashboard")
            .and().field("guid").isNotEmpty();
        
        containers.assertThat().entriesListCountIs(1)
            .and().entriesListContains("folderId", ContainerName.documentLibrary.toString());
        
        siteMembers.assertThat().entriesListCountIs(1)
            .assertThat().entriesListContains("id", adminUserModel.getUsername())
            .assertThat().entriesListContains("role", UserRole.SiteManager.toString());
        siteMembers.getOneRandomEntry().onModel().assertThat().field("person.firstName").is("Administrator")
            .and().field("person.id").is("admin");
    }
}