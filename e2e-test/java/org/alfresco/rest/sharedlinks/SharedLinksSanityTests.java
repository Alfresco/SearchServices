package org.alfresco.rest.sharedlinks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSharedLinksModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Class includes Sanity tests for the shared-links api. Detailed tests would be covered in the alfresco-remote-api test project
 * 
 * @author meenal bhave
 */
public class SharedLinksSanityTests extends RestTest
{
    private UserModel adminUser;
    private UserModel testUser1;

    private SiteModel siteModel1;

    private FolderModel folder1;

    private FileModel file1;
    private FileModel file2;
    private FileModel file3;
    private FileModel file4;
    private FileModel file5;

    private RestSharedLinksModel sharedLink1;
    private RestSharedLinksModel sharedLink2;
    private RestSharedLinksModel sharedLink3;
    private RestSharedLinksModel sharedLink4;
    private RestSharedLinksModel sharedLink5;

    private String expiryDate = "2027-03-23T23:00:00.000+0000";

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {

        adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);

        // Create Standard User
        testUser1 = dataUser.usingUser(adminUser).createRandomTestUser();

        // Create Site
        siteModel1 = dataSite.usingUser(testUser1).createPublicRandomSite();

        folder1 = dataContent.usingUser(adminUser).usingSite(siteModel1).createFolder();

        file1 = dataContent.usingUser(adminUser).usingResource(folder1).createContent(DocumentType.TEXT_PLAIN);
        file2 = dataContent.usingUser(adminUser).usingResource(folder1).createContent(DocumentType.TEXT_PLAIN);
        file3 = dataContent.usingUser(adminUser).usingResource(folder1).createContent(DocumentType.TEXT_PLAIN);
        file4 = dataContent.usingUser(adminUser).usingResource(folder1).createContent(DocumentType.TEXT_PLAIN);
        file5 = dataContent.usingUser(adminUser).usingResource(folder1).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SHAREDLINKS }, executionType = ExecutionType.SANITY, description = "Verify create sharedLinks with and without Path")
    @Test(groups = { TestGroup.REST_API, TestGroup.SANITY })
    public void testCreateAndGetSharedLinks() throws Exception
    {
        // Post without includePath
        sharedLink1 = restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().createSharedLink(file1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.onResponse().assertThat().body("entry.nodeId", org.hamcrest.Matchers.equalTo(file1.getNodeRefWithoutVersion()));
        restClient.onResponse().assertThat().body("entry.name", org.hamcrest.Matchers.equalTo(file1.getName()));
        restClient.onResponse().assertThat().body("entry.id", org.hamcrest.Matchers.equalTo(sharedLink1.getId()));
        restClient.onResponse().assertThat().body("entry.path", org.hamcrest.Matchers.nullValue());

        // Same Checks above using sharedLink methods: GET sharedLink: without includePath
        Assert.assertEquals(sharedLink1.getNodeId(), file1.getNodeRefWithoutVersion());
        Assert.assertEquals(sharedLink1.getName(), file1.getName());
        Assert.assertNull(sharedLink1.getPath(), "Path is expected to be null for noauth api: Response shows: " + sharedLink1.toJson());

        // Get without includePath
        restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().getSharedLink(sharedLink1);
        restClient.onResponse().assertThat().body("entry.nodeId", org.hamcrest.Matchers.equalTo(file1.getNodeRefWithoutVersion()));
        restClient.onResponse().assertThat().body("entry.name", org.hamcrest.Matchers.equalTo(file1.getName()));
        restClient.onResponse().assertThat().body("entry.id", org.hamcrest.Matchers.equalTo(sharedLink1.getId()));
        restClient.onResponse().assertThat().body("entry.path", org.hamcrest.Matchers.nullValue());

        // Post with includePath
        sharedLink2 = restClient.authenticateUser(testUser1).withCoreAPI().includePath().usingSharedLinks().createSharedLink(file2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.onResponse().assertThat().body("entry.nodeId", org.hamcrest.Matchers.equalTo(file2.getNodeRefWithoutVersion()));
        restClient.onResponse().assertThat().body("entry.name", org.hamcrest.Matchers.equalTo(file2.getName()));
        restClient.onResponse().assertThat().body("entry.id", org.hamcrest.Matchers.equalTo(sharedLink2.getId()));
        restClient.onResponse().assertThat().body("entry.path", org.hamcrest.Matchers.notNullValue());

        // Same Checks above using sharedLink methods: POST sharedLink: includePath
        Assert.assertEquals(sharedLink2.getNodeId(), file2.getNodeRefWithoutVersion());
        Assert.assertEquals(sharedLink2.getName(), file2.getName());
        Assert.assertNotNull(sharedLink2.getPath(), "Path not expected to be null for noauth api: Response shows: " + sharedLink1.toJson());

        // Get with includePath
        restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().getSharedLink(sharedLink2);
        restClient.onResponse().assertThat().body("entry.nodeId", org.hamcrest.Matchers.equalTo(file2.getNodeRefWithoutVersion()));
        restClient.onResponse().assertThat().body("entry.name", org.hamcrest.Matchers.equalTo(file2.getName()));
        restClient.onResponse().assertThat().body("entry.id", org.hamcrest.Matchers.equalTo(sharedLink2.getId()));
        // Verify that path is null since includePath is not supported for this noAuth api
        restClient.onResponse().assertThat().body("entry.path", org.hamcrest.Matchers.nullValue());

        // Get: noAuth with includePath
        sharedLink2 = restClient.withCoreAPI().usingSharedLinks().getSharedLink(sharedLink2);
        restClient.onResponse().assertThat().body("entry.nodeId", org.hamcrest.Matchers.equalTo(file2.getNodeRefWithoutVersion()));
        restClient.onResponse().assertThat().body("entry.name", org.hamcrest.Matchers.equalTo(file2.getName()));
        restClient.onResponse().assertThat().body("entry.id", org.hamcrest.Matchers.equalTo(sharedLink2.getId()));
        // Verify that path is null since includePath is not supported for this noAuth api
        restClient.onResponse().assertThat().body("entry.path", org.hamcrest.Matchers.nullValue());

        // Same Checks above using sharedLink methods: GET sharedLink: noAuth
        Assert.assertEquals(sharedLink2.getNodeId(), file2.getNodeRefWithoutVersion());
        Assert.assertEquals(sharedLink2.getName(), file2.getName());
        Assert.assertNull(sharedLink2.getPath(), "Path is expected to be null for noauth api: Response shows: " + sharedLink2.toJson());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SHAREDLINKS }, executionType = ExecutionType.SANITY, description = "Verify delete sharedLinks with and without Path")
    @Test(groups = { TestGroup.REST_API, TestGroup.SANITY })
    public void testDeleteSharedLinks() throws Exception
    {
        sharedLink3 = restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().createSharedLink(file3);
        restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().deleteSharedLink(sharedLink3);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        sharedLink4 = restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().createSharedLink(file4);
        restClient.authenticateUser(testUser1).withCoreAPI().includePath().usingSharedLinks().deleteSharedLink(sharedLink4);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SHAREDLINKS }, executionType = ExecutionType.REGRESSION, description = "Verify get sharedLink/content and get/renditions")
    @Test(groups = { TestGroup.REST_API, TestGroup.REGRESSION })
    public void testCreateWithExpiryDateAndGetSharedLinkRendition() throws Exception
    {
        sharedLink5 = restClient.authenticateUser(testUser1).withCoreAPI().includePath().usingSharedLinks().createSharedLinkWithExpiryDate(file5, expiryDate);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        Assert.assertEquals(sharedLink5.getExpiresAt(), expiryDate);
        Assert.assertNotNull(sharedLink5.getPath(), "Path not expected to be null: Response shows: " + sharedLink5.toJson());

        restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().getSharedLinkRenditions(sharedLink5);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.authenticateUser(testUser1).withCoreAPI().usingSharedLinks().getSharedLinkRendition(sharedLink5, "doclib");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}