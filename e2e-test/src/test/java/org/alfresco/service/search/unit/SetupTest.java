/*
 * Copyright 2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.service.search.unit;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.SearchSqlJDBCRequest;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Purpose of this Test is to test that the dependencies have been wired correctly and work well
 * 
 * @author meenal bhave
 */
public class SetupTest extends AbstractSearchServiceE2E
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    protected SiteModel testSite;

    protected FolderModel targetFolder;

    private UserModel testUser;

    private FolderModel testFolder;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception
    {
        serverHealth.assertServerIsOnline();

        testSite = dataSite.createPublicRandomSite();
        targetFolder = dataContent.usingSite(testSite).createFolder(FolderModel.getRandomFolderModel());

        // Create test user and add the user as a SiteContributor
        testUser = dataUser.createRandomTestUser();

        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();
    }

    // Test CMIS API works
    @Test(priority = 1)
    public void testCMISFileCreation() throws Exception
    {
        // Create document in a folder in a collaboration site
        FileModel testFile = new FileModel("TestFile");
        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(testFile).assertThat().existsInRepo();
    }

    // Test Custom Model: Music can be used
    @Test(priority = 2)
    public void testModelMusicCanBeUsed() throws Exception
    {
        // Create document of custom type
        FileModel customFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "searchContent-music");
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:music:song");
        properties.put(PropertyIds.NAME, customFile.getName());
        properties.put("music:genre", "pop");
        properties.put("music:lyricist", "Me");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile, properties, VersioningState.MAJOR).assertThat()
                .existsInRepo();
    }

    // Test Custom Model: Finance can be used
    @Test(priority = 3)
    public void testModelFinanceCanBeUsed() throws Exception
    {
        // Create document of custom type

        FileModel customFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "searchContent-finance");
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Receipt");
        properties.put(PropertyIds.NAME, customFile.getName());
        properties.put("finance:ReceiptNo", 1);
        properties.put("finance:ReceiptValue", 30);

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile, properties, VersioningState.MAJOR).assertThat()
                .existsInRepo();

        Assert.assertTrue("New content could not be found", waitForIndexing("cm:name:" + customFile.getName(), true));
    }

    // Test sql API can be used
    @Test(priority = 4)
    public void testSQLAPICanBeUsed() throws Exception
    {
        // Select distinct site: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name from alfresco where finance_ReceiptValue > 0");
        sqlRequest.setLimit(10);

        RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertNotNull(response);
    }
    
    // Test sql can be executed via jdbc
    @Test(priority = 5)
    public void testSQLViaJDBC() throws Exception
    {
        SiteModel publicSite = new SiteModel(RandomData.getRandomName("SiteSearch"));
        publicSite.setVisibility(Visibility.PUBLIC);

        publicSite = dataSite.usingUser(testUser).createSite(publicSite);

        String sql = "select SITE,CM_OWNER from alfresco where SITE ='" + publicSite.getTitle() + "' group by SITE,CM_OWNER";
        SearchSqlJDBCRequest sqlRequest = new SearchSqlJDBCRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(testUser);

        ResultSet rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);

        while (rs.next())
        {
            // User can see the Public Site created by other user
            Assert.assertNotNull(rs.getString("SITE"));
            Assert.assertTrue(publicSite.getTitle().equalsIgnoreCase(rs.getString("SITE")));
            
            Assert.assertNotNull(rs.getString("CM_OWNER"));
            Assert.assertTrue(rs.getString("CM_OWNER").contains(testUser.getUsername()));
        }
    }
}
