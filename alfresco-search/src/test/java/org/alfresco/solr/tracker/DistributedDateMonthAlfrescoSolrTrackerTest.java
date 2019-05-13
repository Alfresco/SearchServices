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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

/**
 * @author Joel
 *
 *
 *
 *
 */

@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedDateMonthAlfrescoSolrTrackerTest extends AbstractAlfrescoDistributedTest
{
    @BeforeClass
    private static void initData() throws Throwable
    {
        initSolrServers(5, "DistributedDateMonthAlfrescoSolrTrackerTest", getShardMethod());
    }

    @AfterClass
    private static void destroyData() throws Throwable
    {
        dismissSolrServers();
    }
    
    @Test
    public void testDateMonth() throws Exception
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
                    list("joel" + bulkAcl.getId()),
                    list("phil" + bulkAcl.getId()),
                    null));
        }

        indexAclChangeSet(bulkAclChangeSet,
                bulkAcls,
                bulkAclReaders);

        int numNodes = 1000;
        List<Node> nodes = new ArrayList();
        List<NodeMetaData> nodeMetaDatas = new ArrayList();

        Transaction bigTxn = getTransaction(0, numNodes);

        Date[] dates = new Date[5];

        Calendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (int i = 0; i < dates.length; i++) {

            cal.set(1980, i, 21);
            dates[i] = cal.getTime();
        }

        int[] counts = new int[dates.length];

        for (int i = 0; i < numNodes; i++) {
            int aclIndex = i % numAcls;
            int dateIndex = i % dates.length;
            String dateString = DefaultTypeConverter.INSTANCE.convert(String.class, dates[dateIndex]);
            counts[dateIndex]++;
            Node node = getNode(bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            node.setShardPropertyValue(dateString);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaData.getProperties().put(ContentModel.PROP_CREATED,
                    new StringPropertyValue(dateString));

            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);
        waitForDocCountAllCores(new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL)), numAcls, 100000);

        List<AlfrescoSolrDataModel.FieldInstance> fieldInstanceList = AlfrescoSolrDataModel.getInstance().getIndexedFieldNamesForProperty(MetadataTracker.getShardProperty("created")).getFields();
        AlfrescoSolrDataModel.FieldInstance fieldInstance = fieldInstanceList.get(0);
        String fieldName = fieldInstance.getField();


        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        for (int i = 0; i < dates.length; i++)
        {
            String startDate = format.format(dates[i]);
            Calendar gcal = new GregorianCalendar();
            gcal.setTime(dates[i]);
            gcal.add(Calendar.SECOND, 1);
            String endDate = format.format(gcal.getTime());

            SolrQuery solrQuery = new SolrQuery("{!lucene}" + escapeQueryChars(fieldName) +
                    ":[" + escapeQueryChars(startDate) + " TO " + escapeQueryChars(endDate) + " } " );
            assertCountAndColocation(solrQuery, counts[i]);
            assertShardSequence(i, solrQuery, counts[i]);
        }

        nodes.clear();
        nodeMetaDatas.clear();

        Transaction bigTxn1 = getTransaction(0, numNodes);

        for (int i = 0; i < numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode(bigTxn1, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn1, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn1, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes*2, 100000);
        //There are 5 shards. We should expect roughly 20% of the nodes on each shard
        assertNodesPerShardGreaterThan((int)((numNodes*2)*.17));
    }

    protected static Properties getShardMethod()
    {
        Properties prop = new Properties();
        prop.put("shard.method", ShardMethodEnum.DATE.toString());
        prop.put("shard.date.grouping", "1");
        return prop;
    }
}

