package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
public class GetSiteContainersSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel userModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithManagerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteAPI.getSiteContainers(siteModel)
        	.assertThat().entriesListIsNotEmpty()
        	.assertThat().paginationExist()
        	.and().paginationField("count").isNot("0");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        siteAPI.getSiteContainers(siteModel)
        	.assertThat().entriesListIsNotEmpty()
        	.assertThat().paginationExist()
        	.and().paginationField("count").isNot("0");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithContributorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        siteAPI.getSiteContainers(siteModel)
        	.assertThat().entriesListIsNotEmpty()
        	.assertThat().paginationExist()
        	.and().paginationField("count").isNot("0");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithConsumerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        siteAPI.getSiteContainers(siteModel)
        	.assertThat().entriesListIsNotEmpty()
        	.assertThat().paginationExist()
        	.and().paginationField("count").isNot("0");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Admin user gets site containers information and gets status code OK (200)")
    public void getSiteContainersWithAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        siteAPI.getSiteContainers(siteModel)
        	.assertThat().entriesListIsNotEmpty()
        	.assertThat().paginationExist()
        	.and().paginationField("count").isNot("0");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site containers call returns status code 401 with Manager role")
    public void unauthenticatedUserIsNotAuthorizedToRetrieveSiteContainers() throws JsonToModelConversionException, Exception
    {
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel);
        siteAPI.getSiteContainers(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
