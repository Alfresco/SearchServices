/*
 * Copyright 2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
*/

package org.alfresco.service.search.e2e.insightEngine.sql;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Purpose of this TestClass is to test that the SQLs work after model changes
 * 
 * @author meenal bhave
 */
public class CustomModelChangesTest extends AbstractSearchServiceE2E
{
    protected SiteModel testSite;

    private UserModel testUser;

    private FolderModel testFolder;

    @BeforeClass(alwaysRun = true)
    public void setupEnvironment() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        deployCustomModel("model/flipStatus-model.xml");

        testSite = dataSite.createPublicRandomSite();

        // Create test user and add the user as a SiteContributor
        testUser = dataUser.createRandomTestUser();

        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();
        
        // Wait for the file to be indexed
        waitForIndexing(testFolder.getName(), true);
    }

    @AfterClass(alwaysRun=true)
    public void deleteModel()
    {
        deleteCustomModel("flipStatus-model.xml");
    }

    // SQL works after model is deactivated
    @Test(priority = 1, groups = { TestGroup.INSIGHT_10 })
    public void testSqlWorksAfterDeactivatingModel() throws Exception
    {
        FileModel customFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:flipFlop:song");
        properties.put(PropertyIds.NAME, customFile.getName());
        properties.put("flipFlop:genre", "Pop");
        properties.put("flipFlop:lyricist", "SomeLyricist");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
                .createFile(customFile, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Wait for the file to be indexed
        waitForIndexing(customFile.getName(), true);

        // Query custom model fields
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name, flipFlop_genre, flipFlop_lyricist from alfresco where cm_name = '" + customFile.getName() + "'");
        sqlRequest.setLimit(10);

        RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));

        // Delete File and Delete from trash
        dataContent.usingSite(testSite).usingUser(testUser).usingResource(customFile).deleteContent();
        restClient.withCoreAPI().usingTrashcan().deleteNodeFromTrashcan(customFile);

        // Wait for the file delete transaction to be indexed, until Search API returns no results
        Boolean fileFound = waitForIndexing("cm:name:'" + customFile.getName() + "'", false);
        Assert.assertTrue(fileFound, "File appears in the search results when deleted from trash");

        // Query custom model fields: No matching content
        response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Deactivate the Model
        deactivateCustomModel("flipStatus-model.xml");
        
        fileFound = waitForIndexing("TYPE:'" + "flipFlop:song" + "'", false);
        Assert.assertTrue(fileFound, "Indexes are not updated after deactivating a model");

        // Query OOTB fields: Custom Model Deactivated
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select count(*) as TotalDocuments from alfresco where type = 'cm:content'");

        response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));
    }
}
