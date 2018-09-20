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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

public class TrackerRegistryTest
{
    private static TrackerRegistry reg = new TrackerRegistry();
    private static Tracker aclTracker = new AclTracker();
    private static Tracker contentTracker = new ContentTracker();
    private static Tracker metadataTracker = new MetadataTracker();
    private static Tracker modelTracker = new ModelTracker();
    private static final String CORE_NAME = "coreName";
    private static final String CORE2_NAME = "core2Name";
    private static final String CORE3_NAME = "core3Name";
    private static final String NOT_A_CORE_NAME = "not a core name";

    public static void registerTrackers(String coreName)
    {
        reg.register(coreName, aclTracker);
        reg.register(coreName, contentTracker );
        reg.register(coreName, metadataTracker);
        reg.register(coreName, modelTracker);
    }
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        registerTrackers(CORE_NAME);
    }


    @Test
    public void testGetCoreNames()
    {
        Set<String> coreNames = reg.getCoreNames();
        assertNotNull(coreNames);
        assertTrue(coreNames.contains(CORE_NAME));
        assertEquals(1, coreNames.size());
        
        registerTrackers(CORE2_NAME);
        coreNames = reg.getCoreNames();
        assertNotNull(coreNames);
        assertTrue(coreNames.contains(CORE_NAME));
        assertFalse(coreNames.contains(NOT_A_CORE_NAME));
        assertEquals(2, coreNames.size());
    }

    @Test
    public void testGetTrackersForCore()
    {
        Collection<Tracker> trackersForCore = reg.getTrackersForCore(CORE_NAME);
        assertNotNull(trackersForCore);
        assertFalse(trackersForCore.isEmpty());
        assertTrue(trackersForCore.contains(aclTracker));
        assertTrue(trackersForCore.contains(contentTracker));
        assertTrue(trackersForCore.contains(modelTracker));
        assertTrue(trackersForCore.contains(metadataTracker));
        
        trackersForCore = reg.getTrackersForCore(NOT_A_CORE_NAME);
        assertTrue(trackersForCore.isEmpty());
    }

    @Test
    public void testHasTrackersForCore()
    {
        assertTrue(reg.hasTrackersForCore(CORE_NAME));
        assertFalse(reg.hasTrackersForCore(NOT_A_CORE_NAME));
    }

    @Test
    public void testGetTrackerForCore()
    {
        assertEquals(aclTracker, reg.getTrackerForCore(CORE_NAME, AclTracker.class));
        assertEquals(contentTracker, reg.getTrackerForCore(CORE_NAME, ContentTracker.class));
        assertEquals(metadataTracker, reg.getTrackerForCore(CORE_NAME, MetadataTracker.class));
        assertEquals(modelTracker, reg.getTrackerForCore(CORE_NAME, ModelTracker.class));
    }
    
    @Test
    public void testRemoveTrackersForCore()
    {
        registerTrackers(CORE3_NAME);
        boolean thereWereTrackers = reg.removeTrackersForCore(CORE3_NAME);
        assertTrue(thereWereTrackers);
        assertTrue(reg.getTrackersForCore(CORE3_NAME).isEmpty());
        thereWereTrackers = reg.removeTrackersForCore(NOT_A_CORE_NAME);
        assertFalse(thereWereTrackers);
    }
}
