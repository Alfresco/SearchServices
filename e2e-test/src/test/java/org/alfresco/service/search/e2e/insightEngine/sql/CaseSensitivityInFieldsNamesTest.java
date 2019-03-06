/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.service.search.e2e.insightEngine.sql;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Purpose of this TestClass is to test that the fields name follows a given case-sensitive rule in different part of the query.
 * Specifically:
 *
 * <ul>
 *     <li>When a field is declared in the field list (e.g. SELECT *a,b,c*) the case matters because a field with
 *          the exact case will be in the returned tuples.
 *     </li>
 *     <li>
 *         When a field is part of the predicate (e.g. WHERE a=somevalue) the case doesn't matter
 *     </li>
 *     <li>
 *         When a field is part of the expression (e.g. order by, group by) the case doesn't matter. However, specifically
 *         for aggregation expressions (e.g. group by), the same field with the exact case needs to be in field list (and
 *         here, see the first point, the case matters).
 *     </li>
 * </ul>
 *
 * @author agazzarini
 * {@link https://issues.alfresco.com/jira/browse/SEARCH-1491}
 */
public class CaseSensitivityInFieldsNamesTest extends AbstractSearchServiceE2E
{
    private static Logger LOG = LogFactory.getLogger();

    /**
     * Creates the test dataset that will be used in this test case.
     */
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

        FolderModel testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        createAndAddNewFile(testFolder, 1, "Maidenhead", "GBP", 10.2);
        createAndAddNewFile(testFolder, 2, "London", "GBP", 100.4);
        createAndAddNewFile(testFolder, 3, "Manchester", "GBP", 1737.22);
        FileModel lastFile = createAndAddNewFile(testFolder, 4, "Liverpool", "GBP", 445.9);

        waitForIndexing(lastFile.getName(), true);
    }

    @Test(priority = 1, groups = { TestGroup.INSIGHT_11 })
    public void fieldsInSelectListAreCaseSensitive()
    {
        List<String> selectLists =
                asList("EXPENSE_LOCATION,cm_name,expense_Currency",
                        "Expense_Currency,CM_NAME,ExPenSe_LOCATion",
                        "expense_location,expense_currency,cM_NaMe");

        int expectedNumberOfResults = 4;

        selectLists.forEach(fields -> {
                    String query = "select " + fields + " from alfresco where expense_Currency='GBP' and TYPE = 'expense:expenseReport'";

                    testSqlQuery(query, expectedNumberOfResults);

                    String[] expectedNames = fields.split(",");

                    range(0, expectedNames.length)
                            .forEach(labelIndex -> {
                                String expectedName = expectedNames[labelIndex];

                                for (int entryIndex = 0; entryIndex < expectedNumberOfResults; entryIndex++)
                                    restClient.onResponse()
                                            .assertThat()
                                            .body("list.entries.entry[" + entryIndex + "][" + labelIndex + "].label", Matchers.equalTo(expectedName));
                            });
                });
    }

    @Test(priority = 1, groups = { TestGroup.INSIGHT_11 })
    public void fieldsInPredicateAreCaseInsensitive()
    {
        List<String> queries =
                asList("select TYPE from alfresco where expense_Currency='GBP' and type = 'expense:expenseReport'",
                        "select TYPE from alfresco where EXPENSE_CURRENCY='GBP' and TYPE = 'expense:expenseReport'",
                        "select TYPE from alfresco where ExPeNsE_CurrENCY='GBP' and TyPe = 'expense:expenseReport'");

        int expectedNumberOfResults = 4;

        queries.forEach(query -> {
            testSqlQuery(query, expectedNumberOfResults);

            range(0, expectedNumberOfResults)
                    .forEach(entryIndex -> {
                        restClient.onResponse().assertThat().body("list.entries.entry[" + entryIndex + "][0].label", Matchers.equalTo("TYPE"));
                        restClient.onResponse().assertThat().body("list.entries.entry[" + entryIndex + "][0].value", Matchers.equalTo("{http://www.mycompany.com/model/expense/1.0}expenseReport"));
                    });
        });
    }

    @Test(priority = 1, groups = { TestGroup.INSIGHT_11 })
    public void fieldsInExpressionsAreCaseInsensitive()
    {
        List<String> fieldNames = asList("expense_Currency", "EXPENSE_CURRENCY", "ExPeNsE_CurrENCY");
        List<String> queries = fieldNames.stream()
                .map(fieldName -> "select " + fieldName + " from alfresco where TYPE = 'expense:expenseReport' group by " + fieldName)
                .collect(Collectors.toList());


        queries.forEach(query -> {
            testSqlQuery(query, 1);
            restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("expense_Currency"));
            restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalTo("GBP"));
        });
    }

    @Test(priority = 1, groups = { TestGroup.INSIGHT_11 })
    public void fieldsInSortExpressionAreCaseInsensitive_descendingOrder()
    {
        List<String> queries =
                asList("select expense_Id from alfresco where expense_Currency='GBP' and type = 'expense:expenseReport' order by expense_id desc",
                        "select expense_Id from alfresco where expense_Currency='GBP' and TYPE = 'expense:expenseReport' order by expense_id desc",
                        "select expense_Id from alfresco where expense_Currency='GBP' and TyPe = 'expense:expenseReport' order by expense_id desc");

        int expectedNumberOfResults = 4;

        queries.forEach(query -> {
            testSqlQuery(query, expectedNumberOfResults);
            restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalTo("4"));

            restClient.onResponse().assertThat().body("list.entries.entry[1][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalTo("3"));

            restClient.onResponse().assertThat().body("list.entries.entry[2][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[2][0].value", Matchers.equalTo("2"));

            restClient.onResponse().assertThat().body("list.entries.entry[3][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[3][0].value", Matchers.equalTo("1"));
        });
    }

    @Test(priority = 1, groups = { TestGroup.INSIGHT_11 })
    public void fieldsInSortExpressionAreCaseInsensitive_ascendingOrder()
    {
        List<String> queries =
                asList("select expense_Id from alfresco where expense_Currency='GBP' and type = 'expense:expenseReport' order by expense_id asc",
                        "select expense_Id from alfresco where expense_Currency='GBP' and TYPE = 'expense:expenseReport' order by expense_id asc",
                        "select expense_Id from alfresco where expense_Currency='GBP' and TyPe = 'expense:expenseReport' order by expense_id asc");

        int expectedNumberOfResults = 4;

        queries.forEach(query -> {
            testSqlQuery(query, expectedNumberOfResults);
            restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalTo("1"));

            restClient.onResponse().assertThat().body("list.entries.entry[1][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalTo("2"));

            restClient.onResponse().assertThat().body("list.entries.entry[2][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[2][0].value", Matchers.equalTo("3"));

            restClient.onResponse().assertThat().body("list.entries.entry[3][0].label", Matchers.equalToIgnoringCase("expense_Id"));
            restClient.onResponse().assertThat().body("list.entries.entry[3][0].value", Matchers.equalTo("4"));
        });
    }

    /**
     * Executes the given SQL query and asserts the response cardinality.
     *
     * @param sql the SQL query.
     * @param expectedCardinality the expected cardinality.
     * @return the {@link RestResponse} instance, that is the query response.
     */
    private RestResponse testSqlQuery(String sql, int expectedCardinality)
    {
        try
        {
            SearchSqlRequest sqlRequest = new SearchSqlRequest();
            sqlRequest.setSql(sql);

            RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);

            restClient.assertStatusCodeIs(HttpStatus.OK);
            restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(expectedCardinality));

            return response;
        }
        catch (Exception exception)
        {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Internal method used for creating a sample file, with some data used within the tests.
     *
     * @param parentFolder the parent folder.
     * @param id the file identifier.
     * @param location the location (a String property)
     * @param currency the currency (another String property)
     * @param amount the amount
     * @return the just created {@link FileModel} instance.
     */
    private FileModel createAndAddNewFile(final FolderModel parentFolder, int id, String location, String currency, double amount)  throws Exception
    {
        FileModel file = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "content #" + System.currentTimeMillis());
        file.setName("file-"+ file.getName());

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:expense:expenseReport");
        properties.put(PropertyIds.NAME, file.getName());
        properties.put("expense:id", id);
        properties.put("expense:Location", location);
        properties.put("expense:Currency", currency);
        properties.put("expense:Approved", true);
        properties.put("expense:Amount", amount);

        cmisApi.authenticateUser(testUser)
                .usingSite(testSite)
                .usingResource(parentFolder)
                .createFile(file, properties, VersioningState.MAJOR)
                .assertThat()
                .existsInRepo();

        return file;
    }
}