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
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;

@SolrTestCaseJ4.SuppressSSL @LuceneTestCase.SuppressCodecs({ "Appending", "Lucene3x", "Lucene40", "Lucene41",
    "Lucene42", "Lucene43", "Lucene44", "Lucene45", "Lucene46", "Lucene47", "Lucene48",
    "Lucene49" }) public class DistributedAlfrescoSolrFacetingTest extends AbstractAlfrescoDistributedTest
{
    @Rule 
    public JettyServerRule jetty = new JettyServerRule(2, this);

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
        index(getDefaultTestClient(), 0, "id", "1", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "a b");
        index(getDefaultTestClient(), 0, "id", "2", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "a");
        index(getDefaultTestClient(), 0, "id", "3", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "b c");
        index(getDefaultTestClient(), 1, "id", "4", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "c b");
        index(getDefaultTestClient(), 1, "id", "5", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "c b");
        index(getDefaultTestClient(), 1, "id", "6", "suggest", "a", "_version_", "0",
            "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "c");
        commit(getDefaultTestClient(), true);
        String expectedFacetField = "{http://www.alfresco.org/model/content/1.0}content:[b (4), c (4)]";

        String jsonQuery = "{\"query\":\"(suggest:a)\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}";
        putHandleDefaults();

        QueryResponse queryResponse = query(getDefaultTestClient(), true, jsonQuery,
            params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "100", "fl", "score,id", "facet", "true",
                "facet.field", "{http://www.alfresco.org/model/content/1.0}content", "facet.limit", "2",
                "facet.overrequest.count", "0", "facet.overrequest.ratio", "1"));

        List<FacetField> facetFields = queryResponse.getFacetFields();
        FacetField facetField = facetFields.get(0);
        Assert.assertThat(facetField.toString(), is(expectedFacetField));
    }
}
