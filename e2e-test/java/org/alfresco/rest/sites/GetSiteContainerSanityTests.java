package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSiteContainerModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.StatusModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
public class GetSiteContainerSanityTests extends RestTest
{    
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private RestSiteContainerModel siteContainerModel;
    private UserModel userModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site container and gets status code OK (200)")
    public void getSiteContainerWithManagerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteContainerModel = restClient.withCoreAPI().usingSite(siteModel).getSiteContainers().getOneRandomEntry();
        restClient.withCoreAPI().usingSite(siteModel).getSiteContainer(siteContainerModel.onModel())
                .assertThat().field("id").is(siteContainerModel.onModel().getId())
                .and().field("folderId").is(siteContainerModel.onModel().getFolderId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site container and gets status code OK (200)")
    public void getSiteContainerWithCollaboratorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        siteContainerModel = restClient.withCoreAPI().usingSite(siteModel).getSiteContainers().getOneRandomEntry();
        restClient.withCoreAPI().usingSite(siteModel).getSiteContainer(siteContainerModel.onModel())
               .and().field("id").is(siteContainerModel.onModel().getId())
               .and().field("folderId").is(siteContainerModel.onModel().getFolderId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site container and gets status code OK (200)")
    public void getSiteContainerWithContributorRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        siteContainerModel = restClient.withCoreAPI().usingSite(siteModel).getSiteContainers().getOneRandomEntry();
        restClient.withCoreAPI().usingSite(siteModel).getSiteContainer(siteContainerModel.onModel())
               .assertThat().field("id").is(siteContainerModel.onModel().getId())
               .and().field("folderId").is(siteContainerModel.onModel().getFolderId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site container and gets status code OK (200)")
    public void getSiteContainerWithConsumerRole() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        siteContainerModel = restClient.withCoreAPI().usingSite(siteModel).getSiteContainers().getOneRandomEntry();
        restClient.withCoreAPI().usingSite(siteModel).getSiteContainer(siteContainerModel.onModel())
               .assertThat().field("id").is(siteContainerModel.onModel().getId())
               .and().field("folderId").is(siteContainerModel.onModel().getFolderId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with admin user gets site container and gets status code OK (200)")
    public void getSiteContainerWithAdminUser() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        siteContainerModel = restClient.withCoreAPI().usingSite(siteModel).getSiteContainers().getOneRandomEntry();
        restClient.withCoreAPI().usingSite(siteModel).getSiteContainer(siteContainerModel.onModel())
               .assertThat().field("id").is(siteContainerModel.onModel().getId())
               .and().field("folderId").is(siteContainerModel.onModel().getFolderId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site container call returns status code 401")
    @Bug(id="MNT-16904")
    public void unauthenticatedUserIsNotAuthorizedToRetrieveSiteContainer() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteContainerModel = restClient.withCoreAPI().usingSite(siteModel).getSiteContainers().getOneRandomEntry();
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel)
                  .withCoreAPI().usingSite(siteModel).getSiteContainer(siteContainerModel.onModel());
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastException().hasName(StatusModel.UNAUTHORIZED);
    }
}
