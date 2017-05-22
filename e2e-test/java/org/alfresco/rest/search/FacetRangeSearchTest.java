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
import java.util.Map;

import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRequestRangesModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Faceted Range Search Query for numeric range
 * {
 *   "query": {
 *       "query": "name:A*"
 *   },
 *   "range": {
 *       "field": "content.size",
 *        "start": "0",
 *        "end": "400",
 *        "gap": "100"
 *   }
 * }
 * Date range query:
 * {
 *  "query": {
 *      "query": "name:A*"
 *  },
 *  "range": {
 *      "field": "created",
 *       "start": "2015-09-29T10:45:15.729Z",
 *       "end": "2016-09-29T10:45:15.729Z",
 *       "gap": "+100DAY"
 *  }
 * }
 * @author Michael Suzuki
 *
 */
public class FacetRangeSearchTest extends AbstractSearchTest
{
    @Override
    public void dataPreparation() throws Exception
    {
        //Skip setup
    }
    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check facet intervals mandatory fields")
    public void checkingFacetsMandatoryErrorMessages()throws Exception
    {
        SearchRequest query = carsQuery();
        List<RestRequestRangesModel> ranges = new ArrayList<RestRequestRangesModel>();
        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "field"));
        ranges.clear();
        facetRangeModel.setField("content.size");
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "start"));
        facetRangeModel.setStart("0");
        ranges.clear();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "end"));
        facetRangeModel.setEnd("400");
        query.setRanges(ranges);
        ranges.clear();
        ranges.add(facetRangeModel);
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "gap"));
        
        facetRangeModel.setGap("100");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check basic facet range search api")
    public void searchWithRange()throws Exception
    {
        SearchRequest query = createQuery("A*");

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("content.size");
        facetRangeModel.setStart("0");
        facetRangeModel.setEnd("500");
        facetRangeModel.setGap("200");
        List<RestRequestRangesModel> ranges = new ArrayList<RestRequestRangesModel>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("0 - 200");
        bucket.assertThat().field("filterQuery").is("content.size:(0 TO 200)");
        Map<String,String> metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        Assert.assertTrue(new Integer(metric.get("count")) > 4);
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"0");
        Assert.assertEquals(info.get("end"),"200");
        Assert.assertTrue(!info.get("count").isEmpty());
        
        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("200 - 400");
        bucket.assertThat().field("filterQuery").is("content.size:(200 TO 400)");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        Integer count = new Integer(metric.get("count"));
        Assert.assertTrue(count >= 4);
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"200");
        Assert.assertEquals(info.get("end"),"400");
        
        bucket = facetResponseModel.getBuckets().get(2);
        bucket.assertThat().field("label").is("400 - 600");
        bucket.assertThat().field("filterQuery").is("content.size:(400 TO 600)");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        Assert.assertTrue(new Integer(metric.get("count")) >= 7);
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"400");
        Assert.assertEquals(info.get("end"),"600");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check date facet intervals search api")
    public void searchWithRangeHardend()throws Exception
    {
        SearchRequest query = createQuery("A*");

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("content.size");
        facetRangeModel.setStart("0");
        facetRangeModel.setEnd("500");
        facetRangeModel.setGap("200");
        facetRangeModel.setHardend(true);
        List<RestRequestRangesModel> ranges = new ArrayList<RestRequestRangesModel>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("0 - 200");
        bucket.assertThat().field("filterQuery").is("content.size:(0 TO 200)");
        Map<String,String> metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        Assert.assertTrue(new Integer(metric.get("count")) >= 4);
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"0");
        Assert.assertEquals(info.get("end"),"200");
        Assert.assertTrue(!info.get("count").isEmpty());
        
        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("200 - 400");
        bucket.assertThat().field("filterQuery").is("content.size:(200 TO 400)");
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"200");
        Assert.assertEquals(info.get("end"),"400");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        Assert.assertTrue(new Integer(metric.get("count")) >= 4);
        Assert.assertTrue(!info.get("count").isEmpty());
        
        bucket = facetResponseModel.getBuckets().get(2);
        bucket.assertThat().field("label").is("400 - 500");
        bucket.assertThat().field("filterQuery").is("content.size:(400 TO 500)");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        Assert.assertTrue(new Integer(metric.get("count")) >= 3);
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"400");
        Assert.assertEquals(info.get("end"),"500");
        Assert.assertTrue(!info.get("count").isEmpty());
    }
    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check date facet intervals search api")
    public void searchDateRange()throws Exception
    {
        SearchRequest query = createQuery("name:A*");

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("created");
        facetRangeModel.setStart("2015-09-29T10:45:15.729Z");
        facetRangeModel.setEnd("2016-09-29T10:45:15.729Z");
        facetRangeModel.setGap("+280DAY");
        List<RestRequestRangesModel> ranges = new ArrayList<RestRequestRangesModel>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);
        
        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("2015-09-29T10:45:15.729Z - 2016-07-05T10:45:15.729Z");
        bucket.assertThat().field("filterQuery").is("created:(2015-09-29T10:45:15.729Z TO 2016-07-05T10:45:15.729Z)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=1}");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"2015-09-29T10:45:15.729Z");
        Assert.assertEquals(info.get("end"),"2016-07-05T10:45:15.729Z");
        Assert.assertEquals(info.get("count"),"1");
        
        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("2016-07-05T10:45:15.729Z - 2017-04-11T10:45:15.729Z");
        bucket.assertThat().field("filterQuery").is("created:(2016-07-05T10:45:15.729Z TO 2017-04-11T10:45:15.729Z)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=0}");
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("start"),"2016-07-05T10:45:15.729Z");
        Assert.assertEquals(info.get("end"),"2017-04-11T10:45:15.729Z");
        Assert.assertEquals(info.get("count"),"0");
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check date facet intervals search api")
    public void searchDateAndSizeRanges()throws Exception
    {
        SearchRequest query = createQuery("name:A*");
        List<RestRequestRangesModel> ranges = new ArrayList<RestRequestRangesModel>();
        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("created");
        facetRangeModel.setStart("2015-09-29T10:45:15.729Z");
        facetRangeModel.setEnd("2016-09-29T10:45:15.729Z");
        facetRangeModel.setGap("+280DAY");
        ranges.add(facetRangeModel);
        RestRequestRangesModel facetCountRangeModel = new RestRequestRangesModel();
        facetCountRangeModel.setField("content.size");
        facetCountRangeModel.setStart("0");
        facetCountRangeModel.setEnd("500");
        facetCountRangeModel.setGap("200");
        ranges.add(facetCountRangeModel);
        query.setRanges(ranges);
    }
}
