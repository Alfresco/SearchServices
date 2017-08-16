/*-
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.solr.query.stream;

import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.stream.AlfrescoSolrStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;

/**
 * @author Michael Suzuki
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AbstractStreamTest extends AbstractAlfrescoDistributedTest
{
    protected Node node1;
    protected Node node2;
    protected Node node3;
    protected Node node4;
    
    @Before
    public void load() throws Exception
    {
        putHandleDefaults();
        /*
        * Create and index an AclChangeSet.
        */

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, list("jim"), list("phil"), null);

        indexAclChangeSet(aclChangeSet,
                list(acl, acl2),
                list(aclReaders, aclReaders2));


        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        //Check that both ACL's are in the index
        BooleanQuery.Builder builder1 = new BooleanQuery.Builder();
        builder1.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL)), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery1 = builder1.build();
        waitForDocCountAllCores(waitForQuery1, 2, 80000);

        /*
        * Create and index a Transaction
        */

        //First create a transaction.
        Transaction txn = getTransaction(0, 4);

        node1 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node2 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node3 = getNode(txn, acl2, Node.SolrApiNodeStatus.UPDATED);
        node4 = getNode(txn, acl2, Node.SolrApiNodeStatus.UPDATED);

        //Next create the NodeMetaData for each node. TODO: Add more metadata

        NodeMetaData nodeMetaData1 = getNodeMetaData(node1, txn, acl, "mike", null, false);
        Date date1 = getDate(2000, 0, 2);
        nodeMetaData1.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1)));

        NodeMetaData nodeMetaData2 = getNodeMetaData(node2, txn, acl, "mike", null, false);
        Date date2 = getDate(2000, 1, 2);
        nodeMetaData2.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date2)));

        NodeMetaData nodeMetaData3 = getNodeMetaData(node3, txn, acl2, "mike", null, false);
        Date date3 = getDate(2000, 2, 2);
        nodeMetaData3.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date3)));

        NodeMetaData nodeMetaData4 = getNodeMetaData(node4, txn, acl2, "mike", null, false);
        Date date4 = getDate(2000, 3, 2);
        nodeMetaData4.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date4)));


        //Index the transaction, nodes, and nodeMetaDatas.
        //Note that the content is automatically created by the test framework.
        indexTransaction(txn,
                list(node1, node2, node3, node4),
                list(nodeMetaData1, nodeMetaData2, nodeMetaData3, nodeMetaData4));

        //Check for the TXN state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();

        waitForDocCountAllCores(waitForQuery, 1, 80000);

        /*
        * Query the index for the content
        */

        waitForDocCountAllCores(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, 80000);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 4, 80000);
    }


    protected void assertNodes(List<Tuple> tuples, Node... nodes) throws Exception {
        for(int i=0; i<nodes.length; i++) {
            Node node = nodes[i];
            Tuple tuple = tuples.get(i);
            if(node.getId() != tuple.getLong("DBID")) {
                throw new Exception("Incorrect Node ID, found "+tuple.getLong("DBID")+" expected "+node.getId());
            }
        }
    }

    protected Date getDate(int year, int month, int day)
    {
        return new Date(new GregorianCalendar(year, month, day, 10, 0).getTimeInMillis());
    }
    /**
     * Build a sql query with alfresco user authentication and parses the response
     * back into tuples.
     * @param sql SQL query to post
     * @param alfrescoJson 
     * @return List<Tuple>
     * @throws IOException if error
     */
    public List<Tuple> sqlQuery(String sql, String alfrescoJson) throws IOException {
        List<SolrClient> clusterClients = getClusterClients();
        String shards = getShardsString(clusterClients);

        SolrParams params = params("stmt", sql, "qt", "/sql", "alfresco.shards", shards);

        AlfrescoSolrStream tupleStream = new AlfrescoSolrStream(((HttpSolrClient) clusterClients.get(0)).getBaseURL(), params);
        tupleStream.setJson(alfrescoJson);
        return getTuples(tupleStream);
    }
}

