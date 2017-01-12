package org.alfresco.rest.favorites;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.LinkModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddFavoritesCoreTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private RestPersonFavoritesModel restPersonFavoritesModel;
    
    FileModel document;
    FolderModel folder;

    private RestCommentModel comment;
    private RestTagModel returnedModel;

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

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
              description = "Check that if target guid does not describe a site, file, or folder status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFavoriteUsingInvalidGuid() throws Exception
    {
        LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
        SiteModel site = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        site.setGuid(link.getNodeRef());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(site);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, site.getGuid().split("/")[3]));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if personId does not exist, status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFavoriteUsingInexistentUser() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(new UserModel("random_user", "random_password")).addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "random_user"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if target guid does not exist, status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFavoriteUsingInexistentGuid() throws Exception
    {
        SiteModel site = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        site.setGuid("random_guid");

        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(site);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "random_guid"));
    }

    @Bug(id = "MNT-17157")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if a favorite already exists with the specified id status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFavoriteTwice() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides file in target but guid is of a folder status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFileToFavoritesUsingFolderGuid() throws Exception
    {
        String nodeRef = document.getNodeRef();
        document.setNodeRef(folder.getNodeRef());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFileToFavorites(document);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, 
                adminUserModel.getUsername(), folder.getNodeRefWithoutVersion()));
        
        document.setNodeRef(nodeRef);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a file for a Manager, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void managerIsAbleToAddFileToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingAuthUser().addFileToFavorites(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(document.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify add favorite, perform getFavorites call, check value is updated")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void verifyGetFavoritesAfterFavoritingSite() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().getFavoriteSites().assertThat().entriesListContains("guid", siteModel.getGuid());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a folder for a Manager, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void managerIsAbleToAddFolderToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingAuthUser().addFolderToFavorites(folder);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(folder.getNodeRefWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Add to favorites a site for a Manager, check it was added")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void managerIsAbleToAddSiteToFavorites() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify add favorite specifying -me- string in place of <personid> for request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void userIsAbleToAddFavoriteWhenUsingMeAsUsername() throws Exception
    {
        restPersonFavoritesModel = restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingMe().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restPersonFavoritesModel.assertThat().field("targetGuid").is(siteModel.getGuid());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().getFavoriteSites().assertThat().entriesListContains("guid", siteModel.getGuid());
    }

    @Bug(id = "MNT-17158")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify add file favorite with comment id returns status code 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFileFavoriteUsingCommentId() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        comment = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment("This is a comment");
        file.setNodeRef(comment.getId());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFileToFavorites(file);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @Bug(id="MNT-16917")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify add file favorite with tag id returns status code 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFileFavoriteUsingTagId() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        returnedModel = restClient.withCoreAPI().usingResource(document).addTag("random_tag");
        file.setNodeRef(returnedModel.getId());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFileToFavorites(file);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND,
                adminUserModel.getUsername(), returnedModel.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides site in target but id is of a file status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addSiteToFavoritesUsingFileId() throws Exception
    {
        String guid = siteModel.getGuid();
        siteModel.setGuid(document.getNodeRefWithoutVersion());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, document.getNodeRefWithoutVersion()));
        
        siteModel.setGuid(guid);    
     }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides folder in target but guid is of a file status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFolderToFavoritesUsingFileGuid() throws Exception
    {
        String nodeRef = folder.getNodeRef();
        folder.setNodeRef(document.getNodeRef());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFolderToFavorites(folder);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, document.getNodeRef()));
        
        folder.setNodeRef(nodeRef);
    }
}
