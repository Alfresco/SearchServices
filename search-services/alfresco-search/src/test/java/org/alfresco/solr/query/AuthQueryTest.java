/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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

import java.io.IOException;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.util.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.junit.Assert;
import org.junit.Test;

/**
 * Ported unit test from legacy AlfrescoCoreAdminTest, set of tests related
 * to authentication.
 * @author Michael Suzuki
 *
 */
public class AuthQueryTest extends AuthDataLoad
{
    @Test
    public void checkAuth()
            throws IOException, org.apache.lucene.queryparser.classic.ParseException
    {
        RefCounted<SolrIndexSearcher> refCounted = null;
        try
        {
            assertFTSQuery("TEXT:\"Test\" ", count);
            assertFTSQuery("TEXT:\"doc\"", count);
            assertFTSQuery("TEXT:\"number\"", count);

            //Assert that root, base folder,folder-0 and 100 documents are returned.
            assertFTSQuery("AUTHORITY:\"GROUP_EVERYONE\" AND "+QueryConstants.FIELD_DOC_TYPE+":"+SolrInformationServer.DOC_TYPE_NODE, 103);
            //Test data load adds lots of AUTHORITY readers by looping count -1
            assertFTSQuery("AUTHORITY:\"READER-1000\" AND "+QueryConstants.FIELD_DOC_TYPE+":"+SolrInformationServer.DOC_TYPE_NODE, 100);
            assertFTSQuery("AUTHORITY:\"READER-902\" AND "+QueryConstants.FIELD_DOC_TYPE+":"+SolrInformationServer.DOC_TYPE_NODE, 2);
            assertFTSQuery("AUTHORITY:\"READER-901\" AND "+QueryConstants.FIELD_DOC_TYPE+":"+SolrInformationServer.DOC_TYPE_NODE, 1);
            //Grouping boundary test that checks ... Andy can explain.
            buildAndRunAuthQuery(count, 8);
            buildAndRunAuthQuery(count, 9);
            buildAndRunAuthQuery(count, 10);
            buildAndRunAuthQuery(count, 98);
            buildAndRunAuthQuery(count, 99);
            buildAndRunAuthQuery(count, 100);
            buildAndRunAuthQuery(count, 998);
            buildAndRunAuthQuery(count, 999);
            buildAndRunAuthQuery(count, 1000);
            buildAndRunAuthQuery(count, 9998);
            buildAndRunAuthQuery(count, 9999);
            buildAndRunAuthQuery(count, 10000);
            buildAndRunAuthQuery(count, 10000);
            buildAndRunAuthQuery(count, 10000);
            buildAndRunAuthQuery(count, 20000);
            buildAndRunAuthQuery(count, 20000);
            buildAndRunAuthQuery(count, 20000);
        }
        finally
        {
            if (refCounted != null)
            {
                refCounted.decref();
            }
        }
    }

    /**
     * Queries the index and asserts if the count matches documents returned.
     * @param queryString
     * @param count
     * @throws IOException
     * @throws org.apache.lucene.queryparser.classic.ParseException
     */
    private void assertFTSQuery(String queryString,
                              int count,
                              String... name) throws IOException, ParseException
    {
        SolrServletRequest solrQueryRequest = null;
        RefCounted<SolrIndexSearcher>refCounted = null;
        try
        {
            solrQueryRequest = new SolrServletRequest(h.getCore(), null);
            refCounted = h.getCore().getSearcher(false, true, null);
            SolrIndexSearcher solrIndexSearcher = refCounted.get();
            
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.setQuery(queryString);
            Query query = dataModel.getFTSQuery(new Pair<SearchParameters, Boolean>(searchParameters, Boolean.FALSE),
                    solrQueryRequest, FTSQueryParser.RerankPhase.SINGLE_PASS);
            System.out.println("##################### Query:"+query);
            TopDocs docs = solrIndexSearcher.search(query, count * 2 + 10);
        
            Assert.assertEquals(count, docs.totalHits);
        } 
        finally
        {
            refCounted.decref();
            solrQueryRequest.close();
        }
    }
    private void buildAndRunAuthQuery(long count, int loop) throws IOException, ParseException
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= loop; i++)
        {
            if (i % 100 == 1)
            {
                if (i == 1)
                {
                    builder.append("|AUTHORITY:\"");
                }
                else
                {
                    builder.append("\" |AUTHORITY:\"");
                }
            }
            builder.append("|READER-" + i);
        }
        builder.append("\"");
        assertFTSQuery(builder.toString(), 0,"Auth-" + loop);
    }
}
