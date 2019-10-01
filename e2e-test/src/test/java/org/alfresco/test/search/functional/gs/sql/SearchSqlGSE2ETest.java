/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.test.search.functional.gs.sql;

import static org.alfresco.rest.rm.community.model.user.UserRoles.ROLE_RM_USER;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.rm.community.model.record.Record;
import org.alfresco.rest.rm.community.model.recordcategory.RecordCategoryChild;
import org.alfresco.rest.rm.community.model.user.UserPermissions;
import org.alfresco.rest.rm.enterprise.core.ClassificationData;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.gs.AbstractGSE2ETest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.hamcrest.Matchers;
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

    private FileModel fileRecord, fileRecordElectronic, fileUnclassified, fileClassifiedAsTopSecret,
            fileClassifiedAsSecret, fileClassifiedAsConfidential;
    
    private String TOPSECRET_FILE, SECRET_FILE, CONFIDENTIAL_FILE, UNCLASSIFIED_FILE;
    
    private final String RECORD_IDENTIFIER = "rma:identifier:'*'";

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        super.setup();

        // Create other users to check access permissions
        testUser = createUserWithRMRole(testUser, ROLE_RM_USER.roleId);
        testUserGSTopSecret = dataUser.createRandomTestUser("UserTopSecret");
        testUserSecret = dataUser.createRandomTestUser("UserSecret");
        testUserConfidential = dataUser.createRandomTestUser("UserConfidential");
        testUserNoAccess = dataUser.createRandomTestUser("UserSearchNoAccess");
        
        // Add users to testSite
        getDataUser().addUserToSite(testUserGSTopSecret, testSite, UserRole.SiteCollaborator);
        getDataUser().addUserToSite(testUserSecret, testSite, UserRole.SiteContributor);
        getDataUser().addUserToSite(testUserConfidential, testSite, UserRole.SiteConsumer);

        // Assign classification levels
        getClassificationService().assignClearance(adminUserModel, testUser, ClassificationData.TOP_SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().assignClearance(adminUserModel, testUserGSTopSecret, ClassificationData.TOP_SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().assignClearance(adminUserModel, testUserSecret, ClassificationData.SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().assignClearance(adminUserModel, testUserConfidential, ClassificationData.CONFIDENTIAL_CLASSIFICATION_LEVEL_ID);
        getClassificationService().assignClearance(adminUserModel, testUserNoAccess, ClassificationData.UNCLASSIFIED_CLASSIFICATION_LEVEL_ID);

        // Create a folder and files
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();

        fileRecord = new FileModel(unique_searchString + "record-1.txt", "record1", "record1", FileType.TEXT_PLAIN, "record1");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileRecord);

        fileRecordElectronic = new FileModel(unique_searchString + "e-record-1.txt", "e-record1", "e-record1", FileType.TEXT_PLAIN, "e-record1");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileRecordElectronic);
        
        // Create Files to be classified later      
        fileClassifiedAsTopSecret = new FileModel(unique_searchString + "TopS-1.txt", "top", "top", FileType.TEXT_PLAIN, "top");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileClassifiedAsTopSecret);
        
        fileClassifiedAsSecret = new FileModel(unique_searchString + "Secret-1.txt", "secret", "secret", FileType.TEXT_PLAIN, "secret");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileClassifiedAsSecret);
        
        fileClassifiedAsConfidential = new FileModel(unique_searchString + "conf-1.txt", "conf", "conf", FileType.TEXT_PLAIN, "conf");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileClassifiedAsConfidential);

        fileUnclassified = new FileModel(unique_searchString + "Unclassified-1.txt", "Unclassified1", "Unclassified1", FileType.TEXT_PLAIN, "Unclassified1");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileUnclassified);

        // Classify files
        getClassificationService().classifyNode(adminUserModel, fileClassifiedAsTopSecret.getNodeRefWithoutVersion(), ClassificationData.TOP_SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().classifyNode(testUser, fileClassifiedAsSecret.getNodeRefWithoutVersion(), ClassificationData.SECRET_CLASSIFICATION_LEVEL_ID);
        getClassificationService().classifyNode(testUser, fileClassifiedAsConfidential.getNodeRefWithoutVersion(), ClassificationData.CONFIDENTIAL_CLASSIFICATION_LEVEL_ID);
        getClassificationService().classifyNode(testUser, fileUnclassified.getNodeRefWithoutVersion(), ClassificationData.UNCLASSIFIED_CLASSIFICATION_LEVEL_ID);
        
        TOPSECRET_FILE = fileClassifiedAsTopSecret.getName();
        SECRET_FILE = fileClassifiedAsSecret.getName();
        CONFIDENTIAL_FILE = fileClassifiedAsConfidential.getName();
        UNCLASSIFIED_FILE = fileUnclassified.getName();
    }

    @Test(priority = 1, groups = { TestGroup.AGS_302 })
    public void testSQLRespectsSitePermissions()
    {
        // Search for a file name to ensure content is indexed
        boolean indexingInProgress = isContentInSearchResults(fileRecord.getName(), fileRecord.getName(), true);

        assertTrue(indexingInProgress, "Expected record file, not found");
        
        indexingInProgress = isContentInSearchResults(testFolder.getName(), testFolder.getName(), true);

        assertTrue(indexingInProgress, "Expected folder, not found");

        // Search using sql: userNoAccess is not expected to find the record
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select * from alfresco where cm_name = '" + fileRecord.getName() + "'");

        // Verify that testUserNoAccess can't see content of the Private site that he is not a member of
        RestResponse response = searchSql(sqlRequest, testUserNoAccess);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(0));

        // Verify that the site member can see the content: Collaborator
        response = searchSql(sqlRequest, testUserSecret);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));

        // Verify that the site member can see the content: Consumer
        response = searchSql(sqlRequest, testUserConfidential);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));
    }

    @Test(priority = 2, groups = { TestGroup.AGS_302 })
    public void testSQLFiltersClassifiedFiles()
    {
        // Wait for the files to be indexed
        boolean fileFound = isContentInSearchResults("sc_classification:c", CONFIDENTIAL_FILE, true);
        assertTrue(fileFound, "Expected confidential file, not found");

        // Check Aggregate query response doesn't bring up classification levels the user should not see
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select sc_classification, count(*) from alfresco group by sc_classification");

        RestResponse response = searchSql(sqlRequest, testUserConfidential);
        // response.assertThat().body("list.pagination.count", Matchers.equalTo(2));

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select sc_classification, count(*) from alfresco where SITE = '" + testSite.getId() + "' group by sc_classification");

        response = searchSql(sqlRequest, testUserGSTopSecret);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(4));

        response = searchSql(sqlRequest, testUserSecret);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(3));

        response = searchSql(sqlRequest, testUserConfidential);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(2));

        response = searchSql(sqlRequest, testUserNoAccess);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(0));

        // Check Query results include content based on classification level, starting with unclassified file
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name from alfresco where sc_classification = '*' and SITE = '" + testSite.getId() + "'");
        sqlRequest.setFormat("solr");

        response = searchSql(sqlRequest, testUserGSTopSecret);
        List<String> results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");
        assertTrue(results.contains(TOPSECRET_FILE), "Top Secret file not in the results");
        assertTrue(results.contains(SECRET_FILE), "Secret file not in the results");
        assertTrue(results.contains(CONFIDENTIAL_FILE), "Confidential file not in the results");
        assertTrue(results.contains(UNCLASSIFIED_FILE), "Unclassified file not in the results");

        response = searchSql(sqlRequest, testUserSecret);
        results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");
        assertFalse(results.contains(TOPSECRET_FILE), "Top Secret file  in the results when not expected");
        assertTrue(results.contains(SECRET_FILE), "Secret file not in the results");
        assertTrue(results.contains(CONFIDENTIAL_FILE), "Confidential file not in the results");
        assertTrue(results.contains(UNCLASSIFIED_FILE), "Unclassified file not in the results");

        response = searchSql(sqlRequest, testUserConfidential);
        results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");        
        assertFalse(results.contains(TOPSECRET_FILE), "Top Secret file in the results when not expected");
        assertFalse(results.contains(SECRET_FILE), "Secret file in the results when not expected");
        assertTrue(results.contains(CONFIDENTIAL_FILE), "Confidential file not in the results");
        assertTrue(results.contains(UNCLASSIFIED_FILE), "Unclassified file not in the results");

        response = searchSql(sqlRequest, testUserNoAccess);
        results = response.getResponse().getBody().jsonPath().getList("result-set.docs.cm_name");
        assertFalse(results.contains(TOPSECRET_FILE), "Top Secret file in the results when not expected");
        assertFalse(results.contains(SECRET_FILE), "Secret file in the results when not expected");
        assertFalse(results.contains(CONFIDENTIAL_FILE), "Confidential file in the results when not expected");
        assertFalse(results.contains(UNCLASSIFIED_FILE), "Unclassified file in the results when not expected");
    }

    @Test(priority = 3, groups = { TestGroup.AGS_302 })
    public void testSQLFiltersElectronicRecords()
    {
        // File a Electronic Record
        RecordCategoryChild recordFolder = createCategoryFolderInFilePlan();
        Record elecRecord = createElectronicRecord(recordFolder.getId(), fileRecord.getName());

        // Add permission for the GS user to Read Records
        getRestAPIFactory().getRMUserAPI().addUserPermission(recordFolder.getParentId(), testUser, UserPermissions.PERMISSION_READ_RECORDS);

        // Wait for the record to be indexed and check that it can be found
        boolean fileFound = isContentInSearchResults(RECORD_IDENTIFIER, elecRecord.getName(), true);
        assertTrue(fileFound, "Expected record file, not found");

        SearchResponse result = queryAsUser(testUserGSTopSecret, RECORD_IDENTIFIER);
        Assert.assertEquals(result.getPagination().getCount(), 0, "Expected count of entries 0, found more");

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name, PATH from alfresco where rma_identifier = '*' and type = 'cm:content'");

        // Verify that user can see the electronic record with minimum read records permissions
        RestResponse response = searchSql(sqlRequest, testUser);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));

        // Verify that users can not see the electronic record without read record permissions
        response = searchSql(sqlRequest, testUserGSTopSecret);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(0));
    }

    @Test(priority = 4, groups = { TestGroup.AGS_302 })
    public void testSQLFiltersInPlaceRecords()
    {
        // Declare a file as Record
        Record inPlaceRecord = getRestAPIFactory().getFilesAPI().declareAsRecord(fileRecord.getNodeRefWithoutVersion());

        // Wait for the record to be indexed
        boolean fileFound = isContentInSearchResults(RECORD_IDENTIFIER, inPlaceRecord.getName(), true);
        assertTrue(fileFound, "Expected in place record file, not found");

        // Verify that user can see the in place record without any additional permissions
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name from alfresco where rma_identifier = '*' and Site = '" + testSite.getId() + "'");

        RestResponse response = searchSql(sqlRequest, testUser);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));
        response.assertThat().body("list.entries.entry[0].value", Matchers.contains(inPlaceRecord.getName()));

        // Verify that other site members can see the in place record with no additional permissions
        response = searchSql(sqlRequest, testUserGSTopSecret);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));
        response.assertThat().body("list.entries.entry[0].value", Matchers.contains(inPlaceRecord.getName()));

        response = searchSql(sqlRequest, testUserConfidential);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));
        response.assertThat().body("list.entries.entry[0].value", Matchers.contains(inPlaceRecord.getName()));

        response = searchSql(sqlRequest, testUserNoAccess);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(0));
    }

    @Test(priority = 5, groups = { TestGroup.AGS_302 })
    public void testSQLFiltersRecordsCascadedPermissions()
    {
        // Search for records when user does not have read permission
        boolean fileFound = isContentInSearchResults(NON_ELECTRONIC_FILE, nonElectronicRecord.getName(), false);
        assertTrue(fileFound, "Non electronic record file found in the results, when user doesn't have read permissions");
        

        // Search for records when user does not have read permission
        fileFound = isContentInSearchResults(ELECTRONIC_FILE, electronicRecord.getName(), false);
        assertTrue(fileFound, "Electronic record file found in the results, when user doesn't have read permissions");
        

        // Assign Read permissions to the user at rootCategory level
        getRestAPIFactory().getRMUserAPI().addUserPermission(rootCategory.getId(), testUser, UserPermissions.PERMISSION_READ_RECORDS);
        getRestAPIFactory().getRMUserAPI().addUserPermission(rootCategory.getId(), testUserGSTopSecret, UserPermissions.PERMISSION_READ_RECORDS);
        
        // Check that the non electronic record can now be found
        isContentInSearchResults(NON_ELECTRONIC_FILE, nonElectronicRecord.getName(), true);
        assertTrue(fileFound, "Expected non electronic record file, not found");

        // Check that the electronic record can now be found
        fileFound = isContentInSearchResults(ELECTRONIC_FILE, electronicRecord.getName(), true);
        assertTrue(fileFound, "Expected electronic record file, not found");

        // Check that the electronic record and non electronic record both can be found with cascaded permissions
        String recordFiles = String.format("('%s' , '%s')", nonElectronicRecord.getName(), electronicRecord.getName());
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name from alfresco where rma_identifier = '*' and cm_name in " + recordFiles + " order by cm_name");

        RestResponse response = searchSql(sqlRequest, testUser);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(2));
        response.assertThat().body("list.entries.entry[0].value", Matchers.contains(electronicRecord.getName()));
        response.assertThat().body("list.entries.entry[1].value", Matchers.contains(nonElectronicRecord.getName()));

        response = searchSql(sqlRequest, testUserGSTopSecret);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(2));
        response.assertThat().body("list.entries.entry[0].value", Matchers.contains(electronicRecord.getName()));
        response.assertThat().body("list.entries.entry[1].value", Matchers.contains(nonElectronicRecord.getName()));
    }
}
