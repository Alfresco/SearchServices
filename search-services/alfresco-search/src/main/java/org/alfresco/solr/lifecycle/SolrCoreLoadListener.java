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

import static java.util.Arrays.asList;
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
import org.alfresco.solr.tracker.SlaveCoreStatePublisher;
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
        if (getCore().isReloaded())
        {
            LOGGER.info("Solr Core {}, instance {} has been reloaded. " +
                    "The previous tracking subsystem will be stopped and another set of trackers will be registered on this new instance.",
                    getCore().getName(),
                    getCore().hashCode());
        }
        else
        {
            LOGGER.info("Solr Core {}, instance {}, has been registered for the first time.",
                    getCore().getName(),
                    getCore().hashCode());
        }

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

        SolrContentStore contentStore = admin.getSolrContentStore();
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
                LOGGER.info("Solr Core instance {} with name {} is going to be closed. Tracking Subsystem shutdown callback procedure has been started.", core.hashCode(), core.getName());

                // IMPORTANT: the closure needs to be created with the trackers created in this method
                shutdownTrackers(core, trackers, scheduler, false);
            }

            @Override
            public void postClose(SolrCore core)
            {
                LOGGER.info("Solr Core instance {} with name {} has been closed. Tracking Subsystem shutdown callback procedure has been completed.", core.hashCode(), core.getName());
            }
        });

        boolean trackersHaveBeenEnabled = Boolean.parseBoolean(coreProperties.getProperty("enable.alfresco.tracking", "true"));
        boolean owningCoreIsSlave = isSlaveModeEnabledFor(core);
        contentStore.toggleReadOnlyMode(owningCoreIsSlave);

        if (trackerRegistry.hasTrackersForCore(core.getName()))
        {
            LOGGER.info("Trackers (it could be only the node state publisher in case this node is a slave) for " + core.getName() + " are already registered, shutting them down.");
            Collection<Tracker> alreadyRegisteredTrackers = trackerRegistry.getTrackersForCore(core.getName());
            trackerRegistry.removeTrackersForCore(core.getName());

            shutdownTrackers(core, alreadyRegisteredTrackers, scheduler, core.isReloaded());
            admin.getInformationServers().remove(core.getName());
        }

        // Re-put the information server in the map because a Core reload (see above) could have removed the reference.
        admin.getInformationServers().put(core.getName(), informationServer);

        // Guard conditions: if trackers must be disabled then immediately return, we've done here.
        // Case #1: trackers have been explicitly disabled.
        if (!trackersHaveBeenEnabled)
        {
            LOGGER.info("SearchServices Core Trackers have been explicitly disabled on core \"{}\" through \"enable.alfresco.tracking\" configuration property.", core.getName());

            SlaveCoreStatePublisher statePublisher = new SlaveCoreStatePublisher(false, coreProperties, repositoryClient, core.getName(), informationServer);
            trackerRegistry.register(core.getName(), statePublisher);
            scheduler.schedule(statePublisher, core.getName(), coreProperties);
            trackers.add(statePublisher);

            LOGGER.info("SearchServices Slave Node Provider have been created and scheduled for core \"{}\".", core.getName());

            return;
        }

        // Case #2: we are on a slave node.
        if (owningCoreIsSlave)
        {
            LOGGER.info("SearchServices Core Trackers have been disabled on core \"{}\" because it is a slave core.", core.getName());

            SlaveCoreStatePublisher statePublisher = new SlaveCoreStatePublisher(false, coreProperties, repositoryClient, core.getName(), informationServer);
            trackerRegistry.register(core.getName(), statePublisher);
            scheduler.schedule(statePublisher, core.getName(), coreProperties);
            trackers.add(statePublisher);

            LOGGER.info("SearchServices Slave Node Provider have been created and scheduled for Core instance {} with name {}.", core.hashCode(), core.getName());

            return;
        }

        LOGGER.info("SearchServices Tracking Subsystem starts on Solr Core instance {} with name {}", core.hashCode(), core.getName());

        trackers.addAll(createAndScheduleCoreTrackers(core, trackerRegistry, coreProperties, scheduler, repositoryClient, informationServer));

        CommitTracker commitTracker = new CommitTracker(coreProperties, repositoryClient, core.getName(), informationServer, trackers);
        trackerRegistry.register(core.getName(), commitTracker);
        scheduler.schedule(commitTracker, core.getName(), coreProperties);

        LOGGER.info("Tracker {}, instance {}, belonging to Core {}, instance {} has been registered and scheduled.",
                commitTracker.getClass().getSimpleName(),
                commitTracker.hashCode(),
                core.getName(),
                core.hashCode());

        //Add the commitTracker to the list of scheduled trackers that can be shutdown
        trackers.add(commitTracker);
    }

    List<Tracker> createAndScheduleCoreTrackers(SolrCore core,
                                                TrackerRegistry trackerRegistry,
                                                Properties props,
                                                SolrTrackerScheduler scheduler,
                                                SOLRAPIClient repositoryClient,
                                                SolrInformationServer srv)
    {
        AclTracker aclTracker =
                registerAndSchedule(
                        new AclTracker(props, repositoryClient, core.getName(), srv),
                        core,
                        props,
                        trackerRegistry,
                        scheduler);

        ContentTracker contentTracker =
                registerAndSchedule(
                    new ContentTracker(props, repositoryClient, core.getName(), srv),
                        core,
                        props,
                        trackerRegistry,
                        scheduler);

        MetadataTracker metadataTracker =
                registerAndSchedule(
                    new MetadataTracker(true, props, repositoryClient, core.getName(), srv, true),
                        core,
                        props,
                        trackerRegistry,
                        scheduler);

        CascadeTracker cascadeTracker =
                registerAndSchedule(
                    new CascadeTracker(props, repositoryClient, core.getName(), srv),
                        core,
                        props,
                        trackerRegistry,
                        scheduler);

        //The CommitTracker will acquire these locks in order
        //The ContentTracker will likely have the longest runs so put it first to ensure the MetadataTracker is not paused while
        //waiting for the ContentTracker to release it's lock.
        //The aclTracker will likely have the shortest runs so put it last.
        return asList(cascadeTracker, contentTracker, metadataTracker, aclTracker);
    }

    /**
     * Accepts a {@link Tracker} instance, registers and schedules it.
     *
     * @param tracker the tracker that will be scheduled and registered.
     * @param core the owning core.
     * @param properties configuration properties.
     * @param registry the tracker registry instance.
     * @param scheduler the tracker schedule instance.
     * @param <T> the tracker instance.
     * @return the registered and scheduled tracker instance.
     */
    private <T extends Tracker> T registerAndSchedule(T tracker, SolrCore core, Properties properties, TrackerRegistry registry, SolrTrackerScheduler scheduler)
    {
        registry.register(core.getName(), tracker);
        scheduler.schedule(tracker, core.getName(), properties);

        LOGGER.info("Tracker {}, instance {}, belonging to Core {}, instance {} has been registered and scheduled.",
                tracker.getClass().getSimpleName(),
                tracker.hashCode(),
                core.getName(),
                core.hashCode());

        return tracker;
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
     * The coreHasBeenReloaded flag is used just for logging out meaningful messages about the owning core instance.
     * If we are in a RELOAD scenario (coreHasBeenReloaded = true) we no longer have the reference of the closed core
     * so we print only its name. Instead in case we are here because a core has been closed, we can print out the core
     * reference in order to add meaningful information in the log.
     *
     * @param core The owning core name.
     * @param coreTrackers A collection of trackers
     * @param scheduler The scheduler
     * @param coreHasBeenReloaded a flag indicating if we are on a Core RELOAD scenario.
     */
    void shutdownTrackers(SolrCore core, Collection<Tracker> coreTrackers, SolrTrackerScheduler scheduler, boolean coreHasBeenReloaded)
    {
        coreTrackers.forEach(tracker -> shutdownTracker(core, tracker, scheduler, coreHasBeenReloaded));
    }

    /**
     * Shutdown procedure for a single tracker.
     * The coreHasBeenReloaded flag is used just for logging out meaningful messages about the owning core instance.
     * If we are in a RELOAD scenario (coreHasBeenReloaded = true) we no longer have the reference of the closed core
     * so we print only its name. Instead in case we are here because a core has been closed, we can print out the core
     * reference in order to add meaningful information in the log.
     *
     * @param core the owning {@link SolrCore}
     * @param tracker the {@link Tracker} instance we want to stop.
     * @param scheduler the scheduler.
     * @param coreHasBeenReloaded a flag indicating if we are on a Core RELOAD scenario.
     */
    private void shutdownTracker(SolrCore core, Tracker tracker, SolrTrackerScheduler scheduler, boolean coreHasBeenReloaded)
    {
        // In case of reload the input core is not the owner: the owner is instead the previous (closed) core and we don't have its reference here.
        String coreReference = core.getName() + (coreHasBeenReloaded ? "" : ", instance " + core.hashCode());

        if (tracker.isAlreadyInShutDownMode())
        {
            LOGGER.info("Tracker {}, instance {} belonging to core {}, is already in shutdown mode.",
                    tracker.getClass().getSimpleName(),
                    tracker.hashCode(),
                    coreReference);
            return;
        }

        LOGGER.info("Tracker {}, instance {} belonging to core {} shutdown procedure initiated.",
                tracker.getClass().getSimpleName(),
                tracker.hashCode(),
                coreReference);
        try
        {
            tracker.setShutdown(true);
            if (!scheduler.isShutdown())
            {
                scheduler.deleteJobForTrackerInstance(core.getName(), tracker);
            }

            tracker.shutdown();

            LOGGER.info("Tracker {}, instance {}, belonging to core {} shutdown procedure correctly terminated.",
                    tracker.getClass().getSimpleName(),
                    tracker.hashCode(),
                    coreReference);
        }
        catch (Exception exception)
        {
            LOGGER.error("Tracker {}, instance {} belonging to core {}, shutdown procedure failed. " +
                            "See the stacktrace below for further details.",
                    tracker.getClass().getSimpleName(),
                    tracker.hashCode(),
                    coreReference,
                    exception);
        }
    }

    /**
     * Checks if the configuration declares this node as a slave.
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