package org.alfresco.rest.favorites.delete;

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

public class DeleteFavoriteCoreTests extends RestTest
{
    private UserModel adminUserModel;
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
            description = "Verify that status code is 404 if PersonID is incorrect - favorite file.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE})
    public void deleteFavoriteIfPersonIdNotExists() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);        
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        
        restClient.withCoreAPI().usingUser(new UserModel("inexistent", "inexistent"))
                  .deleteFileFromFavorites(fileModel).assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "inexistent"));
    }
    
    @Bug(id="ACE-2413")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that status code is 404 if PersonID is empty - favorite file.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE})
    public void deleteFavoriteIfPersonIdIsEmpty() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);        
        restClient.withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
       
        restClient.withCoreAPI().usingUser(new UserModel ("", ""))
                  .deleteFileFromFavorites(fileModel).assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that status code is 404 if FavoriteID is incorrect - favorite file.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE})
    public void deleteFavoriteFileIfFavoriteIdIsIncorrect() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
     
        fileModel.setNodeRef("wrongFavoriteId");
        restClient.withCoreAPI().usingAuthUser()
                  .deleteFileFromFavorites(fileModel).assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "wrongFavoriteId"));
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that status code is 404 if FavoriteID is incorrect - favorite file.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE})
    public void deleteFavoriteFileIfFavoriteIdNotExists() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);     
        FileModel inexistentDocument = new FileModel();
        inexistentDocument.setNodeRef("inexistent");
        
        restClient.withCoreAPI().usingAuthUser()
                  .deleteFileFromFavorites(inexistentDocument).assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "inexistent"));
    }  
        
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that status code is 404 if FavoriteID is incorrect - favorite site.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE})
    public void deleteFavoriteIfFavoriteIdNotExists() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
        siteModel.setGuid("wrongFavoriteId");      
 
        restClient.withCoreAPI().usingAuthUser()
                  .deleteSiteFromFavorites(siteModel).assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "wrongFavoriteId"));
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that status code is 404 if FavoriteID is incorrect - favorite folder.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE})
    public void deleteFavoriteFolderIfFavoriteIdNotExists() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
        restClient.withCoreAPI().usingAuthUser().addFolderToFavorites(folderModel);
     
        folderModel.setNodeRef("wrongFavoriteId");
        restClient.withCoreAPI().usingAuthUser()
                  .deleteFolderFromFavorites(folderModel).assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "wrongFavoriteId"));
   }               
   
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
              description = "Verify user removes a site from favorites using '-me-' in place of personId with Rest API and response is successful (204)")
    public void deleteFavoriteSiteWithSuccessUsingMeAsPersonId() throws Exception
    {
        siteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
                
        restClient.withCoreAPI().usingMe().deleteSiteFromFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @Bug(id="ACE-5588")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void collaboratorIsNotAbleToDeleteFavoriteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel siteCollaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(siteCollaborator).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI()
                  .usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }    

    @Bug(id="ACE-5588")
    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin user doesn't have permission to delete favorites of another user with Rest API and status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void adminIsNotAbleToDeleteFavoriteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingAuthUser().deleteSiteFromFavorites(siteModel)
                  .assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }  
}
