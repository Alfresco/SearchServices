package org.alfresco.rest.sharedlinks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSharedLinksModel;
import org.alfresco.rest.model.RestSharedLinksModelCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetSharedLinksFullTests extends RestTest
{
    private SiteModel privateSite;
    private UserModel adminUser, userModel;
    protected FileModel file;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        privateSite = dataSite.usingUser(adminUser).createPrivateRandomSite();
    }

    @Bug(id="REPO-2365")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.SHAREDLINKS }, executionType = ExecutionType.REGRESSION, description = "Verify that a user with permission can get allowableOperations on sharedLinks")
    @Test(groups = { TestGroup.REST_API, TestGroup.SHAREDLINKS, TestGroup.FULL })
    public void getSharedLinksWithAllowableOperations() throws Exception
    {
        file = dataContent.usingUser(adminUser).usingSite(privateSite).createContent(DocumentType.TEXT_PLAIN);

        RestSharedLinksModel sharedLink = restClient.authenticateUser(adminUser).withCoreAPI().usingSharedLinks().createSharedLink(file);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        dataContent.usingUser(adminUser).usingResource(file).setPermissionsForUser(userModel, UserRole.SiteCollaborator, false);
        restClient.authenticateUser(userModel).withCoreAPI().usingNode(file).usingParams("include=permissions").getNode();

        restClient.authenticateUser(userModel).withCoreAPI().usingSharedLinks().usingParams("include=allowableOperations").getSharedLinks();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.authenticateUser(userModel).withCoreAPI().usingSharedLinks().getSharedLink(sharedLink);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
