package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetSiteMemberFullTests extends RestTest
{
    private UserModel adminUser;
    private SiteModel siteModel;
    private SiteModel moderatedSiteModel;
    private SiteModel privateSiteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel manager, consumer, collaborator, contributor;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(adminUser).createModeratedRandomSite();
        privateSiteModel = dataSite.usingUser(adminUser).createPrivateRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        
        consumer = usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer);
        manager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        collaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        contributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        
        dataUser.addUserToSite(consumer, moderatedSiteModel, UserRole.SiteConsumer);
        dataUser.addUserToSite(manager, moderatedSiteModel, UserRole.SiteManager);
        dataUser.addUserToSite(consumer, privateSiteModel, UserRole.SiteConsumer);
        dataUser.addUserToSite(manager, privateSiteModel, UserRole.SiteManager);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Collaborator role gets site member with Contributor role and status code is OK (200)")
    public void getSiteContributorMemberWithCollaboratorRole() throws Exception
    {        
        restClient.authenticateUser(collaborator).withCoreAPI().usingSite(siteModel).getSiteMember(contributor)
                    .assertThat().field("id").is(contributor.getUsername())
                    .and().field("role").is(contributor.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Contributor role gets site member with Collaborator role and status code is OK (200)")
    public void getSiteCollaboratorMemberWithContributorRole() throws Exception
    {        
        restClient.authenticateUser(contributor).withCoreAPI().usingSite(siteModel).getSiteMember(collaborator)
                    .assertThat().field("id").is(collaborator.getUsername())
                    .and().field("role").is(collaborator.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Collaborator role gets admin role and status code is OK (200)")
    public void getAdminWithCollaboratorRole() throws Exception
    {        
        restClient.authenticateUser(collaborator).withCoreAPI().usingSite(siteModel).getSiteMember(adminUser)
                    .assertThat().field("id").is(adminUser.getUsername())
                    .and().field("role").is(UserRole.SiteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Collaborator role gets site member with Consumer role and status code is OK (200)")
    public void getSiteConsumerMemberWithCollaboratorRole() throws Exception
    {        
        restClient.authenticateUser(collaborator).withCoreAPI().usingSite(siteModel).getSiteMember(consumer)
                    .assertThat().field("id").is(consumer.getUsername())
                    .and().field("role").is(consumer.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user gets site member of private site and status code is OK (200)")
    public void getSiteMemberOfPrivateSite() throws Exception
    { 
        restClient.authenticateUser(manager).withCoreAPI().usingSite(privateSiteModel).getSiteMember(consumer)
                    .assertThat().field("id").is(consumer.getUsername())
                    .and().field("role").is(consumer.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify not joined user is not is not able to get site member of private site and status code is 404")
    public void userThatIsNotMemberOfPrivateSiteIsNotAbleToGetSiteMember() throws Exception
    { 
        UserModel newMember = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(newMember).withCoreAPI().usingSite(privateSiteModel).getSiteMember(consumer);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, consumer.getUsername(), privateSiteModel.getTitle()));
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify admin is not able to get from site a user that created a member request that was not accepted yet")
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    public void adminIsNotAbleToGetFromSiteANonExistingMember() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSiteModel);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingSite(moderatedSiteModel).getSiteMember(newMember);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, newMember.getUsername(), moderatedSiteModel.getTitle()))
            .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user gets site creator and status code is OK (200)")
    public void getSiteCreator() throws Exception
    {        
        SiteModel newSiteModel = dataSite.usingUser(collaborator).createModeratedRandomSite();
        dataUser.addUserToSite(consumer, siteModel, UserRole.SiteConsumer);
        
        restClient.authenticateUser(consumer).withCoreAPI().usingSite(newSiteModel).getSiteMember(collaborator)
                    .assertThat().field("id").is(collaborator.getUsername())
                    .and().field("role").is(UserRole.SiteManager);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user with Consumer role can get site member using \"-me-\" in place of personId")
    public void getSiteMemberOfPrivateSiteUsingMeForPersonId() throws Exception
    {
        UserModel meUser = new UserModel("-me-", "password");
        
        restClient.authenticateUser(consumer).withCoreAPI().usingSite(privateSiteModel).getSiteMember(meUser)
            .assertThat().field("id").is(consumer.getUsername())
            .and().field("role").is(consumer.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user gets site member of moderated site and status code is OK (200)")
    public void getSiteMemberOfModeratedSite() throws Exception
    { 
        restClient.authenticateUser(manager).withCoreAPI().usingSite(moderatedSiteModel).getSiteMember(consumer)
                    .assertThat().field("id").is(consumer.getUsername())
                    .and().field("role").is(consumer.getUserRole());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify not joined user gets site member of moderated site and status code is OK (200)")
    public void userThatIsNotMemberOfModeratedSiteIsAbleToGetSiteMember() throws Exception
    { 
        UserModel newMember = dataUser.createRandomTestUser();
        
        restClient.authenticateUser(newMember).withCoreAPI().usingSite(moderatedSiteModel).getSiteMember(consumer);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site member request with properties parameter returns status code 200 and parameter is applied")            
    public void getSiteMemberUsingPropertiesParameter() throws Exception
    {
        restClient.authenticateUser(manager)
                .withCoreAPI().usingSite(siteModel).usingParams("properties=id").getSiteMember(consumer)
                .assertThat().fieldsCount().is(1)
                .and().field("id").isNotEmpty()
                .and().field("role").isNull();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
