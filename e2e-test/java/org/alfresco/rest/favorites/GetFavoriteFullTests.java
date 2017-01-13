package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
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
import org.springframework.social.alfresco.api.entities.Site;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetFavoriteFullTests extends RestTest {
	private UserModel adminUserModel;
	private SiteModel siteModel;
	private FolderModel folderModel;
	private FileModel fileModel;
	private ListUserWithRoles usersWithRoles;

	@BeforeClass(alwaysRun = true)
	public void dataPreparation() throws Exception {
		
		adminUserModel = dataUser.getAdminUser();
		restClient.authenticateUser(adminUserModel);
		
		siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();

		folderModel = dataContent.usingUser(adminUserModel).usingSite(siteModel).createFolder();
		fileModel = dataContent.usingUser(adminUserModel).usingResource(folderModel)
				.createContent(DocumentType.TEXT_PLAIN);

		usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator,
				UserRole.SiteConsumer, UserRole.SiteContributor);
	}

    
	@Bug(id = "MNT-16904")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify the get favorite request for invalid network id")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL, TestGroup.NETWORKS })
	public void getFavoriteSiteWithInvalidNetworkId()  throws Exception
	{
			UserModel adminTenantUser = UserModel.getAdminTenantUser();
	        restClient.authenticateUser(adminUserModel);
	        restClient.usingTenant().createTenant(adminTenantUser);
	        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
	        
	        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
	        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
	        
	        tenantUser.setDomain("invalidNetwork");
	        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
	        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
	                  .assertLastError()
	                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED); 
	}

	
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify the get favorite request with tenant user")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL, TestGroup.NETWORKS })
	public void getFavoriteSiteWithTenantUser()  throws Exception
	{
			UserModel adminTenantUser = UserModel.getAdminTenantUser();
	        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);
	        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
	        
	        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
	        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
	        
	        restClient.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
	        restClient.assertStatusCodeIs(HttpStatus.OK);
	}

	
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets favorite site with Rest API and validate details")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyRequestforGetFavoriteSite() throws JsonToModelConversionException, Exception
    {
    	restClient.authenticateUser(adminUserModel);
        restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favoriteSite = restClient.withCoreAPI().usingUser(adminUserModel).getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteSite.assertThat().field("targetGuid").equals(siteModel.getGuid());
        favoriteSite.getTarget().getSite()
        	.assertThat().field("guid").equals(siteModel.getGuid());
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets favorite site with properties filter")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyRequestforGetFavoriteSiteWithProperties() throws JsonToModelConversionException, Exception
    {
    	UserModel manager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
    	restClient.authenticateUser(manager);
        restClient.withCoreAPI().usingUser(manager).addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favoriteSite = restClient.authenticateUser(manager).withParams("properties=targetGuid")
        		.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteSite.assertThat().field("targetGuid").equals(siteModel.getGuid());
        favoriteSite.assertThat().fieldsCount().is(1);	
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets favorite site with inccorect properties filter")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyRequestforGetFavoriteSiteWithInccorectProperties() throws JsonToModelConversionException, Exception
    {
    	UserModel collaborator = usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator);
    	restClient.authenticateUser(collaborator);
        restClient.withCoreAPI().usingUser(collaborator).addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.authenticateUser(collaborator).withParams("properties=tas")
        		.withCoreAPI().usingAuthUser().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFile.assertThat().fieldsCount().is(0);	
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify all values from get favorite rest api for a file")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyRequestFieldsforGetFavoriteFile() throws JsonToModelConversionException, Exception
    {
    	UserModel contributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
    	restClient.authenticateUser(contributor);
        restClient.withCoreAPI().usingUser(contributor).addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.authenticateUser(contributor)
        		.withCoreAPI().usingAuthUser().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        favoriteFile.assertThat().field("targetGuid").is(fileModel.getNodeRefWithoutVersion())
        	.and().field("target.file.isFile").is("true")
        	.and().field("target.file.mimeType").is("text/plain")
        	.and().field("target.file.isFolder").is("false")
        	.and().field("target.file.createdBy").is(adminUserModel.getUsername())
        	.and().field("target.file.versionLabel").is("1.0") 
        	.and().field("target.file.name").is(fileModel.getName())
           	.and().field("target.file.parentId").is(folderModel.getNodeRef())
        	.and().field("target.file.guid").is(fileModel.getNodeRefWithoutVersion()) 
        	.and().field("target.file.modifiedBy").is(adminUserModel.getUsername()) 
        	.and().field("target.file.id").is(fileModel.getNodeRefWithoutVersion());
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify all values from get favorite rest api for a site")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyRequestFieldsforGetFavoriteSite() throws JsonToModelConversionException, Exception
    {
    	UserModel contributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
    	restClient.authenticateUser(contributor);
        restClient.withCoreAPI().usingUser(contributor).addFavoriteSite(siteModel);
        
        RestPersonFavoritesModel favoriteSite = restClient.authenticateUser(contributor)
        		.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        favoriteSite.assertThat().field("targetGuid").is(siteModel.getGuid())
    		.and().field("target.site.role").is(UserRole.SiteContributor.toString())
    		.and().field("target.site.visibility").is(Site.Visibility.PUBLIC.toString())
    		.and().field("target.site.guid").is(siteModel.getGuid())
    		.and().field("target.site.description").is(siteModel.getDescription())
    		.and().field("target.site.id").is(siteModel.getId())
    		.and().field("target.site.preset").is("site-dashboard")
    		.and().field("target.site.title").is(siteModel.getTitle());		
    }
    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify all values from get favorite rest api for a folder")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyRequestFieldsforGetFavoriteFolder() throws JsonToModelConversionException, Exception
    {
    	UserModel contributor = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
    	restClient.authenticateUser(contributor);
        restClient.withCoreAPI().usingUser(contributor).addFolderToFavorites(folderModel);
        
        RestPersonFavoritesModel favoriteFolder = restClient.authenticateUser(contributor)
        		.withCoreAPI().usingAuthUser().getFavorite(folderModel.getNodeRef());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        favoriteFolder.assertThat().field("targetGuid").is(folderModel.getNodeRef())
    	.and().field("target.folder.isFile").is("false")
    	.and().field("target.folder.isFolder").is("true")
    	.and().field("target.folder.createdBy").is(adminUserModel.getUsername())
    	.and().field("target.folder.name").is(folderModel.getName())
    	.and().field("target.folder.guid").is(folderModel.getNodeRef()) 
    	.and().field("target.folder.modifiedBy").is(adminUserModel.getUsername()) 
    	.and().field("target.folder.id").is(folderModel.getNodeRef());
    }
}