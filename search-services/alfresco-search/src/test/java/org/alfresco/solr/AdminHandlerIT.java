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

package org.alfresco.solr;

import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrException;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.Optional.of;
import static org.alfresco.solr.AlfrescoSolrUtils.MAX_WAIT_TIME;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

@SolrTestCaseJ4.SuppressSSL
public class AdminHandlerIT extends AbstractAlfrescoSolrIT
{
    static CoreAdminHandler admin;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
        admin = getMultiCoreHandler();
    }

    @After
    public void clearQueue()
    {
        SOLRAPIQueueClient.NODE_META_DATA_MAP.clear();
        SOLRAPIQueueClient.TRANSACTION_QUEUE.clear();
        SOLRAPIQueueClient.ACL_CHANGE_SET_QUEUE.clear();
        SOLRAPIQueueClient.ACL_READERS_MAP.clear();
        SOLRAPIQueueClient.ACL_MAP.clear();
        SOLRAPIQueueClient.NODE_MAP.clear();
        SOLRAPIQueueClient.NODE_CONTENT_MAP.clear();

        clearIndex();
        assertU(commit());
    }

    @Test(expected = SolrException.class)
    public void testUnhandled() throws Exception
    {
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "totalnonsense",
                CoreAdminParams.NAME, getCore().getName()),
                resp);
    }

    @Test
    public void testhandledCores() throws Exception
    {
        requestAction("newCore");
        requestAction("updateCore");
        requestAction("removeCore");
    }

    @Test
    public void nodeReportOnMasterOrStandaloneContainsTxInfo() throws Exception
    {
        var aclChangeSet = getAclChangeSet(1);
        var acl = getAcl(aclChangeSet);

        indexAclChangeSet(aclChangeSet,
                singletonList(acl),
                singletonList(getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null)));

        var waitForQuery =
                new BooleanQuery.Builder()
                        .add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST))
                        .add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(
                                QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(),
                                aclChangeSet.getId() + 1,
                                true,
                                false), BooleanClause.Occur.MUST))
                        .build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);

        var txn = getTransaction(0, 4);
        var node = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        var nodeMetadata = getNodeMetaData(node, txn, acl, "mike", null, false);

        indexTransaction(txn, singletonList(node), singletonList(nodeMetadata));

        makeSureTransactionHasBeenIndexed(txn.getId());

        var request = req(CoreAdminParams.ACTION, "nodereport", CoreAdminParams.NAME, getCore().getName());
        var params =
                new ModifiableSolrParams(request.getParams())
                        .set("nodeid", Long.toString(node.getId()));
        request.setParams(params);
        var response = new SolrQueryResponse();

        admin.handleRequestBody(request, response);

        var data = response.getValues();

        var report =
                ofNullable(data.get("report"))
                        .map(NamedList.class::cast)
                        .orElseThrow(() -> new AssertionError("'report' section not in response. Response was " + data));

        var collection =
                of(report.get("collection1"))
                    .map(NamedList.class::cast)
                    .orElseThrow(() -> new AssertionError("'collection' section not in response. Response was " + data));

        of(collection.get("Node DBID"))
                .map(Long.class::cast)
                .filter(dbid -> node.getId() == dbid)
                .orElseThrow(() -> new AssertionError("'Node DBID' mismatch or not found. Expected '" + node.getId() + "'Response was " + data));

        of(collection.get("DB TX"))
                .map(Long.class::cast)
                .filter(dbtx -> txn.getId() == dbtx)
                .orElseThrow(() -> new AssertionError("'DB TX' mismatch or not found. Expected '" + txn.getId() + "' Response was " + data));

        of(collection.get("DB TX Status"))
                .map(String.class::cast)
                .filter("UPDATED"::equals)
                .orElseThrow(() -> new AssertionError("'DB TX' mismatch or not found. Expected 'UPDATED', Response was " + data));

    }

    @Test
    public void testhandledReports() throws Exception
    {
        requestAction("CHECK");
        requestAction("REPORT");
        requestAction("SUMMARY");
    }

    private void requestAction(String actionName) throws Exception
    {
        admin.handleRequestBody(req(CoreAdminParams.ACTION, actionName,
                CoreAdminParams.NAME, getCore().getName()),
                new SolrQueryResponse());
    }

    private void makeSureTransactionHasBeenIndexed(long transactionId) throws Exception
    {
        //Check for the TXN state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, transactionId, transactionId + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
    }
}
