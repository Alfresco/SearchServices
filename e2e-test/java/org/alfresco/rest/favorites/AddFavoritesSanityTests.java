package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.constants.UserRole;
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

@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.SANITY })
public class AddFavoritesSanityTests extends RestTest
{
    @Autowired
    DataUser dataUser;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();        
        siteModel.setGuid(restClient.authenticateUser(adminUserModel).onCoreAPI().usingSite(siteModel).getSite().getGuid());

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify Admin user add site to favorites with Rest API and status code is 201")
    public void adminIsAbleToAddToFavorites() throws Exception
    {
        restClient.onCoreAPI().usingAuthUser()
                  .addSiteToFavorites(siteModel)
                  .assertThat().field("targetGuid").is(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify Manager user add site to favorites with Rest API and status code is 201")
    public void managerIsAbleToAddToFavorites() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                  .onCoreAPI().usingAuthUser()
                  .addSiteToFavorites(siteModel)
                  .assertThat().field("targetGuid").is(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify Collaborator user add site to favorites with Rest API and status code is 201")
    public void collaboratorIsAbleToAddToFavorites() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .onCoreAPI().usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify Contributor user add site to favorites with Rest API and status code is 201")
    public void contributorIsAbleToAddToFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                  .onCoreAPI().usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify Consumer user add site to favorites with Rest API and status code is 201")
    public void consumerIsAbleToAddToFavorites() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .onCoreAPI().usingAuthUser()
                  .addSiteToFavorites(siteModel).and().field("targetGuid").is(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify Manager user gets status code 401 if authentication call fails")
    @Bug(id="MNT-16904")
    public void managerIsNotAbleToAddToFavoritesIfAuthenticationFails() throws Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager)
                  .onCoreAPI().usingAuthUser()
                  .addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
