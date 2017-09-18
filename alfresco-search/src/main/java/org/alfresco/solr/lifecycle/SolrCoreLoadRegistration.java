/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.SolrKeyResourceLoader;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.SOLRAPIClientFactory;
import org.alfresco.solr.content.SolrContentStore;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.CascadeTracker;
import org.alfresco.solr.tracker.CommitTracker;
import org.alfresco.solr.tracker.ContentTracker;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.ModelTracker;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptorDecorator;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with core registration when the core is loaded.
 *
 * @author Gethin James
 */
public class SolrCoreLoadRegistration {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Registers with the admin handler the information server and the trackers.
     */
    public static void registerForCore(AlfrescoCoreAdminHandler adminHandler, CoreContainer coreContainer, SolrCore core,
                                       String coreName)
    {
        TrackerRegistry trackerRegistry = adminHandler.getTrackerRegistry();
        Properties props = new CoreDescriptorDecorator(core.getCoreDescriptor()).getProperties();

        if (Boolean.parseBoolean(props.getProperty("enable.alfresco.tracking", "false")))
        {
            SolrTrackerScheduler scheduler = adminHandler.getScheduler();
            SolrResourceLoader loader = core.getLatestSchema().getResourceLoader();
            SolrKeyResourceLoader keyResourceLoader = new SolrKeyResourceLoader(loader);
            if (trackerRegistry.hasTrackersForCore(coreName))
            {
                log.info("Trackers for " + coreName+ " is already registered, shutting them down.");
                shutdownTrackers(coreName, trackerRegistry.getTrackersForCore(coreName),scheduler);
                trackerRegistry.removeTrackersForCore(coreName);
                adminHandler.getInformationServers().remove(coreName);
            }

            SOLRAPIClientFactory clientFactory = new SOLRAPIClientFactory();
            SOLRAPIClient repositoryClient = clientFactory.getSOLRAPIClient(props, keyResourceLoader,
                    AlfrescoSolrDataModel.getInstance().getDictionaryService(CMISStrictDictionaryService.DEFAULT),
                    AlfrescoSolrDataModel.getInstance().getNamespaceDAO());
            //Start content store
            SolrContentStore contentStore = new SolrContentStore(coreContainer.getSolrHome());
            SolrInformationServer srv = new SolrInformationServer(adminHandler, core, repositoryClient, contentStore);
            props.putAll(srv.getProps());
            adminHandler.getInformationServers().put(coreName, srv);

            log.info("Starting to track " + coreName);

            ModelTracker mTracker = null;
            // Prevents other threads from registering the ModelTracker at the same time
            synchronized (trackerRegistry)
            {
                mTracker = trackerRegistry.getModelTracker();
                if (mTracker == null)
                {
                    log.debug("Creating ModelTracker when registering trackers for core " + coreName);
                    mTracker = new ModelTracker(coreContainer.getSolrHome(), props, repositoryClient,
                            coreName, srv);

                    trackerRegistry.setModelTracker(mTracker);

                    log.info("Ensuring first model sync.");
                    mTracker.ensureFirstModelSync();
                    log.info("Done ensuring first model sync.");
                    
                    //Scheduling the ModelTracker.
                    scheduler.schedule(mTracker, coreName, props);
                }
            }

            List<Tracker> trackers = createTrackers(coreName, trackerRegistry, props, scheduler, repositoryClient, srv);

            CommitTracker commitTracker = new CommitTracker(props, repositoryClient, coreName, srv, trackers);
            trackerRegistry.register(coreName, commitTracker);
            scheduler.schedule(commitTracker, coreName, props);
            log.info("The Trackers are now scheduled to run");
            trackers.add(commitTracker); //Add the commitTracker to the list of scheduled trackers that can be shutdown

            core.addCloseHook(new CloseHook() {
                @Override
                public void preClose(SolrCore core) {
                    log.info("Shutting down " + core.getName());
                    SolrCoreLoadRegistration.shutdownTrackers(core.getName(), trackers, scheduler);
                }

                @Override
                public void postClose(SolrCore core) {

                }
            });
        }
    }

    /**
     * Creates the trackers
     *
     * @param coreName
     * @param trackerRegistry
     * @param props
     * @param scheduler
     * @param repositoryClient
     * @param srv
     * @return A list of trackers
     */
    private static List<Tracker> createTrackers(String coreName, TrackerRegistry trackerRegistry, Properties props, SolrTrackerScheduler scheduler, SOLRAPIClient repositoryClient, SolrInformationServer srv) {
        List<Tracker> trackers = new ArrayList<Tracker>();

        AclTracker aclTracker = new AclTracker(props, repositoryClient, coreName, srv);
        trackerRegistry.register(coreName, aclTracker);
        scheduler.schedule(aclTracker, coreName, props);

        ContentTracker contentTrkr = new ContentTracker(props, repositoryClient, coreName, srv);
        trackerRegistry.register(coreName, contentTrkr);
        scheduler.schedule(contentTrkr, coreName, props);

        MetadataTracker metaTrkr = new MetadataTracker(props, repositoryClient, coreName, srv);
        trackerRegistry.register(coreName, metaTrkr);
        scheduler.schedule(metaTrkr, coreName, props);

        CascadeTracker cascadeTrkr = new CascadeTracker(props, repositoryClient, coreName, srv);
        trackerRegistry.register(coreName, cascadeTrkr);
        scheduler.schedule(cascadeTrkr, coreName, props);

        //The CommitTracker will acquire these locks in order
        //The ContentTracker will likely have the longest runs so put it first to ensure the MetadataTracker is not paused while
        //waiting for the ContentTracker to release it's lock.
        //The aclTracker will likely have the shortest runs so put it last.
        trackers.add(cascadeTrkr);
        trackers.add(contentTrkr);
        trackers.add(metaTrkr);
        trackers.add(aclTracker);
        return trackers;
    }

    /**
     * Shuts down the trackers for a core.
     *
     * The trackers are only deleted from the scheduler if they are the exact same instance of the Tracker class
     * passed into this method.
     * For example, you could have 2 cores of the same name and have the trackers registered with the scheduler BUT
     * the scheduler only keys by core name. The Collection<Tracker>s passed into this method are only removed
     * from the scheduler if the instances are == (equal). See scheduler.deleteJobForTrackerInstance()
     *
     * Trackers are not removed from the registry because the registry only keys by core name; its possible to
     * have multiple cores of the same name running.  Left over trackers in the registry are cleaned up by the CoreContainer
     * shutdown, that happens in the the AlfrescoCoreAdminHandler.shutdown().
     *
     * @param coreName The name of the core
     * @param coreTrackers A collection of trackers
     * @param scheduler The scheduler
     */
    public static void shutdownTrackers(String coreName, Collection<Tracker> coreTrackers, SolrTrackerScheduler scheduler)
    {
        try
        {
            log.info("Shutting down " + coreName + " with " + coreTrackers.size() + " trackers.");

            // Sets the shutdown flag on the trackers to stop them from doing any more work
            coreTrackers.forEach(tracker -> tracker.setShutdown(true));

            if (!scheduler.isShutdown())
            {
                coreTrackers.forEach(tracker -> scheduler.deleteJobForTrackerInstance(coreName,tracker) );
            }

            coreTrackers.forEach(tracker -> tracker.shutdown());
        }
        catch (Exception e)
        {
            log.error("Failed to shutdown trackers for core "+coreName, e);
        }
    }
}
