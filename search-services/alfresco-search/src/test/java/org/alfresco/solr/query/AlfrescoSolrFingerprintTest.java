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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoSolrUtils.*;

public class AlfrescoSolrFingerprintTest extends AbstractAlfrescoSolrTests
{
    private static long MAX_WAIT_TIME = 80000;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
    }

    private Acl acl;

    @Before
    public void prepare() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);
        acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, singletonList("jim"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                asList(acl, acl2),
                asList(aclReaders, aclReaders2));

        // Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }

    @After
    public void clearQueue()
    {
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
        SOLRAPIQueueClient.nodeContentMap.clear();

        clearIndex();
        assertU(commit());
    }

    private void makeSureTransactionHasBeenIndexed(long transactionId) throws Exception
    {
        //Check for the TXN state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, transactionId, transactionId + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }

    @Test
    public void testBasicFingerprint() throws Exception
    {
        Transaction txn = getTransaction(0, 4);

        Node node1 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node2 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node3 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node node4 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        NodeMetaData nodeMetaData1 = getNodeMetaData(node1, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData2 = getNodeMetaData(node2, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData3 = getNodeMetaData(node3, txn, acl, "mike", null, false);
        NodeMetaData nodeMetaData4 = getNodeMetaData(node4, txn, acl, "mike", null, false);

        Random randomizer = new Random(1);
        String aFirstToken = Integer.toString(Math.abs(randomizer.nextInt()));

        indexTransaction(txn,
                asList(node1, node2, node3, node4),
                asList(nodeMetaData1, nodeMetaData2, nodeMetaData3, nodeMetaData4),
                randomTextContent());

        makeSureTransactionHasBeenIndexed(txn.getId());
        makeSureContentNodesHaveBeenIndexed("mike", aFirstToken, 4);

        assertFingerprintQueryCorrectness(node1.getId(),
                "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node4.getId()+"']");

        assertFingerprintQueryCorrectness(nodeMetaData1.getNodeRef().getId(),
                "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node4.getId()+"']");

        assertFingerprintQueryCorrectness(node1.getId() + "_70",
                "*[count(//doc)= 2]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node3.getId()+"']");

        assertFingerprintQueryCorrectness(nodeMetaData1.getNodeRef().getId() + "_70",
                "*[count(//doc)= 2]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node3.getId()+"']");

        assertFingerprintQueryCorrectness(node1.getId() + "_45",
                "*[count(//doc)= 3]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']");

        assertFingerprintQueryCorrectness(nodeMetaData1.getNodeRef().getId() + "_45",
                "*[count(//doc)= 3]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']");

        assertFingerprintQueryCorrectness(node4.getId() + "_30",
                "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node4.getId()+"']");

        assertFingerprintQueryCorrectness(nodeMetaData4.getNodeRef().getId() + "_30",
                "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node4.getId()+"']");

        assertFingerprintQueryCorrectness(node4.getId(),
                "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node4.getId()+"']");

        assertFingerprintQueryCorrectness(nodeMetaData4.getNodeRef().getId(),
                "*[count(//doc)= 4]",
                "//result/doc[1]/long[@name='DBID'][.='"+node1.getId()+"']",
                "//result/doc[2]/long[@name='DBID'][.='"+node2.getId()+"']",
                "//result/doc[3]/long[@name='DBID'][.='"+node3.getId()+"']",
                "//result/doc[4]/long[@name='DBID'][.='"+node4.getId()+"']");
    }

    @Test
    public void testFingerprintStillExistsAfterNodeMetadataUpdate() throws Exception
    {
        Transaction txn = getTransaction(0, 1);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData fileMetaData = getNodeMetaData(fileNode, txn, acl, "mike", null, false);

        indexTransaction(
                txn,
                singletonList(fileNode),
                singletonList(fileMetaData),
                singletonList("This is a text content which is longer than the default hello world " + fileNode.getId() +
                        " returned by the Mock SOLRAPIQueueClient. This is needed because the \"min_hash\" field type " +
                        "definition in Solr doesn't take in account fields which produce less than 5 tokens (see the " +
                        "ShingleFilter settings)."));

        makeSureTransactionHasBeenIndexed(txn.getId());
        makeSureContentNodeHasBeenIndexed(fileNode, "mike", "world");

        assertFingerprintQueryCorrectness(fileNode.getId(), "*[count(//doc)=1]","//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");

        // Let's update the test node
        fileMetaData.setOwner("Andrea");
        fileMetaData.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("This is the new file \"title\" metadata attribute."));
        reindexTransactionId(txn.getId());

        makeSureContentNodeHasBeenIndexed(fileNode, "Andrea", "world");

        assertFingerprintQueryCorrectness(fileNode.getId(), "*[count(//doc)=1]","//result/doc[1]/long[@name='DBID'][.='" + fileNode.getId() + "']");
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


    /**
     * Queries the index using a token from the (dummy) text produced by the test framework.
     * Once the query returns a positive result we are sure the ContentTracker
     *
     * <ol>
     *     <li>
     *         Fetched the text content associated with the test nodes, from Alfresco
     *     </li>
     *     <li>
     *         Computed a fingerprint (using the retrieved text) for each node
     *     </li>
     *     <li>
     *         Updated the nodes definitions in the (Solr)ContentStore and in Solr
     *     </li>
     * </ol>
     *
     * Last but not least, we are also making sure that CommitTracker executed its cycle as well (otherwise documents
     * wouldn't be searchable).
     *
     * @param owner the #FIELD_OWNER which will be used as an additional required query clause.
     * @param testTerm a term which is supposed to be in the indexed content
     * @throws Exception in case the MAX_WAIT_TIME is reached and the node is not in results.
     */
    private void makeSureContentNodesHaveBeenIndexed(final String owner, String testTerm, final int expectedCount) throws Exception
    {
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", testTerm)), expectedCount, MAX_WAIT_TIME);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", testTerm)), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_OWNER, owner)), BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), expectedCount, MAX_WAIT_TIME);
    }

    /**
     * Asserts that a query in the format <pre>FINGERPRINT:<DBID></pre> correctly returns the node we are testing.
     *
     * @param id the node identifier.
     */
    private void assertFingerprintQueryCorrectness(long id, String ... assertions)
    {
        assertFingerprintQueryCorrectness(String.valueOf(id), assertions);
    }

    /**
     * Asserts that a query in the format <pre>FINGERPRINT:<DBID></pre> correctly returns the node we are testing.
     *
     * @param id the node identifier.
     */
    private void assertFingerprintQueryCorrectness(String id, String ... assertions)
    {
        ModifiableSolrParams params = new ModifiableSolrParams()
                .add("q", "FINGERPRINT:" + id)
                .add("qt", "/afts")
                .add("start", "0")
                .add("rows", "6")
                .add("sort", "id asc");
               // .add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");

        SolrServletRequest req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"joel\"], \"tenants\": [ \"\" ]}");
        assertQ(req, assertions);
    }

    private List<String> randomTextContent()
    {
        int[] sizes = {2000, 1000, 1500, 750};

        return stream(sizes)
                .mapToObj(item -> {
                    Random randomizer = new Random(1);
                    return range(0, item)
                            .mapToObj(i -> randomizer.nextInt())
                            .map(Object::toString)
                            .collect(Collectors.joining(" "));})
                .collect(Collectors.toList());
    }
}
