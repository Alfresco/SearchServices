package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddFavoritesSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private RestPersonFavoritesModel restPersonFavoritesModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
              description = "Verify Admin user add site to favorites with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsAbleToAddToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify Manager user add site to favorites with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void managerIsAbleToAddToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).withCoreAPI()
                .usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
              description = "Verify Collaborator user add site to favorites with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void collaboratorIsAbleToAddToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.and().field("targetGuid").is(siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
              description = "Verify Contributor user add site to favorites with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void contributorIsAbleToAddToFavorites() throws JsonToModelConversionException, Exception
    {
        restPersonFavoritesModel= restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.and().field("targetGuid").is(siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
              description = "Verify Consumer user add site to favorites with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void consumerIsAbleToAddToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.and().field("targetGuid").is(siteModel.getGuid());
    }

    @Bug(id="MNT-16904", description = "It fails only on environment with tenants")
    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
              description = "Verify Manager user gets status code 401 if authentication call fails")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void managerIsNotAbleToAddToFavoritesIfAuthenticationFails() throws Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
}
