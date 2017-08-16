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
package org.alfresco.solr.tracker;

import com.carrotsearch.randomizedtesting.RandomizedContext;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.*;

/**
 * Test Routes based on an explicit shard
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@SolrTestCaseJ4.SuppressObjectReleaseTracker (bugUrl = "RAMDirectory")
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedExplicitShardRoutingTrackerTest extends AbstractAlfrescoDistributedTest
{
    @Rule
    public JettyServerRule jetty = new JettyServerRule(this.getClass().getSimpleName(), 3, getProperties(), new String[]{DEFAULT_TEST_CORENAME});

    @Test
    public void testShardId() throws Exception
    {
        putHandleDefaults();

        int numAcls = 25;
        AclChangeSet bulkAclChangeSet = getAclChangeSet(numAcls);

        List<Acl> bulkAcls = new ArrayList();
        List<AclReaders> bulkAclReaders = new ArrayList();


        for (int i = 0; i < numAcls; i++) {
            Acl bulkAcl = getAcl(bulkAclChangeSet);
            bulkAcls.add(bulkAcl);
            bulkAclReaders.add(getAclReaders(bulkAclChangeSet,
                    bulkAcl,
                    list("king" + bulkAcl.getId()),
                    list("king" + bulkAcl.getId()),
                    null));
        }

        indexAclChangeSet(bulkAclChangeSet,
                bulkAcls,
                bulkAclReaders);

        int numNodes = 1000;
        List<Node> nodes = new ArrayList();
        List<NodeMetaData> nodeMetaDatas = new ArrayList();

        Transaction bigTxn = getTransaction(0, numNodes);

        for (int i = 0; i < numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode(bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "king", null, false);
            boolean even = i % 2 == 0; // if its even put it on shard 1 otherwise put it on shard 0.
            node.setShardPropertyValue(even?"1":"0");
            nodeMetaDatas.add(nodeMetaData);
        }

        //Add a node that won't get indexed
        Node node = getNode(bigTxn, bulkAcls.get(1), Node.SolrApiNodeStatus.UPDATED);
        nodes.add(node);
        NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(1), "king", null, false);
        node.setShardPropertyValue("node YOU DON'T");
        nodeMetaDatas.add(nodeMetaData);

        //Add a node that won't get indexed
        node = getNode(bigTxn, bulkAcls.get(2), Node.SolrApiNodeStatus.UPDATED);
        nodes.add(node);
        nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(2), "king", null, false);
        //Don't set the Share Property but add it anyway
        nodeMetaDatas.add(nodeMetaData);

        indexTransaction(bigTxn, nodes, nodeMetaDatas);

        Query contentQuery = new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world"));
        Query aclQuery = new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL));
        List<SolrCore> shards = getJettyCores(jettyShards);
        long begin = System.currentTimeMillis();

        for (SolrCore core : shards)
        {
            switch (core.getName())
            {
                case "shard0":
                case "shard1":
                    waitForDocCountCore(core, contentQuery, 500, 50000, begin);
                    break;
                default:
                    //ignore other shards because we will check below
            }
        }

        //lets make sure the other nodes don't have any.
         assertShardCount(2, contentQuery, 0);

        //Acls go to all cores
        waitForDocCountAllCores(aclQuery, numAcls, 20000);
    }

    protected Properties getProperties()
    {
        Properties prop = new Properties();
        prop.put("shard.method", ShardMethodEnum.EXPLICIT_ID.toString());
        //Normally this would be used by the Solr client which will automatically add the property to the node.shardPropertyValue
        //For testing this doesn't work like that so I setShardPropertyValue explicitly above.
        prop.put("shard.key", ContentModel.PROP_SKYPE.toString());
        return prop;
    }
}
