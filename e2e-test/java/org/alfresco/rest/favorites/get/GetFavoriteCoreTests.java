package org.alfresco.rest.favorites.get;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
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

public class GetFavoriteCoreTests extends RestTest {
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

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify get favorite site when person id does't exist")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
	public void getFavoriteSiteWhenPersonIdNotExist() throws JsonToModelConversionException, Exception {
		
		restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
		UserModel someUser = new UserModel("invalidUser", DataUser.PASSWORD);

		restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(someUser).getFavorite(siteModel.getGuid());
		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
				.containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, someUser.getUsername()))
				.statusCodeIs(HttpStatus.NOT_FOUND)
				.descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
				.stackTraceIs(RestErrorModel.STACKTRACE)
				.containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY);
	}
	
	@Bug(id = "ACE-2413")
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify get favorite site when person id is empty")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
	public void getFavoriteSiteWithEmptyUserId() throws JsonToModelConversionException, Exception {
		
		restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
		UserModel someUser = new UserModel("", DataUser.PASSWORD);

		restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(someUser).getFavorite(siteModel.getGuid());
		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
				.containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, someUser.getUsername()))
				.statusCodeIs(HttpStatus.BAD_REQUEST);
	}
	
	
	
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify get favorite site when favorite id does't exist")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
	public void getFavoriteSiteWhenFavoriteIdNotExist() throws JsonToModelConversionException, Exception {
		
		restClient.withCoreAPI().usingUser(adminUserModel).addSiteToFavorites(siteModel);
		
		restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(adminUserModel).getFavorite("invalidId");
		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
				.containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidId"))
				.statusCodeIs(HttpStatus.NOT_FOUND)
				.descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
				.stackTraceIs(RestErrorModel.STACKTRACE)
				.containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY);
	}
	
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify get favorite user doesn't have any favorite site, file or folder")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
	public void getFavoriteSiteForUserWithoutAnyFavorites() throws Exception
	{
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        restClient.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
        	.containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, usersWithRoles.getOneUserWithRole(UserRole.SiteManager).getUsername(), siteModel.getGuid()))
			.statusCodeIs(HttpStatus.NOT_FOUND)
			.descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
			.stackTraceIs(RestErrorModel.STACKTRACE)
			.containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY);
	}
	
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify get favorite site for -me-")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
	public void getFavoriteSiteUsingMe()  throws Exception
	{
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        
        RestPersonFavoritesModel favoriteSite = restClient.withCoreAPI().usingMe().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteSite.assertThat().field("targetGuid").equals(siteModel.getGuid());   
	}
	
	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify get favorite site for -me-")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
	public void getFavoriteFileUsingMe()  throws Exception
	{
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).withCoreAPI().usingAuthUser().addFileToFavorites(fileModel);
        
        RestPersonFavoritesModel favoriteFile = restClient.withCoreAPI().usingMe().getFavorite(fileModel.getNodeRefWithoutVersion());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        favoriteFile.assertThat().field("targetGuid").equals(fileModel.getNodeRef());   
	}
}