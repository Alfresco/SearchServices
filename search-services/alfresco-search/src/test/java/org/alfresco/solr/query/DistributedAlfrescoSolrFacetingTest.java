/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.solr.query;

import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.AbstractAlfrescoDistributedTestStatic;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.util.NamedList;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;

@SolrTestCaseJ4.SuppressSSL @LuceneTestCase.SuppressCodecs({ "Appending", "Lucene3x", "Lucene40", "Lucene41",
    "Lucene42", "Lucene43", "Lucene44", "Lucene45", "Lucene46", "Lucene47", "Lucene48",
    "Lucene49" }) public class DistributedAlfrescoSolrFacetingTest extends AbstractAlfrescoDistributedTestStatic
{
    @BeforeClass
    private static void initData() throws Throwable
    {
        initSolrServers(2, "AdminHandlerDistributedTest", null);
        indexSampleDocumentsForFacetingMincount();
        
    }

    @AfterClass
    private static void destroyData() throws Throwable
    {
        dismissSolrServers();
    }

    @Test 
    public void distributedSearch_fieldFacetingRequiringRefinement_shouldReturnCorrectCounts() throws Exception
    {
        /*
         * The way this index is built is to have a correct distributed faceting count only after refining happens.
         * Given these params : facet.limit=2&facet.overrequest.count=0&facet.overrequest.ration=1
         * Initially you will get these counts:
         * Shard 0
         * a(2) b(2)
         * Shard 1
         * c(3) b(2)
         * We didn't get [c] from shard0 and [a] from shard1.
         * The refinement phase will ask those shards for those counts.
         * Only if refinement works we'll see the correct results (compared to not sharded Solr)
         * */
        index(getDefaultTestClient(), 0, "id", "10", "suggest", "b", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "a b");
        index(getDefaultTestClient(), 0, "id", "20", "suggest", "b", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "a");
        index(getDefaultTestClient(), 0, "id", "30", "suggest", "b", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "b c");
        index(getDefaultTestClient(), 1, "id", "40", "suggest", "b", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "c b");
        index(getDefaultTestClient(), 1, "id", "50", "suggest", "b", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "c b");
        index(getDefaultTestClient(), 1, "id", "60", "suggest", "b", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "c");
        commit(getDefaultTestClient(), true);
        String expectedFacetField = "{http://www.alfresco.org/model/content/1.0}content:[b (4), c (4)]";

        String jsonQuery = "{\"query\":\"(suggest:b)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}content", "facet.limit", "2",
                "facet.overrequest.count", "0", "facet.overrequest.ratio", "1"));

        List<FacetField> facetFields = queryResponse.getFacetFields();
        FacetField facetField = facetFields.get(0);
        Assert.assertThat(facetField.toString(), is(expectedFacetField));
    }

    @Test
    public void fieldFaceting_mincountMissing_shouldReturnFacetsMincountOne() throws Exception
    {
        String expectedContentFacetField = "{http://www.alfresco.org/model/content/1.0}content:[contenttwo (4), contentone (1)]";
        String expectedNameFacetField = "{http://www.alfresco.org/model/content/1.0}name:[nametwo (4), nameone (1)]";

        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}content","facet.field", "{http://www.alfresco.org/model/content/1.0}name"));

        List<FacetField> facetFields = queryResponse.getFacetFields();
        FacetField contentFacetField = facetFields.get(0);
        Assert.assertThat(contentFacetField.toString(), is(expectedContentFacetField));
        FacetField nameFacetField = facetFields.get(1);
        Assert.assertThat(nameFacetField.toString(), is(expectedNameFacetField));
    }

    @Test
    public void pivotFaceting_mincountMissing_shouldReturnFacetsMincountOne() throws Exception
    {
        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.pivot", "{http://www.alfresco.org/model/content/1.0}content,{http://www.alfresco.org/model/content/1.0}name"));

        NamedList<List<PivotField>> facetPivot = queryResponse.getFacetPivot();
        
        List<PivotField> firstLevelValues = facetPivot.getVal(0);
        Assert.assertThat(firstLevelValues.size(), is(2));
        PivotField firstLevelPivot0 = firstLevelValues.get(0);
        Assert.assertThat(firstLevelPivot0.getValue(), is("contenttwo"));
        Assert.assertThat(firstLevelPivot0.getCount(), is(4));
        
        List<PivotField> firstLevelPivot0Children = firstLevelPivot0.getPivot();
        Assert.assertThat(firstLevelPivot0Children.size(), is(1));
        PivotField secondLevelPivot0 = firstLevelPivot0Children.get(0);
        Assert.assertThat(secondLevelPivot0.getValue(), is("nametwo"));
        Assert.assertThat(secondLevelPivot0.getCount(), is(4));

        PivotField firstLevelPivot1 = firstLevelValues.get(1);
        Assert.assertThat(firstLevelPivot1.getValue(), is("contentone"));
        Assert.assertThat(firstLevelPivot1.getCount(), is(1));
        
        List<PivotField> firstLevelPivot1Children = firstLevelPivot1.getPivot();
        Assert.assertThat(firstLevelPivot1Children.size(), is(1));
        PivotField secondLevelPivot1 = firstLevelPivot1Children.get(0);
        Assert.assertThat(secondLevelPivot1.getValue(), is("nameone"));
        Assert.assertThat(secondLevelPivot1.getCount(), is(1));
    }
    
    @Test
    public void fieldFaceting_mincountSetZero_shouldReturnFacetsMincountOne() throws Exception
    {
        String expectedContentFacetField = "{http://www.alfresco.org/model/content/1.0}content:[contenttwo (4), contentone (1)]";
        String expectedNameFacetField = "{http://www.alfresco.org/model/content/1.0}name:[nametwo (4), nameone (1)]";

        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}content",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}name",
                "facet.mincount", "0"));

        List<FacetField> facetFields = queryResponse.getFacetFields();
        FacetField contentFacetField = facetFields.get(0);
        Assert.assertThat(contentFacetField.toString(), is(expectedContentFacetField));
        FacetField nameFacetField = facetFields.get(1);
        Assert.assertThat(nameFacetField.toString(), is(expectedNameFacetField));
    }

    @Test
    public void pivotFaceting_mincountSetZero_shouldReturnFacetsMincountOne() throws Exception
    {
        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.pivot", "{http://www.alfresco.org/model/content/1.0}content,{http://www.alfresco.org/model/content/1.0}name","facet.pivot.mincount","0"));

        NamedList<List<PivotField>> facetPivot = queryResponse.getFacetPivot();

        List<PivotField> firstLevelValues = facetPivot.getVal(0);
        Assert.assertThat(firstLevelValues.size(), is(2));
        PivotField firstLevelPivot0 = firstLevelValues.get(0);
        Assert.assertThat(firstLevelPivot0.getValue(), is("contenttwo"));
        Assert.assertThat(firstLevelPivot0.getCount(), is(4));

        List<PivotField> firstLevelPivot0Children = firstLevelPivot0.getPivot();
        Assert.assertThat(firstLevelPivot0Children.size(), is(1));
        PivotField secondLevelPivot0 = firstLevelPivot0Children.get(0);
        Assert.assertThat(secondLevelPivot0.getValue(), is("nametwo"));
        Assert.assertThat(secondLevelPivot0.getCount(), is(4));

        PivotField firstLevelPivot1 = firstLevelValues.get(1);
        Assert.assertThat(firstLevelPivot1.getValue(), is("contentone"));
        Assert.assertThat(firstLevelPivot1.getCount(), is(1));

        List<PivotField> firstLevelPivot1Children = firstLevelPivot1.getPivot();
        Assert.assertThat(firstLevelPivot1Children.size(), is(1));
        PivotField secondLevelPivot1 = firstLevelPivot1Children.get(0);
        Assert.assertThat(secondLevelPivot1.getValue(), is("nameone"));
        Assert.assertThat(secondLevelPivot1.getCount(), is(1));
    }

    @Test
    public void fieldFaceting_mincountSetTwo_shouldReturnFacetsOriginalMincount() throws Exception
    {
        String expectedContentFacetField = "{http://www.alfresco.org/model/content/1.0}content:[contenttwo (4)]";
        String expectedNameFacetField = "{http://www.alfresco.org/model/content/1.0}name:[nametwo (4)]";

        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}content",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}name",
                "facet.mincount", "2"));

        List<FacetField> facetFields = queryResponse.getFacetFields();
        FacetField contentFacetField = facetFields.get(0);
        Assert.assertThat(contentFacetField.toString(), is(expectedContentFacetField));
        FacetField nameFacetField = facetFields.get(1);
        Assert.assertThat(nameFacetField.toString(), is(expectedNameFacetField));
    }

    @Test
    public void pivotFaceting_mincountSetTwo_shouldReturnFacetsOriginalMincount() throws Exception
    {
        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.pivot", "{http://www.alfresco.org/model/content/1.0}content,{http://www.alfresco.org/model/content/1.0}name","facet.pivot.mincount","2"));

        NamedList<List<PivotField>> facetPivot = queryResponse.getFacetPivot();

        List<PivotField> firstLevelValues = facetPivot.getVal(0);
        Assert.assertThat(firstLevelValues.size(), is(1));
        PivotField firstLevelPivot0 = firstLevelValues.get(0);
        Assert.assertThat(firstLevelPivot0.getValue(), is("contenttwo"));
        Assert.assertThat(firstLevelPivot0.getCount(), is(4));

        List<PivotField> firstLevelPivot0Children = firstLevelPivot0.getPivot();
        Assert.assertThat(firstLevelPivot0Children.size(), is(1));
        PivotField secondLevelPivot0 = firstLevelPivot0Children.get(0);
        Assert.assertThat(secondLevelPivot0.getValue(), is("nametwo"));
        Assert.assertThat(secondLevelPivot0.getCount(), is(4));
    }
    

    @Test
    public void fieldFaceting_perFieldMincountSetZero_shoulReturnFacetsMincountOne() throws Exception
    {
        String expectedContentFacetField = "{http://www.alfresco.org/model/content/1.0}content:[contenttwo (4), contentone (1)]";
        String expectedNameFacetField = "{http://www.alfresco.org/model/content/1.0}name:[nametwo (4), nameone (1)]";

        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}content",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}name",
                "f.{http://www.alfresco.org/model/content/1.0}content.facet.mincount", "0",
                "f.{http://www.alfresco.org/model/content/1.0}name.facet.mincount", "0"));

        List<FacetField> facetFields = queryResponse.getFacetFields();
        FacetField contentFacetField = facetFields.get(0);
        Assert.assertThat(contentFacetField.toString(), is(expectedContentFacetField));
        FacetField nameFacetField = facetFields.get(1);
        Assert.assertThat(nameFacetField.toString(), is(expectedNameFacetField));
    }
    @Test
    public void fieldFaceting_perFieldMincountSetTwo_shoulReturnFacetsMincountTwo() throws Exception
    {
        String expectedContentFacetField = "{http://www.alfresco.org/model/content/1.0}content:[contenttwo (4)]";
        String expectedNameFacetField = "{http://www.alfresco.org/model/content/1.0}name:[nametwo (4), nameone (1)]";

        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}content",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}name",
                "f.{http://www.alfresco.org/model/content/1.0}content.facet.mincount", "2",
                "f.{http://www.alfresco.org/model/content/1.0}name.facet.mincount", "0"));

        List<FacetField> facetFields = queryResponse.getFacetFields();
        FacetField contentFacetField = facetFields.get(0);
        Assert.assertThat(contentFacetField.toString(), is(expectedContentFacetField));
        FacetField nameFacetField = facetFields.get(1);
        Assert.assertThat(nameFacetField.toString(), is(expectedNameFacetField));
    }

    @Test
    public void rangeFaceting_mincountMissing_shouldReturnFacetsMincountOne() throws Exception
    {
        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.range", "{http://www.alfresco.org/model/content/1.0}created",
                "facet.range.start", "2000-01-02T00:00:00Z",
                "facet.range.end", "2020-01-02T00:00:00Z",
                "facet.range.gap", "+100DAY"));
        
        //facet.distrib.mco=true
        List<RangeFacet> facetRanges = queryResponse.getFacetRanges();
        assertThat(facetRanges.size(),is(1));
        RangeFacet createdDateRangeFacet = facetRanges.get(0);
        List<RangeFacet.Count> createdDateCounts = createdDateRangeFacet.getCounts();
        
        assertThat(createdDateCounts.size(),is(4));
        assertThat(createdDateCounts.get(0).getValue(),is("2014-10-15T00:00:00Z"));
        assertThat(createdDateCounts.get(0).getCount(),is(2));

        assertThat(createdDateCounts.get(1).getValue(),is("2015-01-23T00:00:00Z"));
        assertThat(createdDateCounts.get(1).getCount(),is(1));

        assertThat(createdDateCounts.get(2).getValue(),is("2015-11-19T00:00:00Z"));
        assertThat(createdDateCounts.get(2).getCount(),is(1));

        assertThat(createdDateCounts.get(3).getValue(),is("2016-12-23T00:00:00Z"));
        assertThat(createdDateCounts.get(3).getCount(),is(1));
    }

    @Test
    public void rangeFaceting_mincountSetZero_shouldReturnFacetsMincountOne() throws Exception
    {
        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.range", "{http://www.alfresco.org/model/content/1.0}created",
                "facet.range.start", "2000-01-02T00:00:00Z",
                "facet.range.end", "2020-01-02T00:00:00Z",
                "facet.range.gap", "+100DAY",
                "facet.mincount", "0"));

        //facet.distrib.mco=true
        List<RangeFacet> facetRanges = queryResponse.getFacetRanges();
        assertThat(facetRanges.size(),is(1));
        RangeFacet createdDateRangeFacet = facetRanges.get(0);
        List<RangeFacet.Count> createdDateCounts = createdDateRangeFacet.getCounts();

        assertThat(createdDateCounts.size(),is(4));
        assertThat(createdDateCounts.get(0).getValue(),is("2014-10-15T00:00:00Z"));
        assertThat(createdDateCounts.get(0).getCount(),is(2));

        assertThat(createdDateCounts.get(1).getValue(),is("2015-01-23T00:00:00Z"));
        assertThat(createdDateCounts.get(1).getCount(),is(1));

        assertThat(createdDateCounts.get(2).getValue(),is("2015-11-19T00:00:00Z"));
        assertThat(createdDateCounts.get(2).getCount(),is(1));

        assertThat(createdDateCounts.get(3).getValue(),is("2016-12-23T00:00:00Z"));
        assertThat(createdDateCounts.get(3).getCount(),is(1));
    }

    @Test
    public void rangeFaceting_mincountSetTwo_shouldReturnFacetsMincountTwo() throws Exception
    {
        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "0", "fl", "score,id", "facet", "true",
                "facet.range", "{http://www.alfresco.org/model/content/1.0}created",
                "facet.range.start", "2000-01-02T00:00:00Z",
                "facet.range.end", "2020-01-02T00:00:00Z",
                "facet.range.gap", "+100DAY",
                "facet.mincount", "2"));

        //facet.distrib.mco=true
        List<RangeFacet> facetRanges = queryResponse.getFacetRanges();
        assertThat(facetRanges.size(),is(1));
        RangeFacet createdDateRangeFacet = facetRanges.get(0);
        List<RangeFacet.Count> createdDateCounts = createdDateRangeFacet.getCounts();

        assertThat(createdDateCounts.size(),is(1));
        assertThat(createdDateCounts.get(0).getValue(),is("2014-10-15T00:00:00Z"));
        assertThat(createdDateCounts.get(0).getCount(),is(2));
    }

    private static void indexSampleDocumentsForFacetingMincount() throws Exception
    {
        index(getDefaultTestClient(), 0, "id", "1", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "contentone",
            "text@s____@{http://www.alfresco.org/model/content/1.0}name", "nameone",
            "datetime@sd@{http://www.alfresco.org/model/content/1.0}created", "2015-01-02T00:00:00Z");
        index(getDefaultTestClient(), 0, "id", "2", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "contenttwo",
            "text@s____@{http://www.alfresco.org/model/content/1.0}name", "nametwo",
            "datetime@sd@{http://www.alfresco.org/model/content/1.0}created", "2015-01-02T00:00:00Z");
        index(getDefaultTestClient(), 0, "id", "3", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "contenttwo",
            "text@s____@{http://www.alfresco.org/model/content/1.0}name", "nametwo",
            "datetime@sd@{http://www.alfresco.org/model/content/1.0}created", "2015-02-01T00:00:00Z");
        index(getDefaultTestClient(), 1, "id", "4", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "contenttwo",
            "text@s____@{http://www.alfresco.org/model/content/1.0}name", "nametwo",
            "datetime@sd@{http://www.alfresco.org/model/content/1.0}created", "2016-02-01T00:00:00Z");
        index(getDefaultTestClient(), 1, "id", "5", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "contenttwo",
            "text@s____@{http://www.alfresco.org/model/content/1.0}name", "nametwo",
            "datetime@sd@{http://www.alfresco.org/model/content/1.0}created", "2017-02-01T00:00:00Z");
        index(getDefaultTestClient(), 1, "id", "6", "suggest", "c", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "contentthree",
            "text@s____@{http://www.alfresco.org/model/content/1.0}name", "namethree",
            "datetime@sd@{http://www.alfresco.org/model/content/1.0}created", "2017-02-01T00:00:00Z");
        commit(getDefaultTestClient(), true);
    }
}
