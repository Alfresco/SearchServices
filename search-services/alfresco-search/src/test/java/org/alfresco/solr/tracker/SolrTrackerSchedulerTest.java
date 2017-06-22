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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Properties;

import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

@RunWith(MockitoJUnitRunner.class)
public class SolrTrackerSchedulerTest
{
    @Mock
    private AlfrescoCoreAdminHandler adminHandler;
    @Mock
    private ModelTracker modelTracker;
    @Mock
    private ContentTracker contentTracker;
    @Mock
    private MetadataTracker metadataTracker;
    @Mock
    private AclTracker aclTracker;
    @Mock
    private CascadeTracker cascadeTracker;
    @Mock
    private CascadeTracker cascadeTrackerScheduled;

    private SolrTrackerScheduler trackerScheduler;
    private String CORE_NAME = "coreName";
    private Scheduler spiedQuartzScheduler;

    @Before
    public void setUp() throws Exception
    {
        this.trackerScheduler = new SolrTrackerScheduler(adminHandler);
        this.spiedQuartzScheduler = spy(this.trackerScheduler.scheduler);
        this.trackerScheduler.scheduler = spiedQuartzScheduler;
    }

    @After
    public void tearDown() throws Exception
    {
        if (this.trackerScheduler != null && !this.trackerScheduler.isShutdown())
        {
            this.trackerScheduler.shutdown();
        }
    }

    @Test
    public void testAclSchedule() throws SchedulerException
    {
        Properties props = mock(Properties.class);
        when(props.getProperty("alfresco.tracker.acl.cron", "0/15 * * * * ? *")).thenReturn("0/10 * * * * ? *");
        this.trackerScheduler.schedule(aclTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
    @Test
    public void testContentTrackerSchedule() throws SchedulerException
    {
        Properties props = mock(Properties.class);
        when(props.getProperty("alfresco.tracker.content.cron", "0/15 * * * * ? *")).thenReturn("0/10 * * * * ? *");
        this.trackerScheduler.schedule(contentTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
    @Test
    public void testMetaDataTrackerSchedule() throws SchedulerException
    {
        Properties props = mock(Properties.class);
        when(props.getProperty("alfresco.tracker.metadata.cron", "0/15 * * * * ? *")).thenReturn("0/10 * * * * ? *");
        this.trackerScheduler.schedule(metadataTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
    @Test
    public void testSchedule() throws SchedulerException
    {
        Properties props = mock(Properties.class);
        when(props.getProperty("alfresco.cron", "0/15 * * * * ? *")).thenReturn("0/10 * * * * ? *");
        this.trackerScheduler.schedule(cascadeTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
    @Test
    public void testShutdown() throws SchedulerException
    {
        this.trackerScheduler.shutdown();
        assertTrue(trackerScheduler.isShutdown());
    }

    @Test
    public void testDeleteTrackerJobs() throws SchedulerException
    {
        this.trackerScheduler.deleteTrackerJobs(CORE_NAME,
                    Arrays.asList(new Tracker[] { contentTracker, metadataTracker, aclTracker }));
        verify(spiedQuartzScheduler, times(3)).deleteJob(anyString(), eq(SolrTrackerScheduler.SOLR_JOB_GROUP));
    }

    @Test
    public void testDeleteTrackerJob() throws SchedulerException
    {
        this.trackerScheduler.deleteTrackerJob(CORE_NAME, modelTracker);
        verify(spiedQuartzScheduler).deleteJob(anyString(), eq(SolrTrackerScheduler.SOLR_JOB_GROUP));
    }

    @Test
    public void testDeleteTrackerInstanceJob() throws SchedulerException
    {
        Properties props = mock(Properties.class);
        when(props.getProperty("alfresco.cron", "0/15 * * * * ? *")).thenReturn("0/15 * * * * ? *");
        this.trackerScheduler.schedule(cascadeTrackerScheduled, CORE_NAME, props);

        //Try deleting the same class but a different instance. It not possible.
        this.trackerScheduler.deleteJobForTrackerInstance(CORE_NAME, cascadeTracker);
        verify(spiedQuartzScheduler, never()).deleteJob(anyString(), eq(SolrTrackerScheduler.SOLR_JOB_GROUP));

        //No try deleting the exact instance of the tracker class
        this.trackerScheduler.deleteJobForTrackerInstance(CORE_NAME, cascadeTrackerScheduled);
        verify(spiedQuartzScheduler, times(1)).deleteJob(anyString(), eq(SolrTrackerScheduler.SOLR_JOB_GROUP));

    }

    @Test
    public void newSchedulerIsNotShutdown() throws SchedulerException
    {
        assertFalse(trackerScheduler.isShutdown());
    }

    @Test
    public void testPauseAll() throws SchedulerException
    {
        this.trackerScheduler.pauseAll();
        verify(this.spiedQuartzScheduler).pauseAll();
    }

}
