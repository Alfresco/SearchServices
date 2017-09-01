package org.alfresco.rest.nodes;

import static org.alfresco.utility.report.log.Step.STEP;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.File;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.model.body.RestNodeLockBodyModel;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
/**
 * 
 * @author mpopa
 *
 */
public class NodesContentTests extends RestTest
{
    private UserModel user1, user2;
    private SiteModel site1, site2;
    private FileModel file1;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {  
        user1 = dataUser.createRandomTestUser();
        user2 = dataUser.createRandomTestUser();
        site1 = dataSite.usingUser(user1).createPublicRandomSite();
        site2 = dataSite.usingUser(user2).createPublicRandomSite();
        file1 = dataContent.usingUser(user1).usingSite(site1).createContent(DocumentType.TEXT_PLAIN);
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NODES }, executionType = ExecutionType.SANITY,
            description = "Verify file name in Content-Disposition header")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.SANITY})    
    public void checkFileNameWithRegularCharsInHeader() throws Exception
    {
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("attachment=false").getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.assertHeaderValueContains("Content-Disposition", String.format("filename=\"%s\"", file1.getName()));
    }
    
    @Bug(id="MNT-17545", description = "HTTP Header Injection in ContentStreamer", status = Bug.Status.FIXED)
    @TestRail(section = { TestGroup.REST_API,TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify file name with special chars is escaped in Content-Disposition header")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE})    
    public void checkFileNameWithSpecialCharsInHeader() throws Exception
    {
        char c1 = 127;
        char c2 = 31;
        char c3 = 256;
        FileModel file = dataContent.usingUser(user2).usingSite(site2).createContent(new FileModel("\ntest" + c1 + c2 + c3, FileType.TEXT_PLAIN));
        restClient.authenticateUser(user2).withCoreAPI().usingNode(file).usingParams("attachment=false").getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.assertHeaderValueContains("Content-Disposition","filename=\" test   .txt\"");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NODES }, executionType = ExecutionType.SANITY, description = "Verify that alfresco returns the correct encoding for files created via REST.")
    public void verifyFileEncodingUsingRestAPI() throws Exception
    {
        STEP("1. Create a folder, two text file templates and define the expected encoding.");
        FileModel utf8File = new FileModel("utf8File",FileType.TEXT_PLAIN);
        FileModel iso8859File = new FileModel("iso8859File",FileType.TEXT_PLAIN);
        FolderModel folder = dataContent.usingUser(user1).usingSite(site1).createFolder(FolderModel.getRandomFolderModel());
        String utf8Type = "text/plain;charset=UTF-8";
        String iso8859Type = "text/plain;charset=ISO-8859-1";

        STEP("2. Using multipart data upload (POST nodes/{nodeId}/children) the UTF-8 encoded file.");
        restClient.authenticateUser(user1).configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile("UTF-8File.txt"));
        RestNodeModel fileNode = restClient.withCoreAPI().usingNode(folder).createNode();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        utf8File.setNodeRef(fileNode.getId());

        STEP("3. Using multipart data upload (POST nodes/{nodeId}/children) the ISO-8859-1 file.");
        restClient.configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile("iso8859File.txt"));
        fileNode = restClient.withCoreAPI().usingNode(folder).createNode();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        iso8859File.setNodeRef(fileNode.getId());

        STEP("4. Retrieve the nodes and verify that the content type is the expected one (GET nodes/{nodeId}).");
        restClient.withCoreAPI().usingNode(utf8File).getNodeContent().assertThat().contentType(utf8Type);
        restClient.withCoreAPI().usingNode(iso8859File).getNodeContent().assertThat().contentType(iso8859Type);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NODES }, executionType = ExecutionType.SANITY, description = "Verify updating a node content.")
    public void testUpdateNodeContent() throws Exception
    {
        STEP("1. Retrieve the node in order to get data to compare after update GET /nodes/{nodeId}?include=path.");
        RestNodeModel initialNode = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=path").getNode();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("2. Update the node content (different from the initial one) PUT /nodes/{nodeId}/content?majorVersion=true&name=newfile.txt.");
        File avatarFile = Utility.getResourceTestDataFile("my-file.tif");
        RestNodeModel updatedBodyNode = restClient.withCoreAPI().usingNode(file1).usingParams("majorVersion=true&name=newfile.txt").updateNodeContent(avatarFile);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("3. Compare contentSize, modifiedAt, name and version, they should be different.");
        assertNotSame(initialNode.getContent().getSizeInBytes(), updatedBodyNode.getContent().getSizeInBytes());
        assertNotSame(initialNode.getModifiedAt(), updatedBodyNode.getModifiedAt());
        assertNotSame(initialNode.getName(), updatedBodyNode.getName());

        String initialNodeVersion = new JSONObject(initialNode.toJson()).getJSONObject("properties").getString("cm:versionLabel");
        String updatedBodyNodeVersion =  new JSONObject(updatedBodyNode.toJson()).getJSONObject("properties").getString("cm:versionLabel");
        assertTrue(updatedBodyNodeVersion.charAt(0) > initialNodeVersion.charAt(0));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NODES }, executionType = ExecutionType.SANITY, description = "Test copy a node.")
    public void testCopyNode() throws Exception
    {
        STEP("1. Create a lock and lock the node POST /nodes/{nodeId}/lock?include=path,isLocked.");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("FULL");
        RestNodeModel initialNode = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=path,isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("2. With another user(that has access to the file), copy the node to another path POST /nodes/{nodeId}/copy?include=path,isLocked.");
        String postBody = JsonBodyGenerator.keyValueJson("targetParentId", site2.getGuid());
        RestNodeModel copiedNode = restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=path,isLocked")
                .copyNode(postBody);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        STEP("3. ParentId, createdAt, path and lock are different, but the nodes have the same contentSize.");
        assertNotSame(copiedNode.getParentId(), initialNode.getParentId());
        assertNotSame(copiedNode.getCreatedAt(), initialNode.getCreatedAt());
        assertNotSame(copiedNode.getPath(), initialNode.getPath());
        assertTrue(initialNode.getIsLocked());
        assertSame(copiedNode.getContent().getSizeInBytes(), initialNode.getContent().getSizeInBytes());
        assertFalse(copiedNode.getIsLocked());

        STEP("4. Unlock the node (this node may be used in the next tests).");
        initialNode = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").unlockNode();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        assertFalse(initialNode.getIsLocked());
    }
}
