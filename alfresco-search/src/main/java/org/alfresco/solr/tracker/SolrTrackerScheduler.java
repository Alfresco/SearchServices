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

import java.text.ParseException;
import java.util.Collection;
import java.util.Properties;

import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a decorator of the Quartz Scheduler object to add Solr-specific functionality.
 * @author Ahmed Owian
 */
public class SolrTrackerScheduler
{
    private static final String DEFAULT_CRON = "0/15 * * * * ? *";
    protected static final String SOLR_JOB_GROUP = "Solr";
    protected final static Logger log = LoggerFactory.getLogger(SolrTrackerScheduler.class);
    protected Scheduler scheduler;

    public SolrTrackerScheduler(AlfrescoCoreAdminHandler adminHandler)
    {
        // TODO: pick scheduler properties from SOLR config or file ...
        try
        {
            StdSchedulerFactory factory = new StdSchedulerFactory();
            Properties properties = new Properties();
            properties.setProperty("org.quartz.scheduler.instanceName", adminHandler.toString());
            properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            properties.setProperty("org.quartz.threadPool.threadCount", "40");
            properties.setProperty("org.quartz.threadPool.makeThreadsDaemons", "true");
            properties.setProperty("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
            properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
            properties.setProperty("org.quartz.scheduler.skipUpdateCheck","true");
            factory.initialize(properties);
            scheduler = factory.getScheduler();
            scheduler.start();

        }
        catch (SchedulerException e)
        {
            logError("SolrTrackerScheduler", e);
        }
    }

    private void logError(String jobType, Throwable e)
    {
        log.error("Failed to schedule " + jobType + " Job.", e);
    }
    
    public void schedule(Tracker tracker, String coreName, Properties props)
    {
        String jobName = this.getJobName(tracker, coreName);
        JobDetail job = new JobDetail(jobName, SOLR_JOB_GROUP, TrackerJob.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TrackerJob.JOBDATA_TRACKER_KEY, tracker);
        job.setJobDataMap(jobDataMap);
        Trigger trigger;
        try
        {
            String cron =  props.getProperty("alfresco.cron", DEFAULT_CRON);
            if(tracker instanceof AclTracker) 
            {
                cron = props.getProperty("alfresco.tracker.acl.cron", DEFAULT_CRON);
            }
            if(tracker instanceof ContentTracker) 
            {
                cron = props.getProperty("alfresco.tracker.content.cron", DEFAULT_CRON);
            }
            if(tracker instanceof MetadataTracker) 
            {
                cron = props.getProperty("alfresco.tracker.metadata.cron", DEFAULT_CRON);
            }
            trigger = new CronTrigger(jobName, SOLR_JOB_GROUP, cron);
            log.info("Scheduling job " + jobName);
            scheduler.scheduleJob(job, trigger);
        }
        catch (ParseException e)
        {
            logError("Tracker", e);
        }
        catch (SchedulerException e)
        {   
            logError("Tracker", e);
        }
    }

    protected String getJobName(Tracker tracker, String coreName)
    {
        return tracker.getClass().getSimpleName() + "-" + coreName;
    }

    public void shutdown() throws SchedulerException
    {
        this.scheduler.shutdown(false);
    }

    public void deleteTrackerJobs(String coreName, Collection<Tracker> trackers) throws SchedulerException
    {
        for (Tracker tracker : trackers)
        {
            deleteTrackerJob(coreName, tracker);
        }
    }

    /**
     * Delete a Tracker Job ONLY if its exactly the same tracker instance that was passed in.
     *
     * In theory more than one instance of a core can exist with the same core name but the
     * scheduler stores jobs using the core name as a unique key (even though it may not be unique).
     *
     * This method gets the tracker instance associated with the Job and compares to see if its
     * identical to the instance that is passed in.  If they are identical then the job is deleted.
     * Otherwise, another core (of the same name) scheduled this job, so its left alone.
     *
     * @param coreName
     * @param tracker Specific instance of a tracker
     */
    public void deleteJobForTrackerInstance(String coreName, Tracker tracker)
    {
        String jobName = this.getJobName(tracker, coreName);
        JobDetail detail = null;
        try {
            detail = this.scheduler.getJobDetail(jobName, SOLR_JOB_GROUP);
            if (detail != null)
            {
                Tracker jobTracker = (Tracker) detail.getJobDataMap().get(TrackerJob.JOBDATA_TRACKER_KEY);
                //If this is the exact tracker instance that was scheduled, then delete it.
                if (tracker == jobTracker)
                {
                    this.scheduler.deleteJob(jobName, SOLR_JOB_GROUP);
                }
            }

        } catch (SchedulerException e) {
            log.error("Unable to delete a tracker job "+jobName, e);
        }
    }

    public void deleteTrackerJob(String coreName, Tracker tracker) throws SchedulerException
    {
        String jobName = this.getJobName(tracker, coreName);
        this.scheduler.deleteJob(jobName, SOLR_JOB_GROUP);
    }

    public boolean isShutdown() throws SchedulerException
    {
        return this.scheduler.isShutdown();
    }

    public void pauseAll() throws SchedulerException
    {
        this.scheduler.pauseAll();
    }

    public int getJobsCount() throws SchedulerException
    {
        return this.scheduler.getJobNames(SOLR_JOB_GROUP).length;
    }
}
