/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.test.search.functional.gs.sql;

import static org.alfresco.rest.rm.community.model.user.UserRoles.ROLE_RM_USER;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.rm.community.model.record.Record;
import org.alfresco.rest.rm.community.model.recordcategory.RecordCategoryChild;
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
    private UserModel testUserGSAccess, testUserNoAccess;
    
    private FolderModel testFolder;
    private RecordCategoryChild recordFolder;

    private FileModel fileRecord, fileUnclassified;

    private String fileClassifiedAsTopSecret, fileClassifiedAsSecret, fileClassifiedAsConfidential;

    private Record elecRecord;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        super.setup();
        // Create other users to check access permissions
        testUserGSAccess = createUserWithRMRole(testUser, ROLE_RM_USER.roleId);;
        testUserNoAccess = dataUser.createRandomTestUser("UserSearchNoAccess");

        // Create classified files
        fileClassifiedAsTopSecret = createClassifiedFile(TOP_SECRET_CLASSIFICATION_LEVEL_ID);
        fileClassifiedAsSecret = createClassifiedFile(SECRET_CLASSIFICATION_LEVEL_ID);
        fileClassifiedAsConfidential = createClassifiedFile(CONFIDENTIAL_CLASSIFICATION_LEVEL_ID);

        // Create unclassified file
        fileUnclassified = new FileModel(unique_searchString + "Unclassified-1.txt", "Unclassified1", "Unclassified1", FileType.TEXT_PLAIN, "Unclassified1");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileUnclassified);

        // Create a folder and a file
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        fileRecord = new FileModel(unique_searchString + "record-1.txt", "record1", "record1", FileType.TEXT_PLAIN, "record1");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(fileRecord);

        // File a Electronic Record
        recordFolder = createCategoryFolderInFilePlan();
        elecRecord = createElectronicRecord(recordFolder.getId(), fileRecord.getName());
    }
    
    @Test(priority = 1, groups = {TestGroup.ACS_611n})
    public void testSQLRespectsSitePermissions() throws Exception
    {
        // Search for a file name to ensure content is indexed
        boolean indexingInProgress = isContentInSearchResults(fileRecord.getName(), fileRecord.getName(), true);

        Assert.assertTrue(indexingInProgress, "Expected record file, not found");
        
        indexingInProgress = isContentInSearchResults(testFolder.getName(), testFolder.getName(), true);

        Assert.assertTrue(indexingInProgress, "Expected folder, not found");
        
        // Search using sql: userNoAccess is not expected to find the record
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select * from alfresco where cm_name = '" + fileRecord.getName() + "'");
        sqlRequest.setLimit(10);

        RestResponse response = searchSql(sqlRequest, testUserNoAccess);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(0));
        
        // Add user as a SiteMember
        dataUser.addUserToSite(testUserNoAccess, testSite, UserRole.SiteConsumer);
        
        response = searchSql(sqlRequest, testUserNoAccess);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));
    }
    
    @Test(priority = 2, groups = {TestGroup.ACS_611n}, enabled = false)
    public void testSQLFiltersClassifiedFiles() throws Exception
    {
        // TODO: Relevant tests to be implemented
    }
}
