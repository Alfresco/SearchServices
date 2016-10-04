package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.requests.RestTagsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
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
public class RemoveTagSanityTests extends RestTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestTagsApi tagsAPI;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private RestTagModel tag;
    private FileModel document;

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
        tag = tagsAPI.addTag(document, "testtag");
    }

    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify Admin user deletes tags with Rest API and status code is 204")
    public void adminIsAbleToDeleteTags() throws JsonToModelConversionException, Exception
    {
        tagsAPI.deleteTag(document, tag);
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }


}
