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
package org.alfresco.test.search.functional.searchServices.search;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRequestRangesModel;
import org.alfresco.rest.search.Pagination;
import org.alfresco.rest.search.RestGenericBucketModel;
import org.alfresco.rest.search.RestGenericFacetResponseModel;
import org.alfresco.rest.search.RestRequestFacetFieldModel;
import org.alfresco.rest.search.RestRequestFacetFieldsModel;
import org.alfresco.rest.search.RestRequestPivotModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.search.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Faceted search test.
 * @author Gethin James
 *
 */
public class PivotFacetedSearchTest extends AbstractSearchServicesE2ETest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
        waitForContentIndexing(file4.getContent(), true);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 },
              executionType = ExecutionType.REGRESSION,
              description = "Checks errors with pivot using Search api")
    public void searchWithPivotingErrors()
    {
        SearchRequest query = createQuery("cars");

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

        query(query);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "pivot key"));

        pivots.setKey("none_like_this");
        
        query(query);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError().containsSummary("invalid argument was received")
                    .containsSummary("Pivot parameter none_like_this does not reference");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 },
              executionType = ExecutionType.REGRESSION,
              description = "Checks with pivot using Search api")
    public void searchWithPivoting()
    {
        SearchRequest query = createQuery("cars");

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

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 },
              executionType = ExecutionType.REGRESSION,
              description = "Checks nested pivot using Search api")
    public void searchWithNestedPivoting()
    {
        SearchRequest query = createQuery("cars");

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        RestRequestFacetFieldModel restRequestFacetFieldModel = new RestRequestFacetFieldModel("SITE");
        restRequestFacetFieldModel.setLabel("site");
        list.add(restRequestFacetFieldModel);
        list.add(new RestRequestFacetFieldModel("creator"));
        list.add(new RestRequestFacetFieldModel("modifier"));
        facetFields.setFacets(list);
        query.setFacetFields(facetFields);
        query.setIncludeRequest(false);

        List<RestRequestPivotModel> pivotModelList = new ArrayList<>();
        RestRequestPivotModel sitepivots = new RestRequestPivotModel();
        sitepivots.setKey("site");
        RestRequestPivotModel creatorpivot = new RestRequestPivotModel();
        creatorpivot.setKey("creator");
        RestRequestPivotModel modifierpivot = new RestRequestPivotModel();
        modifierpivot.setKey("modifier");
        sitepivots.setPivots(Arrays.asList(creatorpivot, modifierpivot));
        pivotModelList.add(sitepivots);
        query.setPivots(pivotModelList);

        query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                    .assertLastError().containsSummary("invalid argument was received")
                    .containsSummary("Currently only 1 nested pivot is supported, you have 2");

        pivotModelList = new ArrayList<>();
        sitepivots = new RestRequestPivotModel();
        sitepivots.setKey("site");
        sitepivots.setPivots(Collections.singletonList(modifierpivot));
        pivotModelList.add(sitepivots);
        pivotModelList.add(creatorpivot);
        query.setPivots(pivotModelList);

        SearchResponse response = query(query);
        assertPivotResponse(response, "SITE", "site");

        RestGenericFacetResponseModel siteResponse = response.getContext().getFacets().get(0);
        RestGenericBucketModel bucket = siteResponse.getBuckets().get(0);
        RestGenericFacetResponseModel modifiedResponse = bucket.getFacets().get(0);
        modifiedResponse.assertThat().field("type").is("pivot");
        modifiedResponse.assertThat().field("label").is("modifier");

        RestGenericFacetResponseModel creatorResponse = response.getContext().getFacets().get(1);
        creatorResponse.assertThat().field("type").is("pivot");
        creatorResponse.assertThat().field("label").is("creator");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 },
              executionType = ExecutionType.REGRESSION,
              description = "Checks range pivots using Search api")
    public void searchWithRangePivoting()
    {
        SearchRequest query = createQuery("cars");
        
        String endDate = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")); //the car document is created at runtime, so to include it in range facets end date must be now

        Pagination pagination = new Pagination();
        pagination.setMaxItems(2);
        query.setPaging(pagination);
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("creator"));
        facetFields.setFacets(list);
        query.setFacetFields(facetFields);
        query.setIncludeRequest(false);

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("created");
        facetRangeModel.setStart("2015-09-29T10:45:15.729Z");
        facetRangeModel.setEnd(endDate);
        facetRangeModel.setGap("+280DAY");
        facetRangeModel.setLabel("aRange");
        List<RestRequestRangesModel> ranges = new ArrayList<>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);

        List<RestRequestPivotModel> pivotModelList = new ArrayList<>();
        RestRequestPivotModel creatorpivot = new RestRequestPivotModel();
        creatorpivot.setKey("creator");
        RestRequestPivotModel rangepivot = new RestRequestPivotModel();
        rangepivot.setKey("aRange");
        creatorpivot.setPivots(Collections.singletonList(rangepivot));
        pivotModelList.add(creatorpivot);
        query.setPivots(pivotModelList);

        SearchResponse response =  query(query);
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(1);
        facetResponseModel.assertThat().field("type").is("pivot");
        facetResponseModel.assertThat().field("label").is("creator");
        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        RestGenericFacetResponseModel rangeResponse = bucket.getFacets().get(0);
        rangeResponse.assertThat().field("type").is("range");
    }

    private void assertPivotResponse(SearchResponse response, String field, String alabel)
    {
        String label = alabel!=null?alabel:field;
        response.getContext().assertThat().field("facetsFields").isNull();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);
        facetResponseModel.assertThat().field("type").is("pivot");
        facetResponseModel.assertThat().field("label").is(label);
        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").isNotEmpty();
        bucket.assertThat().field("filterQuery").is(field+":\""+bucket.getLabel()+"\"");
        Assert.assertEquals("count", bucket.getMetrics().get(0).getType());
        Assert.assertTrue(bucket.getMetrics().get(0).getValue().toString().contains("{count="));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 },
              executionType = ExecutionType.REGRESSION,
              description = "Checks with pivot using Search api and a label as a key")
    public void searchWithPivotingUsingLabel()
    {
        SearchRequest query = createQuery("cars");
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
        RestGenericFacetResponseModel labelResponseModel = response.getContext().getFacets().get(1);
        labelResponseModel.assertThat().field("type").is("pivot");
        labelResponseModel.assertThat().field("label").isNotEmpty();
        labelResponseModel.assertThat().field("label").is("aLabel");
        RestGenericBucketModel bucket = labelResponseModel.getBuckets().get(0);
        bucket.assertThat().field("filterQuery").is("modifier:\""+bucket.getLabel()+"\"");
        Assert.assertEquals("count", bucket.getMetrics().get(0).getType());
        Assert.assertTrue(bucket.getMetrics().get(0).getValue().toString().contains("{count="));
    }
}
