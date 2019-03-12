/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.service.search.e2e.insightEngine.sql;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.search.e2e.AbstractSearchServiceE2E;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Purpose of this TestClass is to test that the variants of <select *> query works as expected with CustomModels
 * 
 * @author meenal bhave
 * {@link https://issues.alfresco.com/jira/browse/SEARCH-873}
 */
public class SelectStarTest extends AbstractSearchServiceE2E
{
    private static Logger LOG = LogFactory.getLogger();
    
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    private FolderModel testFolder;

    /**
     * expFile1: id=10, empNo=000001, Location = London,
     * CostCentre=750, amount = 100, ExchangeRate=10, Approved= true,
     * Recorded_At, ExpenseDate=now,
     * Notes='London is a busy city'
     */
    private FileModel expFile1;

    /**
     * expFile2: id=30, empNo= 1, Location = Paris,
     * CostCentre=950, amount = 60.5, ExchangeRate=12, Approved=false,
     * Recorded_At, ExpenseDate=now-1month,
     * Notes='london is a busy city'
     */
    private FileModel expFile2;

    /**
     * expFile3: id=50, empNo= 56, Location = london,
     * CostCentre=750, amount = 60, ExchangeRate=12.5, Approved=true,
     * Recorded_At, ExpenseDate=now-1year,
     * Notes='Paris is a busy city'
     */
    private FileModel expFile3;

    /**
     * expFile4: id=null, empNo=null, Location = null,
     * CostCentre=null, amount = null, ExchangeRate=null, Approved=false,
     * Recorded_At, ExpenseDate=null, Notes=null
     */
    private FileModel expFile4;

    /**
     * expFile5: No custom property is set
     */
    private FileModel expFile5;

    /**
     * TIME_NOW: Date Time now based on system clock
     */
    private static ZonedDateTime TIME_NOW = ZonedDateTime.now();

    /**
     * DATE_NOW: Today's date in ISO Date format
     */
    private static final String DATE_NOW = TIME_NOW.format(DateTimeFormatter.ISO_DATE).replace("Z", "");

    /**
     * DT_NOW: Date Time now, in ISO Instant format
     */
    private static final String DT_NOW = TIME_NOW.format(DateTimeFormatter.ISO_INSTANT);

    /**
     * DT_NOW_MINUS_1_MONTH: ISO Date a month ago, in ISO Instant format
     */
    private static final String DT_NOW_MINUS_1_MONTH = TIME_NOW.minusMonths(1).format(DateTimeFormatter.ISO_INSTANT);

    /**
     * DT_NOW_MINUS_1_YEAR: ISO Date a year ago, in ISO Instant format
     */
    private static final String DT_NOW_MINUS_1_YEAR = TIME_NOW.minusYears(1).format(DateTimeFormatter.ISO_INSTANT);

    @BeforeClass(alwaysRun = true)
    public void setupEnvironment() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        super.springTestContextPrepareTestInstance();

        try
        {
            deployCustomModel("model/expense-model.xml");
        }
        catch (Exception e)
        {
            LOG.warn("Error Loading Expense Model", e);
        }

        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        // Create 5 files with the following data:
        expFile1 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "expense1");
        expFile1.setName("exp1-"+ expFile1.getName());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:expense:expenseReport");
        properties.put(PropertyIds.NAME, expFile1.getName());
        properties.put("expense:id", 10);
        properties.put("expense:EmpNo", 000001);
        properties.put("expense:Location", "London");
        properties.put("expense:CostCentre__1", "750");
        properties.put("expense:Amount", 100);
        properties.put("expense:Currency", "GBP");
        properties.put("expense:ExchangeRate", 10);
        properties.put("expense:Approved", true);
        properties.put("expense:Notes", "London is a busy city");
        properties.put("expense:Recorded_At", Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        properties.put("expense:ExpenseDate", Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));        

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
            .createFile(expFile1, properties, VersioningState.MAJOR).assertThat().existsInRepo();
 
        expFile2 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "expense2");
        expFile2.setName("exp2-"+ expFile2.getName());

        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:expense:expenseReport");
        properties.put(PropertyIds.NAME, expFile2.getName());
        properties.put("expense:id", 30);
        properties.put("expense:EmpNo", 1);
        properties.put("expense:Location", "Paris");
        properties.put("expense:CostCentre__1", "950");
        properties.put("expense:Amount", 60.5);
        properties.put("expense:Currency", "GBP");
        properties.put("expense:ExchangeRate", 12);
        properties.put("expense:Approved", false);
        properties.put("expense:Notes", "london is a busy city");
        properties.put("expense:Recorded_At", Date.from(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC)));
        properties.put("expense:ExpenseDate", Date.from(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC)));        

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
            .createFile(expFile2, properties, VersioningState.MAJOR).assertThat().existsInRepo();
 
        expFile3 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "expense3");
        expFile3.setName("exp3-"+ expFile3.getName());

        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:expense:expenseReport");
        properties.put(PropertyIds.NAME, expFile3.getName());
        properties.put("expense:id", 50);
        properties.put("expense:EmpNo", 56);
        properties.put("expense:Location", "london");
        properties.put("expense:CostCentre__1", "750");
        properties.put("expense:Amount", 60);
        properties.put("expense:Currency", "GBP");
        properties.put("expense:ExchangeRate", 12.5);
        properties.put("expense:Approved", true);
        properties.put("expense:Notes", "Paris is a busy city");
        properties.put("expense:Recorded_At", Date.from(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC)));
        properties.put("expense:ExpenseDate", Date.from(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC)));        

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
            .createFile(expFile3, properties, VersioningState.MAJOR).assertThat().existsInRepo();

        expFile4 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "expense4");
        expFile4.setName("exp4-"+ expFile4.getName());

        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:expense:expenseReport");
        properties.put(PropertyIds.NAME, expFile4.getName());
        properties.put("expense:id", null);
        properties.put("expense:EmpNo", null);
        properties.put("expense:Location", null);
        properties.put("expense:CostCentre__1", null);
        properties.put("expense:Amount", null);
        properties.put("expense:Currency", "GBP");
        properties.put("expense:ExchangeRate", null);
        properties.put("expense:Approved", false);
        properties.put("expense:Notes", null);
        properties.put("expense:Recorded_At", null);
        properties.put("expense:ExpenseDate", null);

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
            .createFile(expFile4, properties, VersioningState.MAJOR).assertThat().existsInRepo();

        expFile5 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "expense5");
        expFile5.setName("exp5-"+ expFile4.getName());

        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:expense:expenseReport");
        properties.put(PropertyIds.NAME, expFile5.getName());

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
            .createFile(expFile5, properties, VersioningState.MAJOR).assertThat().existsInRepo();

        // Wait for the file to be indexed
        waitForIndexing(expFile4.getName(), true);
    }

    @Test(priority = 1, groups = { TestGroup.INSIGHT_11 })
    public void testTextField()
    {
        testSqlQuery("select * from alfresco where `expense:Location` = 'london'", 2);
        testSqlQuery("select * from alfresco where `expense:Location` >= 'London'", 3);
        testSqlQuery("select * from alfresco where `expense:Location` >= 'london'", 3);
        testSqlQuery("select * from alfresco where `expense:Location` <= 'London'", 2);
        testSqlQuery("select * from alfresco where `expense:Location` <='london'", 2);
        testSqlQuery("select * from alfresco where `expense:Location` <> 'london' and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:Location` = 'Reading'", 0);
        testSqlQuery("select * from alfresco where `expense:Location` != 'Reading' and TYPE = 'expense:expenseReport'", 5);
        testSqlQuery("select * from alfresco where `expense:Location` not in ('Paris', 'Reading') and TYPE = 'expense:expenseReport'", 4);
        testSqlQuery("select * from alfresco where `expense:Location` not in ('Paris', 'Reading') and `expense:Location` in ('london')", 2);
        
        // Field name with _
        testSqlQuery("select * from alfresco where expense_Location = 'london'", 2);
    }
    
    // TODO: Enable when fixed: Bug: Search-1457
    @Test(priority = 2, groups = { TestGroup.INSIGHT_11 }, enabled = false)
    public void testTextFieldNullValues()
    {  
        testSqlQuery("select * from alfresco where `expense:Location` = '*' and TYPE = 'expense:expenseReport'", 3); //4 or 3
        testSqlQuery("select * from alfresco where `expense:Location` != '*' and TYPE = 'expense:expenseReport'", 2); //0 or 1 or 2

        testSqlQuery("select * from alfresco where `expense:Location` in ('Paris', 'Reading', null) and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:Location` not in ('Paris', 'Reading', null) and TYPE = 'expense:expenseReport'", 2); //0 or 2

        testSqlQuery("select * from alfresco where `expense:Location` is null and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:Location` is not null and TYPE = 'expense:expenseReport'", 3);
     }

    @Test(priority = 3, groups = { TestGroup.INSIGHT_11 })
    public void testMLTextField()
    {
        testSqlQuery("select * from alfresco where `expense:Notes` = 'London is a busy'", 2);
        testSqlQuery("select * from alfresco where `expense:Notes` = 'london'", 2);
        testSqlQuery("select * from alfresco where `expense:Notes` >= 'London'", 3);
        testSqlQuery("select * from alfresco where `expense:Notes` >= 'london'", 3);
        testSqlQuery("select * from alfresco where `expense:Notes` <= 'London' and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:Notes` <= 'london*' and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:Notes` in ('Paris', 'london')", 3);
        testSqlQuery("select * from alfresco where `expense:Notes` not in ('London', 'london') and TYPE = 'expense:expenseReport'", 3);

        testSqlQuery("select * from alfresco where expense_Notes = 'Paris'", 1);
    }

    // TODO: Update on fix: Search-1457
    @Test(priority = 4, groups = { TestGroup.INSIGHT_11 }, enabled=false)
    public void testMLTextFieldNullValues()
    {
        testSqlQuery("select * from alfresco where `expense:Notes` = '*' and TYPE = 'expense:expenseReport'", 3); //4
        testSqlQuery("select * from alfresco where `expense:Notes` != '*' and TYPE = 'expense:expenseReport'", 2); //0 or 1

        testSqlQuery("select * from alfresco where `expense:Notes` in ('london', null) and TYPE = 'expense:expenseReport'", 4);
        testSqlQuery("select * from alfresco where `expense:Notes` not in ('london', null) and TYPE = 'expense:expenseReport'", 1); //0 or 1

        testSqlQuery("select * from alfresco where `expense:Notes` is Null and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:Notes` is not null and TYPE = 'expense:expenseReport'", 3);
    }

    @Test(priority = 5, groups = { TestGroup.INSIGHT_11 })
    public void testIntegerField()
    {
        testSqlQuery("select * from alfresco where `expense:id` >= '10'", 3);
        testSqlQuery("select * from alfresco where `expense:id` > 10", 2);
        testSqlQuery("select * from alfresco where `expense:id` <= 30", 2);
        testSqlQuery("select * from alfresco where `expense:id` >= 5", 3);
        testSqlQuery("select * from alfresco where `expense:id`< 12 and TYPE = 'expense:expenseReport'", 1);
        testSqlQuery("select * from alfresco where `expense:id` = 50", 1);

        testSqlQuery("select * from alfresco where `expense:id` in (10, 30, 0)", 2);
        testSqlQuery("select * from alfresco where `expense:id` not in (5, 10) and TYPE = 'expense:expenseReport'", 4);

        testSqlQuery("select * from alfresco where expense_id = 50 and TYPE = 'expense:expenseReport'", 1);
    }

    // TODO: Update on fix: Search-1457
    @Test(priority = 6, groups = { TestGroup.INSIGHT_11 }, enabled = false)
    public void testIntegerFieldNullValues()
    {
        testSqlQuery("select * from alfresco where `expense:id` = '*' and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:id` != '*' and TYPE = 'expense:expenseReport'", 2);
        
        testSqlQuery("select * from alfresco where `expense:id` in (5, 10, null) and TYPE = 'expense:expenseReport'", 3); //0 or 3
        testSqlQuery("select * from alfresco where `expense:id` not in (5, 10, null) and TYPE = 'expense:expenseReport'", 2);
        
        testSqlQuery("select * from alfresco where `expense:id` is null and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:id` is not null and TYPE = 'expense:expenseReport'", 3);
    }

    @Test(priority = 7, groups = { TestGroup.INSIGHT_11 })
    public void testLongField()
    {
        testSqlQuery("select * from alfresco where `expense:EmpNo` >= '000001'", 3);
        testSqlQuery("select * from alfresco where `expense:EmpNo` >=000001", 3);
        testSqlQuery("select * from alfresco where `expense:EmpNo` <= 56 and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:EmpNo` >= 50", 1);
        testSqlQuery("select * from alfresco where `expense:EmpNo` = 1", 2);

        testSqlQuery("select * from alfresco where `expense:EmpNo` in (00001, 1, 50)", 2);
        testSqlQuery("select * from alfresco where `expense:EmpNo` not in (1, 50) and TYPE = 'expense:expenseReport'", 3);

        testSqlQuery("select * from alfresco where expense_EmpNo = 56", 1);
    }

    // TODO: Update on fix: Search-1457
    @Test(priority = 8, groups = { TestGroup.INSIGHT_11 }, enabled=false)
    public void testLongFieldNullValues()
    {
        testSqlQuery("select * from alfresco where `expense:EmpNo` = '*' and TYPE = 'expense:expenseReport'", 3); //4
        testSqlQuery("select * from alfresco where `expense:EmpNo` != '*' and TYPE = 'expense:expenseReport'", 1); //0

        testSqlQuery("select * from alfresco where `expense:EmpNo` in (1, 56, null) and TYPE = 'expense:expenseReport'", 4); //0 or 1
        testSqlQuery("select * from alfresco where `expense:EmpNo` not in (1, 56, null) and TYPE = 'expense:expenseReport'", 2);

        testSqlQuery("select * from alfresco where `expense:EmpNo` is null and TYPE = 'expense:expenseReport'", 1); //0 or 1
        testSqlQuery("select * from alfresco where `expense:EmpNo` is not null and TYPE = 'expense:expenseReport'", 1); //0 or 1
    }

    @Test(priority = 9, groups = { TestGroup.INSIGHT_11 })
    public void testDoubleField()
    {
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` >= '12'", 2);
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` >= 12.5", 1);
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` <= 12 and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` >= 12.5", 1);

        testSqlQuery("select * from alfresco where `expense:ExchangeRate` in (12, 10)", 2);
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` not in (12.5, 100) and TYPE = 'expense:expenseReport'", 4);

        testSqlQuery("select * from alfresco where expense_ExchangeRate = 12.5", 1);
    }


    // TODO: Update on fix: Search-1457
    @Test(priority = 10, groups = { TestGroup.INSIGHT_11 }, enabled = false)
    public void testDoubleFieldNullValues()
    {     
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` = '*' and TYPE = 'expense:expenseReport'", 3); //4
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` != '*' and TYPE = 'expense:expenseReport'", 2); //0

        testSqlQuery("select * from alfresco where `expense:ExchangeRate` in (12.5, 100, null) and TYPE = 'expense:expenseReport'", 4);
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` not in (12.5, 100, null) and TYPE = 'expense:expenseReport'", 1);

        testSqlQuery("select * from alfresco where `expense:ExchangeRate` is null and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:ExchangeRate` is not null and TYPE = 'expense:expenseReport'", 3);
    }

    @Test(priority = 11, groups = { TestGroup.INSIGHT_11 })
    public void testFloatField()
    {
        testSqlQuery("select * from alfresco where `expense:amount` >= '60.50'", 2);
        testSqlQuery("select * from alfresco where `expense:amount` >= 60", 3);
        testSqlQuery("select * from alfresco where `expense:amount` <= 60 and TYPE = 'expense:expenseReport'", 1);
        testSqlQuery("select * from alfresco where `expense:amount` = 60", 1);
        testSqlQuery("select * from alfresco where `expense:amount` in (60, 100)", 2);
        testSqlQuery("select * from alfresco where `expense:amount` not in (60.5, 100) and TYPE = 'expense:expenseReport'", 3);
        // unsupported?
        // testSqlQuery("select * from alfresco where `expense:amount` not in (60.5, 100, null) and TYPE = 'expense:expenseReport'", 1);

        testSqlQuery("select * from alfresco where expense_amount = 60", 1);
    }

    // TODO: Update on fix: Search-1457
    @Test(priority = 12, groups = { TestGroup.INSIGHT_11 }, enabled=false)
    public void testFloatFieldNullValues()
    {
        testSqlQuery("select * from alfresco where `expense:amount` >= '60.50'", 2);
        testSqlQuery("select * from alfresco where `expense:amount` >= 60", 3);
        testSqlQuery("select * from alfresco where `expense:amount` <= 60 and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:amount` = 60", 21);
        testSqlQuery("select * from alfresco where `expense:amount` in (60, 100)", 2);
        testSqlQuery("select * from alfresco where `expense:amount` not in (60.5, 100) and TYPE = 'expense:expenseReport'", 2);

        testSqlQuery("select * from alfresco where expense_amount = 60", 1);
    }

    @Test(priority = 13, groups = { TestGroup.INSIGHT_11 })
    public void testBooleanField()
    {
        testSqlQuery("select * from alfresco where `expense:Approved` = 'true'", 2);
        testSqlQuery("select * from alfresco where `expense:Approved` = 'false'", 2);
        testSqlQuery("select * from alfresco where `expense:Approved` <= 'false'", 2); //3 Include the one that's not set
        testSqlQuery("select * from alfresco where `expense:Approved` >= 'true'", 2);

        testSqlQuery("select * from alfresco where expense_Approved = 'true' and `expense:Location` = 'Paris'", 0);
    }

    // TODO: Update on fix: Search-1457
    @Test(priority = 14, groups = { TestGroup.INSIGHT_11 }, enabled = false)
    public void testBooleanFieldNullValues()
    {
        testSqlQuery("select * from alfresco where `expense:Approved` = '*' and TYPE = 'expense:expenseReport'", 3); //4
        testSqlQuery("select * from alfresco where `expense:Approved` != '*' and TYPE = 'expense:expenseReport'", 1); //0

        testSqlQuery("select * from alfresco where `expense:Approved` in ('true', null)", 4);
        testSqlQuery("select * from alfresco where `expense:Approved` not in ('true', null)", 1);
        
        testSqlQuery("select * from alfresco where `expense:Approved` is null", 2);
        testSqlQuery("select * from alfresco where `expense:Approved` is not null", 3);
    }

    @Test(priority = 15, groups = { TestGroup.INSIGHT_11 })
    public void testDateField()
    {
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` <'" + DT_NOW + "'", 2);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` < '" + DT_NOW + "'", 2);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` < '" + DT_NOW_MINUS_1_MONTH + "'", 1);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` <='NOW-1MONTH/DAY'", 1);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` <='" + DT_NOW_MINUS_1_MONTH + "'", 1);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` >='NOW/DAY'", 1);

        testSqlQuery("select * from alfresco where `expense:ExpenseDate` >'" + DT_NOW_MINUS_1_YEAR + "'", 3);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` >'NOW-1YEAR'", 2);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` >'NOW-1YEAR/DAY'", 3);

        testSqlQuery("select * from alfresco where `expense:ExpenseDate` >'" + DATE_NOW + "'", 0);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` >'" + DT_NOW + "'", 1);

        testSqlQuery("select * from alfresco where `expense:ExpenseDate` > 'NOW/DAY'", 1);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` < ('NOW/DAY') AND `expense:ExpenseDate` >= ('NOW-1YEAR/DAY')", 2);

        testSqlQuery("select * from alfresco where `expense:ExpenseDate` in ('" + DATE_NOW + "') and TYPE = 'expense:expenseReport'", 1);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` not in ('" + DT_NOW_MINUS_1_YEAR + "') and TYPE = 'expense:expenseReport'", 5);

        testSqlQuery("select * from alfresco where expense_ExpenseDate >= '1970-02-01T01:01:01Z' and TYPE = 'expense:expenseReport'", 3);
    }

    // TODO: Update on fix: Search-1457
    @Test(priority = 16, groups = { TestGroup.INSIGHT_11 }, enabled = false)
    public void testDateFieldNullValues()
    {
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` = '*' and TYPE = 'expense:expenseReport'", 3); //4
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` != '*' and TYPE = 'expense:expenseReport'", 2); //0
    
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` in (null, zdate) and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` not in (null, zdate) and TYPE = 'expense:expenseReport'", 2);
    
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` is null and TYPE = 'expense:expenseReport'", 2); //4
        testSqlQuery("select * from alfresco where `expense:ExpenseDate` is not null and TYPE = 'expense:expenseReport'", 3); //0
    }
    
    @Test(priority = 17, groups = { TestGroup.INSIGHT_11 })
    public void testDateTimeField()
    {
        testSqlQuery("select * from alfresco where `expense:Recorded_At` <'NOW-1MONTH' and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` < 'NOW/DAY' and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` <='" + DT_NOW + "' and TYPE = 'expense:expenseReport'", 2);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` >='NOW-1YEAR/YEAR'", 3);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` >'2018-01-01'", 3);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` > 'NOW-1MONTHS'", 1);
        
        testSqlQuery("select * from alfresco where `expense:Recorded_At` <'" + DT_NOW + "'  and TYPE = 'expense:expenseReport'", 2);

        testSqlQuery("select * from alfresco where `expense:Recorded_At` in ('" + DATE_NOW + "') and TYPE = 'expense:expenseReport'", 1);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` in ('" + DT_NOW + "') and TYPE = 'expense:expenseReport'", 0);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` not in ('" + DT_NOW_MINUS_1_YEAR + "') and TYPE = 'expense:expenseReport'", 5);

        testSqlQuery("select * from alfresco where expense_Recorded_At >= '1970-02-01T01:01:01Z' and TYPE = 'expense:expenseReport'", 3);
    }

    // TODO: Update on fix: Search-1457
    @Test(priority = 18, groups = { TestGroup.INSIGHT_11 }, enabled = false)
    public void testDateTimeFieldNullValues()
    {
        testSqlQuery("select * from alfresco where `expense:Recorded_At` = '*' and TYPE = 'expense:expenseReport'", 3); //4
        testSqlQuery("select * from alfresco where `expense:Recorded_At` != '*' and TYPE = 'expense:expenseReport'", 1); //0
    
        testSqlQuery("select * from alfresco where `expense:Recorded_At` in (null, zdt) and TYPE = 'expense:expenseReport'", 3);
        testSqlQuery("select * from alfresco where `expense:Recorded_At` not in (null, zdt) and TYPE = 'expense:expenseReport'", 2);
    
        testSqlQuery("select * from alfresco where `expense:Recorded_At` is null and TYPE = 'expense:expenseReport'", 2); //4
        testSqlQuery("select * from alfresco where `expense:Recorded_At` is not null and TYPE = 'expense:expenseReport'", 3); //0
    }
    
    // TODO: Search-1477: Enable, Uncomment Tests when bug is fixed
    @Test(priority = 19, groups = { TestGroup.INSIGHT_11 }, enabled = false)
    public void testVirtualTimeDimensions()
    {
        testSqlQuery("select * from alfresco where TYPE = 'expense:expenseReport' order by cm_created desc", 5);

        // OOTB fields: 400: cm_created_day not found in any table
         testSqlQuery("select cm_created_day, count(*) as total from alfresco"
         + " where cm_created >= 'NOW/DAY' AND `cm:name` >= 'exp'"
         + " group by cm_created_day", 5);
        
         testSqlQuery("Select Site, cm_name, cm_created_day from alfresco "
         + " where `cm:name` >= 'exp'"
         + " group by cm_created "
         + " having sum(`expense:amount`) > 10"
         + " order by `cm:created_day` desc", 5);

        //Custom fields
         testSqlQuery("Select Site, cm_name, expense_Recorded_At_day from alfresco "
         + " where type = 'cm:content' and `cm:name` >= 'exp' "
         + " group by expense_Recorded_At_day"
         + " having sum(`cm:content.size`) > 1000"
         + " order by expense_Recorded_At_day desc", 5);
    }
    
    @Test(priority = 20, groups = { TestGroup.INSIGHT_11 })
    public void testFieldNameWithUnderscore()
    {
        // Field name with _: in where clause
        testSqlQuery("select * from alfresco where `expense:CostCentre__1` = '750' AND TYPE = 'expense:expenseReport' order by cm_created desc", 2);

        // TODO: Search-1478: Uncomment when fixed
        // testSqlQuery("select * from alfresco where TYPE = 'expense:expenseReport' order by `expense:Recorded_At` desc", 5);

        // TODO: Search-1478: Uncomment when Fixed
        // testSqlQuery("select * from alfresco where TYPE = 'expense:expenseReport' order by expense_Recorded_At desc", 5);

        // TODO: Search-1478: Uncomment when Fixed
        // testSqlQuery("select expense_Recorded_At_year, sum(expense_amount) from alfresco"
        // + " where TYPE = 'expense:expenseReport' "
        // + " group by expense_Recorded_At_year"
        // + " order by expense_Recorded_At desc", 5);
    }
}
