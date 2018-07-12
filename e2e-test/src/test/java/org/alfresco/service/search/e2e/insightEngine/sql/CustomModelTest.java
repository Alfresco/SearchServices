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
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Purpose of this TestClass is to test that the SQL end point works as expected with CustomModels
 * 
 * @author meenal bhave
 */

public class CustomModelTest extends AbstractSearchServiceE2E
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    protected SiteModel testSite;

    private UserModel testUser;

    private FolderModel testFolder;

    @BeforeClass(alwaysRun = true)
    public void setupEnvironment() throws Exception
    {
        serverHealth.assertServerIsOnline();

        testSite = dataSite.createPublicRandomSite();

        // Create test user and add the user as a SiteContributor
        testUser = dataUser.createRandomTestUser();

        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();
    }

    // Content of Custom Type is added to the Repo
    @Test(priority = 1, groups = { TestGroup.INSIGHT_10 })
    public void testSqlListCustomFields() throws Exception
    {        
        Long uniqueRef = System.currentTimeMillis();
        FileModel customFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Receipt");
        properties.put(PropertyIds.NAME, customFile.getName());
        properties.put("finance:ReceiptNo", uniqueRef);
        properties.put("finance:ReceiptValue", 30);
        
        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
            .createFile(customFile, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        // Wait for the file to be indexed
        waitForIndexing(customFile.getName(), true);

        // Select distinct site: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select finance_ReceiptNo, finance_ReceiptValue from alfresco where finance_ReceiptNo = " + uniqueRef);
        sqlRequest.setLimit(10);

        RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.maxItems", Matchers.equalTo(10));
        response.assertThat().body("list.pagination.count", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("finance_ReceiptValue"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("finance_ReceiptNo"));
        
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase("30.0"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalTo(uniqueRef.toString()));
        
        // Select distinct cm_name: solr format
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select finance_ReceiptNo, finance_ReceiptValue from alfresco where finance_ReceiptNo = " + uniqueRef + " limit 10");
        sqlRequest.setFormat("solr");

        response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_ReceiptValue", Matchers.equalTo(30));
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_ReceiptNo", Matchers.equalTo(uniqueRef));
    }
}
