/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

/**
 * @author Joel
 * @author Andrea Gazzarini
 */
@SolrTestCaseJ4.SuppressSSL
public class DistributedAlfrescoSolrTrackerRaceIT extends AbstractAlfrescoDistributedIT
{

    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(2, "DistributedAlfrescoSolrTrackerRaceIT", null);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    @Test
    public void testTracker() throws Exception {
        putHandleDefaults();

        AclChangeSet aclChangeSet = getAclChangeSet(2, 1, System.currentTimeMillis() -
                AbstractTracker.TIME_STEP_32_DAYS_IN_MS);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders =
                getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        AclReaders aclReaders2 =
                getAclReaders(aclChangeSet, acl2, singletonList("jim"), singletonList("phil"), null);

        // Transaction between [1-2000] is required, when greater value checking the core will fail
        Transaction txn = getTransaction(0, 2, 1,
                System.currentTimeMillis() - AbstractTracker.TIME_STEP_32_DAYS_IN_MS);

        //Next create two nodes to update for the transaction
        Node folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node errorNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        // Next, create the node metadata for each node.
        // Note: the error node metadata will cause an exception.
        NodeMetaData folderMetaData =
                getNodeMetaData(folderNode, txn, acl, "mike", null, false);
        NodeMetaData fileMetaData = getNodeMetaData(fileNode, txn, acl, "mike",
                ancestors(folderMetaData.getNodeRef()), false);
        NodeMetaData errorMetaData = getNodeMetaData(errorNode, txn, acl, "lisa",
                ancestors(folderMetaData.getNodeRef()), true);

        // Index the transaction, nodes, and nodeMetaDatas.
        // Note that the content is automatically created by the test framework.
        indexTransaction(txn, asList(errorNode, folderNode, fileNode), asList(errorMetaData, folderMetaData, fileMetaData));
        indexAclChangeSet(aclChangeSet, asList(acl, acl2), asList(aclReaders, aclReaders2));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")),
                BooleanClause.Occur.MUST));

        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID,
                aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false),
                BooleanClause.Occur.MUST));

        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, INDEX_TIMEOUT);

        // This ACL should have one record in each core with DBID sharding
        waitForDocCountAllCores(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, INDEX_TIMEOUT);

        // We should have 2 document in totals (1 folder and 1 file)
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content",
                "world")), 2, INDEX_TIMEOUT);

        // There should be 1 with the folder node identifier.
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content",
                Long.toString(folderNode.getId()))), 1, INDEX_TIMEOUT);

        // There should be 1 with the file node identifier.
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content",
                Long.toString(fileNode.getId()))), 1, INDEX_TIMEOUT);

        // and last but not least, the error node shouldn't be in the index.
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content",
                Long.toString(errorNode.getId()))), 0, INDEX_TIMEOUT);

        // This will run the same query on the control client and the cluster and compare the result.
        query(getDefaultTestClient(),
                true,
                "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                params("q", "t1:world", "qt", "/afts", "shards.qt", "/afts", "start", "0", "rows", "6", "sort", "id asc"));
    }
}