package org.alfresco.rest.nodes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
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

}
