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

import java.util.ArrayList;
import java.util.List;

import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import junit.framework.Assert;

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
    @TestRail(section = { TestGroup.REST_API, TestGroup.SEARCH,
            TestGroup.ASS_1 }, executionType = ExecutionType.REGRESSION, description = "Checks that only restricted fields appear in the response")
    public void searchWithFields() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("alfresco");
        query.setQuery(queryReq);

        // Restrict to fields
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

}
