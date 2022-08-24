/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.solr.tracker;

import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.MAX_WAIT_TIME;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
public class DistributedDbidRangeAlfrescoSolrTrackerIT extends AbstractAlfrescoDistributedIT
{
    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(3, getSimpleClassName(), getShardMethod());
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    @After
    public void deleteDataFromIndex() throws Exception
    {
        SOLRAPIQueueClient.TRANSACTION_QUEUE.clear();
        SOLRAPIQueueClient.NODE_MAP.clear();
        deleteByQueryAllClients("*:*");
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 0, MAX_WAIT_TIME);
    }

    private List<Acl> createAcls(int numAcls)
    {
        AclChangeSet bulkAclChangeSet = getAclChangeSet(numAcls);

        List<Acl> bulkAcls = new ArrayList<>();
        List<AclReaders> bulkAclReaders = new ArrayList<>();

        for(int i=0; i<numAcls; i++)
        {
            Acl bulkAcl = getAcl(bulkAclChangeSet);
            bulkAcls.add(bulkAcl);
            bulkAclReaders.add(getAclReaders(bulkAclChangeSet,
                    bulkAcl,
                    Collections.singletonList("joel"+bulkAcl.getId()),
                    Collections.singletonList("phil"+bulkAcl.getId()),
                    null));
        }

        indexAclChangeSet(bulkAclChangeSet,
                bulkAcls,
                bulkAclReaders);

        return bulkAcls;
    }
    
    @Test
    public void testDbIdRange() throws Exception
    {
        putHandleDefaults();

        int numAcls = 250;
        var bulkAcls = createAcls(numAcls);

        int numNodes = 150;
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaDatas = new ArrayList<>();

        Transaction bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<numNodes; i++)
        {
            int aclIndex = i % numAcls;
            Node node = getNode(i, bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, MAX_WAIT_TIME);
        waitForDocCountAllCores(new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL)), numAcls, 80000);

        //The test framework has ranges 0-100, 100-200, ...
        assertShardCount(0, new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 100);
        assertShardCount(1, new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 50);
    }

    @Test
    public void testIndexLastTransaction() throws Exception
    {
        var acls = createAcls(1);
        int txId = 0;

        // index 50 trx for each shard
        for (int k = 0; k < 3; k++)
        {
            for (int i = 0; i< 50; i++)
            {
                Transaction trx = getTransaction(0, 1);
                trx.setId(txId++);
                var node = getNode(k*100 + 10 + i, trx, acls.get(0), Node.SolrApiNodeStatus.UPDATED);
                indexTransaction(trx, List.of(node), List.of(getNodeMetaData(node, trx, acls.get(0), "mike", null, false)));
            }
        }

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 150, MAX_WAIT_TIME);

        // Check all the shards have indexed the last transaction
        assertShardCount(0, new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 50);
        assertShardCount(1, new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 50);
        assertShardCount(2, new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 50);

        // check the last transaction has been indexed in all the shards
        assertShardCount(0, new TermQuery(new Term("S_TXID", "149")), 1);
        assertShardCount(1, new TermQuery(new Term("S_TXID", "149")), 1);
        assertShardCount(2, new TermQuery(new Term("S_TXID", "149")), 1);
    }

    protected static Properties getShardMethod()
    {
        Properties prop = new Properties();
        prop.put("shard.method", "DB_ID_RANGE");
        return prop;
    }
}

