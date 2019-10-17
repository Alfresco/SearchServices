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
package org.alfresco.solr.handler;

import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.utils.AlfrescoFileUtils.areDirectoryEquals;
import static org.carrot2.shaded.guava.common.collect.ImmutableList.of;

/**
 * @author Elia Porciani
 *
 * This test check if the synchronization of contentstore between master and slave is done correctly.
 */
public class contentStoreReplicationTest extends AbstractAlfrescoDistributedTest {

    protected static JettySolrRunner master;
    protected static JettySolrRunner slave;
    protected static SolrClient masterClient;
    protected static SolrClient slaveClient;

    protected final static int MASTER_PORT = 2345;

    protected static Path masterSolrHome;
    protected static Path slaveSolrHome;

    protected static Path masterContentStore;
    protected static Path slaveContentStore;

    private static Acl acl;

    private static final int MILLIS_TIMOUT = 80000;


    @BeforeClass
    public static void createMasterSlaveEnv() throws Exception
    {
        Properties properties = new Properties();

        clientShards = new ArrayList<>();
        solrShards = new ArrayList<>();
        solrCollectionNameToStandaloneClient = new HashMap<>();
        jettyContainers = new HashMap<>();

        String coreName = "master";
        boolean basicAuth = properties != null ? Boolean.parseBoolean(properties.getProperty("BasicAuth", "false")) : false;

        String masterKey = "master/solrHome";
        String slaveKey = "slave/solrHome";

        master = createJetty(masterKey, basicAuth, MASTER_PORT);
        addCoreToJetty(masterKey, coreName, coreName, null);
        startJetty(master);

        String slaveCoreName = "slave";
        slave = createJetty(slaveKey, basicAuth);
        addCoreToJetty(slaveKey, slaveCoreName, slaveCoreName, null);
        startJetty(slave);

        String masterStr = buildUrl(master.getLocalPort()) + "/" + coreName;
        String slaveStr = buildUrl(slave.getLocalPort()) + "/" + slaveCoreName;

        masterClient = createNewSolrClient(masterStr);
        slaveClient = createNewSolrClient(slaveStr);

        masterSolrHome = testDir.toPath().resolve(masterKey);
        slaveSolrHome = testDir.toPath().resolve(slaveKey);
        masterContentStore = testDir.toPath().resolve("master/contentstore");
        slaveContentStore = testDir.toPath().resolve("slave/contentstore");

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        acl = getAcl(aclChangeSet);
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                of(acl),
                of(aclReaders));
    }


    @AfterClass
    public static void cleanupMasterSlave() throws Exception {
        master.stop();
        slave.stop();
        FileUtils.forceDelete(new File(masterSolrHome.getParent().toUri()));
        FileUtils.forceDelete(new File(slaveSolrHome.getParent().toUri()));
    }


    @Test
    public void contentStoreReplicationTest() throws Exception {
        // ADD 250 nodes and check they are replicated
        int numNodes = 250;
        Transaction bigTxn = getTransaction(0, numNodes);
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaDatas = new ArrayList<>();
        for(int i = 0; i<numNodes; i++) {
            Node node = getNode(i, bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, acl, "mike", null, false);
            node.setNodeRef(nodeMetaData.getNodeRef().toString());
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);

        waitForDocCountCore(masterClient,
                luceneToSolrQuery(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world"))),
                numNodes, MILLIS_TIMOUT, System.currentTimeMillis());

        long filesInMasterContentStore = Files.walk(Paths.get(masterContentStore.toUri().resolve("_DEFAULT_")))
                .filter(Files::isRegularFile)
                .count();

        Assert.assertEquals( "master contentStore should have " + numNodes + "files", numNodes, filesInMasterContentStore);
        assertTrue("slave content store is not in sync after timeout", waitForContentStoreSync(MILLIS_TIMOUT));


        // ADD other 10 nodes
        int numUpdates = 10;
        int totalNodes = numNodes + numUpdates;
        Transaction updateTx = getTransaction(0, numUpdates);

        List<Node> updateNodes = new ArrayList<>();
        List<NodeMetaData> updateNodeMetaDatas = new ArrayList<>();
        for(int i = numNodes; i < totalNodes; i++) {
            Node node = getNode(i, updateTx, acl, Node.SolrApiNodeStatus.UPDATED);
            updateNodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, updateTx, acl, "mike", null, false);
            node.setNodeRef(nodeMetaData.getNodeRef().toString());
            updateNodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(updateTx, updateNodes, updateNodeMetaDatas);

        waitForDocCountCore(masterClient,
                luceneToSolrQuery(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world"))),
                numNodes + numUpdates, MILLIS_TIMOUT, System.currentTimeMillis());

        filesInMasterContentStore = Files.walk(Paths.get(masterContentStore.toUri().resolve("_DEFAULT_")))
                .filter(Files::isRegularFile)
                .count();

        Assert.assertEquals( "master contentStore should have " + totalNodes + "files", totalNodes, filesInMasterContentStore);
        assertTrue("slave content store is not in sync after timeout", waitForContentStoreSync(MILLIS_TIMOUT));


        // DELETES 30 nodes
        int numDeletes = 30;
        Transaction deleteTx = getTransaction(numDeletes, 0);

        totalNodes = numNodes + numUpdates - numDeletes;
        List<Node> deleteNodes = new ArrayList<>();
        List<NodeMetaData> deleteNodeMetaDatas = new ArrayList<>();

        for(int i = 0; i<numDeletes; i++) {
            Node node = getNode(i, deleteTx, acl, Node.SolrApiNodeStatus.DELETED);
            deleteNodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, deleteTx, acl, "mike", null, false);
            node.setNodeRef(nodeMetaData.getNodeRef().toString());
            deleteNodeMetaDatas.add(nodeMetaData);
        }


        indexTransaction(deleteTx, deleteNodes, deleteNodeMetaDatas);

        waitForDocCountCore(masterClient,
                luceneToSolrQuery(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world"))),
                totalNodes, MILLIS_TIMOUT, System.currentTimeMillis());

        filesInMasterContentStore = Files.walk(Paths.get(masterContentStore.toUri().resolve("_DEFAULT_")))
                .filter(Files::isRegularFile)
                .count();

        Assert.assertEquals( "master contentStore should have " + totalNodes + "files", totalNodes, filesInMasterContentStore);
        assertTrue("slave content store is not in sync after timeout", waitForContentStoreSync(MILLIS_TIMOUT));

    }


    private static boolean waitForContentStoreSync(long waitMillis) throws InterruptedException {

        long startMillis = System.currentTimeMillis();
        long timeout = startMillis + waitMillis;
        long increment = 1;

        while(new Date().getTime() < timeout)
        {
            try{

                if (areDirectoryEquals(masterContentStore, slaveContentStore, new String[]{"gz"}, true))
                {
                    return true;
                }
            } catch (Exception e){
                // do nothing
            }
            Thread.sleep(500 * increment);
        }

        return false;
    }

}
