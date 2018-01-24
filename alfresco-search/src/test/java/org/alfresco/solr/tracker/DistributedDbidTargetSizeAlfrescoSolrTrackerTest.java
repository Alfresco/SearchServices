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
package org.alfresco.solr.tracker;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_SOLR4_ID;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.solr.AbstractAlfrescoDistributedTest;
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
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedDbidTargetSizeAlfrescoSolrTrackerTest extends AbstractAlfrescoDistributedTest
{
    @Rule
    public JettyServerRule jetty = new JettyServerRule(2,this, getShardMethod());

    @Test
    public void testDbIdRange() throws Exception
    {
        putHandleDefaults();

        int numAcls = 250;
        AclChangeSet bulkAclChangeSet = getAclChangeSet(numAcls);

        List<Acl> bulkAcls = new ArrayList();
        List<AclReaders> bulkAclReaders = new ArrayList();

        for(int i=0; i<numAcls; i++) {
            Acl bulkAcl = getAcl(bulkAclChangeSet);
            bulkAcls.add(bulkAcl);
            bulkAclReaders.add(getAclReaders(bulkAclChangeSet,
                bulkAcl,
                list("joel"+bulkAcl.getId()),
                list("phil"+bulkAcl.getId()),
                null));
        }

        indexAclChangeSet(bulkAclChangeSet,
            bulkAcls,
            bulkAclReaders);

        int numNodes = 500;
        List<Node> nodes = new ArrayList();
        List<NodeMetaData> nodeMetaDatas = new ArrayList();

        Transaction bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode((long)i, bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);

        //Wait for the cap to appear
        waitForDocCountAllShards(new TermQuery(new Term(FIELD_SOLR4_ID, "TRACKER!STATE!CAP")), 1, 80000);

        //Assert that all shards have data consistent will the cap and starts.
        List<Long> initialCaps = assertCapOnAllShards(this.starts);
        //System.out.println("######Initial caps:"+initialCaps);
        //assert(false);

    }

    protected Properties getShardMethod()
    {
        Properties prop = new Properties();
        prop.put("shard.method", "DB_ID_RANGE");
        prop.put("shard.targetsize", "100");
        return prop;
    }
}

