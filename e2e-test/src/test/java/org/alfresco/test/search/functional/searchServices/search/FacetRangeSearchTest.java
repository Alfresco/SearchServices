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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRequestRangesModel;
import org.alfresco.rest.search.RestGenericBucketModel;
import org.alfresco.rest.search.RestGenericFacetResponseModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.search.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
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
public class FacetRangeSearchTest extends AbstractSearchServicesE2ETest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
        waitForContentIndexing(file4.getContent(), true);
    }

    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
            description = "Check facet intervals mandatory fields")
    public void checkingFacetsMandatoryErrorMessages()
    {
        SearchRequest query = createQuery("cars");
        List<RestRequestRangesModel> ranges = new ArrayList<>();
        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        query(query);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "field"));
        ranges.clear();
        facetRangeModel.setField("content.size");
        ranges.add(facetRangeModel);
        query.setRanges(ranges);

        query(query);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "start"));
        facetRangeModel.setStart("0");
        ranges.clear();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);

        query(query);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "end"));
        facetRangeModel.setEnd("400");
        query.setRanges(ranges);
        ranges.clear();
        ranges.add(facetRangeModel);

        query(query);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "gap"));

        facetRangeModel.setGap("100");
    }

    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
            description = "Check basic facet range search api")
    @SuppressWarnings("unchecked")
    public void searchWithRange()
    {
        SearchRequest query = createQuery("* AND SITE:'" + testSite.getId() + "'");

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("content.size");
        facetRangeModel.setStart("0");
        facetRangeModel.setEnd("200");
        facetRangeModel.setGap("20");
        List<RestRequestRangesModel> ranges = new ArrayList<>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("[20 - 40)");
        bucket.assertThat().field("filterQuery").is("content.size:[\"20\" TO \"40\">");
        Map<String,String> metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 2, "Unexpected count for first bucket.");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"20");
        assertEquals(info.get("end"),"40");
        assertNull(info.get("count"));
        assertEquals(info.get("startInclusive"),"true");
        assertEquals(info.get("endInclusive"),"false");

        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("[40 - 120)");
        bucket.assertThat().field("filterQuery").is("content.size:[\"40\" TO \"120\">");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 1, "Unexpected count for second bucket.");
        info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"40");
        assertEquals(info.get("end"),"120");
        assertEquals(info.get("startInclusive"),"true");
        assertEquals(info.get("endInclusive"),"false");

        bucket = facetResponseModel.getBuckets().get(2);
        bucket.assertThat().field("label").is("[120 - 200]");
        bucket.assertThat().field("filterQuery").is("content.size:[\"120\" TO \"200\"]");
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 1, "Unexpected count for third bucket.");
        info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"120");
        assertEquals(info.get("end"),"200");
        assertEquals(info.get("startInclusive"),"true");
        assertEquals(info.get("endInclusive"),"true");
    }

    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
            description = "Check date facet intervals search api")
    @SuppressWarnings("unchecked")
    public void searchWithRangeHardend()
    {
        SearchRequest query = createQuery("* AND SITE:'" + testSite.getId() + "'");

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("content.size");
        facetRangeModel.setStart("0");
        facetRangeModel.setEnd("200");
        facetRangeModel.setGap("20");
        facetRangeModel.setHardend(true);
        List<RestRequestRangesModel> ranges = new ArrayList<>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("[20 - 40)");
        bucket.assertThat().field("filterQuery").is("content.size:[\"20\" TO \"40\">");
        Map<String,String> metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 2, "Unexpected count for first bucket.");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"20");
        assertEquals(info.get("end"),"40");
        assertEquals(info.get("startInclusive"),"true");
        assertEquals(info.get("endInclusive"),"false");
        assertNull(info.get("count"));

        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("[40 - 120)");
        bucket.assertThat().field("filterQuery").is("content.size:[\"40\" TO \"120\">");
        info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"40");
        assertEquals(info.get("end"),"120");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 1, "Unexpected count for second bucket.");
        assertNull(info.get("count"));
        assertEquals(info.get("startInclusive"),"true");
        assertEquals(info.get("endInclusive"),"false");

        bucket = facetResponseModel.getBuckets().get(2);
        bucket.assertThat().field("label").is("[120 - 200]");
        bucket.assertThat().field("filterQuery").is("content.size:[\"120\" TO \"200\"]");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 1, "Unexpected count for third bucket.");
        info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"120");
        assertEquals(info.get("end"),"200");
        assertNull(info.get("count"));
        assertEquals(info.get("startInclusive"),"true");
        assertEquals(info.get("endInclusive"),"true");
    }

    /** This test relies on a document created in 2015 existing, probably part of the sample site. */
    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
            description = "Check date facet intervals search api")
    @SuppressWarnings("unchecked")
    public void searchDateRange()
    {
        SearchRequest query = createQuery("name:A*");

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("created");
        facetRangeModel.setStart("2015-09-29T10:45:15.729Z");
        facetRangeModel.setEnd("2016-09-29T10:45:15.729Z");
        facetRangeModel.setGap("+280DAY");
        List<RestRequestRangesModel> ranges = new ArrayList<>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        List<RestGenericBucketModel> buckets = facetResponseModel.getBuckets();
        assertThat(buckets.size(),is(1));

        RestGenericBucketModel bucket = buckets.get(0);
        bucket.assertThat().field("label").is("[2015-09-29T10:45:15.729Z - 2017-04-11T10:45:15.729Z]");
        bucket.assertThat().field("filterQuery").is("created:[\"2015-09-29T10:45:15.729Z\" TO \"2017-04-11T10:45:15.729Z\"]");
        bucket.getMetrics().get(0).assertThat().field("value").is("{count=1}");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"2015-09-29T10:45:15.729Z");
        assertEquals(info.get("end"),"2017-04-11T10:45:15.729Z");
        assertNull(info.get("count"),"1");
        assertEquals(info.get("startInclusive"),"true");
        assertEquals(info.get("endInclusive"),"true");
    }

    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
            description = "Check date facet intervals search api")
    public void searchDateAndSizeRanges()
    {
        SearchRequest query = createQuery("* AND SITE:'" + testSite.getId() + "'");
        List<RestRequestRangesModel> ranges = new ArrayList<>();
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

    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH}, executionType = ExecutionType.REGRESSION,
            description = "Check basic facet range search api")
    @SuppressWarnings("unchecked")
    public void searchWithRangeAndIncludeUpperBound()
    {
        SearchRequest query = createQuery("* AND SITE:'" + testSite.getId() + "'");

        RestRequestRangesModel facetRangeModel = new RestRequestRangesModel();
        facetRangeModel.setField("content.size");
        facetRangeModel.setStart("0");
        facetRangeModel.setEnd("200");
        facetRangeModel.setGap("20");
        List<String> include = new ArrayList<>();
        include.add("upper");
        facetRangeModel.setInclude(include);
        List<RestRequestRangesModel> ranges = new ArrayList<>();
        ranges.add(facetRangeModel);
        query.setRanges(ranges);
        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facets").isNotEmpty();
        RestGenericFacetResponseModel facetResponseModel = response.getContext().getFacets().get(0);

        RestGenericBucketModel bucket = facetResponseModel.getBuckets().get(0);
        bucket.assertThat().field("label").is("(20 - 40]");
        bucket.assertThat().field("filterQuery").is("content.size:<\"20\" TO \"40\"]");
        Map<String,String> metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 2, "Unexpected count for first bucket.");
        Map<String, String> info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"20");
        assertEquals(info.get("end"),"40");
        assertNull(info.get("count"));
        assertEquals(info.get("startInclusive"),"false");
        assertEquals(info.get("endInclusive"),"true");

        bucket = facetResponseModel.getBuckets().get(1);
        bucket.assertThat().field("label").is("(40 - 120]");
        bucket.assertThat().field("filterQuery").is("content.size:<\"40\" TO \"120\"]");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 1, "Unexpected count for second bucket.");
        info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"40");
        assertEquals(info.get("end"),"120");
        assertEquals(info.get("startInclusive"),"false");
        assertEquals(info.get("endInclusive"),"true");

        bucket = facetResponseModel.getBuckets().get(2);
        bucket.assertThat().field("label").is("(120 - 200]");
        bucket.assertThat().field("filterQuery").is("content.size:<\"120\" TO \"200\"]");
        metric = (Map<String, String>) bucket.getMetrics().get(0).getValue();
        assertEquals(Integer.valueOf(metric.get("count")).intValue(), 1, "Unexpected count for third bucket.");
        info = (Map<String, String>) bucket.getBucketInfo();
        assertEquals(info.get("start"),"120");
        assertEquals(info.get("end"),"200");
        assertEquals(info.get("startInclusive"),"false");
        assertEquals(info.get("endInclusive"),"true");
    }    
}