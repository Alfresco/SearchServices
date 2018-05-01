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

import org.alfresco.utility.model.TestGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

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
     *         "buckets": [
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
        SearchRequest searchQuery = new SearchRequest();
        RestRequestQueryModel queryReq =  new RestRequestQueryModel();
        queryReq.setQuery("name:*");
        searchQuery.setQuery(queryReq);
        
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("APATH","0"));
        facetFields.setFacets(list);
        searchQuery.setFacetFields(facetFields);
        SearchResponse response =  query(searchQuery);
        RestResultBucketsModel fresponse = response.getContext().getFacetsFields().get(0);
        fresponse.assertThat().field("buckets").isNotEmpty();
        Assert.assertEquals(2,fresponse.getBuckets().size());
        fresponse.getBuckets().get(0).assertThat().field("label").contains("0/");
        fresponse.getBuckets().get(1).assertThat().field("label").is("0/");
    }
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    public void searchLevel0andIncludeSubLevel1() throws Exception
    {
        SearchRequest searchQuery = new SearchRequest();
        RestRequestQueryModel queryReq =  new RestRequestQueryModel();
        queryReq.setQuery("name:*");
        searchQuery.setQuery(queryReq);
        
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("APATH","1"));
        facetFields.setFacets(list);
        searchQuery.setFacetFields(facetFields);
        SearchResponse response =  query(searchQuery);
        RestResultBucketsModel fresponse = response.getContext().getFacetsFields().get(0);
        Assert.assertEquals(4,fresponse.getBuckets().size());
        fresponse.getBuckets().get(0).assertThat().field("label").contains("1/");
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    public void searchLevel2() throws Exception
    {
        String queryString = "name:"+ "cars";
        
        SearchRequest searchQuery = new SearchRequest();
        RestRequestQueryModel queryReq =  new RestRequestQueryModel();
        queryReq.setQuery(queryString);
        searchQuery.setQuery(queryReq);
        
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("APATH","1/"));
        facetFields.setFacets(list);
        searchQuery.setFacetFields(facetFields);
        
        SearchResponse response =  query(searchQuery);
        
        RestResultBucketsModel fresponse = response.getContext().getFacetsFields().get(0);
        String path = fresponse.getBuckets().get(0).getLabel().replace("1/", "2/");
        list.remove(0);
        list.add(new RestRequestFacetFieldModel("APATH", path));
        
        facetFields.setFacets(list);
        searchQuery.setFacetFields(facetFields);
        response =  query(searchQuery);
        fresponse = response.getContext().getFacetsFields().get(0);
        Assert.assertTrue(fresponse.getBuckets().size() >= 1);
        fresponse.getBuckets().get(0).assertThat().field("label").contains("2/");
        fresponse.getBuckets().get(0).assertThat().field("label").contains(path);
        /**
         * To return the contents of the path and its sub directory change the prefix
         * Below 2/path/path -> will return whats in path/path only
         * if 3/path/path -> will return contents from path/path/path
         * @throws Exception
         */
        searchQuery = new SearchRequest();
        queryReq =  new RestRequestQueryModel();
        queryReq.setQuery(queryString);
        searchQuery.setQuery(queryReq);
        facetFields = new RestRequestFacetFieldsModel();
        list.remove(0);
        list.add(new RestRequestFacetFieldModel("APATH",path.replace("2/", "3/")));
        facetFields.setFacets(list);
        searchQuery.setFacetFields(facetFields);
        response =  query(searchQuery);
        fresponse = response.getContext().getFacetsFields().get(0);
        Assert.assertTrue(fresponse.getBuckets().size() >= 1);
        fresponse.getBuckets().get(0).assertThat().field("label").contains("3/");
    }
}
