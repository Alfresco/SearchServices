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
import org.testng.annotations.Test;
/**
 * 
 * @author mpopa
 *
 */
public class NodesContentTests extends RestTest
{
    @TestRail(section = { TestGroup.REST_API,TestGroup.NODES }, executionType = ExecutionType.SANITY,
            description = "Verify file name in Content-Disposition header")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.SANITY})    
    public void checkFileNameWithRegularCharsInHeader() throws Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        SiteModel site = dataSite.usingUser(user).createPublicRandomSite();
        FileModel file = dataContent.usingUser(user).usingSite(site).createContent(DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(user).withCoreAPI().usingNode(file).usingParams("attachment=false").getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.assertHeaderValueContains("Content-Disposition", String.format("filename=\"%s\"", file.getName()));
    }
    
    @Bug(id="REPO-2112")
    @TestRail(section = { TestGroup.REST_API,TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify file name with special chars is escaped in Content-Disposition header")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE})    
    public void checkFileNameWithSpecialCharsInHeader() throws Exception
    {
        char c1 = 127;
        char c2 = 31;
        char c3 = 256;
        UserModel user = dataUser.createRandomTestUser();
        SiteModel site = dataSite.usingUser(user).createPublicRandomSite();
        FileModel file = dataContent.usingUser(user).usingSite(site).createContent(new FileModel("\ntest\""+c1+c2+c3,FileType.TEXT_PLAIN));
        restClient.authenticateUser(user).withCoreAPI().usingNode(file).usingParams("attachment=false").getNodeContent();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.assertHeaderValueContains("Content-Disposition","filename=\" test    .txt\"");
    }

}
