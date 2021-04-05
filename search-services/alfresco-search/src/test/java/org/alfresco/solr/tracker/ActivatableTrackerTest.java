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
package org.alfresco.solr.tracker;

import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.SOLRAPIClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ActivatableTrackerTest
{
    private static class TestActivatableTracker extends ActivatableTracker {

        protected TestActivatableTracker(Properties properties, TrackerState state) {
            super(properties, mock(SOLRAPIClient.class), "thisIsTheCoreName", mock(InformationServer.class), Type.NODE_STATE_PUBLISHER);
            this.state = state;
        }

        @Override
        protected void doTrack(String iterationId) {
            // Nothing to be done here, it's a fake implementation.
        }


        @Override
        public void maintenance() {

        }

        @Override
        public boolean hasMaintenance() {
            return false;
        }

        @Override
        public Semaphore getWriteLock() {
            return null;
        }

        @Override
        public Semaphore getRunLock() {
            return null;
        }
    }

    private ActivatableTracker tracker;
    private TrackerState state;

    @Before
    public void setUp()
    {
        state = new TrackerState();
        state.setRunning(false);
        tracker = spy(new TestActivatableTracker(new Properties(), state));
        tracker.enable();
        assertTrue(tracker.isEnabled());
        assertFalse(tracker.state.isRunning());
    }

    @Test
    public void enabledShouldBeTheDefaultState()
    {
       assertTrue(tracker.isEnabled());
    }

    @Test
    public void trackersCanBeExplicitlyDisabled()
    {
        assertTrue(tracker.isEnabled());

        tracker.disable();

        assertFalse(tracker.isEnabled());
    }

    @Test
    public void disablingATracker_shouldClearTheScheduledMaintenanceWork()
    {
        assertTrue(tracker.isEnabled());

        tracker.disable();

        assertFalse(tracker.isEnabled());
        verify(tracker).clearScheduledMaintenanceWork();
    }

    @Test
    public void enableIsIdempotent()
    {
        assertTrue(tracker.isEnabled());

        tracker.enable();

        assertTrue(tracker.isEnabled());

        tracker.disable();

        assertFalse(tracker.isEnabled());

        tracker.enable();
        assertTrue(tracker.isEnabled());

        tracker.enable();
        assertTrue(tracker.isEnabled());
    }

    @Test
    public void disableIsIdempotent()
    {
        assertTrue(tracker.isEnabled());

        tracker.disable();
        assertFalse(tracker.isEnabled());

        tracker.disable();
        assertFalse(tracker.isEnabled());

        tracker.enable();
        assertTrue(tracker.isEnabled());

        tracker.disable();
        assertFalse(tracker.isEnabled());

        tracker.disable();
        assertFalse(tracker.isEnabled());
    }

    @Test
    public void disableIndexingOnRunningTracker_shouldDisableTheTrackerAnSetItInRollbackMode()
    {
        state.setRunning(true);
        assertTrue(tracker.isEnabled());
        assertTrue(tracker.state.isRunning());

        tracker.disable();

        state.setRunning(true);
        assertTrue(tracker.state.isRunning());

        assertFalse(tracker.isEnabled());
        verify(tracker).setRollback(true, null);
    }

    @Test
    public void assertActivatableTrackersList() {
        Stream.of(MetadataTracker.class, AclTracker.class, ContentTracker.class, CascadeTracker.class)
                .forEach(clazz -> assertTrue("Warning: " + clazz + " is supposed to be enabled/disabled", ActivatableTracker.class.isAssignableFrom(clazz)));
    }

    @Test
    public void assertAlwaysActivatedTrackersList() {
        Stream.of(CommitTracker.class, ModelTracker.class)
                .forEach(clazz -> assertFalse("Warning: " + clazz + " is not supposed to be enabled/disabled", ActivatableTracker.class.isAssignableFrom(clazz)));
    }
}
