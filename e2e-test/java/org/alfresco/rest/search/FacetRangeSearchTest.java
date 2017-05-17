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

import java.util.Map;

import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRequestRangeModel;
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

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check facet intervals mandatory fields")
    public void checkingFacetsMandatoryErrorMessages()throws Exception
    {
        SearchRequest query = carsQuery();

        RestRequestRangeModel facetRangeModel = new RestRequestRangeModel();
        query.setRange(facetRangeModel);

        SearchResponse response = query(query);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "field"));
        facetRangeModel.setField("content.size");
        
        query.setRange(facetRangeModel);
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "start"));
        facetRangeModel.setStart("0");

        query.setRange(facetRangeModel);
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "end"));
        facetRangeModel.setEnd("400");
        query.setRange(facetRangeModel);
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

        RestRequestRangeModel facetRangeModel = new RestRequestRangeModel();
        facetRangeModel.setField("content.size");
        facetRangeModel.setStart("0");
        facetRangeModel.setEnd("500");
        facetRangeModel.setGap("200");
        query.setRange(facetRangeModel);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("0 - 200");
        bucket.assertThat().field("filterQuery").is("content.size:(0 TO 200)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=4}");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"0");
        Assert.assertEquals(info.get("to"),"200");
        Assert.assertEquals(info.get("count"),"4");
        
        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("200 - 400");
        bucket.assertThat().field("filterQuery").is("content.size:(200 TO 400)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=4}");
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"200");
        Assert.assertEquals(info.get("to"),"400");
        Assert.assertEquals(info.get("count"),"4");
        
        bucket = facetResponseModel.getBuckets().get(2);
        bucket.assertThat().field("label").is("400 - 600");
        bucket.assertThat().field("filterQuery").is("content.size:(400 TO 600)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=7}");
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"400");
        Assert.assertEquals(info.get("to"),"600");
        Assert.assertEquals(info.get("count"),"7");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check date facet intervals search api")
    public void searchWithRangeHardend()throws Exception
    {
        SearchRequest query = createQuery("A*");

        RestRequestRangeModel facetRangeModel = new RestRequestRangeModel();
        facetRangeModel.setField("content.size");
        facetRangeModel.setStart("0");
        facetRangeModel.setEnd("500");
        facetRangeModel.setGap("200");
        facetRangeModel.setHardend(true);
        query.setRange(facetRangeModel);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("0 - 200");
        bucket.assertThat().field("filterQuery").is("content.size:(0 TO 200)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=4}");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"0");
        Assert.assertEquals(info.get("to"),"200");
        Assert.assertEquals(info.get("count"),"4");
        
        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("200 - 400");
        bucket.assertThat().field("filterQuery").is("content.size:(200 TO 400)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=4}");
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"200");
        Assert.assertEquals(info.get("to"),"400");
        Assert.assertEquals(info.get("count"),"4");
        
        bucket = facetResponseModel.getBuckets().get(2);
        bucket.assertThat().field("label").is("400 - 500");
        bucket.assertThat().field("filterQuery").is("content.size:(400 TO 500)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=3}");
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"400");
        Assert.assertEquals(info.get("to"),"500");
        Assert.assertEquals(info.get("count"),"3");
    }
    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check date facet intervals search api")
    public void searchDateRange()throws Exception
    {
        SearchRequest query = createQuery("name:A*");

        RestRequestRangeModel facetRangeModel = new RestRequestRangeModel();
        facetRangeModel.setField("created");
        facetRangeModel.setStart("2015-09-29T10:45:15.729Z");
        facetRangeModel.setEnd("2016-09-29T10:45:15.729Z");
        facetRangeModel.setGap("+280DAY");
        query.setRange(facetRangeModel);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);
        
        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("2015-09-29T10:45:15.729Z - 2016-07-05T10:45:15.729Z");
        bucket.assertThat().field("filterQuery").is("created:(2015-09-29T10:45:15.729Z TO 2016-07-05T10:45:15.729Z)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=1}");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"2015-09-29T10:45:15.729Z");
        Assert.assertEquals(info.get("to"),"2016-07-05T10:45:15.729Z");
        Assert.assertEquals(info.get("count"),"1");
        
        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("2016-07-05T10:45:15.729Z - 2017-04-11T10:45:15.729Z");
        bucket.assertThat().field("filterQuery").is("created:(2016-07-05T10:45:15.729Z TO 2017-04-11T10:45:15.729Z)");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=0}");
        info = (Map<String, String>) bucket.getBucketInfo();
        Assert.assertEquals(info.get("from"),"2016-07-05T10:45:15.729Z");
        Assert.assertEquals(info.get("to"),"2017-04-11T10:45:15.729Z");
        Assert.assertEquals(info.get("count"),"0");
    }
}
