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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.alfresco.solr.IndexTrackingShutdownException;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclChangeSets;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.commons.lang.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for the {@link AclTracker} class.
 * 
 * @author Matt Ward
 */
@RunWith(MockitoJUnitRunner.class)
public class AclTrackerTest
{
    private static final Object CUSTOM_ALFRESCO_VERSION = "99.9.9";
    // The class under test
    private AclTracker tracker;
    private @Mock SOLRAPIClient client;
    private @Mock InformationServer informationServer;
    private TrackerState trackerState;
    
    @Before
    public void setUp() throws Exception
    {
        trackerState = new TrackerState();
        trackerState.setRunning(false); // Nothing would happen if it were already running.
        Properties props = createProperties();
        // Spy in order to stub some methods and verify others. Note: non-stubbed methods go to the real class.
        tracker = spy(new AclTracker(props, client, "core-name", informationServer));
    }

    private Properties createProperties()
    {
        Properties props = new Properties();
        props.put("alfresco.stores", "workspace://SpacesStore");
        props.put("alfresco.version", CUSTOM_ALFRESCO_VERSION);
        return props;
    }

    protected void testTrackChangesRan()
    {
        when(tracker.getTrackerState()).thenReturn(trackerState);
        
        tracker.track();

        assertFalse(trackerState.isRunning());
        assertFalse(trackerState.isCheck());
    }
    
    
    // High level test of doTrack() workflow.
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void checkTrackingOperaionsPerformed() throws Throwable
    {
        testTrackChangesRan();
        
        verify(tracker).purgeAclChangeSets();
        verify(tracker).purgeAcls();

        verify(tracker).reindexAclChangeSets();
        verify(tracker).reindexAcls();

        verify(tracker).indexAclChangeSets();
        verify(tracker).indexAcls();

        verify(tracker).trackRepository();
    }
    

    // Tests the purgeAclChangeSets() call made in AclTracker.doTrack()
    // TODO: the other operations in doTrack().
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void checkTrackingWhenAclChangeSetsToPurge() throws IllegalAccessException, IOException
    {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<Long> aclChangeSetsToPurge = (ConcurrentLinkedQueue<Long>)
        FieldUtils.readField(tracker, "aclChangeSetsToPurge", true);
        aclChangeSetsToPurge.add(101L);
        aclChangeSetsToPurge.add(102L);
        aclChangeSetsToPurge.add(103L);
        
        // Invoke the behaviour we're testing
        testTrackChangesRan();

        // aclChangeSetsToPurge
        verify(informationServer).deleteByAclChangeSetId(101L);
        verify(informationServer).deleteByAclChangeSetId(102L);
        verify(informationServer).deleteByAclChangeSetId(103L);
        
        // TODO: verify checkShutdown
        verify(informationServer).commit();
    }
    
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void checkTrackingWhenAclsToPurge() throws IllegalAccessException, IOException
    {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<Long> aclsToPurge = (ConcurrentLinkedQueue<Long>)
        FieldUtils.readField(tracker, "aclsToPurge", true);
        aclsToPurge.add(201L);
        aclsToPurge.add(202L);
        aclsToPurge.add(203L);
        
        // Invoke the behaviour we're testing
        testTrackChangesRan();
        
        // aclsToPurge
        verify(informationServer).deleteByAclId(201L);
        verify(informationServer).deleteByAclId(202L);
        verify(informationServer).deleteByAclId(203L);
        
        // TODO: verify checkShutdown
        verify(informationServer).commit();
    }

    // TODO: Commented out, approach not working
    // Rethink or finish later.
    /*@Test
    public void checkTrackingWhenAclChangeSetsToReindex() throws IllegalAccessException, IOException, AuthenticationException, JSONException
    {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<Long> aclChangeSetsToReindex = (ConcurrentLinkedQueue<Long>)
        FieldUtils.readField(tracker, "aclChangeSetsToReindex", true);
        aclChangeSetsToReindex.add(301L);
        aclChangeSetsToReindex.add(302L);
        aclChangeSetsToReindex.add(303L);
        
        // tracker will loop through list of ACLs to reindex, and get changesets for each changeset ID.
        when(client.getAclChangeSets(null, 201L, null, 202L, 1)).thenReturn(mockChangeSets(201L));
        when(client.getAclChangeSets(null, 202L, null, 203L, 1)).thenReturn(mockChangeSets(202L));
        when(client.getAclChangeSets(null, 203L, null, 204L, 1)).thenReturn(mockChangeSets(203L));
    
        when(client.getAcls(Collections.singletonList(new AclChangeSet(201L, 0L, 1)), null, Integer.MAX_VALUE)).thenReturn(Collections.singletonList(new Acl(201L, 0L)));
        when(client.getAcls(Collections.singletonList(new AclChangeSet(202L, 0L, 1)), null, Integer.MAX_VALUE)).thenReturn(Collections.singletonList(new Acl(202L, 0L)));
        when(client.getAcls(Collections.singletonList(new AclChangeSet(203L, 0L, 1)), null, Integer.MAX_VALUE)).thenReturn(Collections.singletonList(new Acl(203L, 0L)));
    
        // TODO: ...more...
        
        // Invoke the behaviour we're testing
        testTrackChangesRan();
        
        verify(informationServer).deleteByAclChangeSetId(301L);
        verify(informationServer).deleteByAclChangeSetId(302L);
        verify(informationServer).deleteByAclChangeSetId(303L);
        
        // TODO: verify checkShutdown
        verify(informationServer).commit();
    }*/
    
    private AclChangeSets mockChangeSets(long id)
    {
        List<AclChangeSet> changeSets = Collections.singletonList(new AclChangeSet(id, 0L, 1));
        AclChangeSets acs = mock(AclChangeSets.class);
        when(acs.getAclChangeSets()).thenReturn(changeSets);
        return acs;
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void trackingAbortsWhenAlreadyRunning() throws Throwable
    {
        trackerState.setRunning(true);
        // Prove running state, before attempt to track()
        assertTrue(trackerState.isRunning());
        
        FieldUtils.writeField(tracker, "state", trackerState, true);
        tracker.track();

        // Still running - these values are unaffected.
        assertTrue(trackerState.isRunning());
        assertFalse(trackerState.isCheck());
        
        // Prove doTrack() was not called
        verify(tracker, never()).doTrack();
    }
    
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void willRollbackOnThrowableDuringTracking() throws Throwable
    {
        doThrow(new RuntimeException("Simulated problem during tracking")).when(tracker).doTrack();
        
        testTrackChangesRan();
        
        verify(informationServer).rollback();
    }
    
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void willRollbackOnIndexTrackingShutdownException() throws Throwable
    {
        doThrow(new IndexTrackingShutdownException()).when(tracker).doTrack();
        
        testTrackChangesRan();
        
        verify(informationServer).rollback();
    }
    
    @Ignore("Not yet implemented.")
    @Test
    public void canCheckIndex()
    {
        // TODO
//        tracker.checkIndex(fromTx, toTx, fromAclTx, toAclTx, fromTime, toTime);
    }
    
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void canGetAlfrescoVersion()
    {
        // Check we're testing something useful
        assertNotNull(CUSTOM_ALFRESCO_VERSION);
        // Check alfresco version retrieved from properties
        assertEquals(CUSTOM_ALFRESCO_VERSION, tracker.getAlfrescoVersion());
    }
    
    
    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void canClose() throws IllegalAccessException
    {
        ThreadHandler threadHandler = (ThreadHandler) FieldUtils.readField(tracker, "threadHandler", true);
        threadHandler = spy(threadHandler);
        FieldUtils.writeField(tracker, "threadHandler", threadHandler, true);
        
        tracker.close();
        
        // AclTracker specific
        verify(threadHandler).shutDownThreadPool();
        
        // Applicable to all AbstractAclTracker
        verify(client).close();
    }
}
