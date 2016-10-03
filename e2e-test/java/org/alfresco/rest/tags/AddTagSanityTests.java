package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
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
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/3/2016.
 */
@Test(groups = { "rest-api", "tags", "sanity" })
public class AddTagSanityTests extends RestTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestTagsApi tagsAPI;

    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        tagsAPI.useRestClient(restClient);
    }
    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify admin user adds tags with Rest API and status code is 201")
    public void adminIsAbleToAddTag() throws JsonToModelConversionException, Exception
    {
        tagsAPI.addTag(document, "tag" + adminUserModel.getUsername());
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api",
            "tags" }, executionType = ExecutionType.SANITY, description = "Verify Manager user adds tags with Rest API and status code is 201")
    public void managerIsAbleToAddTag() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        tagsAPI.addTag(document, "tag" + UserRole.SiteManager);
        tagsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
}
