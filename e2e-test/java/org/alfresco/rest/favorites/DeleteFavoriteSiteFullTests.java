package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteModel;
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

public class DeleteFavoriteSiteFullTests extends RestTest
{
    UserModel userModel;
    SiteModel siteModel, siteModel1;
    UserModel adminUserModel, adminTenantUser, tenantUser;
    RestSiteModel restSiteModel;
    ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        adminUserModel = dataUser.getAdminUser();
                    
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
    description = "User is not able to remove a favorite site of admin user")
    public void userIsNotAbleToDeleteFavoritesOfAdmin() throws Exception
    {         
        UserModel contributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        dataSite.usingUser(contributor).usingSite(siteModel).addSiteToFavorites();
                      
        restClient.authenticateUser(contributor)
                  .withCoreAPI().usingUser(adminUserModel).removeFavoriteSite(siteModel);
                       
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Users removed twice from favorites same site.")
    public void managerUserRemovesDeletedFavoriteSite() throws Exception
    {
        UserModel managerUser = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();
      
        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NOT_FAVOURITE_SITE, siteModel.getTitle()));
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Users removes from favorite a site that is already deleted.")
    public void consumerUserRemovesDeletedFavoriteSite() throws Exception
    {
        siteModel1 = dataSite.usingUser(userModel).createPublicRandomSite();
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();    
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel1, UserRole.SiteConsumer);
        dataSite.usingUser(consumerUser).usingSite(siteModel1).addSiteToFavorites();
              
        dataSite.usingAdmin().deleteSite(siteModel1);
        restClient.authenticateUser(consumerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel1);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, 
                          consumerUser.getUsername(), siteModel1.getId()));
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Delete  favorite site that is NOT favorite.")
    public void adminUserRemovesDeletedFavoriteSiteThatIsNotFavorite() throws Exception
    {
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NOT_FAVOURITE_SITE, siteModel.getTitle()));
    }    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify user doesn't have permission to delete favorites of another user with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userIsNotAbleToDeleteFavoritesOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        dataSite.usingUser(siteCollaborator).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                   .withCoreAPI()
                   .usingUser(siteCollaborator)
                   .removeFavoriteSite(siteModel);
        
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is not able to delete favorites using an invalid network ID")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantIsNotAbleToDeleteFavoriteSiteWithInvalidNetworkID() throws JsonToModelConversionException, Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");        
        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();        
        siteModel.setGuid(restClient.authenticateUser(tenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);   
        tenantUser.setDomain("invalidNetwork");       
        
        restClient.withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError()
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)    
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
       }  
      
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantDeleteFavoriteSiteValidNetwork() throws JsonToModelConversionException, Exception
    {              
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);       
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");      
        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(tenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        tenantUser.setDomain(tenantUser.getDomain());  
        
        restClient.withCoreAPI()
                  .usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
              
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site created by admin tenant - same network and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantUserIsAbleToDeleteFavoriteSiteAddedByAdminSameNetwork() throws JsonToModelConversionException, Exception
    {      
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);       
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");      
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(adminTenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        tenantUser.setDomain(adminTenantUser.getDomain());  
        
        restClient.withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
    }               
          
}
