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

import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.client.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Rule;
import org.junit.Test;

import static org.alfresco.solr.AlfrescoSolrUtils.*;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedAlfrescoSolrTrackerRaceTest extends AbstractAlfrescoDistributedTest
{
//
//    @Rule
//    public JettyServerRule jetty = new JettyServerRule(2, this);
//
//    @Test
//    public void testTracker() throws Exception
//    {
//        putHandleDefaults();
//
//
//        AclChangeSet aclChangeSet = getAclChangeSet(1);
//
//        Acl acl = getAcl(aclChangeSet);
//        Acl acl2 = getAcl(aclChangeSet);
//
//        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
//        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, list("jim"), list("phil"), null);
//
//        Transaction txn = getTransaction(0, 2);
//        long txnCommitTimeMs = txn.getCommitTimeMs();
//
//        //Subtract from the commit time to go beyond hole retention
//        long backdatedCommitTimeMs = txnCommitTimeMs-(4600000);
//        txn.setCommitTimeMs(backdatedCommitTimeMs);
//
//        //Next create two nodes to update for the transaction
//        Node folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
//        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
//        Node errorNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
//
//
//        //Next create the NodeMetaData for each node. TODO: Add more metadata
//        NodeMetaData folderMetaData = getNodeMetaData(folderNode, txn, acl, "mike", null, false);
//        NodeMetaData fileMetaData   = getNodeMetaData(fileNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
//        //The errorNodeMetaData will cause an exception.
//        NodeMetaData errorMetaData   = getNodeMetaData(errorNode, txn, acl, "lisa", ancestors(folderMetaData.getNodeRef()), true);
//
//        //Index the transaction, nodes, and nodeMetaDatas.
//        //Note that the content is automatically created by the test framework.
//        indexTransaction(txn,
//            list(errorNode, folderNode, fileNode),
//            list(errorMetaData, folderMetaData, fileMetaData));
//
//
//        indexAclChangeSet(aclChangeSet,
//            list(acl, acl2),
//            list(aclReaders, aclReaders2));
//
//        System.out.println(backdatedCommitTimeMs + ":" + txnCommitTimeMs);
//
//        BooleanQuery.Builder builder = new BooleanQuery.Builder();
//        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
//        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
//        BooleanQuery waitForQuery = builder.build();
//        waitForDocCountAllCores(waitForQuery, 1, 80000);
//
//        /*
//        * Query the index for the content
//        */
//        //This acl should have one record in each core with DBID sharding
//        waitForDocCountAllCores(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, 80000);
//        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2, 100000);
//        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", Long.toString(fileNode.getId()))), 1, 80000);
//
//
//        //This will run the query on the control client and the cluster and compare the result.
//        query(getDefaultTestClient(), true, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
//            params("q", "t1:world", "qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "6", "sort", "id asc"));
//
//    }
//      TODO Investigate how this ever got the correct txn times

}

