package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMemberModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddSiteMemberFullTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel publicSiteModel;
    private SiteModel moderatedSiteModel;
    private SiteModel privateSiteModel;
    private String addMembersJson;
    private RestSiteMemberModel memberModel;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        publicSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        privateSiteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        addMembersJson = "{\"role\":\"%s\",\"id\":\"%s\"}";
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, description = "Check default error schema when request fails")
    public void checkDefaultErrorSchema() throws Exception
    {
        UserModel testUser = new UserModel("inexistentUser", "password");
        testUser.setUserRole(UserRole.SiteManager);
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, testUser.getUsername()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
        description = "Add new site member request by providing an empty body")
    public void addSiteMemberUsingEmptyBody() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "", "sites/{siteId}/members?{parameters}", 
                publicSiteModel.getId(), restClient.getParameters());
        restClient.processModel(RestSiteMemberModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsSummary(String.format(RestErrorModel.NO_CONTENT, "No content to map to Object due to end of input"))
            .containsErrorKey(String.format(RestErrorModel.NO_CONTENT, "No content to map to Object due to end of input"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
        description = "Check lower and upper case letters for role field")
    public void checkLowerUpperCaseLettersForRole() throws Exception
    {
        UserModel user = dataUser.createRandomTestUser();
       
        String json = String.format(addMembersJson, "SITEMANAGER", user.getUsername());
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, json, "sites/{siteId}/members?{parameters}", publicSiteModel.getId(), restClient.getParameters());
        restClient.processModel(RestSiteMemberModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsSummary(String.format(RestErrorModel.UNKNOWN_ROLE, "SITEMANAGER"))
            .containsErrorKey(String.format(RestErrorModel.UNKNOWN_ROLE, "SITEMANAGER"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);       

        json = String.format(addMembersJson, "sitemanager", user.getUsername());
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        request = RestRequest.requestWithBody(HttpMethod.POST, json, "sites/{siteId}/members?{parameters}", publicSiteModel.getId(), restClient.getParameters());
        restClient.processModel(RestSiteMemberModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsSummary(String.format(RestErrorModel.UNKNOWN_ROLE, "sitemanager"))
            .containsErrorKey(String.format(RestErrorModel.UNKNOWN_ROLE, "sitemanager"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE); 
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
        description = "Check empty value for user role")
    public void checkEmptyValueForRole() throws Exception
    {
        UserModel user = dataUser.createRandomTestUser();
       
        String json = String.format(addMembersJson, "", user.getUsername());
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, json, "sites/{siteId}/members?{parameters}", publicSiteModel.getId(), restClient.getParameters());
        restClient.processModel(RestSiteMemberModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsSummary(String.format(RestErrorModel.NO_CONTENT, 
                    "N/A (through reference chain: org.alfresco.rest.api.model.SiteMember[\"role\"])"));

    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that collaborator role can be added to public site")
    public void addCollaboratorToPublicSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteCollaborator);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that contributor role can be added to public site")
    public void addContributorToPublicSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteContributor);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that consumer role can be added to public site")
    public void addConsumerToPublicSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(publicSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that collaborator role can be added to moderated site")
    public void addCollaboratorToModeratedSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteCollaborator);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that contributor role can be added to moderated site")
    public void addContributorToModeratedSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteContributor);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that consumer role can be added to moderated site")
    public void addConsumerToModeratedSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(moderatedSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that collaborator role can be added to private site")
    public void addCollaboratorToPrivateSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteCollaborator);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that contributor role can be added to private site")
    public void addContributorToPrivateSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteContributor);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that consumer role can be added to private site")
    public void addConsumerToPrivateSite() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);
        memberModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(privateSiteModel).addPerson(testUser);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(testUser.getUsername())
               .and().field("role").is(testUser.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can be added to private site by site manager")
    public void adminCanBeAddedToPrivateSiteBySiteManager() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser();
        SiteModel privateSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        memberModel = restClient.authenticateUser(testUser).withCoreAPI().usingSite(privateSite).addPerson(adminUserModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        memberModel.assertThat().field("id").is(adminUserModel.getUsername())
               .and().field("role").is(adminUserModel.getUserRole());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin can be added to private site by site manager")
    public void adminCanBeAddedToPrivateSiteBySiteCollaborator() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser();
        SiteModel privateSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        UserModel siteCollaborator = dataUser.createRandomTestUser();
        dataUser.addUserToSite(siteCollaborator, privateSite, UserRole.SiteCollaborator);
        memberModel = restClient.authenticateUser(siteCollaborator).withCoreAPI().usingSite(privateSite).addPerson(adminUserModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);   
    }
}