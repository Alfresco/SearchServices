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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.ContentTracker;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.ModelTracker;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchemaFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.SchedulerException;

@RunWith(MockitoJUnitRunner.class)
public class AlfrescoSolrCloseHookTest
{
    private SolrCore core;
    private AlfrescoSolrCloseHook hook;

    @Mock
    private AlfrescoCoreAdminHandler adminHandler;
    @Mock
    private SolrTrackerScheduler scheduler;
    @Mock
    private ModelTracker modelTracker;
    @Mock
    private ContentTracker contentTracker;
    @Mock
    private MetadataTracker metadataTracker;
    @Mock
    private AclTracker aclTracker;
    @Mock
    private TrackerRegistry trackerRegistry;

    private final String CORE_NAME = "coreName";
    private Collection<Tracker> coreTrackers;

    @Before
    public void setUp() throws Exception
    {
        when(trackerRegistry.getModelTracker()).thenReturn(modelTracker);
        coreTrackers = Arrays.asList(new Tracker[] { contentTracker, metadataTracker, aclTracker });
        when(trackerRegistry.getTrackersForCore(CORE_NAME)).thenReturn(coreTrackers);
        when(trackerRegistry.getCoreNames()).thenReturn(new HashSet<String>(Arrays.asList(CORE_NAME)));
        when(adminHandler.getTrackerRegistry()).thenReturn(trackerRegistry);
        when(adminHandler.getScheduler()).thenReturn(scheduler);
        
        Properties properties = new Properties();
        properties.put("solr.tests.maxBufferedDocs", "1000");
        properties.put("solr.tests.maxIndexingThreads", "10");
        properties.put("solr.tests.ramBufferSizeMB", "1024");
        properties.put("solr.tests.mergeScheduler", "org.apache.lucene.index.ConcurrentMergeScheduler");
        properties.put("solr.tests.mergePolicy", "org.apache.lucene.index.TieredMergePolicy");
        
        CoreContainer coreContainer = new CoreContainer("../source/test-files");
        SolrResourceLoader resourceLoader = new SolrResourceLoader(Paths.get("../source/test-files/collection1/conf/"), null, properties);
        SolrConfig solrConfig = new SolrConfig(resourceLoader, "solrconfig-afts.xml", null);
        IndexSchemaFactory.buildIndexSchema("schema-afts.xml", solrConfig);
        CoreDescriptor coreDescriptor = new CoreDescriptor(coreContainer, "name", Paths.get("../source/test-files/collection1/"));
        
        core = new SolrCore(CORE_NAME, null, solrConfig, null, null, coreDescriptor, null, null, null);
        hook = new AlfrescoSolrCloseHook(adminHandler);
    }

    @Test
    public void testPreCloseSolrCore() throws SchedulerException
    {
        // Runs system under test
        hook.preClose(core);
        
        // Validates behavior
        verify(modelTracker).setShutdown(true);
        verify(aclTracker).setShutdown(true);
        verify(contentTracker).setShutdown(true);
        verify(metadataTracker).setShutdown(true);
        
        verify(modelTracker).close();
        verify(aclTracker).close();
        verify(contentTracker).close();
        verify(metadataTracker).close();
        verify(trackerRegistry).removeTrackersForCore(CORE_NAME);

        verify(scheduler).pauseAll();
        verify(scheduler).shutdown();
        verify(scheduler).deleteTrackerJobs(CORE_NAME, coreTrackers);
        verify(scheduler).deleteTrackerJob(CORE_NAME, modelTracker);
    }
}
