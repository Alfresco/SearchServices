/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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

import static java.util.Optional.ofNullable;

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
import org.alfresco.solr.tracker.SlaveNodeStateProvider;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptorDecorator;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.ReplicationHandler;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Listeners for *FIRST SEARCHER* events in order to prepare and register the SolrContentStore and the Tracking Subsystem.
 *
 * @author Gethin James
 * @author Andrea Gazzarini
 */
public class SolrCoreLoadListener extends AbstractSolrEventListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCoreLoadListener.class);

    /**
     * Builds a new listener instance with the given {@link SolrCore} (event source).
     *
     * @param core the {@link SolrCore} instance representing the event source of this listener.
     */
    public SolrCoreLoadListener(SolrCore core)
    {
        super(core);
    }

    @Override
    public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher)
    {
        CoreContainer coreContainer = getCore().getCoreContainer();
        AlfrescoCoreAdminHandler admin = (AlfrescoCoreAdminHandler) coreContainer.getMultiCoreHandler();
        SolrCore core = getCore();

        TrackerRegistry trackerRegistry = admin.getTrackerRegistry();
        Properties coreProperties = new CoreDescriptorDecorator(core.getCoreDescriptor()).getProperties();

        SolrResourceLoader loader = core.getLatestSchema().getResourceLoader();
        SolrKeyResourceLoader keyResourceLoader = new SolrKeyResourceLoader(loader);
        SOLRAPIClientFactory clientFactory = new SOLRAPIClientFactory();
        SOLRAPIClient repositoryClient =
                clientFactory.getSOLRAPIClient(coreProperties, keyResourceLoader,
                    AlfrescoSolrDataModel.getInstance().getDictionaryService(CMISStrictDictionaryService.DEFAULT),
                    AlfrescoSolrDataModel.getInstance().getNamespaceDAO());

        SolrContentStore contentStore = new SolrContentStore(coreContainer.getSolrHome());
        SolrInformationServer informationServer = new SolrInformationServer(admin, core, repositoryClient, contentStore);
        coreProperties.putAll(informationServer.getProps());
        admin.getInformationServers().put(core.getName(), informationServer);

        final SolrTrackerScheduler scheduler = admin.getScheduler();

        // Prevents other threads from registering the ModelTracker at the same time
        // Create model tracker and load all the persisted models
        synchronized(SolrCoreLoadListener.class)
        {
            createModelTracker(core.getName(),
                    trackerRegistry,
                    coreProperties,
                    coreContainer.getSolrHome(),
                    repositoryClient,
                    informationServer,
                    scheduler);
        }

        /*
         * The shutdown hook needs to be registered regardless we are slave or masters.
         * This because if we are master all trackers will be scheduled, if we are slave the node state publisher
         * will be scheduled.
         *
         * As consequence of that, regardless the node role, we will always have something to shutdown in the tracker
         * registry.
         */
        final List<Tracker> trackers = new ArrayList<>();
        core.addCloseHook(new CloseHook()
        {
            @Override
            public void preClose(SolrCore core)
            {
                LOGGER.info("Tracking Subsystem shutdown procedure for core {} has been started.", core.getName());
                shutdownTrackers(core.getName(), trackers, scheduler);
            }

            @Override
            public void postClose(SolrCore core)
            {
                LOGGER.info("Tracking Subsystem shutdown procedure for core {} has been completed.", core.getName());
            }
        });

        boolean trackersHaveBeenEnabled = Boolean.parseBoolean(coreProperties.getProperty("enable.alfresco.tracking", "true"));
        boolean owningCoreIsSlave = isSlaveModeEnabledFor(core);

        // Guard conditions: if trackers must be disabled then immediately return, we've done here.
        // Case #1: trackers have been explicitly disabled.
        if (!trackersHaveBeenEnabled)
        {
            LOGGER.info("SearchServices Core Trackers have been explicitly disabled on core \"{}\" through \"enable.alfresco.tracking\" configuration property.", core.getName());
            return;
        }

        // Case #2: we are on a slave node.
        if (owningCoreIsSlave)
        {
            LOGGER.info("SearchServices Core Trackers have been disabled on core \"{}\" because it is a slave core.", core.getName());

            SlaveNodeStateProvider stateProvider = new SlaveNodeStateProvider(coreProperties, repositoryClient, core.getName(), informationServer);
            trackerRegistry.register(core.getName(), stateProvider);
            scheduler.schedule(stateProvider, core.getName(), coreProperties);

            LOGGER.info("SearchServices Slave Node Provider have been created and scheduled for core \"{}\".", core.getName());

            return;
        }

        LOGGER.info("SearchServices Tracking Subsystem starts on core {}", core.getName());
        if (trackerRegistry.hasTrackersForCore(core.getName()))
        {
            LOGGER.info("Trackers for " + core.getName()+ " is already registered, shutting them down.");
            shutdownTrackers(core.getName(), trackerRegistry.getTrackersForCore(core.getName()), scheduler);
            trackerRegistry.removeTrackersForCore(core.getName());
            admin.getInformationServers().remove(core.getName());
        }

        trackers.addAll(createCoreTrackers(core.getName(), trackerRegistry, coreProperties, scheduler, repositoryClient, informationServer));

        CommitTracker commitTracker = new CommitTracker(coreProperties, repositoryClient, core.getName(), informationServer, trackers);
        trackerRegistry.register(core.getName(), commitTracker);
        scheduler.schedule(commitTracker, core.getName(), coreProperties);

        LOGGER.info("SearchServices Core Trackers have been correctly registered and scheduled.");

        //Add the commitTracker to the list of scheduled trackers that can be shutdown
        trackers.add(commitTracker);
    }

    List<Tracker> createCoreTrackers(String coreName,
                                                    TrackerRegistry trackerRegistry,
                                                    Properties props,
                                                    SolrTrackerScheduler scheduler,
                                                    SOLRAPIClient repositoryClient,
                                                    SolrInformationServer srv)
    {
        List<Tracker> trackers = new ArrayList<>();

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

    private void createModelTracker(String coreName,
                                    TrackerRegistry trackerRegistry,
                                    Properties props,
                                    String solrHome,
                                    SOLRAPIClient repositoryClient,
                                    SolrInformationServer srv,
                                    SolrTrackerScheduler scheduler)
    {
        ModelTracker mTracker = trackerRegistry.getModelTracker();
        if (mTracker == null)
        {
            LOGGER.debug("Creating a new Model Tracker instance.");
            mTracker = new ModelTracker(solrHome, props, repositoryClient, coreName, srv);
            trackerRegistry.setModelTracker(mTracker);

            LOGGER.info("Model Tracker: ensuring first model sync.");
            mTracker.ensureFirstModelSync();

            scheduler.schedule(mTracker, coreName, props);

            LOGGER.info("Model Tracker has been correctly initialised, registered and scheduled.");
        }
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
    void shutdownTrackers(String coreName, Collection<Tracker> coreTrackers, SolrTrackerScheduler scheduler)
    {
        try
        {
            LOGGER.info("Shutting down Trackers Subsystem for core \"{}\" which contains {} core trackers.", coreName, coreTrackers.size());

            // Sets the shutdown flag on the trackers to stop them from doing any more work
            coreTrackers.forEach(tracker -> tracker.setShutdown(true));

            if (!scheduler.isShutdown())
            {
                coreTrackers.forEach(tracker -> scheduler.deleteJobForTrackerInstance(coreName, tracker) );
            }

            coreTrackers.forEach(Tracker::shutdown);
        }
        catch (Exception e)
        {
            LOGGER.error("Tracking Subsystem shutdown procedure failed to shutdown trackers for core {}. See the stacktrace below for further details.", coreName, e);
        }
    }

    /**
     * Checks if the content store belonging to the hosting Solr node must be set in read only mode.
     *
     * @param core the hosting {@link SolrCore} instance.
     * @return true if the content store must be set in read only mode, false otherwise.
     */
    boolean isSlaveModeEnabledFor(SolrCore core)
    {
        Predicate<PluginInfo> onlyReplicationHandler =
                plugin -> "/replication".equals(plugin.name)
                        || plugin.className.endsWith(ReplicationHandler.class.getSimpleName());

        Function<NamedList, Boolean> isSlaveModeEnabled =
                params ->  ofNullable(params)
                        .map(configuration -> {
                            Object enable = configuration.get("enable");
                            return enable == null ||
                                    (enable instanceof String ? StrUtils.parseBool((String)enable) : Boolean.TRUE.equals(enable));})
                        .orElse(false);

        return core.getSolrConfig().getPluginInfos(SolrRequestHandler.class.getName())
                .stream()
                .filter(PluginInfo::isEnabled)
                .filter(onlyReplicationHandler)
                .findFirst()
                .map(plugin -> plugin.initArgs)
                .map(params -> params.get("slave"))
                .map(NamedList.class::cast)
                .map(isSlaveModeEnabled)
                .orElse(false);
    }
}