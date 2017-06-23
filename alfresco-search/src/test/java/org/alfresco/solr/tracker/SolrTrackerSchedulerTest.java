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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Properties;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;


@RunWith(MockitoJUnitRunner.class)
public class SolrTrackerSchedulerTest
{
    @Mock
    private AlfrescoCoreAdminHandler adminHandler;
    private SolrTrackerScheduler trackerScheduler;
    private String CORE_NAME = "coreName";
    private Scheduler spiedQuartzScheduler;
    @Mock
    SOLRAPIClient client;
    String coreName = "alfresco";
    @Mock
    InformationServer informationServer;
    Properties props;
    @Before
    public void setUp() throws Exception
    {
        this.trackerScheduler = new SolrTrackerScheduler(adminHandler);
        this.spiedQuartzScheduler = spy(this.trackerScheduler.scheduler);
        this.trackerScheduler.scheduler = spiedQuartzScheduler;
        props = new Properties();
        props.put("alfresco.stores", "workspace://SpacesStore");
        props.put("alfresco.batch.count", "5000");
        props.put("alfresco.maxLiveSearchers", "2");
        props.put("enable.slave", "false");
        props.put("enable.master", "true");
        props.put("shard.count", "1");
        props.put("shard.instance", "0");
        props.put("shard.method", "SHARD_METHOD_DBID");
        props.put("alfresco.template", "");
        props.put("alfresco.index.transformContent", "true");
        props.put("alfresco.version", "5.0.0");
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
        String exp = "0/12 * * * * ? *";
        props.put("alfresco.acl.tracker.cron", exp);
        AclTracker aclTracker = new AclTracker(props, client, coreName, informationServer);
        this.trackerScheduler.schedule(aclTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        checkCronExpression(exp);
    }
    @Test
    public void testContentTrackerSchedule() throws SchedulerException
    {
        String exp = "0/5 * * * * ? *";
        props.put("alfresco.content.tracker.cron", exp);
        ContentTracker contentTracker = new ContentTracker(props, client, coreName, informationServer);
        this.trackerScheduler.schedule(contentTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        checkCronExpression(exp);
    }
    
    private void checkCronExpression(String exp) throws SchedulerException
    {
        for (String jobName : this.trackerScheduler.scheduler.getJobNames(SolrTrackerScheduler.SOLR_JOB_GROUP)) 
        {
            Trigger[] triggers = this.trackerScheduler.scheduler.getTriggersOfJob(jobName,SolrTrackerScheduler.SOLR_JOB_GROUP);
            CronTrigger t = (CronTrigger) triggers[0];
            String cronExp = t.getCronExpression();
            Assert.assertEquals(exp, cronExp);
        }
    }
    @Test
    public void testMetaDataTrackerSchedule() throws SchedulerException
    {
        String exp = "0/4 * * * * ? *";
        props.put("alfresco.metadata.tracker.cron", exp);
        MetadataTracker metadataTracker = new MetadataTracker(props, client, exp, informationServer);
        this.trackerScheduler.schedule(metadataTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        checkCronExpression(exp);
    }
    @Test
    public void testSchedule() throws SchedulerException
    {
        String exp = "0/1 * * * * ? *";
        props.put("alfresco.cascade.tracker.cron",exp);
        CascadeTracker cascadeTracker = new CascadeTracker(props, client, exp, informationServer);
        this.trackerScheduler.schedule(cascadeTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        checkCronExpression(exp);
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
        ContentTracker contentTracker = new ContentTracker();
        MetadataTracker metadataTracker = new MetadataTracker();
        AclTracker aclTracker = new AclTracker();
        this.trackerScheduler.deleteTrackerJobs(CORE_NAME,
                    Arrays.asList(new Tracker[] { contentTracker, metadataTracker, aclTracker }));
        verify(spiedQuartzScheduler, times(3)).deleteJob(anyString(), eq(SolrTrackerScheduler.SOLR_JOB_GROUP));
    }

    @Test
    public void testDeleteTrackerJob() throws SchedulerException
    {
        ModelTracker modelTracker = new ModelTracker();
        this.trackerScheduler.deleteTrackerJob(CORE_NAME, modelTracker);
        verify(spiedQuartzScheduler).deleteJob(anyString(), eq(SolrTrackerScheduler.SOLR_JOB_GROUP));
    }

    @Test
    public void testDeleteTrackerInstanceJob() throws SchedulerException
    {
        CascadeTracker cascadeTracker = new CascadeTracker(props, client, coreName, informationServer);
        CascadeTracker cascadeTracker2 = new CascadeTracker(props, client, coreName, informationServer);
        this.trackerScheduler.schedule(cascadeTracker, CORE_NAME, props);

        //Try deleting the same class but a different instance. It not possible.
        this.trackerScheduler.deleteJobForTrackerInstance(CORE_NAME, cascadeTracker2);
        verify(spiedQuartzScheduler, never()).deleteJob(anyString(), eq(SolrTrackerScheduler.SOLR_JOB_GROUP));

        //No try deleting the exact instance of the tracker class
        this.trackerScheduler.deleteJobForTrackerInstance(CORE_NAME, cascadeTracker);
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
    @Test
    public void testModeltTrackerSchedule() throws SchedulerException
    {
        String exp = "0/20 * * * * ? *";
        props.put("alfresco.model.tracker.cron", exp);
        ModelTracker modelTracker = new ModelTracker("alfresco", props, client, CORE_NAME, informationServer);
        this.trackerScheduler.schedule(modelTracker, CORE_NAME, props);
        verify(spiedQuartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        checkCronExpression(exp);
    }

}
