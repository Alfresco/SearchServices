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

import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Faceted Intervals Search Test
 * @author Gethin James
 *
 */
public class FacetIntervalSearchTest extends AbstractSearchTest
{

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check facet intervals mandatory fields")
    public void checkingFacetsMandatoryErrorMessages()throws Exception
    {
        SearchRequest query = carsQuery();

        RestRequestFacetIntervalsModel facetIntervalsModel = new RestRequestFacetIntervalsModel();
        FacetInterval facetInterval = new FacetInterval(null, null, null);
        facetIntervalsModel.setIntervals(Arrays.asList(facetInterval));
        query.setFacetIntervals(facetIntervalsModel);

        SearchResponse response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "facetIntervals intervals field"));
        facetInterval.setField("created");
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_COLLECTION, "facetIntervals intervals sets"));

        RestRequestFacetSetModel restFacetSetModel = new RestRequestFacetSetModel();
        restFacetSetModel.setLabel("theRest");
        facetInterval.setSets(Arrays.asList(restFacetSetModel));
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "facetIntervals intervals created sets start"));

        restFacetSetModel.setStart("A");
        response = query(query);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                    .containsSummary(String.format(RestErrorModel.MANDATORY_PARAM, "facetIntervals intervals created sets end"));

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check basic facet intervals search api")
    public void searchWithBasicInterval()throws Exception
    {
        SearchRequest query = carsQuery();

        RestRequestFacetIntervalsModel facetIntervalsModel = new RestRequestFacetIntervalsModel();
        RestRequestFacetSetModel restRequestFacetSetModel = new RestRequestFacetSetModel();
        restRequestFacetSetModel.setStart("a");
        restRequestFacetSetModel.setEnd("user");
        restRequestFacetSetModel.setLabel("aUser");

        RestRequestFacetSetModel restFacetSetModel = new RestRequestFacetSetModel();
        restFacetSetModel.setStart("user");
        restFacetSetModel.setEnd("z");
        restFacetSetModel.setStartInclusive(false);
        restFacetSetModel.setLabel("theRest");

        FacetInterval facetInterval = new FacetInterval("creator", null, Arrays.asList(restRequestFacetSetModel, restFacetSetModel));
        facetIntervalsModel.setIntervals(Arrays.asList(facetInterval));
        query.setFacetIntervals(facetIntervalsModel);

        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facetIntervals").isNotEmpty();
        RestResultBucketsModel resultBucketsModel = response.getContext().getFacetIntervals().get(0);

        FacetFieldBucket bucket = resultBucketsModel.getBuckets().get(0);
        Assert.assertEquals(resultBucketsModel.getBuckets().size(), 2);
        bucket.assertThat().field("label").is("aUser");
        bucket.assertThat().field("count").isGreaterThan(1);
        bucket.assertThat().field("filterQuery").is("creator:[a,user]");

        bucket = resultBucketsModel.getBuckets().get(1);

        bucket.assertThat().field("label").is("theRest");
        bucket.assertThat().field("count").isLessThan(1);
        bucket.assertThat().field("filterQuery").is("creator:(user,z]");

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1 })
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1  }, executionType = ExecutionType.REGRESSION,
              description = "Check date facet intervals search api")
    public void searchWithDates() throws Exception
    {
        SearchRequest query = carsQuery();

        RestRequestFacetIntervalsModel facetIntervalsModel = new RestRequestFacetIntervalsModel();
        RestRequestFacetSetModel restRequestFacetSetModel = new RestRequestFacetSetModel();
        restRequestFacetSetModel.setStart("*");
        restRequestFacetSetModel.setEnd("2016");
        restRequestFacetSetModel.setEndInclusive(false);
        restRequestFacetSetModel.setLabel("Before2016");

        RestRequestFacetSetModel restFacetSetModel = new RestRequestFacetSetModel();
        restFacetSetModel.setStart("2016");
        restFacetSetModel.setEnd("now");
        restFacetSetModel.setLabel("From2016");

        FacetInterval facetInterval = new FacetInterval("cm:modified", "modified", Arrays.asList(restRequestFacetSetModel, restFacetSetModel));
        facetIntervalsModel.setIntervals(Arrays.asList(facetInterval));
        query.setFacetIntervals(facetIntervalsModel);

        SearchResponse response = query(query);
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facetIntervals").isNotEmpty();
        RestResultBucketsModel resultBucketsModel = response.getContext().getFacetIntervals().get(0);

        resultBucketsModel.assertThat().field("label").is("modified");
        FacetFieldBucket bucket = resultBucketsModel.getBuckets().get(0);
        Assert.assertEquals(resultBucketsModel.getBuckets().size(), 2);

        bucket.assertThat().field("label").is("From2016");
        bucket.assertThat().field("count").isGreaterThan(1);
        bucket.assertThat().field("filterQuery").is("cm:modified:[2016,now]");

        bucket = resultBucketsModel.getBuckets().get(1);

        bucket.assertThat().field("label").is("Before2016");
        bucket.assertThat().field("count").isLessThan(1);
        bucket.assertThat().field("filterQuery").is("cm:modified:[*,2016)");

    }

    private SearchRequest carsQuery()
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cars");
        query.setQuery(queryReq);
        return query;
    }
}
