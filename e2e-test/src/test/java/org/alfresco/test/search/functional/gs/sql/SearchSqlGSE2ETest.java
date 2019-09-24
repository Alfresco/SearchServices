/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.test.search.functional.gs.sql;

import static org.alfresco.rest.rm.community.model.user.UserRoles.ROLE_RM_USER;

import java.util.List;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.rm.community.model.record.Record;
import org.alfresco.rest.rm.community.model.recordcategory.RecordCategoryChild;
import org.alfresco.rest.rm.enterprise.core.ClassificationData;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.gs.AbstractGSE2ETest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The purpose of this test is to create basic queries using a RM site and a basic file plan.
 * 
 * @author Meenal Bhave
 */
public class SearchSqlGSE2ETest extends AbstractGSE2ETest
{
    private UserModel testUserGSTopSecret, testUserSecret, testUserConfidential, testUserNoAccess;
    
    private FolderModel testFolder;
    private RecordCategoryChild recordFolder;

    private FileModel fileRecord, fileUnclassified, fileClassifiedAsTopSecret, fileClassifiedAsSecret, fileClassifiedAsConfidential;

    private Record elecRecord;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        super.setup();

        // Create other users to check access permissions
        testUserGSTopSecret = createUserWithRMRole(testUser, ROLE_RM_USER.roleId);
        testUserSecret = dataUser.createRandomTestUser("UserSecret");
        testUserConfidential = dataUser.createRandomTestUser("UserConfidential");
        testUserNoAccess = dataUser.createRandomTestUser("UserSearchNoAccess");
        
        // Add users to testSite
        getDataUser().addUserToSite(testUserSecret, testSite, UserRole.SiteContributor);
        getDataUser().addUserToSite(testUserConfidential, testSite, UserRole.SiteConsumer);

        // Assign classification levels
        getClassificationService().assignClearance(dataContent.getAdminUser(), testUserGSTopSecret, ClassificationData.TOP_SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().assignClearance(dataContent.getAdminUser(), testUserSecret, ClassificationData.SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().assignClearance(dataContent.getAdminUser(), testUserConfidential, ClassificationData.CONFIDENTIAL_CLASSIFICATION_LEVEL_ID);
        getClassificationService().assignClearance(dataContent.getAdminUser(), testUserNoAccess, ClassificationData.UNCLASSIFIED_CLASSIFICATION_LEVEL_ID);

        // Create a folder and a file
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        fileRecord = new FileModel(unique_searchString + "record-1.txt", "record1", "record1", FileType.TEXT_PLAIN, "record1");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileRecord);

        // File a Electronic Record
        recordFolder = createCategoryFolderInFilePlan();
        elecRecord = createElectronicRecord(recordFolder.getId(), fileRecord.getName());
        
        // Create Files to be classified later      
        fileClassifiedAsTopSecret = new FileModel(unique_searchString + "TopS-1.txt", "top", "top", FileType.TEXT_PLAIN, "top");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileClassifiedAsTopSecret);
        
        fileClassifiedAsSecret = new FileModel(unique_searchString + "Secret-1.txt", "secret", "secret", FileType.TEXT_PLAIN, "secret");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileClassifiedAsSecret);
        
        fileClassifiedAsConfidential = new FileModel(unique_searchString + "conf-1.txt", "conf", "conf", FileType.TEXT_PLAIN, "conf");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileClassifiedAsConfidential);

        fileUnclassified = new FileModel(unique_searchString + "Unclassified-1.txt", "Unclassified1", "Unclassified1", FileType.TEXT_PLAIN, "Unclassified1");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileUnclassified);
    }
    
    @Test(priority = 1, groups = {TestGroup.ACS_611n})
    public void testSQLRespectsSitePermissions()
    {
        // Search for a file name to ensure content is indexed
        boolean indexingInProgress = isContentInSearchResults(fileRecord.getName(), fileRecord.getName(), true);

        Assert.assertTrue(indexingInProgress, "Expected record file, not found");
        
        indexingInProgress = isContentInSearchResults(testFolder.getName(), testFolder.getName(), true);

        Assert.assertTrue(indexingInProgress, "Expected folder, not found");
        
        // Search using sql: userNoAccess is not expected to find the record
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select * from alfresco where cm_name = '" + fileRecord.getName() + "'");

        // Verify that testUserNoAccess can't see content of the Private site that he is not a member of
        RestResponse response = searchSql(sqlRequest, testUserNoAccess);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(0));

        // Verify that the site member can see the content: Contributor
        response = searchSql(sqlRequest, testUserSecret);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));

        // Verify that the site member can see the content: Consumer
        response = searchSql(sqlRequest, testUserConfidential);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));        
    }
    
    @Test(priority = 2, groups = {TestGroup.ACS_611n})
    public void testSQLFiltersClassifiedFiles()
    {        
        // Create classified files as testUser        
        getClassificationService().classifyNode(dataContent.getAdminUser(), fileClassifiedAsTopSecret.getNodeRefWithoutVersion(), ClassificationData.TOP_SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().classifyNode(testUserGSTopSecret, fileClassifiedAsSecret.getNodeRefWithoutVersion(), ClassificationData.SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().classifyNode(testUserGSTopSecret, fileClassifiedAsConfidential.getNodeRefWithoutVersion(), ClassificationData.CONFIDENTIAL_CLASSIFICATION_LEVEL_ID);

        // Wait for the files to be indexed
        boolean fileFound = isContentInSearchResults("sc_classification:c", fileClassifiedAsConfidential.getName(), true);
        Assert.assertTrue(fileFound, "Expected confidential file, not found");

        // Check Aggregate query response doesn't bring up classification levels the user should not see
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select sc_classification, count(*) from alfresco where SITE = '" + testSite.getId() + "' group by sc_classification");

        RestResponse response = searchSql(sqlRequest, testUserGSTopSecret);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(3));

        response = searchSql(sqlRequest, testUserSecret);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(2));
 
        response = searchSql(sqlRequest, testUserConfidential);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));

        response = searchSql(sqlRequest, testUserNoAccess);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(0));
        
        // Check Query results, in a Solr format
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name from alfresco where sc_classification = '*' and SITE = '" + testSite.getId() + "'");
        sqlRequest.setFormat("solr");

        response = searchSql(sqlRequest, testUserGSTopSecret);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        List<String> results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");
        Assert.assertTrue(results.contains(fileClassifiedAsTopSecret.getName()), "Top Secret file not included in the results");
        Assert.assertTrue(results.contains(fileClassifiedAsSecret.getName()), "Secret file not included in the results");
        Assert.assertTrue(results.contains(fileClassifiedAsConfidential.getName()), "Confidential file not included in the results");
        Assert.assertFalse(results.contains(fileUnclassified.getName()), "Unclassified file included in the results when not expected");
        
        response = searchSql(sqlRequest, testUserSecret);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");
        Assert.assertFalse(results.contains(fileClassifiedAsTopSecret.getName()), "Top Secret file is included in the results when not expected");
        Assert.assertTrue(results.contains(fileClassifiedAsSecret.getName()), "Secret file not included in the results");
        Assert.assertTrue(results.contains(fileClassifiedAsConfidential.getName()), "Confidential file not included in the results");
        Assert.assertFalse(results.contains(fileUnclassified.getName()), "Unclassified file included in the results when not expected");
 
        response = searchSql(sqlRequest, testUserConfidential);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");
        Assert.assertFalse(results.contains(fileClassifiedAsTopSecret.getName()), "Top Secret file is included in the results when not expected");
        Assert.assertFalse(results.contains(fileClassifiedAsSecret.getName()), "Secret file is included in the results when not expected");
        Assert.assertTrue(results.contains(fileClassifiedAsConfidential.getName()), "Confidential file not included in the results");
        Assert.assertFalse(results.contains(fileUnclassified.getName()), "Unclassified file included in the results when not expected");

        response = searchSql(sqlRequest, testUserNoAccess);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");
        Assert.assertFalse(results.contains(fileClassifiedAsTopSecret.getName()), "Top Secret file is included in the results when not expected");
        Assert.assertFalse(results.contains(fileClassifiedAsSecret.getName()), "Secret file is included in the results when not expected");
        Assert.assertFalse(results.contains(fileClassifiedAsConfidential.getName()), "Confidential file is included in the results when not expected");
        Assert.assertFalse(results.contains(fileUnclassified.getName()), "Unclassified file included in the results when not expected");
    }
}
