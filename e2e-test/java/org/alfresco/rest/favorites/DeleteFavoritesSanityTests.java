package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
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

public class DeleteFavoritesSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).getSite().getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Admin user deletes site from favorites with Rest API and status code is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
        
        restClient.withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Manager user deletes site from favorites with Rest API and status code is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void managerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        restClient.authenticateUser(siteManager).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Collaborator user deletes site from favorites with Rest API and status code is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void collaboratorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Contributor user deletes site from favorites with Rest API and status code is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void contributorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteContributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        restClient.authenticateUser(siteContributor).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.NO_CONTENT); 
        restClient.withCoreAPI().usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify Consumer user delets site from favorites with Rest API and status code is 204")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void consumerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteConsumer = usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer);
        restClient.authenticateUser(siteConsumer).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @Bug(id="MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void userIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Bug(id="MNT-16557")
    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify user doesn't have permission to delete favorites of admin user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void userIsNotAbleToDeleteFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Bug(id="MNT-16557")
    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify admin user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void adminIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY,
            description = "Verify user gets status code 401 if authentication call fails")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
    public void userIsNotAbleToDeleteFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager).withCoreAPI().usingAuthUser()
                  .deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
