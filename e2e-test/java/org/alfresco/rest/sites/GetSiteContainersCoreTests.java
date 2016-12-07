package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.ContainerName;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
public class GetSiteContainersCoreTests  extends RestTest
{
    
    private UserModel adminUserModel;
    private SiteModel siteModel, siteModel1;
    private SiteModel moderatedSiteModel, privateSiteModel;
    private ListUserWithRoles usersWithRoles;
    private ListUserWithRoles usersWithRoles1;
    
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        siteModel1 = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(adminUserModel).createModeratedRandomSite();
        privateSiteModel =   dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        
        usersWithRoles1 = dataUser.addUsersWithRolesToSite(siteModel1, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        
        dataLink.usingAdmin().usingSite(siteModel1).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(siteModel1).createRandomDiscussion();
        
        dataLink.usingAdmin().usingSite(moderatedSiteModel).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(moderatedSiteModel).createRandomDiscussion();
        
        dataLink.usingAdmin().usingSite(privateSiteModel).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(privateSiteModel).createRandomDiscussion();
}

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 200 with valid maxItems parameter")
    public void getContainersWithMaxItems() throws Exception
    {
        restClient.authenticateUser(usersWithRoles1.getOneUserWithRole(UserRole.SiteManager))
            .withParams("maxItems=5")
            .withCoreAPI().usingSite(siteModel1).getSiteContainers()
            .assertThat().entriesListCountIs(3)
            .and().entriesListContains("folderId" ,ContainerName.documentLibrary.toString())
            .and().entriesListContains("folderId", ContainerName.links.toString())
            .and().entriesListContains("folderId", ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        restClient.withParams("maxItems=1")
            .withCoreAPI().usingSite(siteModel1).getSiteContainers()
            .assertThat().entriesListCountIs(1);
       
        restClient.withParams("maxItems=3")
        .withCoreAPI().usingSite(siteModel).getSiteContainers()
            .assertThat().entriesListCountIs(1)
            .and().entriesListContains("folderId", ContainerName.documentLibrary.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
       
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 400 when invalid maxItems parameter is used")
    public void getContainersWithMaxItemsZero () throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withParams("maxItems=0")
            .withCoreAPI().usingSite(siteModel).getSiteContainers();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary("Only positive values supported for maxItems");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 400 when invalid maxItems parameter is used")
    public void getContainersWithMaxItemsCharacter () throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withParams("maxItems=test")
                .withCoreAPI().usingSite(siteModel).getSiteContainers();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "test"));
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 400 when invalid maxItems parameter is used")
    public void getContainersWithMaxItemsMultipleZero () throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withParams("maxItems=000007")
                .withCoreAPI().usingSite(siteModel1).getSiteContainers()
                .assertThat().entriesListCountIs(3);
        restClient.assertStatusCodeIs(HttpStatus.OK);    
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 200 when valid skipCount parameter is used")
    public void getSitesWithValidSkipCount() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withParams("skipCount=1")
                .withCoreAPI().usingSite(siteModel).getSiteContainers()
                .assertThat().entriesListCountIs(0);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withParams("skipCount=1")
            .withCoreAPI().usingSite(siteModel1).getSiteContainers()
            .assertThat().entriesListCountIs(2);
            restClient.assertStatusCodeIs(HttpStatus.OK);
            
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withParams("skipCount=2")
            .withCoreAPI().usingSite(siteModel1).getSiteContainers()
            .assertThat().entriesListCountIs(1);
            restClient.assertStatusCodeIs(HttpStatus.OK);
            
            
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withParams("skipCount=0")
            .withCoreAPI().usingSite(siteModel1).getSiteContainers()
            .assertThat().entriesListCountIs(3);
            restClient.assertStatusCodeIs(HttpStatus.OK);
        }
    
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 400 when invalid skipCount parameter is used")
    public void getSitesWithSkipCountCharacter() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withParams("skipCount=abc")
                .withCoreAPI().usingSite(siteModel).getSiteContainers();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"));
    }
    
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 400 when invalid skipCount parameter is used")
    public void getSitesWithSkipCountZero() throws Exception
    {
   }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 400 when invalid skipCount parameter is used")
    public void getSitesWithSkipCountMultipleZero() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withParams("skipCount=00002")
                .withCoreAPI().usingSite(siteModel1).getSiteContainers()
                .assertThat().entriesListCountIs(1);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 400 when invalid skipCount parameter is used")            
    public void getSiteContainerWithNonExistentSite() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                .withCoreAPI().usingSite("NonExistentSiteId").getSiteContainers();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "NonExistentSiteId"));
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get site container request resturns status 200 for private site")            
    public void getSiteContainerForPrivateSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel)
                .withCoreAPI().usingSite(privateSiteModel).getSiteContainers()
                .assertThat().entriesListCountIs(3);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get site container request resturns status 200 for moderated site")            
    public void getSiteContainerForModeratedSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel)
                .withCoreAPI().usingSite(moderatedSiteModel).getSiteContainers()
                .assertThat().entriesListCountIs(3);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get site container request resturns status 200 for several container")            
    public void getSiteContainerForSeveralItems() throws Exception
    {
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingSite(siteModel1).getSiteContainers()
            .assertThat().entriesListCountIs(3)
            .and().entriesListContains("folderId" ,ContainerName.documentLibrary.toString())
            .and().entriesListContains("folderId", ContainerName.links.toString())
            .and().entriesListContains("folderId", ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }  
    
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get site container request resturns status 200 for one container")            
    public void getSiteContainerWithOneItem() throws Exception
    {
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingSite(siteModel).getSiteContainers()
            .assertThat().entriesListCountIs(1)
            .and().entriesListContains("folderId" ,ContainerName.documentLibrary.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }  
}
