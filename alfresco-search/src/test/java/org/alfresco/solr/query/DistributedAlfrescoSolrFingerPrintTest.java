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

import static java.util.Arrays.asList;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
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
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedAlfrescoSolrFingerPrintTest extends AbstractAlfrescoDistributedTest
{
    private static long MAX_WAIT_TIME = 80000;

    private static Node[] NODES = new Node[4];
    private static NodeMetaData[] NODES_METADATA = new NodeMetaData[4];
    private static Acl ACL;

    @BeforeClass
    private static void initData() throws Throwable
    {
        initSolrServers(2,getClassName(),null);

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        ACL = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, ACL, singletonList("joel"), singletonList("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, singletonList("jim"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet, asList(ACL, acl2), asList(aclReaders, aclReaders2));

        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        Transaction txn = getTransaction(0, 4);

        //Next create two NODES to update for the transaction
        NODES[0] = getNode(txn, ACL, Node.SolrApiNodeStatus.UPDATED);
        NODES[1] = getNode(txn, ACL, Node.SolrApiNodeStatus.UPDATED);
        NODES[2] = getNode(txn, ACL, Node.SolrApiNodeStatus.UPDATED);
        NODES[3] = getNode(txn, ACL, Node.SolrApiNodeStatus.UPDATED);


        //Next create the NodeMetaData for each node. TODO: Add more metadata
        NODES_METADATA[0] = getNodeMetaData(NODES[0], txn, ACL, "mike", null, false);
        NODES_METADATA[1] = getNodeMetaData(NODES[1], txn, ACL, "mike", null, false);
        NODES_METADATA[2] = getNodeMetaData(NODES[2], txn, ACL, "mike", null, false);
        NODES_METADATA[3] = getNodeMetaData(NODES[3], txn, ACL, "mike", null, false);

        List<String> content = new ArrayList<>();
        int[] sizes = {2000, 1000, 1500, 750};

        Random r = new Random(1);
        String token1 = Integer.toString(Math.abs(r.nextInt()));

        for(int i=0; i<4; i++) {
            Random rand = new Random(1);
            StringBuilder buf = new StringBuilder();
            int size = sizes[i];
            for(int s=0; s<size; s++) {
                if(s>0) {
                    buf.append(" ");
                }
                buf.append(Math.abs(rand.nextInt()));
            }
            content.add(buf.toString());
        }

        //Index the transaction, NODES, and nodeMetaDatas.
        //Note that the content is automatically created by the test framework.
        indexTransaction(txn,
            asList(NODES[0], NODES[1], NODES[2], NODES[3]),
            asList(NODES_METADATA[0], NODES_METADATA[1], NODES_METADATA[2], NODES_METADATA[3]),
            content);

        //Check for the TXN state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();

        waitForDocCountAllCores(waitForQuery, 1, 80000);

        /*
         * Query the index for the content
         */

        waitForDocCountAllCores(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, 80000);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", token1)), 4, 80000);
    }

    @AfterClass
    private static void destroyData()
    {
        dismissSolrServers();
    }
    
    @Test
    public void testFingerPrint() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
                                       "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
                                        params("q", "FINGERPRINT:"+ NODES[0].getId(),
                                                "qt", "/afts",
                                                "shards.qt", "/afts",
                                                "start", "0",
                                                "fl","DBID,score",
                                                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertEquals(4, docs.getNumFound());
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertEquals(dbid0, NODES[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertEquals(dbid1, NODES[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertEquals(dbid2, NODES[1].getId());

        SolrDocument doc3 = docs.get(3);
        long dbid3 = (long)doc3.getFieldValue("DBID");
        assertEquals(dbid3, NODES[3].getId());
    }

    @Test
    public void testFingerPrint2() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + NODES[0].getId()+"_70",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertEquals(2, docs.getNumFound());
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertEquals(dbid0, NODES[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertEquals(dbid1, NODES[2].getId());
    }

    @Test
    public void testFingerPrint3() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + NODES[0].getId()+"_45",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertEquals(3, docs.getNumFound());
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertEquals(dbid0, NODES[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertEquals(dbid1, NODES[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertEquals(dbid2, NODES[1].getId());
    }

    @Test
    public void testFingerPrint4() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:"+ NODES_METADATA[0].getNodeRef().getId(),
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl","DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertEquals(4, docs.getNumFound());
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertEquals(dbid0, NODES[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertEquals(dbid1, NODES[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertEquals(dbid2, NODES[1].getId());

        SolrDocument doc3 = docs.get(3);
        long dbid3 = (long)doc3.getFieldValue("DBID");
        assertEquals(dbid3, NODES[3].getId());
    }

    @Test
    public void testFingerPrint5() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + NODES_METADATA[0].getNodeRef().getId() +"_70",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertEquals(2, docs.getNumFound());
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertEquals(dbid0, NODES[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertEquals(dbid1, NODES[2].getId());
    }

    @Test
    public void testFingerPrint6() throws Exception
    {
        putHandleDefaults();
        QueryResponse response = query(getDefaultTestClient(), true,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
            params("q", "FINGERPRINT:" + NODES_METADATA[0].getNodeRef().getId() +"_45",
                "qt", "/afts",
                "shards.qt", "/afts",
                "start", "0",
                "fl", "DBID,score",
                "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertEquals(3, docs.getNumFound());
        SolrDocument doc0 = docs.get(0);
        long dbid0 = (long)doc0.getFieldValue("DBID");
        assertEquals(dbid0, NODES[0].getId());

        SolrDocument doc1 = docs.get(1);
        long dbid1 = (long)doc1.getFieldValue("DBID");
        assertEquals(dbid1, NODES[2].getId());

        SolrDocument doc2 = docs.get(2);
        long dbid2 = (long)doc2.getFieldValue("DBID");
        assertEquals(dbid2, NODES[1].getId());
    }

    @Test
    public void testFingerprintStillExistsAfterNodeMetadataUpdate() throws Exception
    {
        putHandleDefaults();

        Transaction txn = getTransaction(0, 1);
        Node fileNode = getNode(txn, ACL, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData fileMetaData = getNodeMetaData(fileNode, txn, ACL, "mike", null, false);

        indexTransaction(
                txn,
                singletonList(fileNode),
                singletonList(fileMetaData),
                singletonList("This is a text content which is longer than the default hello world " + fileNode.getId() +
                        " returned by the Mock SOLRAPIQueueClient. This is needed because the \"min_hash\" field type " +
                        "definition in Solr doesn't take in account fields which produce less than 5 tokens (see the " +
                        "ShingleFilter settings)."));

        makeSureContentNodeHasBeenIndexed(fileNode, "mike", "longer");

        QueryResponse response = query(getDefaultTestClient(), true,
                "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
                params("q", "FINGERPRINT:" + fileMetaData.getNodeRef().getId(),
                        "qt", "/afts",
                        "shards.qt", "/afts",
                        "start", "0",
                        "fl", "DBID,score",
                        "rows", "100"));

        SolrDocumentList docs = response.getResults();
        assertEquals(1, docs.getNumFound());
        assertEquals(fileNode.getId(), docs.iterator().next().getFieldValue("DBID"));

        // Let's update the test node
        fileMetaData.setOwner("Andrea");
        fileMetaData.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("This is the new file \"title\" metadata attribute."));

        txn = getTransaction(0, 1);

        indexTransaction(
                txn,
                singletonList(fileNode),
                singletonList(fileMetaData));

        makeSureContentNodeHasBeenIndexed(fileNode, "Andrea", "longer");

        response = query(getDefaultTestClient(), true,
                "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"joel\"], \"tenants\": []}",
                params("q", "FINGERPRINT:" + fileMetaData.getNodeRef().getId(),
                        "qt", "/afts",
                        "shards.qt", "/afts",
                        "start", "0",
                        "fl", "DBID,score",
                        "rows", "100"));
        docs = response.getResults();
        assertEquals(1, docs.getNumFound());
        assertEquals(fileNode.getId(), docs.iterator().next().getFieldValue("DBID"));
    }

    /**
     * Queries the index using a token from the (dummy) text produced by the test framework ("world", actually).
     * Once the query returns a positive result we are sure the ContentTracker
     *
     * <ol>
     *     <li>
     *         Fetched the text content associated with the current node, from Alfresco
     *     </li>
     *     <li>
     *         Computed a fingerprint (using the retrieved text) for the node
     *     </li>
     *     <li>
     *         Updated the node definition in the (Solr)ContentStore and in Solr
     *     </li>
     * </ol>
     *
     * Last but not least, we are also making sure that CommitTracker executed its cycle as well (otherwise document
     * wouldn't be searchable).
     *
     * @param node an addition term which will be appended as a required clause in the executed query.
     * @param testTerm a term which is supposed to be in the indexed content
     * @param owner the #FIELD_OWNER which will be used as an additional required query clause.
     * @throws Exception in case the MAX_WAIT_TIME is reached and the node is not in results.
     */
    private void makeSureContentNodeHasBeenIndexed(final Node node, final String owner, String testTerm) throws Exception
    {
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", testTerm)), 1, MAX_WAIT_TIME);

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(node.getId()))), 1, MAX_WAIT_TIME);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", testTerm)), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, owner)), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, MAX_WAIT_TIME);
    }
}