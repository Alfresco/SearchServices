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

import org.alfresco.rest.model.RestRequestFacetFieldsModel;
import org.alfresco.utility.model.TestGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Search high lighting test.
 * @author Michael Suzuki
 *
 */
public class FacetedSearchTest extends AbstractSearchTest
{
    
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API})
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
        list.add(new FacetField("'content.size'"));
        facetFields.setFacets(list);
        
        query.setFacetFields(facetFields);
        
        SearchResponse nodes =  query(query);
        nodes.assertThat().entriesListIsNotEmpty();
        nodes.getContext().assertThat().field("facetQueries").isNotEmpty();
        
    }
    
}
