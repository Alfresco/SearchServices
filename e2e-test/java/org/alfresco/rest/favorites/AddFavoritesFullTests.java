package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestFileModel;
import org.alfresco.rest.model.RestFolderModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
import org.alfresco.rest.model.RestSiteModel;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddFavoritesFullTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private RestPersonFavoritesModel restPersonFavoritesModel;
    
    FileModel document;
    FolderModel folder;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        folder = dataContent.usingSite(siteModel).usingUser(adminUserModel).createFolder();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a file for a Collaborator, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void collaboratorIsAbleToAddFileToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingAuthUser().addFileToFavorites(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(document.getNodeRefWithoutVersion());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a file for a Contributor, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void contributorIsAbleToAddFileToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
            .withCoreAPI().usingAuthUser().addFileToFavorites(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(document.getNodeRefWithoutVersion());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a file for a Consumer, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void consumerIsAbleToAddFileToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
            .withCoreAPI().usingAuthUser().addFileToFavorites(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(document.getNodeRefWithoutVersion());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a folder for a Collaborator, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void collaboratorIsAbleToAddFolderToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).addFolderToFavorites(folder);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(folder.getNodeRefWithoutVersion());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a folder for a Contributor, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void contributorIsAbleToAddFolderToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
            .withCoreAPI().usingAuthUser().addFolderToFavorites(folder);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(folder.getNodeRefWithoutVersion());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a folder for a Consumer, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void consumerIsAbleToAddFolderToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
            .withCoreAPI().usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).addFolderToFavorites(folder);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(folder.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a site for a Collaborator, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void collaboratorIsAbleToAddSiteToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a site for a Contributor, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void contributorIsAbleToAddSiteToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
            .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a site for a Consumer, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void consumerIsAbleToAddSiteToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
            .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides file in target but guid is of a site status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void addSiteToFavoritesUsingFolderId() throws Exception
    {
        SiteModel newSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        newSiteModel.setGuid(folder.getNodeRef());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(newSiteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, folder.getNodeRef()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides folder in target but guid is of a site status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void addFolderToFavoritesUsingSiteId() throws Exception
    {
        FolderModel newFolder = dataContent.usingSite(siteModel).usingUser(adminUserModel).createFolder();
        newFolder.setNodeRef(siteModel.getGuid());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFolderToFavorites(newFolder);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, 
                adminUserModel.getUsername(), siteModel.getGuid()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the response of favorite a site call is valid")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyFavoriteASiteResponseIsValid() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
        
        RestSiteModel restSiteModel = restPersonFavoritesModel.getTarget().getSite();
        restSiteModel.assertThat().field("visibility").is(siteModel.getVisibility()).and()
        .field("guid").is(siteModel.getGuid()).and()
        .field("description").is(siteModel.getDescription()).and()
        .field("id").is(siteModel.getId()).and()
        .field("title").is(siteModel.getTitle());    
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the response of favorite a file call is valid")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyFavoriteAFileResponseIsValid() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingAuthUser().addFileToFavorites(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(document.getNodeRefWithoutVersion());
        
        RestFileModel restFileModel = restPersonFavoritesModel.getTarget().getFile();
        restFileModel.assertThat().field("mimeType").is("text/plain").and()
                                  .field("isFile").is("true").and()
                                  .field("isFolder").is("false").and()
                                  .field("createdBy").is(adminUserModel.getUsername());    
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the response of favorite a folder call is valid")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyFavoriteAFolderResponseIsValid() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingAuthUser().addFolderToFavorites(folder);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(folder.getNodeRef());
        
        RestFolderModel restFolderModel = restPersonFavoritesModel.getTarget().getFolder();
        restFolderModel.assertThat().field("isFile").is("false").and()
                                  .field("isFolder").is("true").and()
                                  .field("createdBy").is(adminUserModel.getUsername());    
    }
    
    @Bug(id = "REPO-1061")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user does not have permission to favorite a site status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyFavoriteASiteIfTheUserDoesNotHavePermission() throws Exception
    {
        SiteModel privateSite = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingAuthUser().addSiteToFavorites(privateSite);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, privateSite.getGuid()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .containsErrorKey(RestErrorModel.NOT_FOUND_ERRORKEY)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the post favorites request when network id is invalid for tenant user")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL, TestGroup.NETWORKS})
    public void addFavoriteSitesWhenNetworkIdIsInvalid() throws JsonToModelConversionException, Exception
    {   
        UserModel adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        restClient.usingTenant().createTenant(adminTenantUser);
        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        
        tenantUser.setDomain("invalidNetwork");
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
            .assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);        
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the response of favorite a sie with empty body at request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void verifyFavoriteASiteWithEmptyBody() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "", "people/{personId}/favorites", adminUserModel.getUsername());
        restClient.processModel(RestPersonFavoritesModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsSummary(String.format(RestErrorModel.NO_CONTENT, "No content to map to Object due to end of input"));;   
    }
}
