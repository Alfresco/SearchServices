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
package org.alfresco.rest.search;

import static java.util.Arrays.asList;
import static java.util.Collections.reverse;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.rest.model.body.RestNodeLockBodyModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Search end point Public API test.
 * @author Michael Suzuki
 *
 */
public class SearchTest extends AbstractSearchTest
{
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API})
    public void searchOnIndexedData() throws Exception
    {        
        SearchResponse nodes =  query(unique_searchString);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();
        
        SearchNodeModel entity = nodes.getEntryByIndex(0);
        entity.assertThat().field("search").contains("score");
        entity.getSearch().assertThat().field("score").isNotEmpty();
        Assert.assertEquals("pangram.txt",entity.getName());
    }
    
    @Test(groups={TestGroup.SEARCH,TestGroup.REST_API})
    public void searchNonIndexedData() throws Exception
    {        
        SearchResponse nodes =  query("yeti");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Checks its possible to include the original request in the response")
    public void searchWithRequest() throws Exception
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

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
            description = "Tests a search request containing a sort clause.")
    public void searchWithOneSortClause() throws Exception
    {
        // Tests the ascending order first
        List<String> expectedOrder = asList("alfresco.txt", "cars.txt", "pangram.txt");

        SearchRequest searchRequest = createQuery("cm_name:alfresco\\.txt cm_name:cars\\.txt cm_name:pangram\\.txt");
        searchRequest.addSortClause("FIELD", "name", true);

        RestRequestFilterQueryModel filters = new RestRequestFilterQueryModel();
        filters.setQuery("SITE:'" + siteModel.getId() + "'");
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
    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
            description = "Tests a search request containing a sort clause.")
    public void searchWithTwoSortClauses() throws Exception
    {
        // Tests the ascending order first
        List<String> expectedOrder = asList("alfresco.txt", "cars.txt", "pangram.txt");

        SearchRequest searchRequest = createQuery("cm_name:alfresco\\.txt cm_name:cars\\.txt cm_name:pangram\\.txt");
        searchRequest.addSortClause("FIELD", "name", true);
        searchRequest.addSortClause("FIELD", "createdByUser.id", true);

        RestRequestFilterQueryModel filters = new RestRequestFilterQueryModel();
        filters.setQuery("SITE:'" + siteModel.getId() + "'");
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

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ACS_61n }) @TestRail(section = {
        TestGroup.REST_API, TestGroup.SEARCH,
        TestGroup.ACS_61n }, executionType = ExecutionType.REGRESSION, description = "Checks the \"include\" request parameter support the 'permissions' option") public void searchQuery_includePermissions_shouldReturnNodeWithPermissionsInformation()
        throws Exception
    {
        String query = "fox";
        String include = "permissions";

        SearchRequest retrievalQueryIncludingPermissionsInformation = createQuery(query);
        retrievalQueryIncludingPermissionsInformation.setInclude(asList(include));

        query(retrievalQueryIncludingPermissionsInformation);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.entries[0].entry.permissions", notNullValue());

        SearchRequest retrievalQueryNotIncludingLockInformation = createQuery(query);

        query(retrievalQueryNotIncludingLockInformation);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.entries[0].entry.permissions", nullValue());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ACS_61n })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ACS_61n  }, executionType = ExecutionType.REGRESSION,
            description = "Checks the \"include\" request parameter support the 'isLocked' option")
    public void searchQuery_includeIsLocked_shouldReturnNodeWithLockInformation() throws Exception {
        String query = "fox";
        String include = "isLocked";

        SearchRequest retrievalQueryIncludingLockInformation = createQuery(query);
        retrievalQueryIncludingLockInformation.setInclude(asList(include));

        query(retrievalQueryIncludingLockInformation);
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.entries[0].entry.isLocked", equalTo(false));

        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(userModel).withCoreAPI().usingNode(file).usingParams("include=isLocked").lockNode(lockBodyModel);
        
        query(retrievalQueryIncludingLockInformation);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.entries[0].entry.isLocked", equalTo(true));
        
        SearchRequest retrievalQueryNotIncludingLockInformation = createQuery(query);

        query(retrievalQueryNotIncludingLockInformation);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.entries[0].entry.isLocked", nullValue());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ACS_61n })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ACS_61n  }, executionType = ExecutionType.REGRESSION,
            description = "Checks the \"include\" request parameter does not support the 'notValid' option")
    public void searchQuery_includeInvalid_shouldReturnBadResponse() throws Exception
    {
        String query = "fox";
        String notValidInclude = "notValid";
        SearchRequest permissionsRetrieval = createQuery(query);
        permissionsRetrieval.setInclude(asList(notValidInclude));
        
        query(permissionsRetrieval);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.onResponse()
                .assertThat()
                .body("error.briefSummary", containsString("An invalid argument was received "+notValidInclude));
    }

    // Test that when fields parameter is set, only restricted fields appear in the response
    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    public void searchWithFields() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("alfresco");
        query.setQuery(queryReq);

        // Restrict to fields: parentId
        List<String> fields = new ArrayList<String>();
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
    
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API })
    public void searchSpecialCharacters() throws Exception
    {
        // Create a file with Special Characters
        String specialCharfileName = "è¥äæ§ç§-åæ.pdf";
        FileModel file = new FileModel(specialCharfileName, "è¥äæ§ç§-åæ¬¯¸" + "è¥äæ§ç§-åæ¬¯¸", "è¥äæ§ç§-åæ¬¯¸", FileType.TEXT_PLAIN,
                "Text file with Special Characters: " + specialCharfileName);
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(file);

        waitForIndexing(file.getName(), true);

        // Search
        SearchRequest searchReq = createQuery("name:'" + specialCharfileName + "'");
        SearchResponse nodes = query(searchReq);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();

        restClient.onResponse().assertThat().body("list.entries.entry[0].name", Matchers.equalToIgnoringCase(specialCharfileName));
    }
}
