/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
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

import com.google.common.collect.ImmutableMap;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.client.SOLRAPIClientFactory;
import org.alfresco.solr.config.ConfigUtil;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.CoreStatePublisher;
import org.alfresco.solr.tracker.DBIDRangeRouter;
import org.alfresco.solr.tracker.DocRouter;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.SlaveCoreStatePublisher;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.alfresco.solr.utils.Utils;
import org.alfresco.util.Pair;
import org.alfresco.util.shard.ExplicitShardingPolicy;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.alfresco.solr.HandlerOfResources.extractCustomProperties;
import static org.alfresco.solr.HandlerOfResources.getSafeBoolean;
import static org.alfresco.solr.HandlerOfResources.getSafeLong;
import static org.alfresco.solr.HandlerOfResources.openResource;
import static org.alfresco.solr.HandlerOfResources.updatePropertiesFile;
import static org.alfresco.solr.HandlerOfResources.updateSharedProperties;
import static org.alfresco.solr.HandlerReportHelper.addMasterOrStandaloneCoreSummary;
import static org.alfresco.solr.HandlerReportHelper.addSlaveCoreSummary;
import static org.alfresco.solr.HandlerReportHelper.buildAclReport;
import static org.alfresco.solr.HandlerReportHelper.buildAclTxReport;
import static org.alfresco.solr.HandlerReportHelper.buildNodeReport;
import static org.alfresco.solr.HandlerReportHelper.buildTrackerReport;
import static org.alfresco.solr.HandlerReportHelper.buildTxReport;
import static org.alfresco.solr.utils.Utils.notNullOrEmpty;

/**
 * Alfresco Solr administration endpoints provider.
 * A customisation of the existing Solr {@link CoreAdminHandler} which offers additional administration endpoints.
 *
 * Since 1.5 the behaviour of these endpoints differs a bit depending on the target core. This because a lot of these
 * endpoints rely on the information obtained from the trackers, and trackers (see SEARCH-1606) are disabled on slave
 * cores.
 *
 * When a request arrives to this handler, the following are the possible scenarios:
 *
 * <ul>
 *     <li>
 *         a core is specified in the request: if the target core is a slave then a minimal response or an empty
 *         response with an informational message is returned. If instead the core is a master (or it is a standalone
 *         core) the service will return as much information as possible (as it happened before 1.5)
 *     </li>
 *     <li>
 *         a core isn't specified in the request: the request is supposed to target all available cores. However, while
 *         looping, slave cores are filtered out. In case all cores are slave (i.e. we are running a "pure" slave node)
 *         the response will be empty, it will include an informational message in order to warn the requestor.
 *         Sometimes this informative behaviour is not feasible: in those cases an empty response will be returned.
 *     </li>
 * </ul>
 *
 * @author Andrea Gazzarini
 */
public class AlfrescoCoreAdminHandler extends CoreAdminHandler
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoCoreAdminHandler.class);

    private static final String REPORT = "report";
    private static final String ARG_ACLTXID = "acltxid";
    static final String ARG_TXID = "txid";
    private static final String ARG_ACLID = "aclid";
    private static final String ARG_NODEID = "nodeid";
    private static final String ARG_QUERY = "query";
    private static final String DATA_DIR_ROOT = "data.dir.root";
    public static final String ALFRESCO_DEFAULTS = "create.alfresco.defaults";
    private static final String NUM_SHARDS = "num.shards";
    private static final String SHARD_IDS = "shard.ids";
    static final String DEFAULT_TEMPLATE = "rerank";

    static final String ALFRESCO_CORE_NAME = "alfresco";
    static final String ARCHIVE_CORE_NAME = "archive";
    static final String VERSION_CORE_NAME = "version";
    static final Map<String, StoreRef> STORE_REF_MAP = ImmutableMap.of(
                ALFRESCO_CORE_NAME, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                ARCHIVE_CORE_NAME, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE,
                VERSION_CORE_NAME, new StoreRef("workspace", "version2Store"));

    private SolrTrackerScheduler scheduler;
    private TrackerRegistry trackerRegistry;
    private ConcurrentHashMap<String, InformationServer> informationServers;

    private static List<String> CORE_PARAMETER_NAMES = asList(CoreAdminParams.CORE, "coreName", "index");

    public AlfrescoCoreAdminHandler()
    {
        super();
    }

    public AlfrescoCoreAdminHandler(CoreContainer coreContainer)
    {
        super(coreContainer);

        LOGGER.info("Starting Alfresco Core Administration Services");

        trackerRegistry = new TrackerRegistry();
        informationServers = new ConcurrentHashMap<>();
        this.scheduler = new SolrTrackerScheduler(this);

        String createDefaultCores = ConfigUtil.locateProperty(ALFRESCO_DEFAULTS, "");
        int numShards = Integer.parseInt(ConfigUtil.locateProperty(NUM_SHARDS, "1"));
        String shardIds = ConfigUtil.locateProperty(SHARD_IDS, null);
        if (createDefaultCores != null && !createDefaultCores.isEmpty())
        {
            Thread thread = new Thread(() ->
            {
                waitForTenSeconds();
                setupNewDefaultCores(createDefaultCores, numShards, 1, 1, 1, shardIds);
            });
            thread.start();
        }
    }

    /**
     * Creates new default cores based on the "createDefaultCores" String passed in.
     *
     * @param names comma delimited list of core names that will be created.
     */
    void setupNewDefaultCores(String names)
    {
        setupNewDefaultCores(names, 1, 1, 1, 1, null);
    }

    /**
     * Creates new default cores based on the "createDefaultCores" String passed in.
     *
     * @param names comma delimited list of core names that will be created.
     * @param numShards The total number of shards.
     * @param replicationFactor - Not sure why the core needs to know this.
     * @param nodeInstance - Not sure why the core needs to know this.
     * @param numNodes - Not sure why the core needs to know this.
     * @param shardIds A comma separated list of shard ids for this core (or null).
     */
    private void setupNewDefaultCores(String names, int numShards, int replicationFactor, int nodeInstance, int numNodes, String shardIds)
    {
        try
        {
            List<String> coreNames =
                    ofNullable(names)
                            .map(String::toLowerCase)
                            .map(parameter -> parameter.split(","))
                            .map(Arrays::asList)
                            .orElse(Collections.emptyList());

            coreNames.stream()
                    .map(String::trim)
                    .filter(coreName -> !coreName.isEmpty())
                    .forEach(coreName -> {
                        LOGGER.info("Attempting to create default alfresco core: {}", coreName);
                        if (!STORE_REF_MAP.containsKey(coreName))
                        {
                            throw new AlfrescoRuntimeException("Invalid '" + ALFRESCO_DEFAULTS + "' permitted values are " + STORE_REF_MAP.keySet());
                        }
                        StoreRef storeRef = STORE_REF_MAP.get(coreName);
                        newCore(coreName, numShards, storeRef, DEFAULT_TEMPLATE, replicationFactor, nodeInstance,
                                    numNodes, shardIds, null, new SolrQueryResponse());
                    });
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to create default alfresco cores (workspace/archive stores)", exception);
        }
    }

    /**
     * Shut down services that exist outside of the core.
     */
    @Override
    public void shutdown()
    {
        super.shutdown();
        try
        {
            LOGGER.info("Shutting down Alfresco core container services");

            AlfrescoSolrDataModel.getInstance().close();
            SOLRAPIClientFactory.close();
            MultiThreadedHttpConnectionManager.shutdownAll();

            coreNames().forEach(trackerRegistry::removeTrackersForCore);
            informationServers.clear();

            if (!scheduler.isShutdown())
            {
                scheduler.pauseAll();

                if (trackerRegistry.getModelTracker() != null) trackerRegistry.getModelTracker().shutdown();

                trackerRegistry.setModelTracker(null);
                scheduler.shutdown();
            }
        }
        catch(Exception exception)
        {
            LOGGER.error("Problem shutting down Alfresco core container services", exception);
        }
    }

    private void initResourceBasedLogging(String resource)
    {
        try
        {
            Class<?> clazz = Class.forName("org.apache.log4j.PropertyConfigurator");
            Method method = clazz.getMethod("configure", Properties.class);
            InputStream is = openResource(coreContainer.getSolrHome(), resource);
            Properties p = new Properties();
            p.load(is);
            method.invoke(null, p);
        }
        catch (ClassNotFoundException e)
        {
            // Do nothing here
        }
        catch (Exception e)
        {
            LOGGER.info("Failed to load " + resource, e);
        }
    }

    protected void handleCustomAction(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        SolrParams params = req.getParams();
        String action =
                ofNullable(params.get(CoreAdminParams.ACTION))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .orElse("");
        try
        {
            switch (action)
            {
                case "NEWCORE":
                case "NEWINDEX":
                    newCore(req, rsp);
                    break;
                case "UPDATECORE":
                case "UPDATEINDEX":
                    updateCore(req);
                    break;
                case "UPDATESHARED":
                    updateShared(req);
                    break;
                case "REMOVECORE":
                    removeCore(req);
                    break;
                case "NEWDEFAULTINDEX":
                case "NEWDEFAULTCORE":
                    newDefaultCore(req, rsp);
                    break;
                case "CHECK":
                    actionCHECK(params);
                    break;
                case "NODEREPORT":
                    actionNODEREPORTS(rsp, params);
                    break;
                case "ACLREPORT":
                    actionACLREPORT(rsp, params);
                    break;
                case "TXREPORT":
                    actionTXREPORT(rsp, params);
                    break;
                case "ACLTXREPORT":
                    actionACLTXREPORT(rsp, params);
                    break;
                case "RANGECHECK":
                    rangeCheck(rsp, params);
                    break;
                case "EXPAND":
                    expand(rsp, params);
                    break;
                case "REPORT":
                    actionREPORT(rsp, params);
                    break;
                case "PURGE":
                    actionPURGE(params);
                    break;
                case "REINDEX":
                    actionREINDEX(params);
                    break;
                case "RETRY":
                    actionRETRY(rsp, params);
                    break;
                case "INDEX":
                    actionINDEX(params);
                    break;
                case "FIX":
                    actionFIX(params);
                    break;
                case "SUMMARY":
                    actionSUMMARY(rsp, params);
                    break;
                case "LOG4J":
                    initResourceBasedLogging(
                            ofNullable(params.get("resource"))
                                .orElse("log4j-solr.properties"));
                    break;
                default:
                    super.handleCustomAction(req, rsp);
                    break;
            }
        }
        catch (Exception ex)
        {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                        "Error executing implementation of admin request " + action, ex);
        }
    }

    private void newCore(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        SolrParams params = req.getParams();
        req.getContext();

        // If numCore > 1 we are creating a collection of cores for a sole node in a cluster
        int numShards = params.getInt("numShards", 1);

        String store = params.get("storeRef");
        if (store == null || store.trim().length() == 0)
        {
            return;
        }

        StoreRef storeRef = new StoreRef(store);

        String templateName = ofNullable(params.get("template")).orElse("vanilla");

        int replicationFactor =  params.getInt("replicationFactor", 1);
        int nodeInstance =  params.getInt("nodeInstance", -1);
        int numNodes =  params.getInt("numNodes", 1);

        String coreName = coreName(params);
        String shardIds = params.get("shardIds");

        newCore(coreName, numShards, storeRef, templateName, replicationFactor, nodeInstance, numNodes, shardIds, extractCustomProperties(params), rsp);
    }

    private void newDefaultCore(SolrQueryRequest req, SolrQueryResponse response)
    {
        SolrParams params = req.getParams();
        String coreName = ofNullable(coreName(params)).orElse(ALFRESCO_CORE_NAME);
        String templateName =
                params.get("template") != null
                        ? params.get("template")
                        : DEFAULT_TEMPLATE;

        Properties extraProperties = extractCustomProperties(params);

        newDefaultCore(
                coreName,
                ofNullable(params.get("storeRef"))
                        .map(StoreRef::new)
                        .orElse(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE),
                templateName,
                extraProperties,
                response);
    }

    private void newDefaultCore(String coreName, StoreRef storeRef, String templateName, Properties extraProperties, SolrQueryResponse rsp)
    {
        newCore(coreName, 1, storeRef, templateName, 1, 1, 1, null, extraProperties, rsp);
    }

    protected void newCore(String coreName, int numShards, StoreRef storeRef, String templateName, int replicationFactor, int nodeInstance, int numNodes, String shardIds, Properties extraProperties, SolrQueryResponse rsp)
    {
        try
        {
            // copy core from template
            File solrHome = new File(coreContainer.getSolrHome());
            File templates = new File(solrHome, "templates");
            File template = new File(templates, templateName);

            if(numShards > 1)
            {
                String collectionName = templateName + "--" + storeRef.getProtocol() + "-" + storeRef.getIdentifier() + "--shards--"+numShards + "-x-"+replicationFactor+"--node--"+nodeInstance+"-of-"+numNodes;
                String coreBase = storeRef.getProtocol() + "-" + storeRef.getIdentifier() + "-";
                if (coreName != null)
                {
                    collectionName = templateName + "--" + coreName + "--shards--"+numShards + "-x-"+replicationFactor+"--node--"+nodeInstance+"-of-"+numNodes;
                    coreBase = coreName + "-";
                }

                File baseDirectory = new File(solrHome, collectionName);

                if(nodeInstance == -1)
                {
                    return;
                }

                List<Integer> shards;
                if(shardIds != null)
                {
                    shards = extractShards(shardIds, numShards);
                }
                else
                {
                    ExplicitShardingPolicy policy = new ExplicitShardingPolicy(numShards, replicationFactor, numNodes);
                    if(!policy.configurationIsValid())
                    {
                        return;
                    }
                    shards = policy.getShardIdsForNode(nodeInstance);
                }

                for(Integer shard : shards)
                {
                    coreName = coreBase + shard;
                    File newCore = new File(baseDirectory, coreName);
                    String solrCoreName = coreName;
                    if (coreName == null)
                    {
                        if(storeRef.equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE))
                        {
                            solrCoreName = "alfresco-" + shard;
                        }
                        else if(storeRef.equals(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE))
                        {
                            solrCoreName = "archive-" + shard;
                        }
                    }
                    createAndRegisterNewCore(rsp, extraProperties, storeRef, template, solrCoreName, newCore, numShards, shard, templateName);
                }
            }
            else
            {
                if (coreName == null)
                {
                    coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
                }
                File newCore = new File(solrHome, coreName);
                createAndRegisterNewCore(rsp, extraProperties, storeRef, template, coreName, newCore, 0, 0, templateName);
            }
        }
        catch (IOException exception)
        {
            LOGGER.error("I/O Failure detected while creating the new core " +
                    "(name={}, numShard={}, storeRef={}, template={}, replication factor={}, node instance={}, num nodes={}, shard ids={})",
                    coreName,
                    numShards,
                    storeRef,
                    templateName,
                    replicationFactor,
                    nodeInstance,
                    numNodes,
                    shardIds,
                    exception);
        }
    }

    /**
     * Extracts the list of shard identifiers from the given input string.
     * the "excludeFromShardId" parameter is used to filter out those shards whose identifier is equal or greater than
     * that parameter.
     *
     * @param shardIds the shards input string, where shards are separated by comma.
     * @param excludeFromShardId filter out those shards whose identifier is equal or greater than this value.
     * @return the list of shard identifiers.
     */
    List<Integer> extractShards(String shardIds, int excludeFromShardId)
    {
        return stream(Objects.requireNonNullElse(shardIds, "").split(","))
                .map(String::trim)
                .map(Utils::toIntOrNull)
                .filter(Objects::nonNull)
                .filter(shard -> shard < excludeFromShardId)
                .collect(Collectors.toList());
    }

    private void createAndRegisterNewCore(SolrQueryResponse rsp, Properties extraProperties, StoreRef storeRef, File template, String coreName, File newCore, int shardCount, int shardInstance, String templateName) throws IOException
    {
        if (coreContainer.getLoadedCoreNames().contains(coreName))
        {
            //Core alfresco exists
            LOGGER.warn(coreName + " already exists, not creating again.");
            rsp.add("core", coreName);
            return;
        }

        FileUtils.copyDirectory(template, newCore, false);

        // fix configuration properties
        File config = new File(newCore, "conf/solrcore.properties");
        Properties properties = new Properties();

        String defaultRoot = newCore.getCanonicalPath();
        if (defaultRoot.endsWith(coreName)) defaultRoot = defaultRoot.substring(0,defaultRoot.length()-coreName.length());
        //Set defaults
        properties.setProperty(DATA_DIR_ROOT, defaultRoot);
        properties.setProperty("data.dir.store", coreName);
        properties.setProperty("alfresco.stores", storeRef.toString());

        //Potentially override the defaults
        try (FileInputStream fileInputStream = new FileInputStream(config))
        {
            properties.load(fileInputStream);
        }

        //Don't override these
        properties.setProperty("alfresco.template", templateName);
        if(shardCount > 0)
        {
            properties.setProperty("shard.count", "" + shardCount);
            properties.setProperty("shard.instance", "" + shardInstance);
        }

        //Allow "data.dir.root" to be set via config
        properties.setProperty(DATA_DIR_ROOT, ConfigUtil.locateProperty(DATA_DIR_ROOT, properties.getProperty(DATA_DIR_ROOT)));

        //Still allow the properties to be overidden via url params
        if (extraProperties != null && !extraProperties.isEmpty())
        {
            properties.putAll(extraProperties);
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(config))
        {
            properties.store(fileOutputStream, null);
        }

        SolrCore core = coreContainer.create(coreName, newCore.toPath(), new HashMap<>(), false);
        rsp.add("core", core.getName());
    }

    boolean hasAlfrescoCore(Collection<SolrCore> cores)
    {
        return notNullOrEmpty(cores).stream()
                .map(SolrCore::getName)
                .anyMatch(trackerRegistry::hasTrackersForCore);
    }

    // :::::

    private void updateShared(SolrQueryRequest req)
    {
        SolrParams params = req.getParams();

        try
        {
            File config = new File(AlfrescoSolrDataModel.getResourceDirectory(), AlfrescoSolrDataModel.SHARED_PROPERTIES);
            updateSharedProperties(params, config, hasAlfrescoCore(coreContainer.getCores()));

            coreContainer.getCores().forEach(aCore -> coreContainer.reload(aCore.getName()));
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to update Shared properties ", e);
        }
    }

    private void updateCore(SolrQueryRequest req)
    {
        ofNullable(coreName(req.getParams()))
                .map(String::trim)
                .filter(coreName -> !coreName.isEmpty())
                .ifPresent(coreName -> {
                    try (SolrCore core = coreContainer.getCore(coreName))
                    {

                        if (core == null)
                        {
                            return;
                        }

                        String configLocaltion = core.getResourceLoader().getConfigDir();
                        File config = new File(configLocaltion, "solrcore.properties");
                        updatePropertiesFile(req.getParams(), config, null);

                        coreContainer.reload(coreName);
                    }
                });
    }

    private void removeCore(SolrQueryRequest req)
    {
        String store = "";
        SolrParams params = req.getParams();
        if (params.get("storeRef") != null)
        {
            store = params.get("storeRef");
        }

        if ((store == null) || (store.length() == 0)) { return; }

        StoreRef storeRef = new StoreRef(store);

        String coreName = ofNullable(coreName(req.getParams())).orElse(storeRef.getProtocol() + "-" + storeRef.getIdentifier());
        coreContainer.unload(coreName, true, true, true);
    }

    private void actionCHECK(SolrParams params)
    {
        String cname = coreName(params);
        coreNames().stream()
                .filter(coreName -> cname == null || coreName.equals(cname))
                .map(trackerRegistry::getTrackersForCore)
                .flatMap(Collection::stream)
                .map(Tracker::getTrackerState)
                .forEach(state -> state.setCheck(true));
    }

    private void actionNODEREPORTS(SolrQueryResponse rsp, SolrParams params) throws JSONException
    {
        Long dbid =
                ofNullable(params.get(ARG_NODEID))
                        .map(Long::valueOf)
                        .orElseThrow(() -> new AlfrescoRuntimeException("No dbid parameter set."));

        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add(REPORT, report);

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(trackerRegistry::hasTrackersForCore)
                .map(coreName -> new Pair<>(coreName, nodeStatePublisher(coreName)))
                .filter(coreNameAndPublisher -> coreNameAndPublisher.getSecond() != null)
                .forEach(coreNameAndPublisher ->
                        report.add(
                                coreNameAndPublisher.getFirst(),
                                buildNodeReport(coreNameAndPublisher.getSecond(), dbid)));
    }

    private void actionACLREPORT(SolrQueryResponse rsp, SolrParams params) throws JSONException
    {
        Long aclid =
                ofNullable(params.get(ARG_ACLID))
                        .map(Long::valueOf)
                        .orElseThrow(() -> new AlfrescoRuntimeException("No " + ARG_ACLID + " parameter set."));

        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add(REPORT, report);

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .map(coreName -> new Pair<>(coreName, trackerRegistry.getTrackerForCore(coreName, AclTracker.class)))
                .filter(coreNameAndAclTracker -> coreNameAndAclTracker.getSecond() != null)
                .forEach(coreNameAndAclTracker ->
                        report.add(
                                coreNameAndAclTracker.getFirst(),
                                buildAclReport(coreNameAndAclTracker.getSecond(), aclid)));

        if (report.size() == 0)
        {
            addAlertMessage(report);
        }
    }

    private void actionTXREPORT(SolrQueryResponse rsp, SolrParams params) throws JSONException
    {
        String coreName =
                ofNullable(coreName(params))
                    .orElseThrow(() -> new AlfrescoRuntimeException("No " + params.get(CoreAdminParams.CORE + " parameter set.")));

        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add(REPORT, report);

        if (isMasterOrStandalone(coreName))
        {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            Long txid =
                    ofNullable(params.get(ARG_TXID))
                            .map(Long::valueOf)
                            .orElseThrow(() -> new AlfrescoRuntimeException("No " + ARG_TXID + " parameter set."));

            report.add(coreName, buildTxReport(trackerRegistry, informationServers.get(coreName), coreName, tracker, txid));
        }
        else
        {
            addAlertMessage(report);
        }
    }

    private void actionACLTXREPORT(SolrQueryResponse rsp, SolrParams params) throws JSONException
    {
        Long acltxid =
                ofNullable(params.get(ARG_ACLTXID))
                        .map(Long::valueOf)
                        .orElseThrow(() -> new AlfrescoRuntimeException("No " + ARG_ACLTXID + " parameter set."));

        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add(REPORT, report);

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .map(coreName -> new Pair<>(coreName, trackerRegistry.getTrackerForCore(coreName, AclTracker.class)))
                .filter(coreNameAndAclTracker -> coreNameAndAclTracker.getSecond() != null)
                .forEach(coreNameAndAclTracker ->
                        report.add(
                                coreNameAndAclTracker.getFirst(),
                                buildAclTxReport(
                                        trackerRegistry,
                                        informationServers.get(coreNameAndAclTracker.getFirst()),
                                        coreNameAndAclTracker.getFirst(),
                                        coreNameAndAclTracker.getSecond(),
                                        acltxid)));

        if (report.size() == 0)
        {
            addAlertMessage(report);
        }
    }

    private void rangeCheck(SolrQueryResponse rsp, SolrParams params) throws IOException
    {
        String coreName =
                ofNullable(coreName(params))
                        .orElseThrow(() -> new AlfrescoRuntimeException("No " + params.get(CoreAdminParams.CORE + " parameter set.")));

        if (isMasterOrStandalone(coreName))
        {
            InformationServer informationServer = informationServers.get(coreName);

            DocRouter docRouter = getDocRouter(coreName);

            if(docRouter instanceof DBIDRangeRouter)
            {
                DBIDRangeRouter dbidRangeRouter = (DBIDRangeRouter) docRouter;

                if(!dbidRangeRouter.getInitialized())
                {
                    rsp.add("expand", 0);
                    rsp.add("exception", "DBIDRangeRouter not initialized yet.");
                    return;
                }

                long startRange = dbidRangeRouter.getStartRange();
                long endRange = dbidRangeRouter.getEndRange();

                long maxNodeId = informationServer.maxNodeId();
                long minNodeId = informationServer.minNodeId();
                long nodeCount = informationServer.nodeCount();

                long bestGuess = -1;  // -1 means expansion cannot be done. Either because expansion
                // has already happened or we're above safe range

                long range = endRange - startRange; // We want this many nodes on the server

                long midpoint = startRange + ((long) (range * .5));

                long safe = startRange + ((long) (range * .75));

                long offset = maxNodeId-startRange;

                double density = 0;

                if(offset > 0)
                {
                    density = ((double)nodeCount) / ((double)offset); // This is how dense we are so far.
                }

                if (!dbidRangeRouter.getExpanded())
                {
                    if(maxNodeId <= safe)
                    {
                        if (maxNodeId >= midpoint)
                        {
                            if(density >= 1 || density == 0)
                            {
                                //This is fully dense shard or an empty shard.
                                // If it does happen, no expand is required.
                                bestGuess=0;
                            }
                            else
                            {
                                double multiplier = 1/density;
                                bestGuess = (long)(range*multiplier)-range; // This is how much to add
                            }
                        }
                        else
                        {
                            bestGuess = 0; // We're below the midpoint so it's to early to make a guess.
                        }
                    }
                }

                rsp.add("start", startRange);
                rsp.add("end", endRange);
                rsp.add("nodeCount", nodeCount);
                rsp.add("minDbid", minNodeId);
                rsp.add("maxDbid", maxNodeId);
                rsp.add("density", Math.abs(density));
                rsp.add("expand", bestGuess);
                rsp.add("expanded", dbidRangeRouter.getExpanded());
            }
            else
            {
                rsp.add("expand", -1);
                rsp.add("exception", "ERROR: Wrong document router type:"+docRouter.getClass().getSimpleName());
            }
        }
        else
        {
            NamedList<Object> report = new SimpleOrderedMap<>();
            rsp.add(REPORT, report);
            addAlertMessage(report);
        }
    }

    private synchronized void expand(SolrQueryResponse rsp, SolrParams params) throws IOException
    {
        String coreName =
                ofNullable(coreName(params))
                        .orElseThrow(() -> new AlfrescoRuntimeException("No " + params.get(CoreAdminParams.CORE + " parameter set.")));

        if (isMasterOrStandalone(coreName))
        {
            InformationServer informationServer = informationServers.get(coreName);
            DocRouter docRouter = getDocRouter(coreName);

            if(docRouter instanceof DBIDRangeRouter)
            {
                long expansion = Long.parseLong(params.get("add"));
                DBIDRangeRouter dbidRangeRouter = (DBIDRangeRouter)docRouter;

                if(!dbidRangeRouter.getInitialized())
                {
                    rsp.add("expand", -1);
                    rsp.add("exception", "DBIDRangeRouter not initialized yet.");
                    return;
                }

                if(dbidRangeRouter.getExpanded())
                {
                    rsp.add("expand", -1);
                    rsp.add("exception", "dbid range has already been expanded.");
                    return;
                }

                long currentEndRange = dbidRangeRouter.getEndRange();
                long startRange = dbidRangeRouter.getStartRange();
                long maxNodeId = informationServer.maxNodeId();

                long range = currentEndRange - startRange;
                long safe = startRange + ((long) (range * .75));

                if(maxNodeId > safe)
                {
                    rsp.add("expand", -1);
                    rsp.add("exception", "Expansion cannot occur if max DBID in the index is more then 75% of range.");
                    return;
                }

                long newEndRange = expansion+dbidRangeRouter.getEndRange();
                try
                {
                    informationServer.capIndex(newEndRange);
                    informationServer.hardCommit();
                    dbidRangeRouter.setEndRange(newEndRange);
                    dbidRangeRouter.setExpanded(true);
                    assert newEndRange == dbidRangeRouter.getEndRange();
                    rsp.add("expand", dbidRangeRouter.getEndRange());
                }
                catch(Throwable t)
                {
                    rsp.add("expand", -1);
                    rsp.add("exception", t.getMessage());
                    LOGGER.error("exception expanding", t);
                }
            }
            else
            {
                rsp.add("expand", -1);
                rsp.add("exception", "Wrong document router type:" + docRouter.getClass().getSimpleName());
            }
        }
        else
        {
            NamedList<Object> report = new SimpleOrderedMap<>();
            rsp.add(REPORT, report);
            addAlertMessage(report);
        }
    }

    private void actionREPORT(SolrQueryResponse rsp, SolrParams params) throws JSONException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add(REPORT, report);

        Long fromTime = getSafeLong(params, "fromTime");
        Long toTime = getSafeLong(params, "toTime");
        Long fromTx = getSafeLong(params, "fromTx");
        Long toTx = getSafeLong(params, "toTx");
        Long fromAclTx = getSafeLong(params, "fromAclTx");
        Long toAclTx = getSafeLong(params, "toAclTx");

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(trackerRegistry::hasTrackersForCore)
                .filter(this::isMasterOrStandalone)
                .forEach(coreName ->
                        report.add(
                                coreName,
                                buildTrackerReport(
                                        trackerRegistry,
                                        informationServers.get(coreName),
                                        coreName,
                                        fromTx,
                                        toTx,
                                        fromAclTx,
                                        toAclTx,
                                        fromTime,
                                        toTime)));

        if (report.size() == 0)
        {
            addAlertMessage(report);
        }
    }

    private void actionPURGE(SolrParams params)
    {
        Consumer<String> purgeOnSpecificCore = coreName -> {
            final MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            final AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            apply(params, ARG_TXID, metadataTracker::addTransactionToPurge)
                    .andThen(apply(params, ARG_ACLTXID, aclTracker::addAclChangeSetToPurge))
                    .andThen(apply(params, ARG_NODEID, metadataTracker::addNodeToPurge))
                    .andThen(apply(params, ARG_ACLID, aclTracker::addAclToPurge));
        };

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(purgeOnSpecificCore);
    }

    private void actionREINDEX(SolrParams params)
    {
        Consumer<String> reindexOnSpecificCore = coreName -> {
            final MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            final AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            apply(params, ARG_TXID, metadataTracker::addTransactionToReindex)
                    .andThen(apply(params, ARG_ACLTXID, aclTracker::addAclChangeSetToReindex))
                    .andThen(apply(params, ARG_NODEID, metadataTracker::addNodeToReindex))
                    .andThen(apply(params, ARG_ACLID, aclTracker::addAclToReindex));

            ofNullable(params.get(ARG_QUERY)).ifPresent(metadataTracker::addQueryToReindex);
        };

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(reindexOnSpecificCore);
    }

    private void actionRETRY(SolrQueryResponse rsp, SolrParams params)
    {
        final Consumer<String> retryOnSpecificCore = coreName -> {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            InformationServer srv = informationServers.get(coreName);

            try
            {
                for (Long nodeid : srv.getErrorDocIds())
                {
                    tracker.addNodeToReindex(nodeid);
                }
                rsp.add(coreName, srv.getErrorDocIds());
            }
            catch (Exception exception)
            {
                LOGGER.error("I/O Exception while adding Node to reindex.", exception);
            }
        };

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(retryOnSpecificCore);
    }

    private void actionINDEX(SolrParams params)
    {
        Consumer<String> indexOnSpecificCore = coreName -> {
            final MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            final AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            apply(params, ARG_TXID, metadataTracker::addTransactionToIndex)
                    .andThen(apply(params, ARG_ACLTXID, aclTracker::addAclChangeSetToIndex))
                    .andThen(apply(params, ARG_NODEID, metadataTracker::addNodeToIndex))
                    .andThen(apply(params, ARG_ACLID, aclTracker::addAclToIndex));
        };

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(indexOnSpecificCore);
    }

    private void actionFIX(SolrParams params) throws JSONException
    {
        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(this::fixOnSpecificCore);
    }

    private void fixOnSpecificCore(String coreName)
    {
        try
        {
            // Gets Metadata health and fixes any problems
            MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            IndexHealthReport indexHealthReport = metadataTracker.checkIndex(null, null, null, null);
            IOpenBitSet toReindex = indexHealthReport.getTxInIndexButNotInDb();
            toReindex.or(indexHealthReport.getDuplicatedTxInIndex());
            toReindex.or(indexHealthReport.getMissingTxFromIndex());
            long current = -1;
            // Goes through problems in the index
            while ((current = toReindex.nextSetBit(current + 1)) != -1) {
                metadataTracker.addTransactionToReindex(current);
            }

            // Gets the Acl health and fixes any problems
            AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            indexHealthReport = aclTracker.checkIndex(null, null, null, null);
            toReindex = indexHealthReport.getAclTxInIndexButNotInDb();
            toReindex.or(indexHealthReport.getDuplicatedAclTxInIndex());
            toReindex.or(indexHealthReport.getMissingAclTxFromIndex());
            current = -1;
            // Goes through the problems in the index
            while ((current = toReindex.nextSetBit(current + 1)) != -1) {
                aclTracker.addAclChangeSetToReindex(current);
            }
        }
        catch(Exception exception)
        {
            throw new AlfrescoRuntimeException("", exception);
        }
    }

    private void actionSUMMARY(SolrQueryResponse rsp, SolrParams params)
    {
        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add("Summary", report);

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(coreName -> coreSummary(params, report, coreName));
    }

    private void coreSummary(SolrParams params, NamedList<Object> report, String coreName)
    {
        boolean detail = getSafeBoolean(params, "detail");
        boolean hist = getSafeBoolean(params, "hist");
        boolean values = getSafeBoolean(params, "values");
        boolean reset = getSafeBoolean(params, "reset");

        InformationServer srv = informationServers.get(coreName);
        if (srv != null)
        {
            try
            {
                if (isMasterOrStandalone(coreName))
                {
                    addMasterOrStandaloneCoreSummary(trackerRegistry, coreName, detail, hist, values, srv, report);

                    if (reset)
                    {
                        srv.getTrackerStats().reset();
                    }
                } else
                    {
                    addSlaveCoreSummary(trackerRegistry, coreName, detail, hist, values, srv, report);
                }
            }
            catch(Exception exception)
            {
                throw new AlfrescoRuntimeException("", exception);
            }
        }
        else
        {
            report.add(coreName, "Core unknown");
        }
    }

    private DocRouter getDocRouter(String cname)
    {
        return notNullOrEmpty(trackerRegistry.getTrackersForCore(cname))
                .stream()
                .filter(tracker -> tracker instanceof MetadataTracker)
                .findAny()
                .map(MetadataTracker.class::cast)
                .map(MetadataTracker::getDocRouter)
                .orElse(null);
    }

    public ConcurrentHashMap<String, InformationServer> getInformationServers()
    {
        return this.informationServers;
    }

    public TrackerRegistry getTrackerRegistry()
    {
        return trackerRegistry;
    }

    void setTrackerRegistry(TrackerRegistry trackerRegistry)
    {
        this.trackerRegistry = trackerRegistry;
    }

    public SolrTrackerScheduler getScheduler()
    {
        return scheduler;
    }

    private void waitForTenSeconds()
    {
        try
        {
            TimeUnit.SECONDS.sleep(10);
        }
        catch (InterruptedException e)
        {
            //Don't care
        }
    }

    /**
     * Returns, for the given core, the component which is in charge to publish the core state.
     *
     * @param coreName the owning core name.
     * @return the component which is in charge to publish the core state.
     */
    private CoreStatePublisher nodeStatePublisher(String coreName)
    {
        return ofNullable(trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class))
                .map(CoreStatePublisher.class::cast)
                .orElse(trackerRegistry.getTrackerForCore(coreName, SlaveCoreStatePublisher.class));
    }

    /**
     * Quickly checks if the given name is associated to a master or standalone core.
     *
     * @param coreName the core name.
     * @return true if the name is associated with a master or standalone mode, false otherwise.
     */
    private boolean isMasterOrStandalone(String coreName)
    {
        return trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class) != null;
    }

    /**
     * Adds to the returned report an information message alerting the receiver that this core is a slave,
     * and therefore the same request should be re-submited to the corresponding master.
     *
     * @param report the response report.
     */
    private void addAlertMessage(NamedList<Object> report)
    {
        report.add(
                "WARNING",
                "The requested endpoint is not available on the slave. " +
                        "Please re-submit the same request to the corresponding Master");
    }

    private BiConsumer<String, Consumer<Long>> apply(SolrParams params, String parameterName, Consumer<Long> executeSideEffectAction)
    {
        return (parameter, consumer) ->
                ofNullable(params.get(parameterName))
                        .map(Long::valueOf)
                        .ifPresent(executeSideEffectAction);
    }

    private Collection<String> coreNames()
    {
        return notNullOrEmpty(trackerRegistry.getCoreNames());
    }

    /**
     * Returns the core name indicated in the request parameters.
     * A first attempt is done in order to check if a standard {@link CoreAdminParams#CORE} parameter is in the request.
     * If not, the alternative "coreName" parameter name is used.
     *
     * @param params the request parameters.
     * @return the core name specified in the request, null if the parameter is not found.
     */
    private String coreName(SolrParams params)
    {
        return CORE_PARAMETER_NAMES.stream()
                .map(params::get)
                .filter(Objects::nonNull)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }
}