package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DeleteFavoriteFullTests extends RestTest
{
    private UserModel adminUserModel, tenantUser, adminTenantUser;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private FileModel fileModel;
    private FolderModel folderModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
               
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        folderModel = dataContent.usingSite(siteModel).usingUser(adminUserModel).createFolder();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin deletes files from favorites with Rest API and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void adminIsAbleToDeleteFavoriteFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.withCoreAPI().usingAuthUser().deleteFileFromFavorites(fileModel).assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListDoesNotContain("targetGuid", fileModel.getNodeRefWithoutVersion());
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin deletes folder from favorites with Rest API and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void adminIsAbleToDeleteFavoriteFolder() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFolderToFavorites(folderModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.withCoreAPI().usingAuthUser().deleteFolderFromFavorites(folderModel)
                  .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListDoesNotContain("targetGuid", fileModel.getNodeRefWithoutVersion());
    }  
    
    @Bug(id="MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify consumer user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void consumerIsNotAbleToDeleteFavoriteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteConsumer = usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer);
        restClient.authenticateUser(siteConsumer).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }  
        
    @Bug(id="MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify contributor user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void contributorIsNotAbleToDeleteFavoriteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteContributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        restClient.authenticateUser(siteContributor).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }  
    
    @Bug(id="MNT-16557")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify contributor user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void adminIsNotAbleToDeleteFavoriteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        restClient.authenticateUser(user).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin is not able to deletes files from favorites that were already deleted with Rest API and status code is (404)")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void adminIsNotAbleToDeletesADeletedFavoriteFile() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.withCoreAPI().usingAuthUser().deleteFileFromFavorites(fileModel).assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListDoesNotContain("targetGuid", fileModel.getNodeRefWithoutVersion());
        
        restClient.withCoreAPI().usingAuthUser().deleteFileFromFavorites(fileModel)
                  .assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND,
                   adminUserModel.getUsername(), fileModel.getNodeRef()));
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify user can't delete favorites using an invalid network ID.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsNotAbleToDeleteFavoriteSiteWithInvalidNetworkID() throws Exception
    {
        UserModel networkUserModel = dataUser.createRandomTestUser();       
        restClient.authenticateUser(networkUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);    
        networkUserModel.setDomain("invalidNetwork");
        
        restClient.withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError()
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)                
                  .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)    
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify contributor user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void adminDeletesFavoriteForNotFavouriteFile() throws JsonToModelConversionException, Exception
    {
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser()
                  .deleteFileFromFavorites(fileModel)
                  .assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND,
                   adminUserModel.getUsername(), fileModel.getNodeRef()))
                  .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)    
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is not able to delete favorites using an invalid network ID")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantIsNotAbleToDeleteFavoriteSiteWithInvalidNetworkID() throws JsonToModelConversionException, Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");        
        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(tenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        tenantUser.setDomain("invalidNetwork");       
        
        restClient.withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError()
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)    
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
       }  
      
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES,TestGroup.NETWORKS, TestGroup.FULL })
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
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.NO_CONTENT);
              
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site created by admin tenant - same network and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantUserIsAbleToDeleteFavoriteSiteAddedByAdminSameNetwork() throws JsonToModelConversionException, Exception
    {      
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);  
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");      
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(adminTenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        tenantUser.setDomain(adminTenantUser.getDomain());  
        
        restClient.withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingAuthUser().getFavorites()
        .assertThat()
        .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
    }             
  
}
