/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.test.search.functional.gs;

import static org.alfresco.rest.rm.community.utils.FilePlanComponentsUtil.createRecordCategoryChildModel;
import static org.alfresco.rest.rm.community.utils.FilePlanComponentsUtil.createRecordCategoryModel;
import static org.alfresco.utility.data.RandomData.getRandomAlphanumeric;
import java.util.List;

import org.alfresco.dataprep.SiteService.RMSiteCompliance;
import org.alfresco.rest.core.RestAPIFactory;
import org.alfresco.rest.rm.community.model.record.Record;
import org.alfresco.rest.rm.community.model.recordcategory.RecordCategory;
import org.alfresco.rest.rm.community.model.recordcategory.RecordCategoryChild;
import org.alfresco.rest.rm.community.requests.gscore.api.RMSiteAPI;
import org.alfresco.rest.rm.community.requests.gscore.api.RecordFolderAPI;
import org.alfresco.rest.rm.community.requests.gscore.api.RecordsAPI;
import org.alfresco.rest.rm.enterprise.service.ClassificationService;
import org.alfresco.test.search.functional.insightEngine.AbstractInsightEngineE2ETest;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

/**
 * Base test class for GS tests.
 * 
 * @author Cristina Diaconu
 * @author Meenal Bhave
 */
public abstract class AbstractGSE2ETest extends AbstractInsightEngineE2ETest
{
    protected static final String FILE_PLAN_ALIAS = "-filePlan-";
    protected static final String RECORD_TYPE = "rma:record";
    protected static final String RECORD_FOLDER_TYPE = "rma:recordFolder";
    protected static final String RECORD_CATEGORY_TYPE = "rma:recordCategory";
    protected static final String CONTENT_TYPE = "cm:content";
    protected static final String ASPECTS_COMPLETED_RECORD = "rma:declaredRecord";
    protected static final String NON_ELECTRONIC_RECORD_TYPE = "rma:nonElectronicDocument";
    protected static final String RECORD_CATEGORY_TITLE = "RM_CATEGORY_TITLE";

    protected static final String ROOT_CATEGORY_NAME = RandomData.getRandomName("RM_Cat");
    protected static final String CATEGORY1 = RandomData.getRandomName("RM_child_cat1");
    protected static final String CATEGORY2 = RandomData.getRandomName("RM_child_cat2");
    protected static final String FOLDER1 = RandomData.getRandomName("RM_folder1");
    protected static final String FOLDER2 = RandomData.getRandomName("RM_folder2");
    protected static final String ELECTRONIC_FILE = RandomData.getRandomName("RM_electonic_file");
    protected static final String NON_ELECTRONIC_FILE = RandomData.getRandomName("RM_nonelectonic_file");

    protected static final String SEARCH_LANGUAGE_CMIS = "cmis";
    

    @Autowired
    protected RestAPIFactory restAPIFactory;
    
    @Autowired
    private ClassificationService classificationService;

    @BeforeClass(alwaysRun = true)
    public void rmDataPreparation()
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
    
    public ClassificationService getClassificationService()
    {
        return classificationService;
    }

    /**
     * Create a new RM Site. If the site already exists then remove it and create a new one.
     */
    public SiteModel createRMSite()
    {
        RMSiteAPI rmSiteAPI = restAPIFactory.getRMSiteAPI();

        if (rmSiteAPI.existsRMSite())
        {
            rmSiteAPI.deleteRMSite();
        }

        return dataSite.createRMSite(RMSiteCompliance.STANDARD);
    }
    
    /**
     * Helper method to create a test user with rm role
     *
     * @param userRole the rm role
     * @return the created user model
     */
    protected UserModel createUserWithRMRole(UserModel user, String userRole)
    {
        getRestAPIFactory().getRMUserAPI().assignRoleToUser(user.getUsername(), userRole);
        return user;
    }

    protected RestAPIFactory getRestAPIFactory()
    {
        return restAPIFactory;
    }
    /**
     * Helper method to create root category as the admin user
     *
     * @param categoryName The name of the category
     * @return The created category
     */
    public RecordCategory createRootCategory(String categoryName)
    {
        return createRootCategory(adminUserModel, categoryName, RECORD_CATEGORY_TITLE);
    }

    /**
     * Helper method to create root category
     *
     * @param userModel The user under whose privileges this structure is going to be created
     * @param categoryName The name of the category
     * @param categoryTitle The title of the category
     * @return The created category
     */
    public RecordCategory createRootCategory(UserModel userModel, String categoryName, String categoryTitle)
    {
        RecordCategory recordCategoryModel = createRecordCategoryModel(categoryName, categoryTitle);
        return restAPIFactory.getFilePlansAPI(userModel).createRootRecordCategory(recordCategoryModel, FILE_PLAN_ALIAS);
    }

    /**
     * Helper method to create a record category as the admin user
     *
     * @param recordCategoryId The id of the record category
     * @param name The name of the record category child
     * @return The created child category
     */
    public RecordCategoryChild createRecordCategoryChild(String recordCategoryId, String name)
    {
        return createRecordCategoryChild(adminUserModel, recordCategoryId, name, RECORD_CATEGORY_TYPE);
    }

    /**
     * Helper method to create a record category child
     *
     * @param user The user under whose privileges the node is going to be created
     * @param recordCategoryId The id of the record category
     * @param name The name of the record category child
     * @param type The type of the record category child
     * @return The created child category
     */
    public RecordCategoryChild createRecordCategoryChild(UserModel user, String recordCategoryId, String name,
                String type)
    {
        RecordCategoryChild recordCategoryChildModel = createRecordCategoryChildModel(name, type);
        return restAPIFactory.getRecordCategoryAPI(user).createRecordCategoryChild(recordCategoryChildModel,
                    recordCategoryId);
    }

    /**
     * Helper method to create a record folder as the admin user
     *
     * @param recordCategoryId The id of the record category
     * @param name The name of the record category child
     * @return The created record folder.
     */
    public RecordCategoryChild createRecordFolder(String recordCategoryId, String name)
    {
        return createRecordCategoryChild(adminUserModel, recordCategoryId, name, RECORD_FOLDER_TYPE);
    }

    /**
     * Create an electronic record
     *
     * @param parentId The id of the parent
     * @param name The name of the record
     * @return The created record
     */
    public Record createElectronicRecord(String parentId, String name)
    {
        return createElectronicRecord(parentId, name, null);
    }

    /**
     * Create an electronic record
     *
     * @param parentId The id of the parent
     * @param name The name of the record
     * @return The created electronic record
     */
    public Record createElectronicRecord(String parentId, String name, UserModel user)
    {
        RecordFolderAPI recordFolderAPI = restAPIFactory.getRecordFolderAPI(user);
        Record recordModel = Record.builder().name(name).nodeType(CONTENT_TYPE).build();
        return recordFolderAPI.createRecord(recordModel, parentId);
    }

    /**
     * Create a non-electronic record
     *
     * @param parentId The id of the parent
     * @param name The name of the record
     * @return The created non electronic record
     */
    public Record createNonElectronicRecord(String parentId, String name)
    {
        return createNonElectronicRecord(parentId, name, null);
    }

    /**
     * Create a non-electronic record
     *
     * @param parentId The id of the parent
     * @param name The name of the record
     * @param user The user who creates the non-electronic record
     * @return The created non electronic record
     */
    public Record createNonElectronicRecord(String parentId, String name, UserModel user)
    {
        RecordFolderAPI recordFolderAPI = restAPIFactory.getRecordFolderAPI(user);
        Record recordModel = Record.builder().name(name).nodeType(NON_ELECTRONIC_RECORD_TYPE).build();
        return recordFolderAPI.createRecord(recordModel, parentId);
    }

    /**
     * Helper method to complete record
     *
     * @param recordId The id of the record to complete
     * @return The completed record
     */
    public Record completeRecord(String recordId)
    {
        RecordsAPI recordsAPI = restAPIFactory.getRecordsAPI();
        List<String> aspects = recordsAPI.getRecord(recordId).getAspectNames();

        // this operation is only valid for records
        Assert.assertTrue(aspects.contains(RECORD_TYPE));
        // a record mustn't be completed
        Assert.assertFalse(aspects.contains(ASPECTS_COMPLETED_RECORD));
        // add completed record aspect
        aspects.add(ASPECTS_COMPLETED_RECORD);

        Record updateRecord = recordsAPI.updateRecord(Record.builder().aspectNames(aspects).build(), recordId);
        restAPIFactory.getRmRestWrapper().assertStatusCodeIs(HttpStatus.OK);

        return updateRecord;
    }
    
    /**
     * Helper method to create root category
     *
     * @param userModel The user under whose privileges this structure is going to be created
     * @param categoryName The name of the category
     * @return The created category
     */
    public RecordCategoryChild createCategoryFolderInFilePlan()
    {
        // create root category
        RecordCategory recordCategory = createRootCategory("Category " + getRandomAlphanumeric());

        return createFolder(recordCategory.getId(), "Folder " + getRandomAlphanumeric());
    }
    
    /**
     * Helper method to create record folder
     *
     * @param user The user under whose privileges this structure is going to be created
     * @param recordCategoryId The id of the record category
     * @param name The name of the folder
     * @return The created folder
     */
    public RecordCategoryChild createFolder(String recordCategoryId, String name)
    {
        UserModel asUser = dataContent.getAdminUser();
        RecordCategoryChild recordFolderModel = createRecordCategoryChildModel(name, RECORD_FOLDER_TYPE);
        return getRestAPIFactory().getRecordCategoryAPI(asUser).createRecordCategoryChild(recordFolderModel, recordCategoryId);
    }
    
    /**
     * Create a new file and classify it
     *
     * @param classificationLevel the file classification level
     * @return the classified file name
     */
    public String createClassifiedFile(String classificationLevel)
    {
        UserModel asUser = dataContent.getAdminUser();
        FileModel file = new FileModel(RandomData.getRandomName("classified") + ".txt", FileType.TEXT_PLAIN);
        file = dataContent.usingUser(asUser).usingSite(testSite).createContent(file);
        classificationService.classifyNodeAsAdmin(file.getNodeRefWithoutVersion(), classificationLevel);
        return file.getName();
    }
}
