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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Tests for the {@link TrackerJob} class.
 * 
 * @author Matt Ward
 */
@RunWith(MockitoJUnitRunner.class)
public class TrackerJobTest
{
    private TrackerJob trackerJob; // Class under test
    private @Mock Tracker tracker;
    private @Mock JobExecutionContext jec;
    private @Mock JobDetail jobDetail;
    private JobDataMap jobDataMap;
    
    @Before
    public void setUp() throws Exception
    
    {
        trackerJob = new TrackerJob();
        
        jobDataMap = new JobDataMap();
        jobDataMap.put("TRACKER", tracker);
        
        when(jec.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    }

    @Test
    public void canExecuteTrackerJob() throws JobExecutionException
    {
        trackerJob.execute(jec);
        
        // When the TrackerJob is triggered, then the Tracker's execute() method
        // should be invoked.
        verify(tracker).track();
    }

}
