package org.alfresco.rest.nodes;

import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNodeModel;
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

}
