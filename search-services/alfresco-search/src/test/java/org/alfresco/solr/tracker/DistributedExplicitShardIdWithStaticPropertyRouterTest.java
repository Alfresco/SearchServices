/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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
package org.alfresco.solr.tracker;

import java.util.Properties;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.carrot2.shaded.guava.common.collect.ImmutableList.of;

/**
 * Test Routes based on an last registered shard
 *
 * @author Elia
 */
@SolrTestCaseJ4.SuppressSSL
@SolrTestCaseJ4.SuppressObjectReleaseTracker (bugUrl = "RAMDirectory")
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedExplicitShardIdWithStaticPropertyRouterTest extends AbstractAlfrescoDistributedTest
{
    private static long MAX_WAIT_TIME = 80000;
    private final int timeout = 100000;

    @Before
    private void initData() throws Throwable
    {
        initSolrServers(2, getClass().getSimpleName(), getProperties());
        indexData();
    }

    @AfterClass
    private static void destroyData() throws Throwable
    {
        dismissSolrServers();
    }

    /**
     * Default data is indexed in solr.
     * 1 folder node with 2 children nodes.
     * 1 Child is on the same shard of the parent folder (shard 0) while the other is on shard 1.
     */
    private void indexData() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                of(acl),
                of(aclReaders));

        indexTestData(acl);
    }


    public void indexTestData(Acl acl) throws Exception
    {
        Transaction txn = getTransaction(0, 3);

        /*
         * Create node1 in the first shard
         */
        Node node1 = getNode(0, txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node1.setExplicitShardId(0);
        NodeMetaData nodeMetaData1 = getNodeMetaData(node1, txn, acl, "elia", null, false);
        nodeMetaData1.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("first"));

        /*
         * Create node2 in the second shard
         */
        Node node2 = getNode(1, txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node2.setExplicitShardId(1);
        NodeMetaData nodeMetaData2 = getNodeMetaData(node2, txn, acl, "elia", null, false);
        nodeMetaData2.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("second"));

        /*
         * Create node3 with no explicitShardId
         */
        Node node3 = getNode(2, txn, acl, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData nodeMetaData3 = getNodeMetaData(node3, txn, acl, "elia", null, false);
        nodeMetaData3.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("third"));

        /*
         * Create node4 in second shard
         */
        Node node4 = getNode(3, txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node4.setExplicitShardId(1);
        NodeMetaData nodeMetaData4 = getNodeMetaData(node4, txn, acl, "elia", null, false);
        nodeMetaData4.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("forth"));

        /*
         * Create node4 in shard 4 (which does not exist)
         */
        Node node5 = getNode(4, txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node5.setExplicitShardId(4);
        NodeMetaData nodeMetaData5 = getNodeMetaData(node5, txn, acl, "elia", null, false);
        nodeMetaData5.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("fifth"));

        indexTransaction(txn,
                of(node1, node2, node3, node4, node5),
                of(nodeMetaData1, nodeMetaData2, nodeMetaData3, nodeMetaData4, nodeMetaData5));

        /*
         * Get sure the nodes are indexed correctly in the shards
         */
        waitForShardsCount(params("q", "cm:content:world", "qt", "/afts"), 3, timeout, System.currentTimeMillis());
    }

    @Test
    public void testNodesShouldBeIndexedInSpecifiedShard() throws Exception
    {
        assertShardCount(0, params("q", "cm:content:world", "qt", "/afts"), 1);
        assertShardCount(1, params("q", "cm:content:world", "qt", "/afts"), 2);

        assertShardCount(0, params("q", "cm:name:first", "qt", "/afts", "shards", shards), 1);

        // The third node shouldn't be found because no explicit shard is set
        assertShardCount(0, params("q", "cm:name:third", "qt", "/afts", "shards", shards), 0);

        // The fifth node shouldn't be found because the explicit shard set is not running
        assertShardCount(0, params("q", "cm:name:fifth", "qt", "/afts", "shards", shards), 0);
    }

    protected static Properties getProperties()
    {
        Properties prop = new Properties();
        prop.put("shard.method", ShardMethodEnum.LAST_REGISTERED_INDEXING_SHARD.toString());
        return prop;
    }
}