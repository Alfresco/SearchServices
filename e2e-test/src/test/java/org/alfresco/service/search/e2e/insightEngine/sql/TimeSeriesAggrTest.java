/*
 * Copyright 2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.service.search.e2e.insightEngine.sql;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.service.search.e2e.AbstractSearchServiceE2E;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Purpose of this TestClass is to test that the TimeSeries Aggregation works with/out Nulls and when values are not available
 * 
 * @author meenal bhave
 */

public class TimeSeriesAggrTest extends AbstractSearchServiceE2E
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    protected SiteModel testSite;

    private UserModel testExpenseUser1, testExpenseAdmin;

    private FolderModel testFolderUser1, testFolderAdmin;
    
    private FileModel expense1, expense2, expense3, expense4;
    
    private Long uniqueRef;
    
    /**
     * sql query to retrieve aggregated results based on Virtual Time Dimension _day
     */
    private static final String VIRTUAL_TIME_DIMENTION_DAY = ""
            + "select "
            + "finance_CreatedAt_day, "
            + "count(*) as ExpensesCount, "
            + "sum(finance_amount) as TotalExpenses, avg(finance_amount) as AvgExpenses, "
            + "min(finance_amount) as MinExpenses, max(finance_amount) as MaxExpenses "
            + "from alfresco "
            + "group by finance_CreatedAt_day";
 
    /**
     * sql query to retrieve aggregated results based on Virtual Time Dimension _month
     */
    private static final String VIRTUAL_TIME_DIMENTION_MONTH = ""
            + "select "
            + "finance_CreatedAt_month, "
            + "count(*) as ExpensesCount, "
            + "sum(finance_amount) as TotalExpenses, avg(finance_amount) as AvgExpenses, "
            + "min(finance_amount) as MinExpenses, max(finance_amount) as MaxExpenses "
            + "from alfresco "
            + "group by finance_CreatedAt_month";

    /**
     * sql query to retrieve aggregated results based on Virtual Time Dimension _year
     * Includes order by desc: order by virtual time dimension _year
     */
    private static final String VIRTUAL_TIME_DIMENTION_YEAR = ""
            + "select "
            + "finance_CreatedAt_year, "
            + "count(*) as ExpensesCount, "
            + "sum(finance_amount) as TotalExpenses, avg(finance_amount) as AvgExpenses, "
            + "min(finance_amount) as MinExpenses, max(finance_amount) as MaxExpenses "
            + "from alfresco "
            + "group by finance_CreatedAt_Year "
            + "order by finance_CreatedAt_YEAR desc";    

    /**
     * String that adds having clause based on min amount to the query
     */
    private static final String HAVING_MIN_AMOUNT = " having min(finance_amount) > 0";

    /**
     * String that adds having clause based on count to the query
     */
    private static final String HAVING_COUNT = " having count(*) >= 1";

    /**
     * String that adds order by desc clause to the query
     */
    private static final String ORDER_BY_DESC = " order by finance_CreatedAt_month desc";

    /**
     * LocalDateTime that represents date 1 month ago
     */
    private static final LocalDateTime dateLastMonth = LocalDateTime.now().minusMonths(1);

    @BeforeClass(alwaysRun = true)
    public void setupEnvironment() throws Exception
    {
        serverHealth.assertServerIsOnline();

        // Create test users
        testExpenseUser1 = dataUser.createRandomTestUser();
        testExpenseAdmin = dataUser.createRandomTestUser();

        // Create private test site as testExpenseUser1 and add testExpenseAdmin
        testSite = new SiteModel(RandomData.getRandomName("SiteFinance1"));
        testSite.setVisibility(Visibility.PRIVATE);
        
        testSite = dataSite.usingUser(testExpenseUser1).createSite(testSite);

        dataUser.addUserToSite(testExpenseAdmin, testSite, UserRole.SiteContributor);

        // Create test folders for users
        testFolderUser1 = dataContent.usingSite(testSite).usingUser(testExpenseUser1).createFolder();
        testFolderAdmin = dataContent.usingSite(testSite).usingUser(testExpenseAdmin).createFolder();
        
        // Set Node Permissions for testFolder2, to deny access to testExpenseUser1 
        JsonObject userPermission = Json
                .createObjectBuilder()
                .add("permissions",
                        Json.createObjectBuilder()
                        .add("isInheritanceEnabled", false)
                        .add("locallySet",
                                        Json.createObjectBuilder()
                                        .add("authorityId", testExpenseUser1.getUsername())
                                        .add("name", "SiteManager")
                                        .add("accessStatus", "DENIED")
                                        )).build();

        String putBody = userPermission.toString();

        restClient.authenticateUser(testExpenseAdmin).withCoreAPI().usingNode(testFolderAdmin).updateNode(putBody);

        uniqueRef = System.currentTimeMillis();

        createTestData();
    }
    
    private void createTestData() throws Exception
    {
        // testExpenseUser1 has 2 expenses, testExpenseAdmin has 2 expense, userAdmin can see all expenses

        // Expense1 for testExpenseUser1 dated today: Amount 100
        expense1 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        expense1.setName("ex-"+ expense1.getName());
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expense1.getName());
        properties.put("finance:No", uniqueRef);
        properties.put("finance:Emp", testExpenseUser1.getUsername());
        properties.put("finance:amount", 100);
        properties.put("finance:CreatedAt", Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        properties.put("finance:Location", "Reading");
        
        cmisApi.authenticateUser(testExpenseUser1).usingSite(testSite).usingResource(testFolderUser1)
            .createFile(expense1, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        // Expense2 for testExpenseUser1 dated <today- 1 month>: Amount 50
        expense2 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        expense2.setName("ex-"+ expense2.getName());
        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expense2.getName());
        properties.put("finance:No", uniqueRef+1);
        properties.put("finance:Emp", testExpenseUser1.getUsername());
        properties.put("finance:amount", 50);
        properties.put("finance:CreatedAt", Date.from(dateLastMonth.toInstant(ZoneOffset.UTC)));
        properties.put("finance:Location", "London");
        
        cmisApi.authenticateUser(testExpenseUser1).usingSite(testSite).usingResource(testFolderUser1)
            .createFile(expense2, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        // Expense1 for testExpenseAdmin dated <today- 1 month>: Amount 400
        expense3 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        expense3.setName("ex-"+ expense3.getName());
        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expense3.getName());
        properties.put("finance:No", uniqueRef+3);
        properties.put("finance:Emp", testExpenseAdmin.getUsername());
        properties.put("finance:amount", 400);
        properties.put("finance:CreatedAt", Date.from(dateLastMonth.toInstant(ZoneOffset.UTC)));
        properties.put("finance:Location", "London");
        
        cmisApi.authenticateUser(testExpenseAdmin).usingSite(testSite).usingResource(testFolderAdmin)
        .createFile(expense3, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        // Expense2 for testExpenseAdmin dated <today- 1 month>: Amount Not specified / Null
        expense4 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        expense4.setName("ex-"+ expense4.getName());
        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expense4.getName());
        properties.put("finance:No", uniqueRef+4);
        properties.put("finance:Emp", testExpenseAdmin.getUsername());
        properties.put("finance:CreatedAt", Date.from(dateLastMonth.plusDays(1).toInstant(ZoneOffset.UTC)));
        properties.put("finance:Location", "Maidenhead");
        
        cmisApi.authenticateUser(testExpenseAdmin).usingSite(testSite).usingResource(testFolderAdmin)
        .createFile(expense4, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        // Wait for the content to be indexed
        waitForIndexing(expense4.getName(), true);
    }

    /**
     * Test that Virtual time series aggregation works for _day
     * Format appears as yyyy
     * Data shows results for [Current full day - 1 month] as default
     * Values are correctly aggregated for _year dimension
     * Order is correct
     */
    @Test(priority = 1, groups = { TestGroup.INSIGHT_10 })
    public void testBasicTimeSeriesAggrDay() throws Exception
    {
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(VIRTUAL_TIME_DIMENTION_DAY);
        sqlRequest.setFormat("json");

        RestResponse response = 
                restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check that response includes the details for expense dated <today - 1 month>
        int noOfDays = LocalDateTime.now().getDayOfYear() - dateLastMonth.getDayOfYear();
        response.assertThat().body("list.pagination.count", Matchers.equalTo(noOfDays + 1));

        // Execute in solr format
        sqlRequest.setFormat("solr");

        response = restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        response.assertThat().body("result-set.docs", Matchers.notNullValue());
        
        // Check the results are in ascending order of date, starting with <today - 1 month>
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        // last date available in the results set is today
        restClient.onResponse().assertThat().body("result-set.docs[" + noOfDays + "].finance_CreatedAt_day", Matchers
                .equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        
    }

    /**
     * Test that Virtual time series aggregation works for _day
     * Format appears as yyyy
     * Data shows results for [Current full day - 1 month] as default
     * Values are correctly aggregated for _year dimension
     * Order is correct
     */
    @Test(priority = 2, groups = { TestGroup.INSIGHT_10 })
    public void testBasicTimeSeriesAggrDayWithHavingClause() throws Exception
    {
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(VIRTUAL_TIME_DIMENTION_DAY + HAVING_MIN_AMOUNT);
        sqlRequest.setFormat("json");

        RestResponse response = 
                restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check Count
        response.assertThat().body("list.pagination.count", Matchers.equalTo(2));

        // Execute in solr format
        sqlRequest.setFormat("solr");

        response = restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());

        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(50));

        // Check that response includes the details for expense dated <today>
        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_day", Matchers
                .equalTo(LocalDateTime.now().minusMonths(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(100));        
    }

    /**
     * Test that Virtual time series aggregation works for _month
     * Format appears as yyyy-mm
     * Data shows results for [Current month - 24 months] as default
     * Values are correctly aggregated for _month dimension
     * Order is correct
     */
    @Test(priority = 3, groups = { TestGroup.INSIGHT_10 })
    public void testBasicTimeSeriesAggrMonth() throws Exception
    {
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(VIRTUAL_TIME_DIMENTION_MONTH + ORDER_BY_DESC);
        sqlRequest.setFormat("json");

        RestResponse response = 
                restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check Count for Month Aggregation is 25: Current month minus 24 months
        response.assertThat().body("list.pagination.count", Matchers.equalTo(25));

        // Execute in solr format
        sqlRequest.setFormat("solr");

        response = restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());

        // Check that response includes the details for expense dated <today>: Order By desc
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_month", Matchers
                .equalTo(LocalDateTime.now().minusMonths(0).format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(100));

        // Check that response includes the details for expense dated <today - 1 month>
        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_month", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(50));
    }

    /**
     * Test that Virtual time series aggregation works for _year
     * Format appears as yyyy
     * Data shows results for [Current year - 5 years] as default
     * Values are correctly aggregated for _year dimension
     * Order is correct
     */
    @Test(priority = 4, groups = { TestGroup.INSIGHT_10 })
    public void testBasicTimeSeriesAggrYear() throws Exception
    {
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(VIRTUAL_TIME_DIMENTION_YEAR);
        sqlRequest.setFormat("json");

        RestResponse response = 
                restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check Count for Year Aggregation is 25: Current year minus 5 years
        response.assertThat().body("list.pagination.count", Matchers.equalTo(6));

        // Execute in solr format
        sqlRequest.setFormat("solr");

        response = restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());

        // Check that response includes the details for expense dated <today>: Order By desc
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_year", Matchers
                .equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(2));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(150));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(75));

        // Check that response includes the details for expense dated <today - 1 year>
        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_year", Matchers
                .equalTo(LocalDateTime.now().minusYears(1).format(DateTimeFormatter.ofPattern("yyyy"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(0));
    }

    /**
     * Test that Aggregation Values appear as 0 when not available for a time dimension
     */
    @Test(priority = 5, groups = { TestGroup.INSIGHT_10 })
    public void testNoValuesAggregateAsZero() throws Exception
    {
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(VIRTUAL_TIME_DIMENTION_DAY);
        sqlRequest.setFormat("json");

        RestResponse response = 
                restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check Count
        response.assertThat().body("list.pagination.count", Matchers.greaterThanOrEqualTo(28));

        // Execute in solr format
        sqlRequest.setFormat("solr");

        response = restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());

        // Check that response includes the details for expense dated <today - 1 month>
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(50));

        // Check that response includes expense dated <today - 1 month + 1 day>: Aggregations = 0, when ! specified
        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(0));
    }

    /**
     * Test that Aggregation Values appear as 0 when null for any time dimension
     */
    @Test(priority = 6, groups = { TestGroup.INSIGHT_10 })
    public void testNullValuesAggregateAsZero() throws Exception
    {
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(VIRTUAL_TIME_DIMENTION_DAY);
        sqlRequest.setFormat("json");

        RestResponse response = 
                restClient.authenticateUser(testExpenseAdmin).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check Count
        response.assertThat().body("list.pagination.count", Matchers.greaterThanOrEqualTo(29));

        // Execute in solr format
        sqlRequest.setFormat("solr");

        response = restClient.authenticateUser(testExpenseUser1).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());

        // Check that response includes the details for expense dated <today - 1 month>
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(50));

        // Check that response includes expense dated <today - 1 month + 1 day>: Aggregations = 0, when ! specified
        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(0));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(0));
    }

    /**
     * Test that aggregation produces correct results in-spite of changing null to zeroes
     */
    @Test(priority = 7, groups = { TestGroup.INSIGHT_10 })
    public void testAggregationsInclNulls() throws Exception
    {
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(VIRTUAL_TIME_DIMENTION_MONTH + HAVING_COUNT);
        sqlRequest.setFormat("solr");

        // User2 can see aggr results for All 4 Content
        RestResponse response = 
                restClient.authenticateUser(testExpenseAdmin).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());

        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_month", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(3));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(400));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(450));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(225));

        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_month", Matchers
                .equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(100));
    }
    
    /**
     * Test that aggregation produces correct results for admin, aggregating data from different users
     * Aggregated Values appear as 0 when actual values include a mix of null and non nul values
     * Results don't show 0 entries: when having clause filters them out
     */
    @Bug(id = "Search-927", status=Bug.Status.OPENED)
    @Test(priority = 8, groups = { TestGroup.INSIGHT_10 })
    public void testAggregationsForOtherUser() throws Exception
    {        
        String timeSeriesSqlDayAdmin = ""
                + "select "
                + "finance_CreatedAt_day, "
                + "count(*) as ExpensesCount, "
                + "sum(finance_amount) as TotalExpenses, avg(finance_amount) as AvgExpenses, "
                + "min(finance_amount) as MinExpenses, max(finance_amount) as MaxExpenses "
                + "from alfresco "
                + "where SITE = '" + testSite.getId() + "'"
                + "group by finance_CreatedAt_day";
        
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(timeSeriesSqlDayAdmin + HAVING_COUNT);
        sqlRequest.setFormat("solr");

        // admin can see aggregation results for All 4 Content added to the Site
        RestResponse response = 
                restClient.authenticateUser(dataUser.getAdminUser()).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());
        
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(2));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(400));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(450));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(225));

        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(1));

        // TODO: Search-927: NaN: Uncomment the following steps when Search-927 is resolved
        // restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(00));
        // restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(00));

        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(00));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(0));

        restClient.onResponse().assertThat().body("result-set.docs[2].finance_CreatedAt_day", Matchers
                .equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[2].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[2].MinExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[2].MaxExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[2].TotalExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[2].AvgExpenses", Matchers.equalTo(100));
    }

    /**
     * Test that aggregation produces correct results for admin, aggregating data from different users
     * Aggregated Values appear as 0 when actual values include a mix of null and non nul values
     * Results show 0 entries: when having clause does not filter them out
     * Order by desc shows the same results as above query in a desc order
     */
    @Bug(id = "Search-927", status=Bug.Status.OPENED)
    @Test(priority = 9, groups = { TestGroup.INSIGHT_10 })
    public void testAggregationsForOtherUserOrderByDesc() throws Exception
    {        
        String timeSeriesSqlDayAdmin = ""
                + "select "
                + "finance_CreatedAt_day, "
                + "count(*) as ExpensesCount, "
                + "sum(finance_amount) as TotalExpenses, avg(finance_amount) as AvgExpenses, "
                + "min(finance_amount) as MinExpenses, max(finance_amount) as MaxExpenses "
                + "from alfresco "
                + "where SITE = '" + testSite.getId() + "'"
                + "group by finance_CreatedAt_day "
                + "having count(*) >= 1 "
                + "order by finance_CreatedAt_day desc";
        
        // Select Data for TimeSeriesAggr: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(timeSeriesSqlDayAdmin);
        sqlRequest.setFormat("solr");

        // admin can see aggregation results for All 4 Content added to the Site
        RestResponse response = 
                restClient.authenticateUser(dataUser.getAdminUser()).withSearchSqlAPI().searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("result-set.docs", Matchers.notNullValue());
        
        restClient.onResponse().assertThat().body("result-set.docs[0].finance_CreatedAt_day", Matchers
                .equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[0].ExpensesCount", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("result-set.docs[0].MinExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[0].MaxExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[0].TotalExpenses", Matchers.equalTo(100));
        restClient.onResponse().assertThat().body("result-set.docs[0].AvgExpenses", Matchers.equalTo(100));

        restClient.onResponse().assertThat().body("result-set.docs[1].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[1].ExpensesCount", Matchers.equalTo(1));

        // TODO: Search-927: NaN: Uncomment the following steps when Search-927 is resolved
        // restClient.onResponse().assertThat().body("result-set.docs[1].MinExpenses", Matchers.equalTo(00));
        // restClient.onResponse().assertThat().body("result-set.docs[1].MaxExpenses", Matchers.equalTo(00));

        restClient.onResponse().assertThat().body("result-set.docs[1].TotalExpenses", Matchers.equalTo(00));
        restClient.onResponse().assertThat().body("result-set.docs[1].AvgExpenses", Matchers.equalTo(0));

        restClient.onResponse().assertThat().body("result-set.docs[2].finance_CreatedAt_day", Matchers
                .equalTo(dateLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        restClient.onResponse().assertThat().body("result-set.docs[2].ExpensesCount", Matchers.equalTo(2));
        restClient.onResponse().assertThat().body("result-set.docs[2].MinExpenses", Matchers.equalTo(50));
        restClient.onResponse().assertThat().body("result-set.docs[2].MaxExpenses", Matchers.equalTo(400));
        restClient.onResponse().assertThat().body("result-set.docs[2].TotalExpenses", Matchers.equalTo(450));
        restClient.onResponse().assertThat().body("result-set.docs[2].AvgExpenses", Matchers.equalTo(225));
    }
}
