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

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
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
import org.alfresco.solr.SolrInformationServer;
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
import org.apache.solr.common.util.NamedList;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedExpandDbidRangeAlfrescoSolrTrackerTest extends AbstractAlfrescoDistributedTest
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

        SolrQueryResponse response0 = rangeCheck(0);
        NamedList values0 = response0.getValues();
        //{start=0,end=100,nodeCount=0,maxDbid=0,density=NaN,expand=0,expanded=false}

        assertEquals((long)values0.get("start"), 0);
        assertEquals((long)values0.get("end"), 100);
        assertEquals((long)values0.get("nodeCount"), 0);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long)values0.get("maxDbid"), 0);
        assertEquals((long)values0.get("expand"), 0);
        assertEquals((boolean)values0.get("expanded"), false);

        System.out.println("RANGECHECK0:"+values0);

        SolrQueryResponse response1 = rangeCheck(1);
        NamedList values1 = response1.getValues();
        //{start=100,end=200,nodeCount=0,maxDbid=0,density=0.0,expand=0,expanded=false}
        System.out.println("RANGECHECK1:" + values1);

        assertEquals((long)values1.get("start"), 100);
        assertEquals((long)values1.get("end"), 200);
        assertEquals((long)values1.get("nodeCount"), 0);
        assertEquals((long)values1.get("minDbid"), 0);
        assertEquals((long)values1.get("maxDbid"), 0);
        assertEquals((long)values1.get("expand"), 0);
        assertEquals((boolean)values1.get("expanded"), false);

        int numNodes = 25;
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
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);
        waitForDocCountAllCores(new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL)), numAcls, 80000);

        response0 = rangeCheck(0);
        values0 = response0.getValues();
        //{start=0,end=100,nodeCount=25,maxDbid=24,density=1.0416666666666667,expand=0,expanded=false}

        assertEquals((long) values0.get("start"), 0);
        assertEquals((long) values0.get("end"), 100);
        assertEquals((long) values0.get("nodeCount"), 25);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values0.get("maxDbid"), 24);
        assertEquals((long) values0.get("expand"), 0);
        assertEquals((boolean) values0.get("expanded"), false);

        System.out.println("_RANGECHECK0:" + values0);

        response1 = rangeCheck(1);
        values1 = response1.getValues();
        assertEquals((long) values1.get("start"), 100);
        assertEquals((long)values1.get("end"), 200);
        assertEquals((long) values1.get("nodeCount"), 0);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values1.get("maxDbid"), 0);
        assertEquals((long) values1.get("expand"), 0);
        assertEquals((boolean) values1.get("expanded"), false);

        System.out.println("_RANGECHECK1:" + values1);



        numNodes = 26;
        nodes = new ArrayList();
        nodeMetaDatas = new ArrayList();

        bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode((long)i+35, bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 51, 100000);

        response0 = rangeCheck(0);
        values0 = response0.getValues();
        //{start=0,end=100,nodeCount=51,maxDbid=60,density=0.85,expand=15,expanded=false}

        assertEquals((long) values0.get("start"), 0);
        assertEquals((long)values0.get("end"), 100);
        assertEquals((long) values0.get("nodeCount"), 51);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values0.get("maxDbid"), 60);
        assertEquals((double) values0.get("density"), .85, 0.0);
        assertEquals((long) values0.get("expand"), 17);
        assertEquals((boolean) values0.get("expanded"), false);

        System.out.println("_RANGECHECK0:" + values0);

        response1 = rangeCheck(1);
        values1 = response1.getValues();
        assertEquals((long) values1.get("start"), 100);
        assertEquals((long)values1.get("end"), 200);
        assertEquals((long) values1.get("nodeCount"), 0);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values1.get("maxDbid"), 0);
        assertEquals((long) values1.get("expand"), 0);
        assertEquals((boolean) values1.get("expanded"), false);

        System.out.println("_RANGECHECK1:" + values1);

        numNodes = 5;
        nodes = new ArrayList();
        nodeMetaDatas = new ArrayList();

        bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode((long)i+72, bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 56, 100000);


        response0 = rangeCheck(0);
        values0 = response0.getValues();
        //{start=0,end=100,nodeCount=56,maxDbid=76,density=0.7368421052631579,expand=-1,expanded=false}
        assertEquals((long) values0.get("start"), 0);
        assertEquals((long) values0.get("end"), 100);
        assertEquals((long) values0.get("nodeCount"), 56);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values0.get("maxDbid"), 76);
        assertEquals((double) values0.get("density"), 0.7368421052631579, 0.0);
        assertEquals((long) values0.get("expand"), -1);
        assertEquals((boolean) values0.get("expanded"), false);

        SolrQueryResponse expandResponse = expand(0, 35);
        NamedList expandValues = expandResponse.getValues();
        String ex = (String)expandValues.get("exception");
        assertEquals(ex, "Expansion cannot occur if max DBID in the index is more then 75% of range.");
        Number expanded = (Number)expandValues.get("expand");
        assertEquals(expanded.intValue(), -1);

        System.out.println("_RANGECHECK0:" + values0);

        response1 = rangeCheck(1);
        values1 = response1.getValues();
        System.out.println("_RANGECHECK1:" + values1);

        assertEquals((long) values1.get("start"), 100);
        assertEquals((long) values1.get("end"), 200);
        assertEquals((long) values1.get("nodeCount"), 0);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values1.get("maxDbid"), 0);
        assertEquals((long) values1.get("expand"), 0);
        assertEquals((boolean) values1.get("expanded"), false);

        numNodes = 5;
        nodes = new ArrayList();
        nodeMetaDatas = new ArrayList();

        bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode((long)i+100, bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 61, 100000);

        response0 = rangeCheck(0);
        values0 = response0.getValues();
        //{start=0,end=100,nodeCount=56,maxDbid=76,density=0.7368421052631579,expand=-1,expanded=false}
        assertEquals((long) values0.get("start"), 0);
        assertEquals((long) values0.get("end"), 100);
        assertEquals((long) values0.get("nodeCount"), 56);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values0.get("maxDbid"), 76);
        assertEquals((double) values0.get("density"), 0.7368421052631579, 0.0);
        assertEquals((long) values0.get("expand"), -1);
        assertEquals((boolean) values0.get("expanded"), false);

        response1 = rangeCheck(1);
        values1 = response1.getValues();
        System.out.println("_RANGECHECK1:" + values1);

        assertEquals((long) values1.get("start"), 100);
        assertEquals((long) values1.get("end"), 200);
        assertEquals((long) values1.get("nodeCount"), 5);
        assertEquals((long)values1.get("minDbid"), 100);
        assertEquals((long) values1.get("maxDbid"), 104);
        assertEquals((long) values1.get("expand"), 0);
        assertEquals((boolean) values1.get("expanded"), false);

        numNodes = 35;
        nodes = new ArrayList();
        nodeMetaDatas = new ArrayList();

        bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode((long)i+120, bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 96 , 100000);

        response0 = rangeCheck(0);
        values0 = response0.getValues();
        //{start=0,end=100,nodeCount=56,maxDbid=76,density=0.7368421052631579,expand=-1,expanded=false}
        assertEquals((long) values0.get("start"), 0);
        assertEquals((long) values0.get("end"), 100);
        assertEquals((long) values0.get("nodeCount"), 56);
        assertEquals((long)values0.get("minDbid"), 0);
        assertEquals((long) values0.get("maxDbid"), 76);
        assertEquals((double) values0.get("density"), 0.7368421052631579, 0.0);
        assertEquals((long) values0.get("expand"), -1);
        assertEquals((boolean) values0.get("expanded"), false);

        response1 = rangeCheck(1);
        values1 = response1.getValues();
        System.out.println("_RANGECHECK1:" + values1);
        //{start=100,end=200,nodeCount=40,maxDbid=154,density=0.7407407407407407,expand=35,expanded=false}

        assertEquals((long) values1.get("start"), 100);
        assertEquals((long) values1.get("end"), 200);
        assertEquals((long) values1.get("nodeCount"), 40);
        assertEquals((long)values1.get("minDbid"), 100);
        assertEquals((long) values1.get("maxDbid"), 154);
        assertEquals((double) values1.get("density"), 0.7407407407407407, 0.0);
        assertEquals((long) values1.get("expand"), 35);
        assertEquals((boolean) values1.get("expanded"), false);


        //expand shard1 by 20
        expandResponse = expand(1, 35);
        expandValues = expandResponse.getValues();

        expanded = (Number)expandValues.get("expand");
        assertEquals(expanded.intValue(), 235);

        waitForShardsCount(new TermQuery(new Term(FIELD_SOLR4_ID, "TRACKER!STATE!CAP")),
            1,
            100000,
            System.currentTimeMillis());

        response1 = rangeCheck(1);
        values1 = response1.getValues();
        System.out.println("_RANGECHECK1:" + values1);
        //{start=100,end=235,nodeCount=40,maxDbid=154,density=0.7407407407407407,expand=-1,expanded=true}

        assertEquals((long) values1.get("start"), 100);
        assertEquals((long) values1.get("end"), 235);
        assertEquals((long) values1.get("nodeCount"), 40);
        assertEquals((long)values1.get("minDbid"), 100);
        assertEquals((long) values1.get("maxDbid"), 154);
        assertEquals((double) values1.get("density"),0.7407407407407407, 0.0);
        assertEquals((long) values1.get("expand"), -1);
        assertEquals((boolean) values1.get("expanded"), true);

        numNodes = 3;
        nodes = new ArrayList();
        nodeMetaDatas = new ArrayList();

        bigTxn = getTransaction(0, numNodes);

        for(int i=0; i<numNodes; i++)
        {
            int aclIndex = i % numAcls;
            Node node = getNode((long)i+230, bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 99, 100000);

        response1 = rangeCheck(1);
        values1 = response1.getValues();
        System.out.println("_RANGECHECK1:" + values1);
        //{start=100,end=235,nodeCount=43,maxDbid=232,density=0.32575757575757575,expand=-1,expanded=true}

        assertEquals((long) values1.get("start"), 100);
        assertEquals((long) values1.get("end"), 235);
        assertEquals((long) values1.get("nodeCount"), 43);
        assertEquals((long)values1.get("minDbid"), 100);
        assertEquals((long) values1.get("maxDbid"), 232);
        assertEquals((double) values1.get("density"), 0.32575757575757575, 0.0);
        assertEquals((long) values1.get("expand"), -1);
        assertEquals((boolean) values1.get("expanded"), true);

        //assert(false);

        //Do a reload
    }

    protected Properties getShardMethod()
    {
        Properties prop = new Properties();
        prop.put("shard.method", "DB_ID_RANGE");
        return prop;
    }
}

