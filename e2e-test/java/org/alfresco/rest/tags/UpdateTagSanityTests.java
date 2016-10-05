package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.Tag;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.requests.RestTagsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/4/2016.
 */
@Test(groups = { "rest-api", "tags", "sanity" })
public class UpdateTagSanityTests extends RestTest
{
    @Autowired RestTagsApi tagsAPI;

    @Autowired DataUser dataUser;

    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private RestTagModel oldTag;
    private DataUser.ListUserWithRoles usersWithRoles;
    private Tag newTag;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @BeforeMethod(alwaysRun=true)
    public void addTagToDocument() throws Exception
    {
        restClient.authenticateUser(adminUserModel);
        tagsAPI.useRestClient(restClient);
        oldTag = tagsAPI.addTag(document, RandomData.getRandomName("old"));
        newTag = new Tag(RandomData.getRandomName("tag"));
    }

    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify Admin user updates tags and status code is 200")
    public void adminIsAbleToUpdateTags() throws JsonToModelConversionException, Exception
    {
        tagsAPI.updateTag(oldTag, newTag).assertTagIs(newTag.getTag());
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify Manager user can't update tags with Rest API and status code is 403")
    public void managerIsNotAbleToUpdateTag() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        tagsAPI.updateTag(oldTag, newTag);
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user can't update tags with Rest API and status code is 403")
    public void collaboratorIsNotAbleToUpdateTag() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        tagsAPI.updateTag(oldTag, newTag);
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify Contributor user can't update tags with Rest API and status code is 403")
    public void contributorIsNotAbleToUpdateTag() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        tagsAPI.updateTag(oldTag, newTag);
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify Consumer user can't update tags with Rest API and status code is 403")
    public void consumerIsNotAbleToUpdateTag() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        tagsAPI.updateTag(oldTag, newTag);
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }



}
