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
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.AlfrescoSolrDataModel;
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
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

/**
 * Abstract sharding by date test
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public abstract class DistributedDateAbstractSolrTrackerTest extends AbstractAlfrescoDistributedTest
{
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
            bulkAclReaders.add(
                    getAclReaders(bulkAclChangeSet,
                        bulkAcl,
                        singletonList("joel" + bulkAcl.getId()),
                        singletonList("phil" + bulkAcl.getId()),
                        null));
        }

        indexAclChangeSet(bulkAclChangeSet,
                bulkAcls,
                bulkAclReaders);

        int numNodes = 1000;
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaDatas = new ArrayList<>();

        Transaction bigTxn = getTransaction(0, numNodes);

        Date[] dates = setupDates();

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
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "mike", null, false);
            nodeMetaData.getProperties().put(ContentModel.PROP_CREATED, new StringPropertyValue(dateString));

            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);

        List<AlfrescoSolrDataModel.FieldInstance> fieldInstanceList =
                AlfrescoSolrDataModel.getInstance().getIndexedFieldNamesForProperty(MetadataTracker.getShardProperty("created")).getFields();

        AlfrescoSolrDataModel.FieldInstance fieldInstance = fieldInstanceList.get(0);
        String fieldName = fieldInstance.getField();

        SimpleDateFormat formatter = CachingDateFormat.getSolrDatetimeFormatWithoutMsecs();
        for (int i = 0; i < dates.length; i++)
        {
            String startDate = formatter.format(dates[i]);

            Calendar calendar = new GregorianCalendar();
            calendar.setTime(dates[i]);
            calendar.add(Calendar.SECOND, 1);
            String endDate = formatter.format(calendar.getTime());

            SolrQuery query =
                    new SolrQuery(
                            "{!lucene}" +
                                    escapeQueryChars(fieldName) +
                                    ":[" + escapeQueryChars(startDate) + " TO " + escapeQueryChars(endDate) + " } " );
            assertCountAndColocation(query, counts[i]);
            assertCorrect(numNodes);
        }
    }

    /**
     * Initializes 12 test date instances with the following scheme:
     *
     * <ul>
     *     <li>Year: 1980</li>
     *     <li>Month: 0 to 11</li>
     *     <li>Day: 21</li>
     * </ul>
     *
     * @return an array containing 12 test dates.
     */
    protected Date[] setupDates()
    {
        return range(0, 12)
                .mapToObj(index -> {
                    Calendar cal = new GregorianCalendar();
                    cal.set(1980, index, 21);
                    return cal;})
                .map(Calendar::getTime)
                .toArray(Date[]::new);
    }
    
    protected abstract void assertCorrect(int numNodes) throws Exception;
}

