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

import org.alfresco.solr.AlfrescoBaseDistributedSearchTestCase;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;

/**
 * @author Joel
 */

@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedAlfrescoFTSQParserPluginTest extends AlfrescoBaseDistributedSearchTestCase
{
    public DistributedAlfrescoFTSQParserPluginTest()
    {
        super();
        fixShardCount(2);
        schemaString = "schema-afts.xml";
        configString = "solrconfig-afts.xml";
    }

    public void doTest() throws Exception
    {
        Thread.sleep(20000); // Allow model trackers to start.
        del("*:*");
        index_specific(0, "id", "1", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
        index_specific(0, "id", "2", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
        index_specific(1, "id", "3", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
        index_specific(1, "id", "4", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
        commit();

        handle.put("explain", SKIPVAL);
        handle.put("timestamp", SKIPVAL);
        handle.put("score", SKIPVAL);
        handle.put("wt", SKIP);
        handle.put("distrib", SKIP);
        handle.put("shards.qt", SKIP);
        handle.put("shards", SKIP);
        handle.put("q", SKIP);
        handle.put("maxScore", SKIPVAL);
        handle.put("_version_", SKIP);

        query("{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                params("q", "t1:YYYY", "qt", "/afts", "shards.qt","/afts","start", "0", "rows", "6", "sort", "id asc"));

    }
}

