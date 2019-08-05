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

import static java.util.Collections.singletonList;
import static java.util.stream.IntStream.range;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

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
import org.alfresco.util.CachingDateFormat;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
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

@SolrTestCaseJ4.SuppressSSL
public class DistributedDateMonthAlfrescoSolrTrackerTest extends AbstractAlfrescoDistributedTest
{
    @BeforeClass
    @SuppressWarnings("unused")
    public static void initData() throws Throwable
    {
        initSolrServers(5, DistributedDateMonthAlfrescoSolrTrackerTest.class.getSimpleName(), getShardMethod());
    }

    @AfterClass
    @SuppressWarnings("unused")
    public static void destroyData()
    {
        dismissSolrServers();
    }

    @Test
    public void testDateMonth() throws Exception
    {
        putHandleDefaults();

        int numAcls = 25;
        AclChangeSet bulkAclChangeSet = getAclChangeSet(numAcls);

        List<Acl> bulkAcls = new ArrayList<>();
        List<AclReaders> bulkAclReaders = new ArrayList<>();

        for (int i = 0; i < numAcls; i++)
        {
            Acl bulkAcl = getAcl(bulkAclChangeSet);
            bulkAcls.add(bulkAcl);
            bulkAclReaders.add(getAclReaders(bulkAclChangeSet,
                    bulkAcl,
                    singletonList("joel" + bulkAcl.getId()),
                    singletonList("phil" + bulkAcl.getId()),
                    null));
        }

        indexAclChangeSet(bulkAclChangeSet, bulkAcls, bulkAclReaders);

        int numNodes = 1000;
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaData = new ArrayList<>();

        Transaction bigTxn = getTransaction(0, numNodes);

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date[] dates = range(0, 5)
                .peek(index -> calendar.set(1980, index, 21))
                .mapToObj(index -> calendar.getTime())
                .toArray(Date[]::new);

        int[] counts = new int[dates.length];

        for (int i = 0; i < numNodes; i++)
        {
            int aclIndex = i % numAcls;
            int dateIndex = i % dates.length;
            String dateString = DefaultTypeConverter.INSTANCE.convert(String.class, dates[dateIndex]);

            counts[dateIndex]++;

            Node node = getNode(bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            node.setShardPropertyValue(dateString);
            nodes.add(node);

            NodeMetaData metadata = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            metadata.getProperties().put(ContentModel.PROP_CREATED, new StringPropertyValue(dateString));

            nodeMetaData.add(metadata);
        }

        indexTransaction(bigTxn, nodes, nodeMetaData);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);
        waitForDocCountAllCores(new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL)), numAcls, 100000);

        List<AlfrescoSolrDataModel.FieldInstance> fieldInstanceList = AlfrescoSolrDataModel.getInstance().getIndexedFieldNamesForProperty(MetadataTracker.getShardProperty("created")).getFields();
        AlfrescoSolrDataModel.FieldInstance fieldInstance = fieldInstanceList.get(0);
        String fieldName = fieldInstance.getField();

        SimpleDateFormat format = CachingDateFormat.getSolrDatetimeFormatWithoutMsecs();
        for (int i = 0; i < dates.length; i++)
        {
            String startDate = format.format(dates[i]);
            Calendar gcal = new GregorianCalendar();
            gcal.setTime(dates[i]);
            gcal.add(Calendar.SECOND, 1);
            String endDate = format.format(gcal.getTime());

            SolrQuery solrQuery =
                    new SolrQuery("{!lucene}" + escapeQueryChars(fieldName) + ":[" + escapeQueryChars(startDate) + " TO " + escapeQueryChars(endDate) + " } " );

            assertCountAndColocation(solrQuery, counts[i]);
            assertShardSequence(i, solrQuery, counts[i]);
        }

        nodes.clear();
        nodeMetaData.clear();

        Transaction bigTxn1 = getTransaction(0, numNodes);

        for (int i = 0; i < numNodes; i++)
        {
            int aclIndex = i % numAcls;
            Node node = getNode(bigTxn1, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);

            NodeMetaData metadata = getNodeMetaData(node, bigTxn1, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaData.add(metadata);
        }

        indexTransaction(bigTxn1, nodes, nodeMetaData);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes*2, 100000);

        //There are 5 shards. We should expect roughly 20% of the nodes on each shard
        assertNodesPerShardGreaterThan((int)((numNodes*2)*.17));
    }

    private static Properties getShardMethod()
    {
        Properties prop = new Properties();
        prop.put("shard.method", ShardMethodEnum.DATE.toString());
        prop.put("shard.date.grouping", "1");
        return prop;
    }
}

