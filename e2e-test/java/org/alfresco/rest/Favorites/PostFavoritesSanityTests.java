package org.alfresco.rest.Favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestFavoritesApi;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "favorites", "sanity" })
public class PostFavoritesSanityTests extends RestTest
{

    @Autowired
    RestFavoritesApi favoritesAPI;
    
    @Autowired
    RestSitesApi sitesApi;
    
    @Autowired
    DataUser dataUser;
    
    private UserModel adminUserModel; 
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        favoritesAPI.useRestClient(restClient);
        sitesApi.useRestClient(restClient);
        siteModel.setGuid(sitesApi.getSite(siteModel).getGuid());
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @TestRail(section={"rest-api", "favorites"}, executionType= ExecutionType.SANITY,
            description= "Verify Admin user add site to favorites with Rest API and status code is 201")
    public void adminIsAbleToAddToFavorites() throws JsonToModelConversionException, Exception
    {
        
        favoritesAPI.addUserFavorites(adminUserModel, siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section={"rest-api", "favorites"}, executionType= ExecutionType.SANITY,
            description= "Verify Manager user add site to favorites with Rest API and status code is 201")
    public void managerIsAbleToAddToFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteManager), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section={"rest-api", "favorites"}, executionType= ExecutionType.SANITY,
            description= "Verify Collaborator user add site to favorites with Rest API and status code is 201")
    public void collaboratorIsAbleToAddToFavorites() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        favoritesAPI.addUserFavorites(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator), siteModel);
        favoritesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
    
}
