/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.service.search.e2e.rm.searchServices;

import org.alfresco.rest.rm.community.model.record.Record;
import org.alfresco.rest.rm.community.model.recordcategory.RecordCategory;
import org.alfresco.rest.rm.community.model.recordcategory.RecordCategoryChild;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.service.search.e2e.AbstractRMSearchServiceE2E;
import org.alfresco.utility.model.TestGroup;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The purpose of this test is to create basic queries using a RM site and a basic file plan.
 * 
 * @author Cristina Diaconu
 */
public class SearchServicesRME2ETest extends AbstractRMSearchServiceE2E
{
    private static final String ROOT_CATEGORY_NAME = "RM_root_category";
    private static final String CATEGORY1 = "RM_child_category_1";
    private static final String CATEGORY2 = "RM_child_category_2";
    private static final String FOLDER1 = "RM_folder_1";
    private static final String FOLDER2 = "RM_folder_2";
    private static final String ELECTRONIC_FILE = "RM_electonic_file";
    private static final String NON_ELECTRONIC_FILE = "RM_nonelectonic_file";
    private static final String SEARCH_LANGUAGE = "cmis";

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {

        serverHealth.assertServerIsOnline();

        testSite = createRMSite();

        // create the Root category
        RecordCategory rootCategory = createRootCategory(ROOT_CATEGORY_NAME);
        Assert.assertNotNull(rootCategory, "Root category was not created!");

        // Add two children (categories) on Root category
        RecordCategoryChild childCategory1 = createRecordCategoryChild(rootCategory.getId(), CATEGORY1);
        Assert.assertNotNull(childCategory1, "Child category 1 was not created!");

        RecordCategoryChild childCategory2 = createRecordCategoryChild(rootCategory.getId(), CATEGORY2);
        Assert.assertNotNull(childCategory2, "Child category 2 was not created!");

        // Create two folders with records (electronic and nonElectronic files) on Child category 1
        RecordCategoryChild folder1 = createRecordFolder(childCategory1.getId(), FOLDER1);
        RecordCategoryChild folder2 = createRecordFolder(childCategory1.getId(), FOLDER2);

        // Create two folders on Child Category2
        createRecordFolder(childCategory2.getId(), FOLDER1);
        createRecordFolder(childCategory2.getId(), FOLDER2);

        // Complete a file
        Record electronicRecord = createElectronicRecord(folder1.getId(), ELECTRONIC_FILE);
        completeRecord(electronicRecord.getId());
        createNonElectronicRecord(folder1.getId(), NON_ELECTRONIC_FILE);

        // Add an electronic record on folder2
        createElectronicRecord(folder2.getId(), ELECTRONIC_FILE);
    }

    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment()
    {
        // dataSite.deleteSite(testSite);
    }

    @Test(priority = 1, groups = { TestGroup.ASS_14 })
    public void testBasicSearch() throws Exception
    {

        // Search for a folder name
        String query = "select * from cmis:folder where cmis:name='" + FOLDER1 + "'";

        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(query);
        queryModel.setLanguage(SEARCH_LANGUAGE);

        SearchResponse response = queryAsUser(dataUser.getAdminUser(), queryModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        Assert.assertEquals(response.getPagination().getCount(), 2,
                    "Wrong number of search elements, expected two folders.");

        // Search for a file name
        query = "select * from cmis:document where cmis:name like '" + NON_ELECTRONIC_FILE + "%'";

        queryModel = new RestRequestQueryModel();
        queryModel.setQuery(query);
        queryModel.setLanguage(SEARCH_LANGUAGE);

        response = queryAsUser(dataUser.getAdminUser(), queryModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        Assert.assertEquals(response.getPagination().getCount(), 1,
                    "Wrong number of search elements, expected 1 non electronic file");
    }

}
