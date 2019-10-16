/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
package org.alfresco.solr.lifecycle;

import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.CascadeTracker;
import org.alfresco.solr.tracker.ContentTracker;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.core.SolrCore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        listener = new SolrCoreLoadListener(core);
        when(core.getName()).thenReturn(coreName);

         coreProperties = new Properties();
    }

    @Test
    public void coreTrackersRegistrationAndScheduling()
    {
        List<Tracker> coreTrackers = listener.createCoreTrackers(core.getName(), registry, coreProperties, scheduler, api, informationServer);

        verify(registry).register(eq(coreName), any(AclTracker.class));
        verify(registry).register(eq(coreName), any(ContentTracker.class));
        verify(registry).register(eq(coreName), any(MetadataTracker.class));
        verify(registry).register(eq(coreName), any(CascadeTracker.class));

        verify(scheduler).schedule(any(AclTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(ContentTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(MetadataTracker.class), eq(coreName), same(coreProperties));
        verify(scheduler).schedule(any(CascadeTracker.class), eq(coreName), same(coreProperties));

        assertEquals(4, coreTrackers.size());
    }

    @Test
    public void trackersShutDownProcedure()
    {
        List<Tracker> coreTrackers =
                asList(mock(AclTracker.class), mock(ContentTracker.class), mock(MetadataTracker.class), mock(CascadeTracker.class));

        listener.shutdownTrackers(coreName, coreTrackers, scheduler);

        coreTrackers.forEach(tracker -> verify(tracker).setShutdown(true));
        coreTrackers.forEach(tracker -> verify(scheduler).deleteJobForTrackerInstance(core.getName(), tracker));
        coreTrackers.forEach(tracker -> verify(tracker).shutdown());
    }
}
