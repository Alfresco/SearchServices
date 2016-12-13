package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.StatusModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/7/2016.
 */
public class AddTagsSanityTests extends RestTest
{
    private UserModel adminUserModel, userModel;
    private FileModel document, contributorDoc;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private String tag1, tag2;
    private RestTagModelsCollection returnedCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
    }

    @BeforeMethod(alwaysRun = true)
    public void generateRandomTagsList()
    {
        tag1 = RandomData.getRandomName("tag");
        tag2 = RandomData.getRandomName("tag");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify admin user adds multiple tags with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void adminIsAbleToAddTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        returnedCollection = restClient.withCoreAPI().usingResource(document).addTags(tag1, tag2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedCollection.assertThat().entriesListContains("tag", tag1)
            .and().entriesListContains("tag", tag2);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify Manager user adds multiple tags with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void managerIsAbleToAddTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        returnedCollection = restClient.withCoreAPI().usingResource(document).addTags(tag1, tag2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedCollection.assertThat().entriesListContains("tag", tag1)
            .and().entriesListContains("tag", tag2);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user adds multiple tags with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void collaboratorIsAbleToAddTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        returnedCollection = restClient.withCoreAPI().usingResource(document).addTags(tag1, tag2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedCollection.assertThat().entriesListContains("tag", tag1)
            .and().entriesListContains("tag", tag2);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify Contributor user doesn't have permission to add multiple tags with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void contributorIsNotAbleToAddTagsToAnotherContent() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        restClient.withCoreAPI().usingResource(document).addTags(tag1, tag2);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify Contributor user adds multiple tags to his content with Rest API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void contributorIsAbleToAddTagsToHisContent() throws JsonToModelConversionException, Exception
    {
        userModel = usersWithRoles.getOneUserWithRole(UserRole.SiteContributor);
        restClient.authenticateUser(userModel);
        contributorDoc = dataContent.usingSite(siteModel).usingUser(userModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        returnedCollection = restClient.withCoreAPI().usingResource(contributorDoc).addTags(tag1, tag2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedCollection.assertThat().entriesListContains("tag", tag1)
            .and().entriesListContains("tag", tag2);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify Consumer user doesn't have permission to add multiple tags with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void consumerIsNotAbleToAddTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).withCoreAPI().usingResource(document).addTags(tag1, tag2);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.TAGS }, executionType = ExecutionType.SANITY, description = "Verify user gets status code 401 if authentication call fails")    
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void userIsNotAbleToAddTagsIfAuthenticationFails() throws JsonToModelConversionException, Exception
    {
        UserModel siteManager = usersWithRoles.getOneUserWithRole(UserRole.SiteManager);
        siteManager.setPassword("wrongPassword");
        restClient.authenticateUser(siteManager).withCoreAPI().usingResource(document).addTags(tag1, tag2);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastStatus().hasName(StatusModel.UNAUTHORIZED);
    }
}