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
package org.alfresco.solr;

import java.util.Collection;
import java.util.Set;

import org.alfresco.solr.tracker.ModelTracker;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.SolrCore;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlfrescoSolrCloseHook extends CloseHook
{
    protected final static Logger log = LoggerFactory.getLogger(AlfrescoSolrCloseHook.class);
    
    private TrackerRegistry trackerRegistry;
    private SolrTrackerScheduler scheduler;
    
    public AlfrescoSolrCloseHook(AlfrescoCoreAdminHandler adminHandler)
    {
        this.trackerRegistry = adminHandler.getTrackerRegistry();
        this.scheduler = adminHandler.getScheduler();
    }
    
    @Override
    public void postClose(SolrCore core)
    {
        // nothing
    }

    private boolean thisIsTheLastCoreRegistered(String coreName)
    {
        Set<String> coreNames = trackerRegistry.getCoreNames();
        return coreNames.size() == 1 && coreNames.contains(coreName);
    }

    @Override
    public void preClose(SolrCore core)
    {
        // Sets the shutdown flag on the trackers to stop them from doing any more work

        String coreName = core.getName();
        boolean thisIsTheLastCoreRegistered = thisIsTheLastCoreRegistered(coreName);
        ModelTracker modelTracker = trackerRegistry.getModelTracker();
        if (thisIsTheLastCoreRegistered)
        {
            modelTracker.setShutdown(true);
        }

        boolean testcase = Boolean.parseBoolean(System.getProperty("alfresco.test", "false"));
        if(testcase) {
            //other trackers not present in test case.
            return;
        }

        Collection<Tracker> coreTrackers = trackerRegistry.getTrackersForCore(coreName);
        for(Tracker tracker : coreTrackers)
        {
            tracker.setShutdown(true);
        }
        
        try
        {
            // Deletes scheduled jobs, and closes trackers
            scheduler.deleteTrackerJobs(coreName, coreTrackers);
            for (Tracker tracker : coreTrackers)
            {
                tracker.close();
            }
            
            if (thisIsTheLastCoreRegistered)
            {
                scheduler.deleteTrackerJob(coreName, modelTracker);
                modelTracker.close();
                
                if (!scheduler.isShutdown())
                {
                    scheduler.pauseAll();
                    scheduler.shutdown();
                }
            }
            
            trackerRegistry.removeTrackersForCore(core.getName());
        }
        catch (SchedulerException e)
        {
            log.error("Failed to shutdown scheduler", e);
        }
    }
}
