package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
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

public class GetRatingSanityTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUser;    
    private ListUserWithRoles usersWithRoles;
    private RestRatingModel restRatingModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role is able to retrieve rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void managerIsAbleToRetrieveRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");

        restRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role is able to retrieve rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void collaboratorIsAbleToRetrieveRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");

        restRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role is able to retrieve rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void contributorIsAbleToRetrieveRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");

        restRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role is able to retrieve rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void consumerIsAbleToRetrieveRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));

        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");

        restRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify admin user is able to retrieve rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    public void adminIsAbleToRetrieveRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        document = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).usingResource(folderModel)
                .createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser);
        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restRatingModel = restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");

        restRatingModel = restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to retrieve rating of a document")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
    @Bug(id = "MNT-16904", description = "It fails only on environment with tenants")
    public void unauthenticatedUserIsNotAbleToRetrieveRating() throws Exception
    {
        FolderModel folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        FileModel document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUser);
        restClient.withCoreAPI().usingResource(document).likeDocument();
        restClient.withCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.authenticateUser(new UserModel("random user", "random password"));

        restClient.withCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);

        restClient.withCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
}