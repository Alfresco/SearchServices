/*
 * Copyright (C) 2017 Alfresco Software Limited.
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
import org.testng.annotations.Test;

/**
 * Faceted search test.
 * @author Michael Suzuki
 *
 */
public class FacetedSearchTest extends AbstractSearchTest
{

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH  }, executionType = ExecutionType.REGRESSION,
              description = "Checks facet queries for the Search api")
    /**
     * Perform the below facet query.
     * {
     *    "query": {
     *      "query": "cars",
     *      "language": "afts"
     *    },
     *      "facetQueries": [
     *          {"query": "content.size:[o TO 102400]", "label": "small"},
     *          {"query": "content.size:[102400 TO 1048576]", "label": "medium"},
     *          {"query": "content.size:[1048576 TO 16777216]", "label": "large"}
     *    ],
     *      "facetFields": {"facets": [{"field": "'content.size'"}]}
     * }
     * 
     * Expected response
     * {"list": {
     *     "entries": [... All the results],
     *     "pagination": {
     *        "maxItems": 100,
     *        "hasMoreItems": false,
     *        "totalItems": 61,
     *        "count": 61,
     *        "skipCount": 0
     *     },
     *     "context": {
     *        "consistency": {"lastTxId": 512},
     *        "facetQueries": [
     *           {
     *              "count": 61,
     *              "label": "small"
     *           },
     *           {
     *              "count": 0,
     *              "label": "large"
     *           },
     *           {
     *              "count": 0,
     *              "label": "medium"
     *           }
     *        ],
     *        //Added below as part of SEARCH-374
     *        "facetsFields": [
     *          {  "label": "woof",
     *             "buckets": [
     *               { "label": "CreatedThisYear", "count": 75, "filterQuery": "created:2017"},
     *               { "label": "CreatedLastYear", "count": 75, "filterQuery": "created:2016"}
     *             ]
     *          }
     *     }
     * }}
     * @throws Exception
     */
    public void searchWithFaceting() throws Exception
    {        
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq =  new RestRequestQueryModel();
        queryReq.setQuery("cars");
        query.setQuery(queryReq);

        List<FacetQuery> facets = new ArrayList<FacetQuery>();
        facets.add(new FacetQuery("content.size:[0 TO 102400]", "small"));
        facets.add(new FacetQuery("content.size:[102400 TO 1048576]", "medium"));
        facets.add(new FacetQuery("content.size:[1048576 TO 16777216]", "large"));
        query.setFacetQueries(facets);
        
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<Object> list = new ArrayList<Object>();
        list.add(new FacetFieldQuery("'content.size'"));
        facetFields.setFacets(list);
        
        query.setFacetFields(facetFields);
        
        SearchResponse response =  query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facetQueries").isNotEmpty();
        FacetFieldBucket facet = response.getContext().getFacetQueries().get(0);
        facet.assertThat().field("label").contains("small").and().field("count").isGreaterThan(0);
        facet.assertThat().field("label").contains("small").and().field("filterQuery").is("content.size:[0 TO 102400]");
        response.getContext().getFacetQueries().get(1).assertThat().field("label").contains("large")
                    .and().field("count").isLessThan(1)
                    .and().field("filterQuery").is("content.size:[1048576 TO 16777216]");
        response.getContext().getFacetQueries().get(2).assertThat().field("label").contains("medium")
                    .and().field("count").isLessThan(1)
                    .and().field("filterQuery").is("content.size:[102400 TO 1048576]");
        
    }
    
}
