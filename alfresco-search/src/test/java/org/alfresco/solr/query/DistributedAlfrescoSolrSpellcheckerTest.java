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

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedAlfrescoSolrSpellcheckerTest extends AbstractAlfrescoDistributedTest
{
    @Rule
    public JettyServerRule jetty = new JettyServerRule(2);

    @Test
    public void testSpellcheckerOutputFormat() throws Exception
    {
        index(getDefaultTestClient(), true, "id", "1",  "suggest", "YYYYYYY BBBBBBB", "_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYYYYY BBBBBBB");
        index(getDefaultTestClient(), true, "id", "2",  "suggest", "AAAAAAAA", "_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "AAAAAAAA");
        index(getDefaultTestClient(), true, "id", "3",  "suggest", "BBBBBBB", "_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "BBBBBBB");
        index(getDefaultTestClient(), true, "id", "4",  "suggest", "CCCC", "_version_","0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "CCCC");
        index(getDefaultTestClient(), true, "id", "5", "suggest", "YYYYYYY", "_version_", "0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYYYYY BBBBBBB");
        index(getDefaultTestClient(), true, "id", "6", "suggest", "EEEE", "_version_", "0", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "EEEE");
        commit(getDefaultTestClient(), true);

        putHandleDefaults();
        handle.put("spellcheck-extras", SKIP); // No longer used can be removed in Solr 6.

        QueryResponse response = query(getDefaultTestClient(), true,
                "{\"query\":\"(YYYYY BBBBB AND (id:(1 2 3 4 5 6)))\",\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
                params("spellcheck.q", "YYYYY BBBBB", "qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "100", "spellcheck", "true"));

        NamedList res = response.getResponse();
        NamedList spellcheck = (NamedList)res.get("spellcheck");
        NamedList suggestions = (NamedList)spellcheck.get("suggestions"); // Solr 4 format
        NamedList collation = (NamedList)suggestions.getVal(2); // The third suggestion should be collation in the Solr format.
        String collationQuery = (String)collation.get("collationQuery");
        String collationQueryString = (String)collation.get("collationQueryString");
        int hits = (int)collation.get("hits");
        assertTrue(hits == 3);
        assertTrue(collationQuery.equals("(yyyyyyy bbbbbbb AND (id:(1 2 3 4 5 6)))"));
        assertTrue(collationQueryString.equals("yyyyyyy bbbbbbb"));
    }
}

