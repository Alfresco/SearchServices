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

package org.alfresco.solr.tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.solr.*;
import org.alfresco.solr.client.GetNodesParameters;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.client.Transactions;
import org.alfresco.util.Pair;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TXID;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetadataTrackerTest
{
    private static final String DEFAULT_INITIAL_TRANSACTION_RANGE = "0-2000";

    private Pair<Long, Long> minTxnIdRange;
    private final static Long TX_ID = 10000000L;
    private final static Long DB_ID = 999L;
    private MetadataTracker metadataTracker;

    @Mock
    private SOLRAPIClient repositoryClient;

    @Mock
    private InformationServer srv;

    @Spy
    private Properties props;

    @Mock
    private TrackerStats trackerStats;

    @Mock
    private TrackerState trackerState;

    @Before
    public void setUp()
    {
        doReturn("2000-8000").when(props).getProperty("solr.initial.transaction.range");
        doReturn("workspace://SpacesStore").when(props).getProperty("alfresco.stores");
        when(srv.getTrackerStats()).thenReturn(trackerStats);
        String coreName = "theCoreName";
        this.metadataTracker = spy(new MetadataTracker(props, repositoryClient, coreName, srv));

        ModelTracker modelTracker = mock(ModelTracker.class);
        TrackerRegistry registry = new TrackerRegistry();
        registry.setModelTracker(modelTracker);
        metadataTracker.state = trackerState;
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void doTrackWithOneTransactionUpdatesOnce() throws AuthenticationException, IOException, JSONException, EncoderException
    {
        TrackerState state = new TrackerState();
        state.setTimeToStopIndexing(2L);
        when(srv.getTrackerInitialState()).thenReturn(state);
        // TrackerState is persisted per tracker
        when(this.metadataTracker.getTrackerState()).thenReturn(state);

        List<Transaction> txsList = new ArrayList<>();
        Transaction tx = new Transaction();
        tx.setCommitTimeMs(1L);
        tx.setDeletes(1);
        tx.setUpdates(1);
        txsList.add(tx);
        Transactions txs = mock(Transactions.class);
        when(txs.getTransactions()).thenReturn(txsList);

        // Subsequent calls to getTransactions must return a different set of transactions to avoid an infinite loop
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(txs)
                    .thenReturn(txs).thenReturn(mock(Transactions.class));
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), isNull())).thenReturn(txs)
        .thenReturn(txs).thenReturn(mock(Transactions.class));
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), any(ShardState.class))).thenReturn(txs)
        .thenReturn(txs).thenReturn(mock(Transactions.class));

        List<Node> nodes = new ArrayList<>();
        Node node = new Node();
        nodes.add(node );
        when(repositoryClient.getNodes(any(GetNodesParameters.class), anyInt())).thenReturn(nodes);
        
        this.metadataTracker.doTrack("AnIterationId");

        InOrder inOrder = inOrder(srv);
        inOrder.verify(srv).indexNodes(nodes, true);
        inOrder.verify(srv).indexTransaction(tx, true);
        inOrder.verify(srv).commit();
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void doTrackWithNoTransactionsDoesNothing() throws AuthenticationException, IOException, JSONException, EncoderException
    {
        TrackerState state = new TrackerState();
        when(srv.getTrackerInitialState()).thenReturn(state);
        when(this.metadataTracker.getTrackerState()).thenReturn(state);

        Transactions txs = mock(Transactions.class);
        List<Transaction> txsList = new ArrayList<>();
        when(txs.getTransactions()).thenReturn(txsList);

        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), isNull())).thenReturn(txs);
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), any(ShardState.class))).thenReturn(txs);
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(txs);

        this.metadataTracker.doTrack("AnIterationId");

        verify(srv, never()).commit();
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void testCheckNodeLong() throws AuthenticationException, IOException, JSONException
    {
        List<Node> nodes = getNodes();
        when(repositoryClient.getNodes(any(GetNodesParameters.class), eq(1))).thenReturn(nodes);
        
        NodeReport nodeReport = this.metadataTracker.checkNode(DB_ID);
        
        assertNotNull(nodeReport);
        assertEquals(DB_ID, nodeReport.getDbid());
        assertEquals(TX_ID, nodeReport.getDbTx());
    }

    private List<Node> getNodes()
    {
        List<Node> nodes = new ArrayList<>();
        Node node = getNode();
        nodes.add(node);
        return nodes;
    }
    
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void testCheckNodeNode()
    {
        Node node = getNode();
        
        NodeReport nodeReport = this.metadataTracker.checkNode(node);
        
        assertNotNull(nodeReport);
        assertEquals(DB_ID, nodeReport.getDbid());
        assertEquals(TX_ID, nodeReport.getDbTx());
    }

    @Test
    public void incomingCommitTimeIsLesserThanLastIndexedTxCommitTime_transactionShouldBeMarkedAsIndexed() throws Exception {
        var incomingTransactionCommitTime = 10L;
        var lastIndexedTransactionCommitTime = incomingTransactionCommitTime + 1;

        var incomingTransaction = new Transaction();
        incomingTransaction.setId(1);
        incomingTransaction.setCommitTimeMs(incomingTransactionCommitTime);

        when(srv.txnInIndex(incomingTransaction.getId(), true)).thenReturn(true);
        when(trackerState.getLastIndexedTxCommitTime()).thenReturn(lastIndexedTransactionCommitTime);

        assertFalse(metadataTracker.isTransactionToBeIndexed(incomingTransaction));
    }

    @Test
    public void incomingCommitTimeIsLesserThanLastIndexedTxCommitTimeButTheTransactionIsNotIndexed_transactionShouldBeMarkedAsToBeIndexed() throws Exception {
        var incomingTransactionCommitTime = 10L;
        var lastIndexedTransactionCommitTime = incomingTransactionCommitTime + 1;

        var incomingTransaction = new Transaction();
        incomingTransaction.setId(1);
        incomingTransaction.setCommitTimeMs(incomingTransactionCommitTime);

        when(srv.txnInIndex(incomingTransaction.getId(), true)).thenReturn(false);
        when(trackerState.getLastIndexedTxCommitTime()).thenReturn(lastIndexedTransactionCommitTime);

        assertTrue(metadataTracker.isTransactionToBeIndexed(incomingTransaction));
    }

    @Test
    public void incomingCommitTimeIsGreaterThanLastIndexedTxCommitTime_transactionShouldBeMarkedAsToBeIndexed() {
        var lastIndexedTransactionCommitTime = 10L;
        var incomingTransactionCommitTime = lastIndexedTransactionCommitTime + 1;

        var incomingTransaction = new Transaction();
        incomingTransaction.setId(1);
        incomingTransaction.setCommitTimeMs(incomingTransactionCommitTime);

        when(trackerState.getLastIndexedTxCommitTime()).thenReturn(lastIndexedTransactionCommitTime);

        assertTrue(metadataTracker.isTransactionToBeIndexed(incomingTransaction));
    }

    @Test
    public void incomingCommitTimeIsGreaterThanLastIndexedTxCommitTimeButTheTransactionIsAlreadyIndexed_transactionShouldBeMarkedAsToBeIndexed() {
        var lastIndexedTransactionCommitTime = 10L;
        var incomingTransactionCommitTime = lastIndexedTransactionCommitTime + 1;

        var incomingTransaction = new Transaction();
        incomingTransaction.setId(1);
        incomingTransaction.setCommitTimeMs(incomingTransactionCommitTime);

        when(trackerState.getLastIndexedTxCommitTime()).thenReturn(lastIndexedTransactionCommitTime);

        assertTrue(metadataTracker.isTransactionToBeIndexed(incomingTransaction));
    }

    @Test
    public void anIOExceptionIsRaised_transactionShouldBeMarkedAsToBeIndexed() throws Exception {
        var incomingTransactionCommitTime = 10L;
        var lastIndexedTransactionCommitTime = incomingTransactionCommitTime + 1;

        var incomingTransaction = new Transaction();
        incomingTransaction.setId(1);
        incomingTransaction.setCommitTimeMs(incomingTransactionCommitTime);

        when(srv.txnInIndex(incomingTransaction.getId(), true)).thenThrow(new IOException());
        when(trackerState.getLastIndexedTxCommitTime()).thenReturn(lastIndexedTransactionCommitTime);

        assertTrue(metadataTracker.isTransactionToBeIndexed(incomingTransaction));
    }

    @Test
    public void testCheckRepoAndIndexConsistency() throws AuthenticationException, IOException, JSONException {
        ModelTracker modelTracker = mock(ModelTracker.class);
        TrackerState trackerState = mock(TrackerState.class);
        when(trackerState.getTrackerCycles()).thenReturn(0);
        when(modelTracker.hasModels()).thenReturn(true);
        AlfrescoCoreAdminHandler alfrescoCoreAdminHandler = mock(AlfrescoCoreAdminHandler.class);
        TrackerRegistry trackerRegistry = mock(TrackerRegistry.class);
        when(this.srv.getAdminHandler()).thenReturn(alfrescoCoreAdminHandler);
        when(alfrescoCoreAdminHandler.getTrackerRegistry()).thenReturn(trackerRegistry);
        when(trackerRegistry.getModelTracker()).thenReturn(modelTracker);
        List<Transaction> txsList = new ArrayList<>();
        Transaction tx1 = new Transaction();
        tx1.setCommitTimeMs(1L);
        tx1.setDeletes(1);
        tx1.setUpdates(1);
        txsList.add(tx1);;
        Transactions txs = new Transactions(txsList, 2000L, 8000L);
        when(repositoryClient.getTransactions(null, 2000L, null, 8000L, 1)).thenReturn(txs);
        when(repositoryClient.getTransactions(0L, 2000L, null, 8000L, 1)).thenReturn(txs);
        when(this.srv.getTxDocsSize("0","1")).thenReturn(1);
        when(this.srv.getMaxTransactionIdAndCommitTimeInIndex()).thenReturn(tx1);
        when(repositoryClient.getTransactions(0L, null, 3600000L, null, 2000)).thenReturn(txs);
        try
        {
            this.metadataTracker.doTrack("AnIterationId");
        }
        catch (Exception ex)
        {
            fail("testCheckRepoAndIndexConsistency test method failed due to " + ex.getMessage());
        }
    }

    private Node getNode()
    {
        Node node = new Node();
        node.setId(DB_ID);
        node.setTxnId(TX_ID);
        return node;
    }
}
