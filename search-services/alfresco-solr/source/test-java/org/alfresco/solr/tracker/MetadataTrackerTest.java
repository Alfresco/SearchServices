/*
 * Copyright (C) 2014 Alfresco Software Limited.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetadataTrackerTest
{
    private final static Long TX_ID = 10000000L;
    private final static Long DB_ID = 999L;
    private MetadataTracker metadataTracker;

    @Mock
    private SOLRAPIClient repositoryClient;
    private String coreName = "theCoreName";
    @Mock
    private InformationServer srv;
    @Spy
    private Properties props;
    @Mock
    private TrackerStats trackerStats;

    @Before
    public void setUp() throws Exception
    {
        doReturn("workspace://SpacesStore").when(props).getProperty("alfresco.stores");
        when(srv.getTrackerStats()).thenReturn(trackerStats);
        this.metadataTracker = spy(new MetadataTracker(props, repositoryClient, coreName, srv));

        ModelTracker modelTracker = mock(ModelTracker.class);
        when(modelTracker.hasModels()).thenReturn(true);
        AlfrescoCoreAdminHandler adminHandler = mock(AlfrescoCoreAdminHandler.class);
        TrackerRegistry registry = new TrackerRegistry();
        registry.setModelTracker(modelTracker);
        when(adminHandler.getTrackerRegistry()).thenReturn(registry);
        when(srv.getAdminHandler()).thenReturn(adminHandler);
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
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), isNull(ShardState.class))).thenReturn(txs)
        .thenReturn(txs).thenReturn(mock(Transactions.class));
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), any(ShardState.class))).thenReturn(txs)
        .thenReturn(txs).thenReturn(mock(Transactions.class));

        List<Node> nodes = new ArrayList<>();
        Node node = new Node();
        nodes.add(node );
        when(repositoryClient.getNodes(any(GetNodesParameters.class), anyInt())).thenReturn(nodes);
        
        this.metadataTracker.doTrack();

        InOrder inOrder = inOrder(srv);
        inOrder.verify(srv).indexNodes(nodes, true, false);
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

        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), isNull(ShardState.class))).thenReturn(txs);
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), any(ShardState.class))).thenReturn(txs);
        when(repositoryClient.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(txs);

        this.metadataTracker.doTrack();

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

    private Node getNode()
    {
        Node node = new Node();
        node.setId(DB_ID);
        node.setTxnId(TX_ID);
        return node;
    }
    
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void testGetFullNodesForDbTransaction() throws AuthenticationException, IOException, JSONException
    {
        List<Node> nodes = getNodes();
        when(repositoryClient.getNodes(any(GetNodesParameters.class), anyInt())).thenReturn(nodes);
        
        List<Node> nodes4Tx = this.metadataTracker.getFullNodesForDbTransaction(TX_ID);
        
        assertSame(nodes4Tx, nodes);
    }
    
    
}
