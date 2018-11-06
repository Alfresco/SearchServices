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

import java.util.Collections;
import java.util.List;

import org.alfresco.utility.model.TestGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the search functionality using an ancestor path.
 * Using the category as an example, it is a node that is located
 * in root/rootCategory/classifiable. We now provide the ability to search by path
 * so that we can return all the child elements of our target path.
 * A search on root/rootCategory/classifiable should return Regions, Languages
 * as they are the child elements of the given path.
 *
 * @author Michael Suzuki
 *
 */
public class SearchAPATHTest extends AbstractSearchTest
{

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    /**
     * {
     *   "query": {
     *     "query": "name:*"
     *   },
     *   "facetFields": {
     *     "facets": [
     *       {"field": "APATH", "prefix": "0" }
     *     ]
     *   }
     * }
     * Expected result
     * entries[],
     *   "pagination": {
     *      "maxItems": 100,
     *      "hasMoreItems": true,
     *      "totalItems": 914,
     *      "count": 100,
     *      "skipCount": 0
     *   },
     *   "context": {
     *      "facetsFields": [{
     *         "getBuckets": [
     *            {
     *               "count": 913,
     *               "label": "0/5c09534f-3ca2-4272-bc25-064a7c1762b4"
     *            },
     *            {
     *               "count": 2,
     *               "label": "0/"
     *            }
     *         ],
     *         "label": "APATH"
     *      }],
     *      "consistency": {"lastTxId": 89}
     *   }
     *}}
     *
     */
    public void searchLevel0() throws Exception
    {
        final SearchRequest searchQuery = searchRequestWithAPATHFacet("name:*", "0");
        final SearchResponse response =  query(searchQuery);

        getBuckets(response).forEach(bucket -> bucket.assertThat().field("label").contains("0/"));
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    public void searchLevel0andIncludeSubLevel1() throws Exception
    {
        final SearchRequest searchQuery = searchRequestWithAPATHFacet("name:*", "1/");
        final SearchResponse response =  query(searchQuery);

        getFirstBucket(response).assertThat().field("label").contains("1/");
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    public void searchLevel2() throws Exception
    {
        final String queryString = "name:cars";

        final SearchRequest l1Request = searchRequestWithAPATHFacet(queryString, "1/");
        final SearchResponse l1Response = query(l1Request);

        final String l2Prefix = getFirstBucket(l1Response).getLabel().replaceFirst("^1/", "2/");

        final SearchRequest l2Request = searchRequestWithAPATHFacet(queryString, l2Prefix);
        final SearchResponse l2Response = query(l2Request);

        final FacetFieldBucket bucket = getFirstBucket(l2Response);
        bucket.assertThat().field("label").contains(l2Prefix);

        final String l3Prefix = bucket.getLabel().replaceFirst("^2/", "3/");

        final SearchRequest l3Request = searchRequestWithAPATHFacet(queryString, l3Prefix);
        final SearchResponse l3response = query(l3Request);

        getFirstBucket(l3response).assertThat().field("label").contains(l3Prefix);
    }

    /**
     * Creates a new {@link SearchRequest} for this test case.
     *
     * @param queryString the query string.
     * @param facetPrefix the facet prefix.
     * @return a new {@link SearchRequest} for this test case.
     */
    private SearchRequest searchRequestWithAPATHFacet(final String queryString, final String facetPrefix)
    {
        final SearchRequest searchRequest = new SearchRequest();

        final RestRequestQueryModel query = new RestRequestQueryModel();
        query.setQuery(queryString);
        searchRequest.setQuery(query);

        final RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        facetFields.setFacets(Collections.singletonList(new RestRequestFacetFieldModel("APATH", facetPrefix)));
        searchRequest.setFacetFields(facetFields);

        return searchRequest;
    }

    /**
     * Extracts the first bucket from the given response.
     *
     * @param response the results of a query execution.
     * @return the first bucket included in the search response.
     */
    protected FacetFieldBucket getFirstBucket(SearchResponse response)
    {
        return getBuckets(response).iterator().next();
    }

    /**
     * Extracts the buckets from the given response.
     * The method also makes sure the buckets list is not empty in the input response.
     *
     * @param response the results of a query execution.
     * @return the getBuckets included in the search response.
     */
    protected List<FacetFieldBucket> getBuckets(SearchResponse response)
    {
        Assert.assertNotNull(response.getContext().getFacetsFields());

        response.getContext().getFacetsFields().stream().iterator().next().getBuckets();

        Assert.assertTrue(response.getContext().getFacetsFields().size() > 0);

        Assert.assertNotNull(response.getContext().getFacetsFields().iterator().next().getBuckets());
        Assert.assertTrue(response.getContext().getFacetsFields().iterator().next().getBuckets().size() > 0);

        return response.getContext().getFacetsFields().iterator().next().getBuckets();
    }
}