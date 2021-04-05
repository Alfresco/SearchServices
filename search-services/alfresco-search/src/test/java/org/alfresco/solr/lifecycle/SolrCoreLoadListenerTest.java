/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

package org.alfresco.solr.lifecycle;

import static java.util.Arrays.asList;

import static org.alfresco.solr.SolrInformationServer.CASCADE_TRACKER_ENABLED;
import static org.alfresco.solr.tracker.Tracker.Type.ACL;
import static org.alfresco.solr.tracker.Tracker.Type.CASCADE;
import static org.alfresco.solr.tracker.Tracker.Type.CONTENT;
import static org.alfresco.solr.tracker.Tracker.Type.METADATA;
import static org.alfresco.solr.tracker.Tracker.Type.NODE_STATE_PUBLISHER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.CascadeTracker;
import org.alfresco.solr.tracker.ContentTracker;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.Tracker.Type;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.InputSource;

/**
 * Unit tests for the {@link SolrCoreLoadListener}.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrCoreLoadListenerTest
{
    private SolrCoreLoadListener listener;

    @Mock
    private SolrCore core;

    @Mock
    private SolrTrackerScheduler scheduler;

    @Mock
    private SOLRAPIClient api;

    @Mock
    private SolrInformationServer informationServer;

    @Mock
    private TrackerRegistry registry;

    private Properties coreProperties;

    private String coreName = "XYZ";

    @Before
    public void setUp()
    {
        initMocks(this);

        listener = new SolrCoreLoadListener(core);
        when(core.getName()).thenReturn(coreName);

        coreProperties = new Properties();
    }

    @Test
    public void coreTrackersRegistrationAndScheduling()
    {
        List<Tracker> coreTrackers = listener.createAndScheduleCoreTrackers(core, registry, coreProperties, scheduler, api, informationServer);

        verify(registry).register(eq(coreName), any(AclTracker.class));
        verify(registry).register(eq(coreName), any(ContentTracker.class));
        verify(registry).register(eq(coreName), any(MetadataTracker.class));
        verify(registry).register(eq(coreName), any(CascadeTracker.class));

        verify(scheduler).schedule(any(AclTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(ContentTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(MetadataTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(CascadeTracker.class), eq(coreName), same(coreProperties));

        Set<Type> trackerTypes = coreTrackers.stream().map(Tracker::getType).collect(Collectors.toSet());
        assertEquals("Unexpected trackers found.", Set.of(ACL, CONTENT, METADATA, NODE_STATE_PUBLISHER, CASCADE), trackerTypes);
    }

    @Test
    public void testDisabledCascadeTracking()
    {
        coreProperties.put(CASCADE_TRACKER_ENABLED, "false");

        List<Tracker> coreTrackers = listener.createAndScheduleCoreTrackers(core, registry, coreProperties, scheduler, api, informationServer);

        verify(registry).register(eq(coreName), any(AclTracker.class));
        verify(registry).register(eq(coreName), any(ContentTracker.class));
        verify(registry).register(eq(coreName), any(MetadataTracker.class));
        verify(registry, never()).register(eq(coreName), any(CascadeTracker.class));

        verify(scheduler).schedule(any(AclTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(ContentTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(MetadataTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler, never()).schedule(any(CascadeTracker.class), eq(coreName), same(coreProperties));

        Set<Type> trackerTypes = coreTrackers.stream().map(Tracker::getType).collect(Collectors.toSet());
        assertEquals("Unexpected trackers found.", Set.of(ACL, CONTENT, METADATA, NODE_STATE_PUBLISHER), trackerTypes);
    }

    @Test
    public void trackersShutDownProcedure()
    {
        List<Tracker> coreTrackers =
                asList(mock(AclTracker.class), mock(ContentTracker.class), mock(MetadataTracker.class), mock(CascadeTracker.class));

        listener.shutdownTrackers(core, coreTrackers, scheduler, false);

        coreTrackers.forEach(tracker -> verify(tracker).setShutdown(true));
        coreTrackers.forEach(tracker -> verify(scheduler).deleteJobForTrackerInstance(core.getName(), tracker));
        coreTrackers.forEach(tracker -> verify(tracker).shutdown());
    }

    @Test
    public void noReplicationHandlerDefined_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_no_replication_handler_defined.xml");
        assertFalse("If no replication handler is defined, then we expect to run a RW content store.", listener.isSlaveModeEnabledFor(core));
    }

    @Test
    public void emptyReplicationHandlerDefined_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_empty_replication_handler.xml");
        assertFalse("If an empty replication handler is defined, then we expect to run a RW content store.", listener.isSlaveModeEnabledFor(core));
    }

    @Test
    public void slaveReplicationHandlerDefinedButDisabled_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_slave_disabled_replication_handler.xml");
        assertFalse("If a slave replication handler is defined but disabled, then we expect to run a RW content store.", listener.isSlaveModeEnabledFor(core));
    }

    @Test
    public void masterReplicationHandlerDefined_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_master_replication_handler.xml");
        assertFalse("If a master replication handler is defined but disabled, then we expect to run a RW content store.", listener.isSlaveModeEnabledFor(core));
    }

    @Test
    public void masterReplicationHandlerDefinedButDisabled_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_master_disabled_replication_handler.xml");
        assertFalse("If a master replication handler is defined but disabled, then we expect to run a RW content store.", listener.isSlaveModeEnabledFor(core));
    }

    @Test
    public void slaveReplicationHandlerDefined_thenContentStoreIsInReadOnlyMode() throws Exception
    {
        prepare("solrconfig_slave_replication_handler.xml");
        assertTrue("If a slave replication handler is defined, then we expect to run a RO content store.", listener.isSlaveModeEnabledFor(core));
    }

    private void prepare(String configName) throws Exception
    {
        SolrConfig solrConfig = new SolrConfig(configName, new InputSource(getClass().getResourceAsStream("/test-files/" + configName)));
        when(core.getSolrConfig()).thenReturn(solrConfig);
    }
}
