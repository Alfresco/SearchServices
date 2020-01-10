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
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

@SolrTestCaseJ4.SuppressSSL
public class CachedDocTransformerIT extends AbstractAlfrescoDistributedIT
{
    public static final String ALFRESCO_JSON = "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}";

    @BeforeClass
    public static void initData() throws Throwable
    {
        // FIXME: 1 shard??
        initSolrServers(1, CachedDocTransformerIT.getSimpleClassName(), null);
        populateAlfrescoData();
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }
    
    @Test
    public void transformDocument_noDocTransformer_shouldReturnBasicFields() throws Exception 
    {
        putHandleDefaults();
        //Test 1: Running a simple query without invoking CachedDocTransformer, expected to see id,DBID and _version_
        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts"));
        assertNotNull(resp);
        SolrDocumentList results = resp.getResults();
        assertEquals("Expecting 5 rows",5, results.size());
        SolrDocument doc = results.get(0);
        assertEquals(3, doc.size());
        assertNotNull(doc);
        String id = (String) doc.get("id");
        assertNotNull(id);
        Long version = (Long) doc.get("_version_");
        assertNotNull(version);
        Long dbid = (Long) doc.get("DBID");
        assertNotNull(dbid);
        //Not expected to see below as part of the solr response.
        String title = (String) doc.get("cm:title");
        assertNull(title);
        String owner = (String) doc.get("OWNER");
        assertNull(owner);
    }

    @Test
    public void transformDocument_docTransformer_shouldReturnAllFields() throws Exception
    {
        putHandleDefaults();
        
        //Test 2: Running simple query with CachedDocTransformer, expected to see all fields returned
        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","*,[cached]"));
        SolrDocument docWithAllFields = resp.getResults().get(0);
        assertTrue(docWithAllFields.size() > 3);

        Long version2 = (Long) docWithAllFields.get("_version_");
        assertNotNull(version2);
        String owner = docWithAllFields.get("OWNER").toString();
        assertNotNull(owner);
        assertEquals("[mike]", owner);
        String title = docWithAllFields.get("cm:title").toString();
        assertEquals("[title1]", title);
        assertNotNull(title);
        Long dbid2 = (Long) docWithAllFields.get("DBID");
        assertNotNull(dbid2);
    }

    @Test
    public void transformDocument_docTransformerAndFieldsSelects_shouldReturnOnlySelectedFields() throws Exception
    {
        putHandleDefaults();

        //Test 3: Running simple query with CachedDocTransformer, expected to see selected fields returned
        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id,DBID,[cached]"));

        assertNotNull(resp);
        SolrDocument docWithRequestedFields = resp.getResults().get(0);
        assertEquals(2, docWithRequestedFields.size());
        assertNotNull(docWithRequestedFields.get("id"));
        assertNotNull(docWithRequestedFields.get("DBID"));
    }

    @Test
    public void transformDocument_docTransformerNotDefaultFields_shouldReternOnlySelectedFields() throws Exception
    {
        putHandleDefaults();
        
        //Test 4: Running simple query with CachedDocTransformer on non default fields, expected to see selected fields returned
        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","id, cm_title,[cached]"));

        assertNotNull(resp);
        SolrDocument docWithRequestedFields3 = resp.getResults().get(0);
        assertEquals(2, docWithRequestedFields3.size());
        assertNotNull(docWithRequestedFields3.get("id"));
        String title = (String) docWithRequestedFields3.getFieldValue("cm_title");
        assertEquals("title1", title);
    }

    @Test
    public void transformDocument_docTransformerAndScoreRequested_shouldReturnScore() throws Exception
    {
        putHandleDefaults();

        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","cm_name, score, [cached]"));
        assertNotNull(resp);
        SolrDocumentList results = resp.getResults();
        SolrDocument docWithAllFields = results.get(0);
        assertEquals(2, docWithAllFields.size());
        assertNotNull(docWithAllFields.get("cm_name"));
        Float score = (Float) docWithAllFields.get("score");
        assertNotNull(score);
    }

    @Test
    public void transformDocument_docTransformerFieldsAndScoreRequested_shouldReturnScoreAndSelectedFields() throws Exception
    {
        putHandleDefaults();

        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","cm_title, cm_created, DBID, score, [cached]"));
        assertNotNull(resp);
        SolrDocumentList results = resp.getResults();
        SolrDocument docWithAllFields = results.get(0);
        assertEquals(4, docWithAllFields.size());
        assertNotNull(docWithAllFields.get("cm_title"));
        assertNotNull(docWithAllFields.get("cm_created"));
        assertNotNull(docWithAllFields.get("score"));
        assertNotNull(docWithAllFields.get("DBID"));
    }

    @Test
    public void transformDocument_docTransformerGlobsFieldRequested_shouldReturnCorrespondingFields() throws Exception
    {
        putHandleDefaults();

        QueryResponse resp = query(getDefaultTestClient(), true, ALFRESCO_JSON, params("q", "*", "qt", "/afts", "shards.qt", "/afts","fl","cm?title, *name, [cached]"));
        assertNotNull(resp);
        SolrDocumentList results = resp.getResults();
        SolrDocument docWithAllFields = results.get(0);
        assertEquals(4, docWithAllFields.size());
        assertNotNull(docWithAllFields.get("cm_title"));
        assertNotNull(docWithAllFields.get("cm:title"));
        assertNotNull(docWithAllFields.get("cm_name"));
    }

    private static void populateAlfrescoData() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                singletonList(acl),
                singletonList(aclReaders));

        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery
            .newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        int numNodes = 5;
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaDatas = new ArrayList<>();

        Transaction bigTxn = getTransaction(0, numNodes);
        Date now = new Date();

        for (int i = 0; i < numNodes; i++)
        {
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
    }
}
