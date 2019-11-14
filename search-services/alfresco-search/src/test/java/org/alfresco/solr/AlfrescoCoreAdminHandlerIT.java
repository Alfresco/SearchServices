/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
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
package org.alfresco.solr;

import static java.util.Arrays.asList;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ALFRESCO_CORE_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARCHIVE_CORE_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARG_TXID;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.STORE_REF_MAP;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.VERSION_CORE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.DocRouter;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.PropertyRouter;
import org.alfresco.solr.tracker.SlaveCoreStatePublisher;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link org.alfresco.solr.AlfrescoCoreAdminHandler}. */
@RunWith(MockitoJUnitRunner.class)
public class AlfrescoCoreAdminHandlerIT
{
    /** The string representing a transaction report. */
    private static final String TXREPORT = "TXREPORT";
    /** A transaction id. */
    private static final String TX_ID = "123";
    /** A core name. */
    private static final String CORE_NAME = "CORE_NAME";

    /** The class under test. */
    @InjectMocks
    private AlfrescoCoreAdminHandler alfrescoCoreAdminHandler;
    @Mock
    private TrackerRegistry trackerRegistry;
    @Mock
    private AclTracker aclTracker;
    @Mock
    private MetadataTracker metadataTracker;
    @Mock
    private IndexHealthReport indexHealthReport;
    @Mock
    private IndexHealthReport metaReport;
    @Mock
    private IOpenBitSet iOpenBitSet;
    @Mock
    private TrackerState trackerState;
    @Mock
    private InformationServer informationServer;
    @Mock
    private SolrQueryRequest req;
    @Mock
    private SolrQueryResponse rsp;
    @Mock
    private SolrParams params;

    @Before
    public void setUp()
    {
        // Wire the mocks into the class under test.
        alfrescoCoreAdminHandler.setTrackerRegistry(trackerRegistry);
        alfrescoCoreAdminHandler.getInformationServers().put(CORE_NAME, informationServer);

        when(req.getParams()).thenReturn(params);
    }

    @Test
    public void extractShardsWithEmptyParameter_shouldReturnAnEmptyList()
    {
        assertTrue(alfrescoCoreAdminHandler.extractShards("", Integer.MAX_VALUE).isEmpty());
    }

    @Test
    public void extractShardsWithNullParameter_shouldReturnAnEmptyList()
    {
        assertTrue(alfrescoCoreAdminHandler.extractShards(null, Integer.MAX_VALUE).isEmpty());
    }

    @Test
    public void extractShardsWithOneInvalidShard_shouldReturnAnEmptyList()
    {
        assertTrue(alfrescoCoreAdminHandler.extractShards("This is an invalid shard id", Integer.MAX_VALUE).isEmpty());
    }

    @Test
    public void extractShardsWithOneShards_shouldReturnSingletonList()
    {
        assertEquals(singletonList(1), alfrescoCoreAdminHandler.extractShards("1", Integer.MAX_VALUE));
    }

    @Test
    public void extractShardsWithSeveralValidShards_shouldReturnAllOfThemInTheList()
    {
        assertEquals(asList(1,5,6,11,23), alfrescoCoreAdminHandler.extractShards("1,5,6,11,23", Integer.MAX_VALUE));
    }

    @Test
    public void extractShardsWithSeveralValidShards_shouldReturnOnlyValidIdentifiers()
    {
        assertEquals(asList(1,5,6,11,23), alfrescoCoreAdminHandler.extractShards("1,5,A,6,xyz,11,BB,23,o01z", Integer.MAX_VALUE));
    }

    @Test
    public void extractShardsWithSeveralValidShardsAndLimit_shouldConsiderOnlyShardsLesserThanLimit()
    {
        assertEquals(asList(1,5,6,11,12), alfrescoCoreAdminHandler.extractShards("1,5,6,11,23,25,99,223,12", 23));
    }

    @Test
    public void hasAlfrescoCoreWhenInputIsNull_shouldReturnFalse()
    {
        assertFalse(alfrescoCoreAdminHandler.hasAlfrescoCore(null));
    }

    @Test
    public void hasAlfrescoCoreWhenWeHaveNoCore_shouldReturnFalse()
    {
        assertFalse(alfrescoCoreAdminHandler.hasAlfrescoCore(emptyList()));
    }

    @Test
    public void hasAlfrescoCoreWhenDoesntHaveAnyTracker_shouldReturnFalse()
    {
        assertFalse(alfrescoCoreAdminHandler.hasAlfrescoCore(emptyList()));
    }

    @Test
    public void hasAlfrescoCoreWithRegisteredTrackers_shouldReturnTrue()
    {
        when(trackerRegistry.hasTrackersForCore("CoreD")).thenReturn(true);
        assertTrue(alfrescoCoreAdminHandler.hasAlfrescoCore(asList(dummyCore("CoreA"), dummyCore("CoreB"), dummyCore("CoreC"), dummyCore("CoreD"))));
    }

    @Test
    public void trackerRegistryHasNoCoreNames_itShouldReturnAnEmptyList()
    {
        assertTrue(alfrescoCoreAdminHandler.coreNames().isEmpty());
    }

    @Test
    public void coreDetectedAsMasterOrStandalone()
    {
        MetadataTracker coreStatePublisher = mock(MetadataTracker.class);

        when(trackerRegistry.getTrackerForCore(anyString(), eq(MetadataTracker.class)))
                .thenReturn(coreStatePublisher);

        assertTrue(alfrescoCoreAdminHandler.isMasterOrStandalone("ThisIsTheCoreName"));
    }

    @Test
    public void coreDetectedAsSlave()
    {
        when(trackerRegistry.getTrackerForCore(anyString(), eq(MetadataTracker.class))).thenReturn(null);
        assertFalse(alfrescoCoreAdminHandler.isMasterOrStandalone("ThisIsTheCoreName"));
    }

    @Test
    public void coreIsMaster_thenCoreStatePublisherInstanceCorrespondsToMetadataTracker()
    {
        MetadataTracker coreStatePublisher = mock(MetadataTracker.class);

        when(trackerRegistry.getTrackerForCore(anyString(), eq(MetadataTracker.class)))
                .thenReturn(coreStatePublisher);

        assertSame(coreStatePublisher, alfrescoCoreAdminHandler.coreStatePublisher("ThisIsTheCoreName"));
    }

    @Test
    public void coreIsSlave_thenCoreStatePublisherInstanceCorrespondsToSlaveCoreStatePublisher()
    {
        SlaveCoreStatePublisher coreStatePublisher = mock(SlaveCoreStatePublisher.class);

        when(trackerRegistry.getTrackerForCore(anyString(), eq(MetadataTracker.class))).thenReturn(null);
        when(trackerRegistry.getTrackerForCore(anyString(), eq(SlaveCoreStatePublisher.class))).thenReturn(coreStatePublisher);

        assertSame(coreStatePublisher, alfrescoCoreAdminHandler.coreStatePublisher("ThisIsTheCoreName"));
    }

    @Test
    public void coreIsSlave_thenDocRouterIsNull()
    {
        String coreName = "aCore";
        when(trackerRegistry.getTrackerForCore(eq(coreName), eq(MetadataTracker.class))).thenReturn(null);
        assertNull(alfrescoCoreAdminHandler.getDocRouter("aCore"));
    }

    @Test
    public void coreIsMaster_thenDocRouterIsProperlyReturned()
    {
        DocRouter expectedRouter = new PropertyRouter("someProperty_.{1,35}");

        MetadataTracker coreStatePublisher = mock(MetadataTracker.class);
        when(coreStatePublisher.getDocRouter()).thenReturn(expectedRouter);
        when(trackerRegistry.getTrackerForCore(anyString(), eq(MetadataTracker.class))).thenReturn(coreStatePublisher);

        assertSame(expectedRouter, alfrescoCoreAdminHandler.getDocRouter("aCore"));
    }

    @Test
    public void targetCoreNameCanBeSpecifiedInSeveralWays()
    {
        String coreName = "ThisIsTheCoreName";

        ModifiableSolrParams params = new ModifiableSolrParams();

        assertNull(alfrescoCoreAdminHandler.coreName(params));

        params.set(CoreAdminParams.CORE, coreName);

        assertEquals(coreName, alfrescoCoreAdminHandler.coreName(params));

        params.remove(CoreAdminParams.CORE);
        assertNull(alfrescoCoreAdminHandler.coreName(params));

        params.set("coreName", coreName);

        assertEquals(coreName, alfrescoCoreAdminHandler.coreName(params));
        assertEquals(coreName, alfrescoCoreAdminHandler.coreName(params));
    }


    private SolrCore dummyCore(String name)
    {
        SolrCore core = mock(SolrCore.class);
        when(core.getName()).thenReturn(name);
        return core;
    }

    /** Check that a transaction report can be generated. */
    @Test
    public void handleCustomActionTXReportSuccess() throws Exception
    {
        // Set up the parameters being passed in.
        when(params.get(CoreAdminParams.ACTION)).thenReturn(TXREPORT);
        when(params.get(CoreAdminParams.CORE)).thenReturn(CORE_NAME);
        when(params.get(ARG_TXID)).thenReturn(TX_ID);
        // Set up the mock ACL tracker.
        when(trackerRegistry.getTrackerForCore(CORE_NAME, AclTracker.class)).thenReturn(aclTracker);
        when(aclTracker.checkIndex(Long.valueOf(TX_ID), 0L, null, null)).thenReturn(indexHealthReport);
        when(indexHealthReport.getDuplicatedAclTxInIndex()).thenReturn(iOpenBitSet);
        when(indexHealthReport.getAclTxInIndexButNotInDb()).thenReturn(iOpenBitSet);
        when(indexHealthReport.getMissingAclTxFromIndex()).thenReturn(iOpenBitSet);
        when(aclTracker.getTrackerState()).thenReturn(trackerState);
        // Set up the mock metadata tracker.
        when(trackerRegistry.getTrackerForCore(CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);
        when(metadataTracker.checkIndex(Long.valueOf(TX_ID), 0L, null, null)).thenReturn(metaReport);
        when(metaReport.getDuplicatedTxInIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getTxInIndexButNotInDb()).thenReturn(iOpenBitSet);
        when(metaReport.getMissingTxFromIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getDuplicatedLeafInIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getDuplicatedErrorInIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getDuplicatedUnindexedInIndex()).thenReturn(iOpenBitSet);
        when(metadataTracker.getTrackerState()).thenReturn(trackerState);

        // Call the method under test.
        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        // Check that a report was generated (don't look at the contents of the report though).
        verify(rsp).add(eq("report"), any(NamedList.class));
    }

    /** Check that when the transaction id is missing we get an exception. */
    @Test(expected = SolrException.class)
    public void handleCustomActionTXReportMissingTXId()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn(TXREPORT);
        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        verify(rsp, never()).add(anyString(), any());
    }

    /** Check that when the core name is missing we get an exception. */
    @Test(expected = SolrException.class)
    public void handleCustomActionTXReportMissingCoreName()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn(TXREPORT);
        when(params.get(CoreAdminParams.CORE)).thenReturn(null);

        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);
    }

    /** Check that when an unknown action is provided we don't generate a report. */
    @Test(expected = SolrException.class)
    public void handleCustomActionUnknownAction()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn("Unknown");

        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        verify(rsp, never()).add(anyString(), any());
    }

    /** Check that when the action is missing we don't generate a report. */
    @Test(expected = SolrException.class)
    public void handleCustomActionMissingAction()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn(null);

        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        verify(rsp, never()).add(anyString(), any());
    }

    @Test
    public void invalidCoreNameInRequest() {
        AlfrescoCoreAdminHandler spy = spy(alfrescoCoreAdminHandler);

        // First let's try a list of invalid names, one by one
        List<String> invalidNames =
                STORE_REF_MAP.keySet().stream()
                    .map(coreName -> coreName + System.currentTimeMillis())
                    .collect(Collectors.toList());

        invalidNames.forEach(spy::setupNewDefaultCores);

        verify(spy, never()).newCore(any(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());

        reset(spy);

        // Then, the same list as a single parameter (e.g. name1, name2, name3, etc)
        String commaSeparatedNames = String.join(",", invalidNames);
        spy.setupNewDefaultCores(commaSeparatedNames);

        verify(spy, never()).newCore(any(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    public void coreNamesAreTrimmed_oneCoreNameAtTime() {
        AlfrescoCoreAdminHandler spy = spy(new AlfrescoCoreAdminHandler() {
            @Override
            protected void newCore(String coreName, int numShards, StoreRef storeRef, String templateName, int replicationFactor, int nodeInstance, int numNodes, String shardIds, Properties extraProperties, SolrQueryResponse rsp)
            {
                // Do nothing here otherwise we cannot spy it
            }
        });

        // First let's try a list of names, one by one
        final List<String> coreNames =
                asList(
                        ARCHIVE_CORE_NAME + "  ", // whitespace char at the end
                        "\t " + ALFRESCO_CORE_NAME, // whitespace chars at the beginning
                        "   " + VERSION_CORE_NAME + "  \t", // beginning and end
                        "   \t"); // empty name

        coreNames.forEach(spy::setupNewDefaultCores);

        verify(spy).newCore(eq(ARCHIVE_CORE_NAME), eq(1), eq(STORE_REF_MAP.get(ARCHIVE_CORE_NAME)), anyString(), eq(1), eq(1), eq(1), eq(null), eq(null), any());
        verify(spy).newCore(eq(ALFRESCO_CORE_NAME), eq(1), eq(STORE_REF_MAP.get(ALFRESCO_CORE_NAME)), anyString(), eq(1), eq(1), eq(1), eq(null), eq(null), any());
        verify(spy).newCore(eq(VERSION_CORE_NAME), eq(1), eq(STORE_REF_MAP.get(VERSION_CORE_NAME)), anyString(), eq(1), eq(1), eq(1), eq(null), eq(null), any());
    }

    @Test
    public void validAndInvalidCoreNames() {
        AlfrescoCoreAdminHandler spy = spy(new AlfrescoCoreAdminHandler() {
            @Override
            protected void newCore(String coreName, int numShards, StoreRef storeRef, String templateName, int replicationFactor, int nodeInstance, int numNodes, String shardIds, Properties extraProperties, SolrQueryResponse rsp)
            {
                // Do nothing here otherwise we cannot spy it
            }
        });

        // First let's try a list of names, one by one
        final List<String> coreNames =
                asList(
                        ARCHIVE_CORE_NAME + "  ", // whitespace char at the end
                        "\t " + ALFRESCO_CORE_NAME, // whitespace chars at the beginning
                        "   " + VERSION_CORE_NAME + "  \t", // beginning and end
                        "   \t"); // empty name

        // Then, the same list as a single parameter (e.g. name1, name2, name3, etc)
        String commaSeparatedNames = String.join(",", coreNames);
        spy.setupNewDefaultCores(commaSeparatedNames);

        verify(spy).newCore(eq(ARCHIVE_CORE_NAME), eq(1), eq(STORE_REF_MAP.get(ARCHIVE_CORE_NAME)), anyString(), eq(1), eq(1), eq(1), eq(null), eq(null), any());
        verify(spy).newCore(eq(ALFRESCO_CORE_NAME), eq(1), eq(STORE_REF_MAP.get(ALFRESCO_CORE_NAME)), anyString(), eq(1), eq(1), eq(1), eq(null), eq(null), any());
        verify(spy).newCore(eq(VERSION_CORE_NAME), eq(1), eq(STORE_REF_MAP.get(VERSION_CORE_NAME)), anyString(), eq(1), eq(1), eq(1), eq(null), eq(null), any());
    }
}