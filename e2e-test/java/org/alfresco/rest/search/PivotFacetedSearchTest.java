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

import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Faceted search test.
 * @author Gethin James
 *
 */
public class PivotFacetedSearchTest extends AbstractSearchTest
{

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH  }, executionType = ExecutionType.REGRESSION,
              description = "Checks errors with pivot using Search api")
    public void searchWithPivotingErrors() throws Exception
    {
        SearchRequest query = carsQuery();

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("'creator'"));
        facetFields.setFacets(list);
        query.setFacetFields(facetFields);
        query.setIncludeRequest(false);
        List<RestRequestPivotModel> pivotModelList = new ArrayList<>();
        RestRequestPivotModel pivots = new RestRequestPivotModel();
        pivotModelList.add(pivots);
        query.setPivots(pivotModelList);
        
        SearchResponse response =  query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "pivot key"));


        pivots.setKey("none_like_this");
        response =  query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary("Pivot parameter none_like_this is does not reference a facet Field");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH  }, executionType = ExecutionType.REGRESSION,
              description = "Checks with pivot using Search api")
    public void searchWithPivoting() throws Exception
    {
        SearchRequest query = carsQuery();

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("creator"));
        facetFields.setFacets(list);
        query.setFacetFields(facetFields);
        query.setIncludeRequest(false);

        SearchResponse response =  query(query);
        response.getContext().assertThat().field("facetsFields").isNotNull();

        List<RestRequestPivotModel> pivotModelList = new ArrayList<>();
        RestRequestPivotModel pivots = new RestRequestPivotModel();
        pivots.setKey("creator");
        pivotModelList.add(pivots);
        query.setPivots(pivotModelList);
        response =  query(query);

        //Pivot key has matched facet field so there is no longer a facet fields response
        assertPivotResponse(response, "creator", null);
    }

    private void assertPivotResponse(SearchResponse response, String field, String alabel) throws Exception
    {
        String label = alabel!=null?alabel:field;
        response.getContext().assertThat().field("facetsFields").isNull();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);
        facetResponseModel.assertThat().field("type").is("pivot");
        facetResponseModel.assertThat().field("label").is(label);
        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").isNotEmpty();
        bucket.assertThat().field("filterQuery").is(field+":"+bucket.getLabel());
        Assert.assertEquals("count", bucket.getMetrics().get(0).getType());
        Assert.assertTrue(bucket.getMetrics().get(0).getValue().toString().contains("{count="));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH  }, executionType = ExecutionType.REGRESSION,
              description = "Checks with pivot using Search api and a label as a key")
    public void searchWithPivotingUsingLabel() throws Exception
    {
        SearchRequest query = carsQuery();
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        RestRequestFacetFieldModel creatorFacetFieldModel = new RestRequestFacetFieldModel("creator");
        creatorFacetFieldModel.setLabel("create");
        list.add(creatorFacetFieldModel);
        RestRequestFacetFieldModel restRequestFacetFieldModel = new RestRequestFacetFieldModel("modifier");
        restRequestFacetFieldModel.setLabel("aLabel");
        list.add(restRequestFacetFieldModel);
        facetFields.setFacets(list);
        query.setFacetFields(facetFields);
        query.setIncludeRequest(false);
        RestRequestPivotModel pivots = new RestRequestPivotModel();
        pivots.setKey("create");
        RestRequestPivotModel pivotmod = new RestRequestPivotModel();
        pivotmod.setKey("aLabel");

        List<RestRequestPivotModel> pivotModelList = new ArrayList<>();
        pivotModelList.add(pivots);
        pivotModelList.add(pivotmod);
        query.setPivots(pivotModelList);
        SearchResponse response =  query(query);
        assertPivotResponse(response, "creator", "create");

        //Now check the nesting
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);
        RestGenericFacetResponseModel nestedFacet = facetResponseModel.getBuckets().get(0).getFacets().get(0);
        RestGenericBucketModel bucket = nestedFacet.getBuckets().get(0);
        nestedFacet.assertThat().field("label").isNotEmpty();
        nestedFacet.assertThat().field("label").is("aLabel");
        bucket.assertThat().field("filterQuery").is("modifier:"+bucket.getLabel());
        Assert.assertEquals("count", bucket.getMetrics().get(0).getType());
        Assert.assertTrue(bucket.getMetrics().get(0).getValue().toString().contains("{count="));
    }
}
