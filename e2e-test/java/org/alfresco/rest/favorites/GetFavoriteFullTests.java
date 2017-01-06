package org.alfresco.rest.favorites;

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
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetFavoriteFullTests extends RestTest {
	private UserModel adminUserModel, networkUserModel;
	private SiteModel siteModel;
	private FolderModel folderModel;
	private FileModel fileModel;
	private ListUserWithRoles usersWithRoles;

	@BeforeClass(alwaysRun = true)
	public void dataPreparation() throws Exception {
		
        networkUserModel = dataUser.createRandomTestUser();
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

	@TestRail(section = { TestGroup.REST_API,
			TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify get favorite site when using invalid network id")
	@Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
	public void getFavoriteSiteWhenNetworkIdIsInvalid() throws JsonToModelConversionException, Exception 
	{
        networkUserModel.setDomain("invalidNetwork");
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
	
}