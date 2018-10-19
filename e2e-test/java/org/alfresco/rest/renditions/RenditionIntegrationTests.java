package org.alfresco.rest.renditions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.model.RestRenditionInfoModel;
import org.alfresco.rest.model.RestRenditionInfoModelCollection;
import org.alfresco.utility.Utility;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * Sanity check for renditions through REST API.<br>
 * Tests upload files and then request renditions for those files. <br>
 * Renditions are requested based on supported renditions according to GET '/nodes/{nodeId}/renditions'. <br>
 */
@Test(groups = {TestGroup.REQUIRE_TRANSFORMATION, TestGroup.RENDITIONS_REGRESSION})
public class RenditionIntegrationTests extends RestTest
{

    private UserModel user;
    private SiteModel site;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        user = dataUser.createRandomTestUser();
        site = dataSite.usingUser(user).createPublicRandomSite();
    }

    /**
     *  Check that a particular rendition can be created for the file and that it has the expected Mime type
     */
    @Test(dataProvider = "RenditionTestDataProvider", groups = {TestGroup.REST_API, TestGroup.RENDITIONS, TestGroup.SANITY})
    @TestRail(section = { TestGroup.REST_API, TestGroup.RENDITIONS }, executionType = ExecutionType.SANITY,
            description = "Verify renditions created for a selection of test files via POST nodes/{nodeId}/renditions")
    public void supportedRenditionTest(String fileName, String renditionId, String expectedMimeType, String nodeId) throws Exception
    {
        FileModel file = new FileModel(fileName);
        file.setNodeRef(nodeId);

        // 1. Create a rendition of the file using RESTAPI
        restClient.withCoreAPI().usingNode(file).createNodeRendition(renditionId);
        Assert.assertEquals(restClient.getStatusCode(), HttpStatus.ACCEPTED.toString(),
                "Failed to submit a request for rendition. [" + fileName+ ", " + renditionId+"] [source file, rendition ID]. ");

        // 2. Verify that a rendition of the file is created and has content using RESTAPI
        RestResponse restResponse = restClient.withCoreAPI().usingNode(file).getNodeRenditionContentUntilIsCreated(renditionId);
        Assert.assertEquals(restClient.getStatusCode(), HttpStatus.OK.toString(),
                "Failed to produce rendition. [" + fileName+ ", " + renditionId+"] [source file, rendition ID] ");

        // 3. Check the returned content type
        Assert.assertEquals(restClient.getResponseHeaders().getValue("Content-Type"), expectedMimeType+";charset=UTF-8",
                "Rendition was created but it has the wrong Content-Type. [" + fileName+ ", " + renditionId + "] [source file, rendition ID]");


        Assert.assertTrue((restResponse.getResponse().body().asInputStream().available() > 0),
                "Rendition was created but its content is empty. [" + fileName+ ", " + renditionId+"] [source file, rendition ID] ");
    }

    @DataProvider(name = "RenditionTestDataProvider")
    protected Iterator<Object[]> renditionTestDataProvider() throws Exception
    {
        List<String> toTest = Arrays.asList("doc", "xls", "ppt", "docx", "xlsx", "pptx", "msg", "png", "gif", "jpg", "pdf", "txt"); // "tiff"

        List<Object[]> renditionsForFiles = new LinkedList<>();

        for (String extensions : toTest)
        {
            String sourceFile = "quick/quick." + extensions;
            renditionsForFiles.addAll(uploadFileAndGetAvailableRenditions(sourceFile));
        }

        return renditionsForFiles.iterator();
    }

    /**
     * Upload a source file, get all supported renditions for it.
     */
    private List<Object[]> uploadFileAndGetAvailableRenditions(String sourceFile) throws Exception
    {

        // Create folder & upload file
        FolderModel folder = FolderModel.getRandomFolderModel();
        folder = dataContent.usingUser(user).usingSite(site).createFolder(folder);
        restClient.authenticateUser(user).configureRequestSpec()
                .addMultiPart("filedata", Utility.getResourceTestDataFile(sourceFile));
        RestNodeModel fileNode = restClient.authenticateUser(user).withCoreAPI().usingNode(folder).createNode();

        Assert.assertEquals(restClient.getStatusCode(), HttpStatus.CREATED.toString(),
                "Failed to created a node for rendition tests using file " + sourceFile);

        FileModel file = new FileModel(sourceFile);
        file.setNodeRef(fileNode.getId());

        List<Object[]> renditionsForFile = new LinkedList<>();

        // Get supported renditions
        RestRenditionInfoModelCollection renditionsInfo = restClient.withCoreAPI().usingNode(file).getNodeRenditionsInfo();
        for (RestRenditionInfoModel m : renditionsInfo.getEntries())
        {
            RestRenditionInfoModel renditionInfo = m.onModel();
            String renditionId = renditionInfo.getId();
            String targetMimeType = renditionInfo.getContent().getMimeType();

            renditionsForFile.add(new Object[]{sourceFile, renditionId, targetMimeType, fileNode.getId()});

        }

        return renditionsForFile;
    }

    @Test(dataProvider = "UnsupportedRenditionTestDataProvider",groups = {TestGroup.REST_API, TestGroup.RENDITIONS, TestGroup.SANITY})
    @TestRail(section = { TestGroup.REST_API, TestGroup.RENDITIONS }, executionType = ExecutionType.SANITY,
            description = "Verify that requests for unsupported renditions return 400 ")
    public void unsupportedRenditionTest(String sourceFile, String renditionId) throws Exception
    {
        // 1. Create a folder in existing site"
        FolderModel folder = FolderModel.getRandomFolderModel();
        folder = dataContent.usingUser(user).usingSite(site).createFolder(folder);

        // 2. Upload a local file using RESTAPI
        restClient.authenticateUser(user).configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile(sourceFile));
        RestNodeModel fileNode = restClient.authenticateUser(user).withCoreAPI().usingNode(folder).createNode();
        Assert.assertEquals(restClient.getStatusCode(), HttpStatus.CREATED.toString(),
                "Failed to created a node for rendition tests using file " + sourceFile);

        // 3. Request rendition of the file using RESTAPI
        FileModel file = new FileModel(sourceFile);
        file.setNodeRef(fileNode.getId());
        restClient.withCoreAPI().usingNode(file).createNodeRendition(renditionId);

        Assert.assertEquals(restClient.getStatusCode(), HttpStatus.BAD_REQUEST.toString(),
                "Expected to see the rendition rejected. [" + sourceFile + ", " + renditionId + "] [source file, rendition ID] ");
    }

    @DataProvider(name = "UnsupportedRenditionTestDataProvider")
    protected Iterator<Object[]> unsupportedRenditionTestDataProvider() throws Exception
    {
        String renditionId = "pdf";

        List<Object[]> toTest = new LinkedList<>();
        toTest.add(new Object[]{"quick/quick.png", renditionId});
        toTest.add(new Object[]{"quick/quick.gif", renditionId});
        toTest.add(new Object[]{"quick/quick.jpg", renditionId});
        toTest.add(new Object[]{"quick/quick.pdf", renditionId});

        return toTest.iterator();
    }
}
