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

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.carrot2.shaded.guava.common.collect.ImmutableList.of;

import org.alfresco.model.ContentModel;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.alfresco.util.Pair;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Properties;

/**
 * @author Elia
 * Test cascade tracker in multi sharded environment.
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedCascadeTrackerTest extends AbstractAlfrescoDistributedTest
{
    private Node parentFolder;
    private NodeMetaData parentFolderMetadata;

    private Node childShard0;
    private NodeMetaData childShardMetadata0;

    private Node childShard1;
    private NodeMetaData childShardMetadata1;

    private final String pathParent = "pathParent";
    private final String pathChild0 = "pathChild0";
    private final String pathChild1 = "pathChild2";


    private final int timeout = 100000;

    @Before
    private void initData() throws Throwable
    {
        initSolrServers(2, getClassName(), getShardMethod());
        indexData();
    }

    @After
    private void destroyData()
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

        indexNodes(acl);
    }

    /**
     * This test checks if after updating the parent folder,
     * both the children(in both the shards) are updated as well in cascading.
     */
    @Test
    public void testCascadeShouldHappenInBothShardsAfterUpdateParentFolder() throws Exception
    {

        String cascadingFirstChild = "cascadingFirstChild";
        String cascadingSecondChild = "cascadingSecondChild";

        /*
         * Modify children paths in order to see if they are updated both after cascading.
         */
        childShardMetadata0.setPaths(of(new Pair<>(cascadingFirstChild, null)));
        childShardMetadata1.setPaths(of(new Pair<>(cascadingSecondChild, null)));

        /*
         * Check the path of the two nodes before cascading
         */
        assertShardCount(0, params("qt", "/afts", "q", "PATH:" + pathChild0), 1);
        assertShardCount(1, params("qt", "/afts", "q", "PATH:" + pathChild1), 1);

        /*
         * Index a transaction with the parent folder update
         */
        indexParentFolderWithCascade();

        waitForDocCount(params("qt", "/afts", "q", "PATH:" + cascadingFirstChild), 1, timeout);

        // Check if the path is updated for both the nodes
        assertShardCount(0, params("qt", "/afts", "q", "PATH:" + cascadingFirstChild), 1);
        assertShardCount(1, params("qt", "/afts", "q", "PATH:" + cascadingSecondChild), 1);

        // The old paths are no longer in the index
        assertShardCount(0, params("qt", "/afts", "q", "PATH:" + pathChild0), 0);
        assertShardCount(1, params("qt", "/afts", "q", "PATH:" + pathChild1), 0);
    }

    private void indexNodes(Acl acl) throws Exception
    {
        Transaction bigTxn = getTransaction(0, 3);

        /*
         * Create parent folder in the first shard
         */
        parentFolder = getNode(0, bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
        parentFolderMetadata = getNodeMetaData(parentFolder, bigTxn, acl, "elia", null, false);
        parentFolderMetadata.setPaths(of(new Pair<>(pathParent, null)));

        /*
         * Create first node.
         * This will be stored in the first shard (range [0-100])
         */
        childShard0 = getNode(99, bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
        childShardMetadata0 = getNodeMetaData(childShard0, bigTxn, acl, "elia", ancestors(parentFolderMetadata.getNodeRef()), false);
        childShardMetadata0.setPaths(of(new Pair<>(pathChild0, null)));

        /*
         * Create second node.
         * This will be stored in the second shard (range [101-200])
         */
        childShard1 = getNode(101, bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
        childShardMetadata1 = getNodeMetaData(childShard1, bigTxn, acl, "elia", ancestors(parentFolderMetadata.getNodeRef()), false);
        childShardMetadata1.setPaths(of(new Pair<>(pathChild1, null)));

        indexTransaction(bigTxn,
                of(parentFolder, childShard0, childShard1),
                of(parentFolderMetadata, childShardMetadata0, childShardMetadata1));

        /*
         * Get sure the nodes are indexed correctly in the shards
         */
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 3, timeout);
        assertShardCount(0, new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2);
        assertShardCount(1, new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 1);
    }

    private void indexParentFolderWithCascade()
    {
        Transaction bigTxn = getTransaction(0, 1);

        parentFolder.setTxnId(bigTxn.getId());
        parentFolderMetadata.setTxnId(bigTxn.getId());
        parentFolderMetadata.getProperties().put(ContentModel.PROP_CASCADE_TX, new StringPropertyValue(Long.toString(bigTxn.getId())));

        indexTransaction(bigTxn, of(parentFolder), of(parentFolderMetadata));
    }

    private Properties getShardMethod()
    {
        Properties prop = new Properties();
        prop.put("shard.method", "DB_ID_RANGE");
        return prop;
    }
}