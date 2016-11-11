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
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.*;

/**
 * Test Routes based on a text property field.
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@SolrTestCaseJ4.SuppressObjectReleaseTracker (bugUrl = "RAMDirectory")
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedPropertyBasedAlfrescoSolrTrackerTest extends AbstractAlfrescoDistributedTest
{
    private static final String[] DOMAINS = {"alfresco.com", "king.com", "gmail.com", "yahoo.com", "cookie.es"};
    private static final Map<String,Integer> domainsCount = new HashMap<>();

    @Rule
    public JettyServerRule jetty = new JettyServerRule(this.getClass().getSimpleName(), 4, getProperties(), new String[]{DEFAULT_TEST_CORENAME});

    @BeforeClass
    public static void setUpDomains()
    {
        for (String domain : DOMAINS)
        {
            domainsCount.put(domain,0);
        }
    }

    @Test
    public void testProperty() throws Exception
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
        RandomizedContext context = RandomizedContext.current();
        Random ints = context.getRandom();

        for (int i = 0; i < numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode(bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            String domain = DOMAINS[ints.nextInt(DOMAINS.length)];
            domainsCount.put(domain, domainsCount.get(domain)+1);
            //String emailAddress = RANDOM_NAMES[ints.nextInt(RANDOM_NAMES.length)]+ "@"+ domain;
            String emailAddress = "peter.pan"+ "@"+ domain;
            node.setShardPropertyValue(emailAddress);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "king", null, false);
            nodeMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue(emailAddress));
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);
        waitForDocCountAllCores(new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL)), numAcls, 100000);

        for (int i = 0; i < DOMAINS.length; i++)
        {
            //We have split by email domain, so those should be co-located on the same shard.
            //I am storing the email address in the cm:name field. This is purely to make it easier to write the TermQuery
            //and doesn't effect the functionality.
            assertCountAndColocation(new TermQuery(new Term("text@s____@{http://www.alfresco.org/model/content/1.0}name", "peter.pan"+ "@"+ DOMAINS[i])), domainsCount.get(DOMAINS[i]));
        }

        //Now test the fallback
        nodes.clear();
        nodeMetaDatas.clear();
        deleteByQueryAllClients("*:*");
        //Should now be nothing in the index
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 0, 100000);

        Transaction bigTxn1 = getTransaction(0, numNodes);

        for (int i = 0; i < numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode(bigTxn1, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            String domain = DOMAINS[ints.nextInt(DOMAINS.length)];
            domainsCount.put(domain, domainsCount.get(domain)+1);
            String emailAddress = "peter.pan"+ "@"+ domain;
            //Don't add shared property so it falls back  //node.setShardPropertyValue(emailAddress);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn1, bulkAcls.get(aclIndex), "king", null, false);
            nodeMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue(emailAddress));
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn1, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);
        //There are 4 shards. We should expect roughly a quarter of the nodes on each shard
        assertNodesPerShardGreaterThan((int)((numNodes)*.21));

    }

    protected Properties getProperties()
    {
        Properties prop = new Properties();
        prop.put("shard.method", ShardMethodEnum.PROPERTY.toString());
        prop.put("shard.regex", "^[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,6})$");
        prop.put("shard.key", ContentModel.PROP_EMAIL.toString());
        return prop;
    }
}
