package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
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
public class GetSitesSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private UserModel userModel;
    private ListUserWithRoles usersWithRoles;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role gets sites information and gets status code OK (200)")
    public void managerIsAbleToRetrieveSites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withParams("maxItems=1000")
                  .withCoreAPI().getSites()             
                	.assertThat().entriesListIsNotEmpty()
                	.assertThat().entriesListContains("id", siteModel.getId())
                	.and().paginationExist();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets sites information and gets status code OK (200)")
    public void collaboratorIsAbleToRetrieveSites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withParams("maxItems=1000")
                  .withCoreAPI().getSites().assertThat().entriesListIsNotEmpty()
                  .assertThat().entriesListContains("id", siteModel.getId())
                  .and().paginationExist();
                  
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets sites information and gets status code OK (200)")
    public void contributorIsAbleToRetrieveSites() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).withParams("maxItems=1000")
                  .withCoreAPI().getSites()	
                	.assertThat().entriesListIsNotEmpty()
                	.assertThat().entriesListContains("id", siteModel.getId())
                	.and().paginationExist();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets sites information and gets status code OK (200)")
    public void consumerIsAbleToRetrieveSites() throws JsonToModelConversionException, Exception
    {

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withParams("maxItems=1000")
                  .withCoreAPI().getSites()
                  .assertThat().entriesListIsNotEmpty()
              	  .assertThat().entriesListContains("id", siteModel.getId())
              	  .and().paginationExist();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Admin user gets sites information and gets status code OK (200)")
    public void adminUserIsAbleToRetrieveSites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withParams("maxItems=1000")
                  .withCoreAPI().getSites()
                	.assertThat().entriesListIsNotEmpty()
                	.assertThat().entriesListContains("id", siteModel.getId())
                	.and().paginationExist();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get sites call returns status code 401")
    public void unauthenticatedUserIsNotAuthorizedToRetrieveSites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel).withParams("maxItems=1000")
                  .withCoreAPI().getSites();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
