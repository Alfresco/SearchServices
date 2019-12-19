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

import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

/**
 * https://issues.alfresco.com/jira/browse/SEARCH-2012
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoSolrSortIT extends AbstractAlfrescoDistributedIT
{
    @Rule
    public JettyServerRule jetty = new JettyServerRule(1, this);

    @Test
    public void AlfrescoCollatableFieldType_emptyValuesSortingAsc__shouldBeRankedFirst() throws Exception {
        prepareIndexSegmentWithAllNonNullFieldValues("text@s__sort@{http://www.alfresco.org/model/content/1.0}title");
        putHandleDefaults();
        // Docs with id 1, 3, 5 and 6 should be first (note that these will be sorted by indexing time).
        String[] expectedRanking = new String[]{"1","3","5","6","4","2"};
        
        QueryResponse response = query(getDefaultTestClient(), true,
                "{\"query\":\"(id:(1 2 3 4 5 6))\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
                params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "100", "sort", "text@s__sort@{http://www.alfresco.org/model/content/1.0}title asc"));

        NamedList res = response.getResponse();
        SolrDocumentList searchResults = (SolrDocumentList)res.get("response");
        for(int i=0;i<searchResults.size();i++){
            assertThat(searchResults.get(i).get("id"),is(expectedRanking[i]));
        }
    }

    @Test
    public void AlfrescoMLCollatableFieldType_emptyValuesSortingDesc_shouldRankThemLast() throws Exception {
        prepareIndexSegmentWithAllNonNullFieldValues("mltext@m__sort@{http://www.alfresco.org/model/content/1.0}title");

        putHandleDefaults();
        // Docs with id 1, 3, 5 and 6 should be last (note that these will be sorted by indexing time).
        String[] expectedRanking = new String[]{"2","4","1","3","5","6"};

        QueryResponse response = query(getDefaultTestClient(), true,
                "{\"query\":\"(id:(1 2 3 4 5 6))\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
                params("qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "100", "sort", "mltext@m__sort@{http://www.alfresco.org/model/content/1.0}title desc"));

        NamedList res = response.getResponse();
        SolrDocumentList searchResults = (SolrDocumentList)res.get("response");
        for(int i=0;i<searchResults.size();i++){
            assertThat(searchResults.get(i).get("id"),is(expectedRanking[i]));
        }
    }

    /** Create six documents where the field is null for docs with id 1, 3, 5 and 6, and populated for docs with id 2 and 4. */
    private void prepareIndexSegmentWithAllNonNullFieldValues(String field) throws Exception {
        index(getDefaultTestClient(), true, "id", "1", "_version_", "0", field, "");
        index(getDefaultTestClient(), true, "id", "2", "_version_", "0", field, "B");
        index(getDefaultTestClient(), true, "id", "3", "_version_", "0", field, "");
        index(getDefaultTestClient(), true, "id", "4", "_version_", "0", field, "A");
        index(getDefaultTestClient(), true, "id", "5", "_version_", "0", field, "");
        index(getDefaultTestClient(), true, "id", "6", "_version_", "0", field, "");
        commit(getDefaultTestClient(), true);
    }
}

