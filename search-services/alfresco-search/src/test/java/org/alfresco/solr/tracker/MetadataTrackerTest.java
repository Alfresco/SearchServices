/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2024 Alfresco Software Limited
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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.NodeReport;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.GetNodesParameters;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.client.Transactions;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetadataTrackerTest
{
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
        doReturn("0-2000").when(props).getProperty("solr.initial.transaction.range");
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
    public void lastIndexedTransactionUsesCommitTimeThenIdOrdering() throws Exception
    {
        TrackerState state = new TrackerState();
        state.setTimeToStopIndexing(1000L);
        state.setLastIndexedTxCommitTime(0L);
        state.setLastIndexedTxId(0L);

        metadataTracker.state = state;
        when(metadataTracker.getTrackerState()).thenReturn(state);

        Transaction tx1 = new Transaction();
        tx1.setId(1L);
        tx1.setCommitTimeMs(100L);
        tx1.setUpdates(1);

        Transaction tx2 = new Transaction();
        tx2.setId(2L);
        tx2.setCommitTimeMs(100L);
        tx2.setUpdates(1);

        Transaction tx3 = new Transaction();
        tx3.setId(3L);
        tx3.setCommitTimeMs(90L);
        tx3.setUpdates(1);

        Transactions transactions = new Transactions(List.of(tx1, tx3, tx2));
        when(repositoryClient.getTransactions(anyLong(), isNull(), anyLong(), isNull(), anyInt()))
            .thenReturn(transactions)
            .thenReturn(new Transactions(Collections.emptyList()));

        metadataTracker.trackTransactions();

        assertEquals(100L, state.getLastIndexedTxCommitTime());
        assertEquals(2L, state.getLastIndexedTxId());
        // Verify all eligible transactions were indexed by the tracker
        verify(srv, times(1)).indexTransaction(eq(tx1), eq(true));
        verify(srv, times(1)).indexTransaction(eq(tx2), eq(true));
        // Ordering is not asserted here; only eligibility and lastIndexed* logic are.
        verify(srv, times(1)).indexTransaction(eq(tx3), eq(true));
        verify(trackerStats, times(3)).addTxDocs(1);
    }

    @Test
    public void lagCutoffFiltersDeferredTransactionsBeforeBatching() throws Exception
    {
        TrackerState state = new TrackerState();
        state.setTimeToStopIndexing(100L);
        state.setLastIndexedTxCommitTime(0L);
        state.setLastIndexedTxId(0L);

        metadataTracker.state = state;
        when(metadataTracker.getTrackerState()).thenReturn(state);

        Transaction oldTx = new Transaction();
        oldTx.setId(10L);
        oldTx.setCommitTimeMs(90L);
        oldTx.setUpdates(1);

        Transaction boundaryTx = new Transaction();
        boundaryTx.setId(11L);
        boundaryTx.setCommitTimeMs(100L);
        boundaryTx.setUpdates(1);

        Transaction newTx = new Transaction();
        newTx.setId(12L);
        newTx.setCommitTimeMs(110L);
        newTx.setUpdates(1);

        Transactions transactions = new Transactions(List.of(oldTx, boundaryTx, newTx));
        when(repositoryClient.getTransactions(anyLong(), isNull(), anyLong(), isNull(), anyInt()))
            .thenReturn(transactions)
            .thenReturn(new Transactions(Collections.emptyList()));

        metadataTracker.trackTransactions();

        // Verify only lag-eligible transactions were indexed
        verify(srv, times(1)).indexTransaction(eq(oldTx), eq(true));
        verify(srv, times(1)).indexTransaction(eq(boundaryTx), eq(true));
        verify(srv, never()).indexTransaction(eq(newTx), anyBoolean());

        // Verify we stop after hitting the lag boundary
        verify(repositoryClient, times(1)).getTransactions(anyLong(), isNull(), anyLong(), isNull(), anyInt());
        verify(trackerStats, times(2)).addTxDocs(1);

        assertEquals(100L, state.getLastIndexedTxCommitTime());
        assertEquals(11L, state.getLastIndexedTxId());
    }

    private Node getNode()
    {
        Node node = new Node();
        node.setId(DB_ID);
        node.setTxnId(TX_ID);
        return node;
    }

    @Test
    public void testCheckRepoAndIndexConsistency() throws AuthenticationException, IOException, JSONException
    {
        TrackerState state = new TrackerState();
        ModelTracker modelTracker = mock(ModelTracker.class);
        when(modelTracker.hasModels()).thenReturn(true);
        when(this.metadataTracker.getTrackerState()).thenReturn(state);

        TrackerRegistry registry = new TrackerRegistry();
        registry.setModelTracker(modelTracker);
        AlfrescoCoreAdminHandler alfrescoCoreAdminHandler = mock(AlfrescoCoreAdminHandler.class);
        when(this.srv.getAdminHandler()).thenReturn(alfrescoCoreAdminHandler);
        when(alfrescoCoreAdminHandler.getTrackerRegistry()).thenReturn(registry);

        List<Transaction> txsList = new ArrayList<>();
        Transaction tx1 = new Transaction();
        tx1.setCommitTimeMs(1L);
        tx1.setDeletes(1);
        tx1.setUpdates(1);
        txsList.add(tx1);

        Transactions txs = new Transactions(txsList, 0L, 2000L);
        when(repositoryClient.getTransactions(null,  0L, null, 2000L, 1)).thenReturn(txs);
        when(repositoryClient.getTransactions(1L, null, 3600001L, null, 2000)).thenReturn(txs);

        this.metadataTracker.doTrack("AnIterationId");

        verify(this.metadataTracker, times(1)).doTrack("AnIterationId");
    }

    @Test
    public void trackDoesNotAdvanceStateWhenRollbackIsPending() throws Exception
    {
        TrackerState state = new TrackerState();
        metadataTracker.state = state;
        metadataTracker.setRollback(true, new RuntimeException("simulated 403 error"));

        metadataTracker.track();

        verify(srv, never()).continueState(any());
        assertEquals(1L, state.getTrackerCycles());
    }

    /**
     * Verifies that the tracker skips past a block of already-indexed transactions
     * after ALL_INDEXED_CYCLES_THRESHOLD consecutive stuck cycles.
     * Cycles 1-3 behave identically to master (break immediately).
     * Cycle 4 uses the bookmark to start scanning past the stale block.
     */
    @Test
    public void trackerAdvancesPastIndexedPage() throws Exception
    {
        TrackerState state = new TrackerState();
        state.setTimeToStopIndexing(5000L);
        state.setLastIndexedTxCommitTime(200L);
        state.setLastIndexedTxId(2L);
        // lastGoodTxCommitTimeInIndex defaults to 0 (simulates hole retention lag)

        metadataTracker.state = state;
        when(metadataTracker.getTrackerState()).thenReturn(state);

        // Batch 1: two transactions that are already indexed
        Transaction alreadyIndexed1 = new Transaction();
        alreadyIndexed1.setId(1L);
        alreadyIndexed1.setCommitTimeMs(100L);
        alreadyIndexed1.setUpdates(1);

        Transaction alreadyIndexed2 = new Transaction();
        alreadyIndexed2.setId(2L);
        alreadyIndexed2.setCommitTimeMs(200L);
        alreadyIndexed2.setUpdates(1);

        // Batch 2: one new transaction that needs indexing
        Transaction newTx = new Transaction();
        newTx.setId(3L);
        newTx.setCommitTimeMs(300L);
        newTx.setUpdates(1);

        Transactions batch1 = new Transactions(List.of(alreadyIndexed1, alreadyIndexed2));
        Transactions batch2 = new Transactions(List.of(newTx));

        // Marking the existing transactions as already in the index
        when(srv.txnInIndex(1L, true)).thenReturn(true);
        when(srv.txnInIndex(2L, true)).thenReturn(true);

        // Cycles 1-3: same batch each time, all already indexed -> bookmark + break
        // Cycle 4: threshold hit, bookmark skips to 200, gets batch2 with new tx
        when(repositoryClient.getTransactions(anyLong(), isNull(), anyLong(), isNull(), anyInt()))
            .thenReturn(batch1)   // cycle 1
            .thenReturn(batch1)   // cycle 2
            .thenReturn(batch1)   // cycle 3
            .thenReturn(batch2)   // cycle 4
            .thenReturn(new Transactions(Collections.emptyList()));

        // Cycles 1-3: identical to master
        metadataTracker.trackTransactions();
        metadataTracker.trackTransactions();
        metadataTracker.trackTransactions();

        // The already-indexed transactions must not be re-indexed
        verify(srv, never()).indexTransaction(eq(alreadyIndexed1), anyBoolean());
        verify(srv, never()).indexTransaction(eq(alreadyIndexed2), anyBoolean());
        // Not yet reached the new transaction
        verify(srv, never()).indexTransaction(eq(newTx), anyBoolean());

        // Cycle 4: threshold reached, bookmark skips past stale block
        metadataTracker.trackTransactions();

        // New transaction gets indexed
        verify(srv, times(1)).indexTransaction(eq(newTx), eq(true));
        verify(trackerStats, times(1)).addTxDocs(1);

        assertEquals(300L, state.getLastIndexedTxCommitTime());
        assertEquals(3L, state.getLastIndexedTxId());
    }
}
