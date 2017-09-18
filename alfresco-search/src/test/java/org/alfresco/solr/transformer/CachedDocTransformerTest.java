/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
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
package org.alfresco.solr.transformer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.client.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.alfresco.solr.AlfrescoSolrUtils.*;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class CachedDocTransformerTest extends AbstractAlfrescoDistributedTest
{
    public static final String ALFRESCO_JSON = "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}";

    @Rule
    public JettyServerRule jetty = new JettyServerRule(1, this);
    
    @Test
    public void transformDocument() throws Exception 
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);

        indexAclChangeSet(aclChangeSet,
                list(acl),
                list(aclReaders));

        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        /*
        * Create and index transactions
        */

        //First create a transaction.
        int numNodes = 5;
        List<Node> nodes = new ArrayList();
        List<NodeMetaData> nodeMetaDatas = new ArrayList();

        Transaction bigTxn = getTransaction(0, numNodes);
        Date now = new Date();

        for (int i = 0; i < numNodes; i++) {
            Node node = getNode(bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, acl, "mike", null, false);
            nodeMetaData.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("title"+(i+1)));
            nodeMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("name"+(i+1)));
            nodeMetaData.getProperties().put(ContentModel.PROP_CREATED,
                    new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, now)));
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);

        putHandleDefaults();

        List<SolrClient> clusterClients = getClusterClients();
        String shards = getShardsString(clusterClients);

        //Test 1: Running a simple query without invoking CachedDocTransformer, expected to see id,DBID and _version_
        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts"));
        assertNotNull(resp);
        SolrDocumentList results = resp.getResults();
        assertEquals("Expecting 5 rows",5, results.size());
        SolrDocument doc = results.get(0);
        assertTrue(doc.size() == 3);
        assertNotNull(doc);
        String id = (String) doc.get("id");
        assertNotNull(id);
        long version = (long) doc.get("_version_");
        assertNotNull(version);
        long dbid = (long) doc.get("DBID");
        assertNotNull(dbid);
        //Not expected to see below as part of the solr response.
        String title = (String) doc.get("cm:title");
        assertNull(title);
        String owner = (String) doc.get("OWNER");
        assertNull(owner);
        
        //Test 2: Running simple query with CachedDocTransformer, expected to see all fields returned
        resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","*,[cached]"));
        SolrDocument docWithAllFields = resp.getResults().get(0);
        assertTrue(docWithAllFields.size() > 3);
        
        Integer version2 = (Integer) docWithAllFields.get("_version_");
        assertNotNull(version2);
        owner = ((ArrayList) docWithAllFields.get("OWNER")).toString();
        assertNotNull(owner);
        assertEquals("[mike]", owner);
        title = ((ArrayList) docWithAllFields.get("cm:title")).toString();
        assertEquals("[title1]", title);
        assertNotNull(title);
        long dbid2 = (long) docWithAllFields.get("DBID");
        assertNotNull(dbid2);
        
        //Test 3: Running simple query with CachedDocTransformer, expected to see selected fields returned
        resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id,DBID,[cached]"));
        
        assertNotNull(resp);
        SolrDocument docWithRequestedFields = resp.getResults().get(0);
        assertTrue(docWithRequestedFields.size() == 2);
        assertNotNull(docWithRequestedFields.get("id"));
        assertNotNull(docWithRequestedFields.get("DBID"));

        //Test 4: Running simple query with CachedDocTransformer on non default fields, expected to see selected fields returned
        resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id, cm_title,[cached]"));
        
        assertNotNull(resp);
        SolrDocument docWithRequestedFields3 = resp.getResults().get(0);
        assertTrue(docWithRequestedFields3.size() == 2);
        assertNotNull(docWithRequestedFields3.get("id"));
        title = (String) docWithRequestedFields3.getFieldValue("cm_title");
        assertEquals("title1", title);

        resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","cm_name, score, [cached]"));
        assertNotNull(resp);
        results = resp.getResults();
        docWithAllFields = results.get(0);
        assertTrue(docWithAllFields.size() == 2);
        assertNotNull(docWithAllFields.get("cm_name"));
        Float score = (Float) docWithAllFields.get("score");
        assertNotNull(score);

        resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","cm_title, cm_created, DBID, score, [cached]"));
        assertNotNull(resp);
        results = resp.getResults();
        docWithAllFields = results.get(0);
        assertTrue(docWithAllFields.size() == 4);
        assertNotNull(docWithAllFields.get("cm_title"));
        assertNotNull(docWithAllFields.get("cm_created"));
        assertNotNull(docWithAllFields.get("score"));
        assertNotNull(docWithAllFields.get("DBID"));

    }
}
