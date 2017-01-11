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

		siteModel.setGuid(
				restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(siteModel).getSite().getGuid());

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
        restClient.withCoreAPI().usingUser(collaborator).addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favoriteSite = restClient.authenticateUser(collaborator).withParams("properties=tas")
        		.withCoreAPI().usingAuthUser().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteSite.assertThat().fieldsCount().is(0);	
    }
}