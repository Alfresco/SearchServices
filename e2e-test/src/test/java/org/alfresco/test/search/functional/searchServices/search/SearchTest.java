/*
 * Copyright (C) 2018 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.test.search.functional.searchServices.search;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.reverse;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.rest.model.body.RestNodeLockBodyModel;
import org.alfresco.rest.search.RestRequestFilterQueryModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.search.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Search end point Public API test.
 * @author Michael Suzuki
 *
 */
public class SearchTest extends AbstractSearchServicesE2ETest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
        waitForContentIndexing(file4.getContent(), true);
    }

    @Test
    public void searchOnIndexedData() throws Exception
    {
        SearchResponse nodes =  query("cm:content:" + unique_searchString);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();
        
        SearchNodeModel entity = nodes.getEntryByIndex(0);
        entity.assertThat().field("search").contains("score");
        entity.getSearch().assertThat().field("score").isNotNull();
        Assert.assertEquals(entity.getName(),"pangram.txt");
    }
    
    @Test
    public void searchNonIndexedData()
    {        
        SearchResponse nodes =  query("yeti");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsEmpty();
    }

    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
              description = "Checks its possible to include the original request in the response")
    public void searchWithRequest()
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("fox");
        query.setQuery(queryReq);
        query.setIncludeRequest(true);

        SearchResponse response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        response.getContext().assertThat().field("request").isNotEmpty();
    }

    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
            description = "Tests a search request containing a sort clause.")
    public void searchWithOneSortClause()
    {
        // Tests the ascending order first
        List<String> expectedOrder = asList("alfresco.txt", "cars.txt", "pangram.txt");

        SearchRequest searchRequest = createQuery("cm_name:alfresco\\.txt cm_name:cars\\.txt cm_name:pangram\\.txt");
        searchRequest.addSortClause("FIELD", "name", true);

        RestRequestFilterQueryModel filters = new RestRequestFilterQueryModel();
        filters.setQuery("SITE:'" + testSite.getId() + "'");
        searchRequest.setFilterQueries(filters);

        SearchResponse responseWithAscendingOrder = query(searchRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        assertEquals(
                expectedOrder,
                responseWithAscendingOrder.getEntries().stream()
                    .map(SearchNodeModel::getModel)
                    .map(SearchNodeModel::getName)
                    .collect(Collectors.toList()));

        // Reverts the expected order...
        reverse(expectedOrder);

        // ...and test the descending order
        searchRequest.getSort().clear();
        searchRequest.addSortClause("FIELD", "name", false);

        SearchResponse responseWithDescendingOrder = query(searchRequest);
        assertEquals(
                expectedOrder,
                responseWithDescendingOrder.getEntries().stream()
                        .map(SearchNodeModel::getModel)
                        .map(SearchNodeModel::getName)
                        .collect(Collectors.toList()));
    }

    /**
     * Tests the query execution with two sort clauses.
     * The first clause has always the same value for all matches so the test makes sure the request is correctly
     * processed and the returned order is determined by the second clause.
     */
    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
            description = "Tests a search request containing a sort clause.")
    public void searchWithTwoSortClauses()
    {
        // Tests the ascending order first
        List<String> expectedOrder = asList("alfresco.txt", "cars.txt", "pangram.txt");

        SearchRequest searchRequest = createQuery("cm_name:alfresco\\.txt cm_name:cars\\.txt cm_name:pangram\\.txt");
        searchRequest.addSortClause("FIELD", "name", true);
        searchRequest.addSortClause("FIELD", "createdByUser.id", true);

        RestRequestFilterQueryModel filters = new RestRequestFilterQueryModel();
        filters.setQuery("SITE:'" + testSite.getId() + "'");
        searchRequest.setFilterQueries(filters);

        SearchResponse responseWithAscendingOrder = query(searchRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        assertEquals(
                expectedOrder,
                responseWithAscendingOrder.getEntries().stream()
                        .map(SearchNodeModel::getModel)
                        .map(SearchNodeModel::getName)
                        .collect(Collectors.toList()));

        // Reverts the expected order...
        reverse(expectedOrder);

        // ...and test the descending order
        searchRequest.getSort().clear();
        searchRequest.addSortClause("FIELD", "name", false);
        searchRequest.addSortClause("FIELD", "createdByUser.id", true);

        SearchResponse responseWithDescendingOrder = query(searchRequest);
        assertEquals(
                expectedOrder,
                responseWithDescendingOrder.getEntries().stream()
                        .map(SearchNodeModel::getModel)
                        .map(SearchNodeModel::getName)
                        .collect(Collectors.toList()));
    }

    // Test that when fields parameter is set, only restricted fields appear in the response
    @Test
    public void searchWithFields()
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("alfresco");
        query.setQuery(queryReq);

        // Restrict to fields: parentId
        List<String> fields = new ArrayList<>();
        fields.add("parentId");
        query.setFields(fields);

        query(query);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Only Field parentId is included in the response
        restClient.onResponse().assertThat().body("list.entries.entry[0].parentId", Matchers.notNullValue());
        // Usual Fields such as 'name, id' aren't included in the response
        restClient.onResponse().assertThat().body("list.entries.entry[0].name", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0].id", Matchers.nullValue());
    }
    
    @Test
    public void searchSpecialCharacters() throws Exception
    {
        // Create a file with Special Characters
        String specialCharfileName = "è¥äæ§ç§-åæ.pdf";
        FileModel file = new FileModel(specialCharfileName, "è¥äæ§ç§-åæ¬¯¸" + "è¥äæ§ç§-åæ¬¯¸", "è¥äæ§ç§-åæ¬¯¸", FileType.TEXT_PLAIN,
                "Text file with Special Characters: " + specialCharfileName);
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

        waitForIndexing(file.getName(), true);

        // Search
        SearchRequest searchReq = createQuery("name:'" + specialCharfileName + "'");
        SearchResponse nodes = query(searchReq);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();

        restClient.onResponse().assertThat().body("list.entries.entry[0].name", Matchers.equalToIgnoringCase(specialCharfileName));
    }
}
