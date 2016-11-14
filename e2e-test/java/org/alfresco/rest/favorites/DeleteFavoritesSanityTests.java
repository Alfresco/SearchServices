package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
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
        siteModel.setGuid(restClient.authenticateUser(adminUserModel).usingSite(siteModel).getSite().getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Admin user deletes site from favorites with Rest API and status code is 204")
    public void adminIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.usingUser(adminUserModel)
                  .addSiteToFavorites(siteModel).assertThat().field("targetGuid").is(siteModel.getGuid());
        
        restClient.usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Manager user deletes site from favorites with Rest API and status code is 204")
    public void managerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        restClient.authenticateUser(siteManager)
                  .usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        
        restClient.usingAuthUser()
                  .deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user deletes site from favorites with Rest API and status code is 204")
    public void collaboratorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator)
                  .usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        restClient.usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Contributor user deletes site from favorites with Rest API and status code is 204")
    public void contributorIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteContributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        restClient.authenticateUser(siteContributor)  
                  .usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        restClient.usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify Consumer user delets site from favorites with Rest API and status code is 204")
    public void consumerIsAbleToDeleteFavorites() throws JsonToModelConversionException, Exception
    {
        UserModel siteConsumer = usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer);
        restClient.authenticateUser(siteConsumer)
                  .usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        restClient.usingAuthUser().deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.usingAuthUser().getFavorites().assertThat().entriesListDoesNotContain("targetGuid", siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API,
        TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void userIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator)
                  .usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .usingAuthUser()
                  .deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user doesn't have permission to delete favorites of admin user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void userIsNotAbleToDeleteFavoritesOfAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
                  .usingAuthUser()
                  .addSiteToFavorites(siteModel)
                  .and().field("targetGuid").is(siteModel.getGuid());
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .usingAuthUser()                  
                  .deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify admin user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Bug(id="MNT-16557")
    public void adminIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        
        restClient.authenticateUser(adminUserModel)
                  .usingAuthUser()
                  .deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")
    @Bug(id = "MNT-16904")
    public void userIsNotAbleToDeleteFavoritesIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager)
                  .usingAuthUser()
                  .deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
