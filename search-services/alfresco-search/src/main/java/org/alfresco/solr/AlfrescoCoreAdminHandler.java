/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
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
import org.apache.solr.core.SolrResourceLoader;
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
import java.util.ArrayList;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.LongToIntFunction;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_INACLTXID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_INTXID;
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
import static org.alfresco.solr.utils.Utils.isNullOrEmpty;
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
    private static final String SUMMARY = "Summary";
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

    /**
     * Action status to be added to response
     * - success: the action has been executed successfully
     * - error: the action has not been executed, error message is added to the response
     * - scheduled: the action will be executed as part of the Tracker Maintenance step
     */
    private static final String ACTION_STATUS_SUCCESS = "success";
    private static final String ACTION_STATUS_ERROR = "error";
    static final String ACTION_STATUS_SCHEDULED = "scheduled";
    static final String ACTION_STATUS_NOT_SCHEDULED = "notScheduled";

    static final String DRY_RUN_PARAMETER_NAME = "dryRun";
    static final String FROM_TX_COMMIT_TIME_PARAMETER_NAME = "fromTxCommitTime";
    static final String TO_TX_COMMIT_TIME_PARAMETER_NAME = "toTxCommitTime";
    static final String MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME = "maxScheduledTransactions";
    static final String MAX_TRANSACTIONS_TO_SCHEDULE_CONF_PROPERTY_NAME = "alfresco.admin.fix.maxScheduledTransactions";
    static final String TX_IN_INDEX_NOT_IN_DB = "txInIndexNotInDb";
    static final String DUPLICATED_TX_IN_INDEX = "duplicatedTxInIndex";
    static final String MISSING_TX_IN_INDEX = "missingTxInIndex";
    static final String ACL_TX_IN_INDEX_NOT_IN_DB = "aclTxInIndexNotInDb";
    static final String DUPLICATED_ACL_TX_IN_INDEX = "duplicatedAclTxInIndex";
    static final String MISSING_ACL_TX_IN_INDEX = "missingAclTxInIndex";

    /**
     * JSON/XML labels for the Action response
     */
    private static final String ACTION_LABEL = "action";
    static final String ACTION_STATUS_LABEL = "status";

    static final String ACTION_ERROR_MESSAGE_LABEL = "errorMessage";
    static final String UNKNOWN_CORE_MESSAGE = "Unknown core:";
    static final String UNPROCESSABLE_REQUEST_ON_SLAVE_NODES = "Requested action cannot be performed on slave nodes.";

    private static final String ACTION_TX_TO_REINDEX = "txToReindex";
    private static final String ACTION_ACL_CHANGE_SET_TO_REINDEX = "aclChangeSetToReindex";

    private SolrTrackerScheduler scheduler;
    TrackerRegistry trackerRegistry;
    ConcurrentHashMap<String, InformationServer> informationServers;

    private final static List<String> CORE_PARAMETER_NAMES = asList(CoreAdminParams.CORE, "coreName", "index");

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
     * Synchronous execution
     *
     * @param names comma delimited list of core names that will be created.
     * @param numShards The total number of shards.
     * @param replicationFactor - Not sure why the core needs to know this.
     * @param nodeInstance - Not sure why the core needs to know this.
     * @param numNodes - Not sure why the core needs to know this.
     * @param shardIds A comma separated list of shard ids for this core (or null).
     * @return Response including the action result:
     * - status: success, when the resource has been reloaded
     * - status: error, when the resource has NOT been reloaded
     * - errorMessage: message, if action status is "error" an error message node is included
     */
    private NamedList<Object> setupNewDefaultCores(String names, int numShards, int replicationFactor, int nodeInstance, int numNodes, String shardIds)
    {

        var wrapper = new Object()
        {
            NamedList<Object> response = new SimpleOrderedMap<>();;
        };

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
                        wrapper.response.addAll(newCore(coreName, numShards, storeRef, DEFAULT_TEMPLATE, replicationFactor, nodeInstance,
                                    numNodes, shardIds, null));
                    });
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to create default alfresco cores (workspace/archive stores)", exception);
            wrapper.response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            wrapper.response.add(ACTION_ERROR_MESSAGE_LABEL, exception.getMessage());
            return wrapper.response;
        }

        wrapper.response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);
        return wrapper.response;

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

                if (trackerRegistry.getModelTracker() != null)
                    trackerRegistry.getModelTracker().shutdown();

                trackerRegistry.setModelTracker(null);
                scheduler.shutdown();
            }
        }
        catch (Exception exception)
        {
            LOGGER.error(
                    "Unable to properly shut down Alfresco core container services. See the exception below for further details.",
                    exception);
        }
    }

    /**
     * Update memory loading from "log4j.properties" file for each Core
     *
     * Synchronous execution
     *
     * @param resource The name of the resource file to be reloaded
     * @result Response including the action result:
     * - status: success, when the resource has been reloaded
     * - status: error, when the resource has NOT been reloaded
     * - errorMessage: message, if action status is "error" an error message node is included
     */
    private NamedList<Object> initResourceBasedLogging(String resource)
    {

        NamedList<Object> response = new SimpleOrderedMap<>();

        try
        {
            Class<?> clazz = Class.forName("org.apache.log4j.PropertyConfigurator");
            Method method = clazz.getMethod("configure", Properties.class);
            InputStream is = openResource(coreContainer.getSolrHome() + "/../logs", resource);
            Properties p = new Properties();
            p.load(is);
            method.invoke(null, p);
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);

        }
        catch (ClassNotFoundException e)
        {
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL, "ClassNotFoundException: org.apache.log4j.PropertyConfigurator");
            return response;
        }
        catch (Exception e)
        {
            LOGGER.info("Failed to load " + resource, e);
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL, e.getMessage());
            return response;
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    protected void handleCustomAction(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        SolrParams params = req.getParams();
        String action =
                ofNullable(params.get(CoreAdminParams.ACTION))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .orElse("");
        String coreName = coreName(params);
        LOGGER.info("Running action {} for core {} with params {}", action, coreName, params);

        try
        {
            switch (action) {
                // Create a new Core in SOLR
                case "NEWCORE":
                case "NEWINDEX":
                    rsp.add(ACTION_LABEL, newCore(req));
                    break;
                // Reload an existing Core in SOLR
                case "UPDATECORE":
                case "UPDATEINDEX":
                    rsp.add(ACTION_LABEL, updateCore(req));
                    break;
                // Update memory loading from "shared.properties" file for each Core
                case "UPDATESHARED":
                    rsp.add(ACTION_LABEL, updateShared(req));
                    break;
                // Unload an existing Core from SOLR
                case "REMOVECORE":
                    rsp.add(ACTION_LABEL, removeCore(req));
                    break;
                // Create a new Core in SOLR with default settings
                case "NEWDEFAULTINDEX":
                case "NEWDEFAULTCORE":
                    rsp.add(ACTION_LABEL, newDefaultCore(req));
                    break;
                // Enable check flag on a SOLR Core
                case "CHECK":
                    rsp.add(ACTION_LABEL, actionCHECK(coreName));
                    break;
                // Get a report from a nodeId with the associated txId and the indexing status
                case "NODEREPORT":
                    rsp.add(REPORT, actionNODEREPORTS(params));
                    break;
                // Get a report from an aclId with the count of documents associated to the ACL
                case "ACLREPORT":
                    rsp.add(REPORT, actionACLREPORT(params));
                    break;
                // Get a report from a txId with detailed information related with the Transaction
                case "TXREPORT":
                    rsp.add(REPORT, actionTXREPORT(params));
                    break;
                // Get a report from an aclTxId with detailed information related with nodes indexed
                // for an ACL inside a Transaction
                case "ACLTXREPORT":
                    rsp.add(REPORT, actionACLTXREPORT(params));
                    break;
                // Get a detailed report including storage and sizing for a Shards configured
                // with Shard DB_ID_RANGE method. If SOLR is not using this configuration,
                // "expand = -1" is returned
                case "RANGECHECK":
                    rsp.getValues().addAll(rangeCheck(params));
                    break;
                // Expand the range for a Shard configured with DB_ID_RANGE having more than 75%
                // space used. This configuration is not persisted in solrcore.properties
                case "EXPAND":
                    rsp.getValues().addAll(expand(params));
                    break;
                // Get a detailed report for a core or for every core. This action accepts
                // filtering based on commitTime, txid and acltxid
                case "REPORT":
                    rsp.add(REPORT, actionREPORT(params));
                    break;
                // Add a nodeid, txid, acltxid or aclid to be purged on the next maintenance
                // operation performed by MetadataTracker and AclTracker.
                // Asynchronous.
                case "PURGE":
                    rsp.add(ACTION_LABEL, actionPURGE(params));
                    break;
                // Add a nodeid, txid, acltxid, aclid or SOLR query to be reindexed on the
                // next maintenance operation performed by MetadataTracker and AclTracker.
                // Asynchronous.
                case "REINDEX":
                    rsp.add(ACTION_LABEL, actionREINDEX(params));
                    break;
                // Reindex every node marked as ERROR in a core or in every core.
                // Asynchronous.
                case "RETRY":
                    rsp.add(ACTION_LABEL, actionRETRY(params));
                    break;
                // Add a nodeid, txid, acltxid or aclid to be indexed on the next maintenance
                // operation performed by MetadataTracker and AclTracker.
                // Asynchronous.
                case "INDEX":
                    rsp.add(ACTION_LABEL, actionINDEX(params));
                    break;
                // Find transactions and acls missing or duplicated in the cores and
                // add them to be reindexed on the next maintenance operation
                // performed by MetadataTracker and AclTracker.
                // Asynchronous.
                case "FIX":
                    rsp.add(ACTION_LABEL, actionFIX(params));
                    break;
                // Get detailed report for a core or for every core including information
                // related with handlers and trackers.
                case "SUMMARY":
                    rsp.add(SUMMARY, actionSUMMARY(params));
                    break;
                 // Update memory loading from "log4j.properties" file for each Core
                case "LOG4J":
                    rsp.add(ACTION_LABEL, initResourceBasedLogging(
                            ofNullable(params.get("resource"))
                                .orElse("log4j.properties")));
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

    /**
     * Create a new Core in SOLR
     *
     * Synchronous execution
     *
     * @param req Query Request including following parameters:
     * - coreName, mandatory, the name of the core to be created
     * - storeRef, mandatory, the storeRef for the SOLR Core (workspace://SpacesStore, archive://SpacesStore)
     * - shardIds, optional, a String including a list of ShardIds separated with comma
     * - numShards, optional, the number of Shards to be created
     * - template, optional, the name of the SOLR template used to create the core (rerank, norerank)
     * - replicationFactor, optional, number of Core replicas
     * - nodeInstance, optional, number of the Node instance
     * - numNodes, optional, number of Nodes
     *
     * @return NamedList including the action result:
     * - status: success, when the core has been created
     * - status: error, when the core has NOT been created
     * - errorMessage: message, if action status is "error" an error message node is included
     */
    private NamedList<Object> newCore(SolrQueryRequest req)
    {
        SolrParams params = req.getParams();
        req.getContext();

        NamedList<Object> response = new SimpleOrderedMap<>();

        // If numCore > 1 we are creating a collection of cores for a sole node in a cluster
        int numShards = params.getInt("numShards", 1);

        String store = params.get("storeRef");
        if (store == null || store.trim().length() == 0)
        {
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL, "Core " + coreName(params) + " has NOT been created as storeRef param is required");
            return response;
        }

        StoreRef storeRef = new StoreRef(store);

        String templateName = ofNullable(params.get("template")).orElse("vanilla");

        int replicationFactor =  params.getInt("replicationFactor", 1);
        int nodeInstance =  params.getInt("nodeInstance", -1);
        int numNodes =  params.getInt("numNodes", 1);

        String coreName = coreName(params);
        String shardIds = params.get("shardIds");

        response = newCore(coreName, numShards, storeRef, templateName, replicationFactor, nodeInstance, numNodes, shardIds, extractCustomProperties(params));
        if (!Objects.equals(response.get(ACTION_STATUS_LABEL), ACTION_STATUS_ERROR))
        {
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);
        }
        return response;
    }

    /**
     * Creates new default cores using default values.
     *
     * Synchronous execution
     *
     * @param req Query Request including following parameters:
     * - coreName, mandatory, the name of the core to be created
     * - storeRef, optional, the storeRef for the SOLR Core (workspace://SpacesStore, archive://SpacesStore)
     * - template, optional, the name of the SOLR template used to create the core (rerank, norerank)
     * @return Response including the action result:
     * - status: success, when the resource has been reloaded
     * - status: error, when the resource has NOT been reloaded
     * - errorMessage: message, if action status is "error" an error message node is included
     */
    private NamedList<Object> newDefaultCore(SolrQueryRequest req)
    {

        NamedList<Object> response = new SimpleOrderedMap<>();

        SolrParams params = req.getParams();
        String coreName = ofNullable(coreName(params)).orElse(ALFRESCO_CORE_NAME);
        String templateName =
                params.get("template") != null
                        ? params.get("template")
                        : DEFAULT_TEMPLATE;

        Properties extraProperties = extractCustomProperties(params);

        response = newDefaultCore(
                coreName,
                ofNullable(params.get("storeRef"))
                        .map(StoreRef::new)
                        .orElse(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE),
                templateName,
                extraProperties);
        if (!Objects.equals(response.get(ACTION_STATUS_LABEL), ACTION_STATUS_ERROR))
        {
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);
        }
        return response;
    }

    private NamedList<Object> newDefaultCore(String coreName, StoreRef storeRef, String templateName, Properties extraProperties)
    {
        return newCore(coreName, 1, storeRef, templateName, 1, 1, 1, null, extraProperties);
    }

    protected NamedList<Object> newCore(String coreName, int numShards, StoreRef storeRef, String templateName, int replicationFactor, int nodeInstance, int numNodes, String shardIds, Properties extraProperties)
    {

        NamedList<Object> response = new SimpleOrderedMap<>();

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

                if (nodeInstance == -1)
                {
                    response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
                    response.add(ACTION_ERROR_MESSAGE_LABEL, "Core " + coreName + " has NOT been created as nodeInstance param is required");
                    return response;
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
                        response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
                        response.add(ACTION_ERROR_MESSAGE_LABEL, "Core " + coreName + " has NOT been created as explicit Sharding policy is not valid");
                        return response;
                    }
                    shards = policy.getShardIdsForNode(nodeInstance);
                }

                List<String> coresNotCreated = new ArrayList<>();
                for (Integer shard : shards)
                {
                    coreName = coreBase + shard;
                    File newCore = new File(baseDirectory, coreName);

                    response.addAll(createAndRegisterNewCore(extraProperties, storeRef, template, coreName,
                            newCore, numShards, shard, templateName));
                    if (Objects.equals(response.get(ACTION_STATUS_LABEL), ACTION_STATUS_ERROR))
                    {
                        coresNotCreated.add(coreName);
                    }
                }

                if (coresNotCreated.size() > 0)
                {
                    response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
                    response.add(ACTION_ERROR_MESSAGE_LABEL,
                            "Following cores have not been created: " + coresNotCreated);
                    return response;
                }
                else
                {
                    response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);
                    return response;
                }
            }
            else
            {
                if (coreName == null)
                {
                    coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
                }
                File newCore = new File(solrHome, coreName);
                return createAndRegisterNewCore(extraProperties, storeRef, template, coreName, newCore, 0, 0, templateName);
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
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL,
                    "Core " + coreName + " has NOT been created. Check the log to find out the reason.");
            return response;
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

    private NamedList<Object> createAndRegisterNewCore(Properties extraProperties, StoreRef storeRef, File template, String coreName, File newCore, int shardCount, int shardInstance, String templateName) throws IOException
    {

        NamedList<Object> response = new SimpleOrderedMap<>();

        if (coreContainer.getLoadedCoreNames().contains(coreName))
        {
            //Core alfresco exists
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL, "core " + coreName + " already exists, not creating again.");
            return response;
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
        response.add("core", core.getName());
        return response;
    }

    boolean hasAlfrescoCore(Collection<SolrCore> cores)
    {
        return notNullOrEmpty(cores).stream()
                .map(SolrCore::getName)
                .anyMatch(trackerRegistry::hasTrackersForCore);
    }

    /**
     * Update memory loading from "shared.properties" file for each Core
     *
     * Synchronous execution
     *
     * @param req Query Request with no parameters
     * @return Response including the action result:
     * - status: success, when the properties has been reloaded
     * - status: error, when the properties has NOT been reloaded
     * - errorMessage: message, if action status is "error" an error message node is included
     */
    private NamedList<Object> updateShared(SolrQueryRequest req)
    {
        SolrParams params = req.getParams();
        NamedList<Object> response = new SimpleOrderedMap<>();

        try
        {
            File config = new File(AlfrescoSolrDataModel.getResourceDirectory(), AlfrescoSolrDataModel.SHARED_PROPERTIES);
            updateSharedProperties(params, config, hasAlfrescoCore(coreContainer.getCores()));

            coreContainer.getCores().stream()
                    .map(SolrCore::getName)
                    .forEach(coreContainer::reload);

            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);

        }
        catch (IOException e)
        {
            LOGGER.error("Failed to update Shared properties ", e);
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL, "Shared properties couldn't be reloaded for some core. Check the log to find out the reason.");
            return response;
        }
        return response;
    }

    /**
     * Reload an existing Core in SOLR
     *
     * Synchronous execution
     *
     * @param req Query Request including following parameters:
     * - coreName, mandatory, the name of the core to be reloaded
     *
     * @return Response including the action result:
     * - status: success, when the core has been reloaded
     * - status: error, when the core has NOT been reloaded
     * - errorMessage: message, if action status is "error" an error message node is included
     */
    private NamedList<Object> updateCore(SolrQueryRequest req)
    {

        var wrapper = new Object()
        {
            NamedList<Object> response = new SimpleOrderedMap<>();;
        };

        ofNullable(coreName(req.getParams()))
                .map(String::trim)
                .filter(coreName -> !coreName.isEmpty())
                .ifPresentOrElse(coreName -> {
                    try (SolrCore core = coreContainer.getCore(coreName))
                    {

                        if (core == null)
                        {
                            wrapper.response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
                            wrapper.response.add(ACTION_ERROR_MESSAGE_LABEL, "Core " + coreName + " has NOT been updated as it doesn't exist");
                        }
                        else
                        {

                            String configLocaltion = core.getResourceLoader().getConfigDir();
                            File config = new File(configLocaltion, "solrcore.properties");
                            updatePropertiesFile(req.getParams(), config, null);

                            coreContainer.reload(coreName);

                            wrapper.response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);

                        }

                    }
                },
                () -> {
                    wrapper.response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
                    wrapper.response.add(ACTION_ERROR_MESSAGE_LABEL, "Core has NOT been updated as coreName param is required");
                });

        return wrapper.response;

    }

    /**
     * Unload an existing Core from SOLR
     *
     * Synchronous execution
     *
     * @param req Query Request including following parameters:
     * - coreName, mandatory, the name of the core to be unloaded
     * - storeRef, mandatory, the storeRef for the SOLR Core (workspace://SpacesStore, archive://SpacesStore)
     *
     * @return Response including the action result:
     * - status: success, when the core has been unloaded
     * - status: error, when the core has NOT been unloaded
     * - errorMessage: message, if action status is "error" an error message node is included
     */
    private NamedList<Object> removeCore(SolrQueryRequest req)
    {
        String store = "";
        SolrParams params = req.getParams();
        NamedList<Object> response = new SimpleOrderedMap<>();

        if (params.get("storeRef") != null)
        {
            store = params.get("storeRef");
        }

        if ((store == null) || (store.length() == 0))
        {
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL, "Core " + params.get("coreName") + " has NOT been removed as storeRef param is required");
            return response;
        }

        StoreRef storeRef = new StoreRef(store);

        String coreName = ofNullable(coreName(req.getParams())).orElse(storeRef.getProtocol() + "-" + storeRef.getIdentifier());
        SolrCore core = coreContainer.getCore(coreName);

        if (core == null)
        {
            response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
            response.add(ACTION_ERROR_MESSAGE_LABEL, "Core " + params.get("coreName") + " has NOT been removed as it doesn't exist");
            return response;
        }

        // Close the references to the core to unload before actually unloading it,
        // otherwise this operation gets into and endless loop
        while (core.getOpenCount() > 1)
        {
            core.close();
        }

        // remove core
        coreContainer.unload(coreName, true, true, true);

        response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);
        return response;

    }

    /**
     * Enable check flag on a SOLR Core or on every SOLR Core
     *
     * Synchronous execution
     *
     * @param cname, optional, the name of the core to be checked
     *
     * @return Response including the action result:
     * - status: success, when the core has been created
     */
    private NamedList<Object> actionCHECK(String cname)
    {
        coreNames().stream()
                .filter(coreName -> cname == null || coreName.equals(cname))
                .map(trackerRegistry::getTrackersForCore)
                .flatMap(Collection::stream)
                .map(Tracker::getTrackerState)
                .forEach(state -> state.setCheck(true));

        NamedList<Object> response = new SimpleOrderedMap<>();
        response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SUCCESS);
        return response;
    }

    /**
     * Get a report from a nodeId with the associated txId and the indexing status
     *
     * Synchronous execution
     *
     * @param params Query Request with following parameters:
     * - nodeId, mandatory: the number of the node to build the report
     * - core, The name of the SOLR Core or "null" to get the report for every core
     * @return Response including the action result:
     * - report: An Object with the report details
     * - error: When mandatory parameters are not set, an error node is returned
     *
     * @throws JSONException
     */
    private NamedList<Object> actionNODEREPORTS(SolrParams params) throws JSONException
    {

        NamedList<Object> report = new SimpleOrderedMap<>();

        if (params.get(ARG_NODEID) == null)
        {
            report.add(ACTION_STATUS_ERROR, "No " + ARG_NODEID +" parameter set.");
            return report;
        }

        Long nodeid = Long.valueOf(params.get(ARG_NODEID));
        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(trackerRegistry::hasTrackersForCore)
                .map(coreName -> new Pair<>(coreName, coreStatePublisher(coreName)))
                .filter(coreNameAndPublisher -> coreNameAndPublisher.getSecond() != null)
                .forEach(coreNameAndPublisher ->
                        report.add(
                                coreNameAndPublisher.getFirst(),
                                buildNodeReport(coreNameAndPublisher.getSecond(), nodeid)));
        return report;
    }

    /**
     * Get a report from an aclId with the count of documents associated to the ACL
     *
     * Synchronous execution
     *
     * @param params Query Request with following parameters:
     * - aclId, mandatory, the number of the ACL to build the report
     * - core, The name of the SOLR Core or "null" to get the report for every core
     * @return Response including the action result:
     * - report: an Object with the details of the report
     * - error: When mandatory parameters are not set, an error node is returned
     *
     * @throws JSONException
     */
    private NamedList<Object> actionACLREPORT(SolrParams params) throws JSONException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();

        if (params.get(ARG_ACLID) == null)
        {
            report.add(ACTION_STATUS_ERROR, "No " + ARG_ACLID + " parameter set.");
            return report;
        }

        Long aclid = Long.valueOf(params.get(ARG_ACLID));
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

        return report;

    }

    /**
     * Get a report from a txId with detailed information related with the Transaction
     *
     * Synchronous execution
     *
     * @param params Query Request with following parameters:
     * - txId, mandatory, the number of the Transaction to build the report
     * - core, The name of the SOLR Core or "null" to get the report for every core
     * @return Response including the action result:
     * - report: an Object with the details of the report
     * - error: When mandatory parameters are not set, an error node is returned
     *
     * @throws JSONException
     */
    private NamedList<Object> actionTXREPORT(SolrParams params) throws JSONException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();

        if (params.get(ARG_TXID) == null)
        {
            report.add(ACTION_STATUS_ERROR, "No " + ARG_TXID + " parameter set.");
            return report;
        }

        Long txid = Long.valueOf(params.get(ARG_TXID));
        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .map(coreName -> new Pair<>(coreName, trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class)))
                .filter(coreNameAndMetadataTracker -> coreNameAndMetadataTracker.getSecond() != null)
                .forEach(coreNameAndMetadataTracker ->
                        report.add(
                                coreNameAndMetadataTracker.getFirst(),
                                buildTxReport(
                                        trackerRegistry,
                                        informationServers.get(coreNameAndMetadataTracker.getFirst()),
                                        coreNameAndMetadataTracker.getFirst(),
                                        coreNameAndMetadataTracker.getSecond(),
                                        txid)));

        if (report.size() == 0)
        {
            addAlertMessage(report);
        }
        return report;

    }

    /**
     * Get a report from an aclTxId with detailed information related with nodes indexed
     * for an ACL inside a Transaction
     *
     * Synchronous execution
     *
     * @param params Query Request with following parameters:
     * - acltxid, mandatory, the number of the ACL TX Id to build the report
     * @return Response including the action result:
     * - report: an Object with the details of the report
     * - error: When mandatory parameters are not set, an error node is returned
     *
     * @throws JSONException
     */
    private NamedList<Object> actionACLTXREPORT(SolrParams params) throws JSONException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();

        if (params.get(ARG_ACLTXID) == null)
        {
            report.add(ACTION_STATUS_ERROR, "No " + ARG_ACLTXID + " parameter set.");
            return report;
        }

        Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
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
        return report;
    }

    /**
     * Get a detailed report including storage and sizing for a Shards configured with Shard DB_ID_RANGE method.
     * If SOLR is not using this configuration,"expand = -1" is returned
     *
     * Synchronous execution
     *
     * @param params
     * - core, The name of the SOLR Core
     * @return Response including the action result:
     * - report: An Object with the report details
     * - error: When mandatory parameters are not set, an error node is returned
     *
     * @throws IOException
     */
    private NamedList<Object> rangeCheck(SolrParams params) throws IOException
    {
        NamedList<Object> response = new SimpleOrderedMap<>();

        String coreName = coreName(params);
        if (coreName == null)
        {
            response.add(ACTION_STATUS_ERROR, "No " + CoreAdminParams.CORE + " parameter set.");
            return response;
        }

        if (isMasterOrStandalone(coreName))
        {
            InformationServer informationServer = informationServers.get(coreName);

            DocRouter docRouter = getDocRouter(coreName);

            if(docRouter instanceof DBIDRangeRouter)
            {
                DBIDRangeRouter dbidRangeRouter = (DBIDRangeRouter) docRouter;

                if(!dbidRangeRouter.getInitialized())
                {
                    response.add("expand", 0);
                    response.add("exception", "DBIDRangeRouter not initialized yet.");
                    return response;
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

                response.add("start", startRange);
                response.add("end", endRange);
                response.add("nodeCount", nodeCount);
                response.add("minDbid", minNodeId);
                response.add("maxDbid", maxNodeId);
                response.add("density", Math.abs(density));
                response.add("expand", bestGuess);
                response.add("expanded", dbidRangeRouter.getExpanded());
            }
            else
            {
                response.add("expand", -1);
                response.add("exception", "ERROR: Wrong document router type:" + docRouter.getClass().getSimpleName());
            }
        }
        else
        {
            addAlertMessage(response);
        }
        return response;
    }

    /**
     * Expand the range for a Shard configured with DB_ID_RANGE having more than 75%
     * space used. This configuration is not persisted in solrcore.properties
     *
     * Synchronous execution
     *
     * @param params
     * - core, mandatory: The name of the SOLR Core
     * - add, mandatory: the number of nodes to be added to the End Range limit
     * @return Response including the action result:
     * - expand: The number of the new End Range limit or -1 if the action failed
     * - exception: Error message if expand is -1
     * - error: When mandatory parameters are not set, an error node is returned
     *
     * @throws IOException
     */
    private synchronized NamedList<Object> expand(SolrParams params) throws IOException
    {
        NamedList<Object> response = new SimpleOrderedMap<>();

        String coreName = coreName(params);
        if (coreName == null)
        {
            response.add(ACTION_STATUS_ERROR, "No " + CoreAdminParams.CORE + " parameter set.");
            return response;
        }

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
                    response.add("expand", -1);
                    response.add("exception", "DBIDRangeRouter not initialized yet.");
                    return response;
                }

                if(dbidRangeRouter.getExpanded())
                {
                    response.add("expand", -1);
                    response.add("exception", "dbid range has already been expanded.");
                    return response;
                }

                long currentEndRange = dbidRangeRouter.getEndRange();
                long startRange = dbidRangeRouter.getStartRange();
                long maxNodeId = informationServer.maxNodeId();

                long range = currentEndRange - startRange;
                long safe = startRange + ((long) (range * .75));

                if(maxNodeId > safe)
                {
                    response.add("expand", -1);
                    response.add("exception", "Expansion cannot occur if max DBID in the index is more then 75% of range.");
                    return response;
                }

                long newEndRange = expansion+dbidRangeRouter.getEndRange();
                try
                {
                    informationServer.capIndex(newEndRange);
                    informationServer.hardCommit();
                    dbidRangeRouter.setEndRange(newEndRange);
                    dbidRangeRouter.setExpanded(true);
                    assert newEndRange == dbidRangeRouter.getEndRange();
                    response.add("expand", dbidRangeRouter.getEndRange());
                }
                catch(Throwable t)
                {
                    response.add("expand", -1);
                    response.add("exception", t.getMessage());
                    LOGGER.error("exception expanding", t);
                    return response;
                }
            }
            else
            {
                response.add("expand", -1);
                response.add("exception", "Wrong document router type:" + docRouter.getClass().getSimpleName());
                return response;
            }
        }
        else
        {
            addAlertMessage(response);
        }
        return response;
    }

    /**
     * Get a detailed report for a core or for every core. This action accepts
     * filtering based on commitTime, txid and acltxid
     *
     * Synchronous execution
     *
     * @param params Query Request with following parameters:
     * - core, mandatory: The name of the SOLR Core
     * - fromTime, optional: from transaction commit time to filter report results
     * - toTime, optional: to transaction commit time to filter report results
     * - fromTx, optional: from transaction Id to filter report results
     * - toTx, optional: to transaction Id time to filter report results
     * - fromAclTx, optional: from ACL transaction Id to filter report results
     * - toCalTx, optional: to ACL transaction Id to filter report results
     *
     * - report.core: multiple Objects with the details of the report ("core" is the name of the Core)
     *
     * @throws JSONException
     */
    private NamedList<Object> actionREPORT(SolrParams params) throws JSONException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();

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
        return report;
    }

    /**
     * Add a nodeid, txid, acltxid or aclid to be purged on the next maintenance
     * operation performed by MetadataTracker and AclTracker.
     *
     * Asynchronous execution
     *
     * @param params Query Request with following parameters:
     * - core, mandatory: The name of the SOLR Core
     * - txid, optional, the number of the Transaction to purge
     * - acltxid, optional, the number of the ACL Transaction to purge
     * - nodeId, optional, the number of the node to purge
     * - aclid, optional, the number of the ACL to purge
     * @return Response including the action result:
     * - status: scheduled, as it will be executed by Trackers on the next maintenance operation
     */
    private NamedList<Object> actionPURGE(SolrParams params)
    {
        Consumer<String> purgeOnSpecificCore = coreName -> {
            final MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            final AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            apply(params, ARG_TXID, metadataTracker::addTransactionToPurge);
            apply(params, ARG_ACLTXID, aclTracker::addAclChangeSetToPurge);
            apply(params, ARG_NODEID, metadataTracker::addNodeToPurge);
            apply(params, ARG_ACLID, aclTracker::addAclToPurge);
        };

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(purgeOnSpecificCore);

        NamedList<Object> response = new SimpleOrderedMap<>();
        response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SCHEDULED);
        return response;
    }

    /**
     * Add a nodeid, txid, acltxid, aclid or SOLR query to be reindexed on the
     * next maintenance operation performed by MetadataTracker and AclTracker.
     *
     * Asynchronous execution
     *
     * @param params Query Request with following parameters:
     * - core, mandatory: The name of the SOLR Core
     * - txid, optional, the number of the Transaction to reindex
     * - acltxid, optional, the number of the ACL Transaction to reindex
     * - nodeId, optional, the number of the node to reindex
     * - aclid, optional, the number of the ACL to reindex
     * - query, optional, SOLR Query to reindex results
     * @return Response including the action result:
     * - action.status: scheduled, as it will be executed by Trackers on the next maintenance operation
     */
    private NamedList<Object> actionREINDEX(SolrParams params)
    {
        Consumer<String> reindexOnSpecificCore = coreName -> {
            final MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            final AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            apply(params, ARG_TXID, metadataTracker::addTransactionToReindex);
            apply(params, ARG_ACLTXID, aclTracker::addAclChangeSetToReindex);
            apply(params, ARG_NODEID, metadataTracker::addNodeToReindex);
            apply(params, ARG_ACLID, aclTracker::addAclToReindex);

            ofNullable(params.get(ARG_QUERY)).ifPresent(metadataTracker::addQueryToReindex);
        };

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(reindexOnSpecificCore);

        NamedList<Object> response = new SimpleOrderedMap<>();
        response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SCHEDULED);
        return response;
    }

    /**
     * Reindex every node marked as ERROR in a core or in every core.
     *
     * Asynchronous execution
     *
     * @param params Query Request with following parameters:
     * - core, optional: The name of the SOLR Core
     * - action.status: scheduled, as it will be executed by Trackers on the next maintenance operation
     * - core: list of Document Ids with error that are going to reindexed
     */
    private NamedList<Object> actionRETRY(SolrParams params)
    {
        NamedList<Object> response = new SimpleOrderedMap<>();

        final Consumer<String> retryOnSpecificCore = coreName -> {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            InformationServer srv = informationServers.get(coreName);

            try
            {
                for (Long nodeid : srv.getErrorDocIds())
                {
                    tracker.addNodeToReindex(nodeid);
                }
                response.add(coreName, srv.getErrorDocIds());
            }
            catch (Exception exception)
            {
                LOGGER.error("I/O Exception while adding Node to reindex.", exception);
                response.add(ACTION_STATUS_LABEL, ACTION_STATUS_ERROR);
                response.add(ACTION_ERROR_MESSAGE_LABEL, exception.getMessage());

            }
        };

        if (Objects.equals(response.get(ACTION_STATUS_LABEL), ACTION_STATUS_ERROR))
        {
            return response;
        }

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(retryOnSpecificCore);

        response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SCHEDULED);
        return response;
    }

    /**
     * Add a nodeid, txid, acltxid or aclid to be indexed on the next maintenance
     * operation performed by MetadataTracker and AclTracker.
     *
     * Asynchronous execution
     *
     * @param params Query Request with following parameters:
     * - core, optional: The name of the SOLR Core
     * - txid, optional, the number of the Transaction to index
     * - acltxid, optional, the number of the ACL Transaction to index
     * - nodeId, optional, the number of the node to index
     * - aclid, optional, the number of the ACL to index
     * @return Response including the action result:
     * - action.status: scheduled, as it will be executed by Trackers on the next maintenance operation
     */
    private NamedList<Object> actionINDEX(SolrParams params)
    {
        Consumer<String> indexOnSpecificCore = coreName -> {
            final MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            final AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            apply(params, ARG_TXID, metadataTracker::addTransactionToIndex);
            apply(params, ARG_ACLTXID, aclTracker::addAclChangeSetToIndex);
            apply(params, ARG_NODEID, metadataTracker::addNodeToIndex);
            apply(params, ARG_ACLID, aclTracker::addAclToIndex);
        };

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(indexOnSpecificCore);

        NamedList<Object> response = new SimpleOrderedMap<>();
        response.add(ACTION_STATUS_LABEL, ACTION_STATUS_SCHEDULED);
        return response;
    }

    /**
     * Find transactions and acls missing or duplicated in the cores and
     * add them to be reindexed on the next maintenance operation
     * performed by MetadataTracker and AclTracker.
     *
     * Asynchronous execution
     *
     * @param params
     * - core, optional: The name of the SOLR Core
     * @return Response including the action result:
     * - action.status: scheduled, as it will be executed by Trackers on the next maintenance operation
     * - txToReindex: list of Transaction Ids that are going to be reindexed
     * - aclChangeSetToReindex: list of ACL Change Set Ids that are going to be reindexed
     */
    NamedList<Object> actionFIX(SolrParams params) throws JSONException
    {
        String requestedCoreName = coreName(params);

        var wrapper = new Object()
        {
            final NamedList<Object> response = new SimpleOrderedMap<>();
        };

        if (isNullOrEmpty(requestedCoreName))
        {
            return wrapper.response;
        }

        if (!coreNames().contains(requestedCoreName))
        {
            wrapper.response.add(ACTION_ERROR_MESSAGE_LABEL, UNKNOWN_CORE_MESSAGE + requestedCoreName);
            return wrapper.response;
        }

        if (!isMasterOrStandalone(requestedCoreName)) {
            wrapper.response.add(ACTION_ERROR_MESSAGE_LABEL, UNPROCESSABLE_REQUEST_ON_SLAVE_NODES);
            return wrapper.response;
        }

        Long fromTxCommitTime = params.getLong(FROM_TX_COMMIT_TIME_PARAMETER_NAME);
        Long toTxCommitTime = params.getLong(TO_TX_COMMIT_TIME_PARAMETER_NAME);
        boolean dryRun = params.getBool(DRY_RUN_PARAMETER_NAME, true);
        int maxTransactionsToSchedule = getMaxTransactionToSchedule(params);

        LOGGER.debug("FIX Admin request on core {}, parameters: " +
                    FROM_TX_COMMIT_TIME_PARAMETER_NAME + " = {}, " +
                    TO_TX_COMMIT_TIME_PARAMETER_NAME + " = {}, " +
                    DRY_RUN_PARAMETER_NAME + " = {}, " +
                        MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME + " = {}",
                    requestedCoreName,
                    ofNullable(fromTxCommitTime).map(Object::toString).orElse("N.A."),
                    ofNullable(toTxCommitTime).map(Object::toString).orElse("N.A."),
                    dryRun,
                    maxTransactionsToSchedule);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .filter(this::isMasterOrStandalone)
                .forEach(coreName ->
                        wrapper.response.add(
                                    coreName,
                                    fixOnSpecificCore(coreName, fromTxCommitTime, toTxCommitTime, dryRun, maxTransactionsToSchedule)));

        if (wrapper.response.size() > 0)
        {
            wrapper.response.add(DRY_RUN_PARAMETER_NAME, dryRun);

            ofNullable(fromTxCommitTime).ifPresent(value -> wrapper.response.add(FROM_TX_COMMIT_TIME_PARAMETER_NAME, value));
            ofNullable(toTxCommitTime).ifPresent(value -> wrapper.response.add(TO_TX_COMMIT_TIME_PARAMETER_NAME, value));

            wrapper.response.add(MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME, maxTransactionsToSchedule);
            wrapper.response.add(ACTION_STATUS_LABEL, dryRun ? ACTION_STATUS_NOT_SCHEDULED : ACTION_STATUS_SCHEDULED);
        }

        return wrapper.response;
    }

    /**
     * Detects the transactions that need a FIX (i.e. reindexing) because the following reasons:
     *
     * <ul>
     *     <li>A transaction is in the index but not in repository</li>
     *     <li>A transaction is duplicated in the index</li>
     *     <li>A transaction is missing in the index</li>
     * </ul>
     *
     * Depending on the dryRun parameter, other than collecting, this method could also schedule the transactions for
     * reindexing.
     *
     * @param coreName the target core name.
     * @param fromTxCommitTime the start commit time we consider for collecting transaction.
     * @param toTxCommitTime the end commit time we consider for collecting transaction.
     * @param dryRun a flag indicating if the collected transactions must be actually scheduled for reindexing.
     * @param maxTransactionsToSchedule the maximum number of transactions to be scheduled for reindexing.
     * @return a report about transactions that need to be fixed.
     */
    NamedList<Object> fixOnSpecificCore(String coreName, Long fromTxCommitTime, Long toTxCommitTime, boolean dryRun, int maxTransactionsToSchedule)
    {
        try
        {
            MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            final IndexHealthReport metadataTrackerIndexHealthReport =
                    metadataTracker.checkIndex(null, fromTxCommitTime, toTxCommitTime);

            LOGGER.debug("FIX Admin action built the MetadataTracker Index Health Report on core {}, parameters: " +
                            FROM_TX_COMMIT_TIME_PARAMETER_NAME + " = {}, " +
                            TO_TX_COMMIT_TIME_PARAMETER_NAME + " = {}, " +
                            DRY_RUN_PARAMETER_NAME + " = {}, " +
                            MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME + " = {}",
                    coreName,
                    ofNullable(fromTxCommitTime).map(Object::toString).orElse("N.A."),
                    ofNullable(toTxCommitTime).map(Object::toString).orElse("N.A."),
                    dryRun,
                    maxTransactionsToSchedule);

            AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            final IndexHealthReport aclTrackerIndexHealthReport =
                    aclTracker.checkIndex(null, fromTxCommitTime, toTxCommitTime);

            LOGGER.debug("FIX Admin action built the AclTracker Index Health Report on core {}, parameters: " +
                            FROM_TX_COMMIT_TIME_PARAMETER_NAME + " = {}, " +
                            TO_TX_COMMIT_TIME_PARAMETER_NAME + " = {}, " +
                            DRY_RUN_PARAMETER_NAME + " = {}, " +
                            MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME + " = {}",
                    coreName,
                    ofNullable(fromTxCommitTime).map(Object::toString).orElse("N.A."),
                    ofNullable(toTxCommitTime).map(Object::toString).orElse("N.A."),
                    dryRun,
                    maxTransactionsToSchedule);

            NamedList<Object> response = new SimpleOrderedMap<>();
            response.add(ACTION_TX_TO_REINDEX,
                         txToReindex(
                                coreName,
                                metadataTracker,
                                metadataTrackerIndexHealthReport,
                                dryRun ? txid -> {} : metadataTracker::addTransactionToReindex,
                                maxTransactionsToSchedule));

            response.add(ACTION_ACL_CHANGE_SET_TO_REINDEX,
                         aclTxToReindex(
                                 coreName,
                                 aclTracker,
                                 aclTrackerIndexHealthReport,
                                 dryRun ? txid -> {} : aclTracker::addAclChangeSetToReindex,
                                 maxTransactionsToSchedule));
            return response;
        }
        catch(Exception exception)
        {
            throw new AlfrescoRuntimeException("", exception);
        }
    }

    /**
     * Detects the transactions that need a FIX (i.e. reindexing) because the following reasons:
     *
     * <ul>
     *     <li>A transaction is in the index but not in repository</li>
     *     <li>A transaction is duplicated in the index</li>
     *     <li>A transaction is missing in the index</li>
     * </ul>
     *
     * Note: the method, as a side effect, could also schedule the detected transactions for reindexing.
     * That is controlled by the scheduler input param (which is directly connected with the FIX tool "dryRun" parameter).
     *
     * @param coreName the target core name.
     * @param tracker the {@link MetadataTracker} instance associated with the target core.
     * @param report the index healt report produced by the tracker.
     * @param scheduler the controller which manages the actual transaction scheduling.
     * @param maxTransactionsToSchedule the maximum number of transactions to schedule for reindexing.
     * @return a report which includes the transactions that need a reindexing.
     * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2233">SEARCH-2233</a>
     * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2248">SEARCH-2248</a>
     */
    NamedList<Object> txToReindex(
            String coreName,
            MetadataTracker tracker,
            final IndexHealthReport report,
            Consumer<Long> scheduler,
            int maxTransactionsToSchedule)
    {
        final AtomicInteger globalLimit = new AtomicInteger(maxTransactionsToSchedule);

        final LongToIntFunction retrieveTransactionRelatedNodesCountFromRepository =
                txid -> notNullOrEmpty(tracker.getFullNodesForDbTransaction(txid)).size();

        final LongToIntFunction retrieveTransactionRelatedNodesCountFromIndex =
                txid -> of(getInformationServers().get(coreName))
                        .map(SolrInformationServer.class::cast)
                        .map(server -> server.getDocListSize(FIELD_INTXID + ":" + txid))
                        .orElse(0);

        NamedList<Object> txToReindex = new SimpleOrderedMap<>();
        txToReindex.add(TX_IN_INDEX_NOT_IN_DB,
                manageTransactionsToBeFixed(
                        report.getTxInIndexButNotInDb(),
                        retrieveTransactionRelatedNodesCountFromIndex,
                        scheduler,
                        globalLimit));

        txToReindex.add(DUPLICATED_TX_IN_INDEX,
                manageTransactionsToBeFixed(
                        report.getDuplicatedTxInIndex(),
                        retrieveTransactionRelatedNodesCountFromIndex,
                        scheduler,
                        globalLimit));

        txToReindex.add(MISSING_TX_IN_INDEX,
                manageTransactionsToBeFixed(
                        report.getMissingTxFromIndex(),
                        retrieveTransactionRelatedNodesCountFromRepository,
                        scheduler,
                        globalLimit));
        return txToReindex;
    }

    /**
     * Detects the ACL transactions that need a FIX (i.e. reindexing) because the following reasons:
     *
     * <ul>
     *     <li>A transaction is in the index but not in repository</li>
     *     <li>A transaction is duplicated in the index</li>
     *     <li>A transaction is missing in the index</li>
     * </ul>
     *
     * This method is almost the same as {@link #txToReindex(String, MetadataTracker, IndexHealthReport, Consumer, int)}.
     * The main difference is the target tracker ({@link AclTracker} in this case, instead of {@link MetadataTracker}).
     *
     * Note: the method, as a side effect, could also schedule the detected transactions for reindexing.
     * That is controlled by the scheduler input param (which is directly connected with the FIX tool "dryRun" parameter).
     *
     * @param coreName the target core name.
     * @param tracker the {@link AclTracker} instance associated with the target core.
     * @param report the index healt report produced by the tracker.
     * @param scheduler the controller which manages the actual transaction scheduling.
     * @return a report which includes the transactions that need a reindexing.
     * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2233">SEARCH-2233</a>
     * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2248">SEARCH-2248</a>
     */
    NamedList<Object> aclTxToReindex(
            String coreName,
            AclTracker tracker,
            final IndexHealthReport report,
            Consumer<Long> scheduler,
            int maxTransactionsToSchedule)
    {
        final AtomicInteger globalLimit = new AtomicInteger(maxTransactionsToSchedule);

        final LongToIntFunction retrieveAclTransactionRelatedNodesCountFromRepository =
                txid -> notNullOrEmpty(tracker.getAclsForDbAclTransaction(txid)).size();

        final LongToIntFunction retrieveAclTransactionRelatedNodesCountFromIndex =
                txid -> of(getInformationServers().get(coreName))
                        .map(SolrInformationServer.class::cast)
                        .map(server -> server.getDocListSize(FIELD_INACLTXID + ":" + txid))
                        .orElse(0);

        NamedList<Object> aclTxToReindex = new SimpleOrderedMap<>();
        aclTxToReindex.add(ACL_TX_IN_INDEX_NOT_IN_DB,
                manageTransactionsToBeFixed(
                        report.getAclTxInIndexButNotInDb(),
                        retrieveAclTransactionRelatedNodesCountFromIndex,
                        scheduler,
                        globalLimit));

        aclTxToReindex.add(DUPLICATED_ACL_TX_IN_INDEX,
                manageTransactionsToBeFixed(
                        report.getDuplicatedAclTxInIndex(),
                        retrieveAclTransactionRelatedNodesCountFromIndex,
                        scheduler,
                        globalLimit));

        aclTxToReindex.add(MISSING_ACL_TX_IN_INDEX,
                manageTransactionsToBeFixed(
                        report.getMissingAclTxFromIndex(),
                        retrieveAclTransactionRelatedNodesCountFromRepository,
                        scheduler,
                        globalLimit));

        return aclTxToReindex;
    }

    NamedList<Object> manageTransactionsToBeFixed(
            IOpenBitSet transactions,
            LongToIntFunction nodesCounter,
            Consumer<Long> scheduler,
            AtomicInteger limit)
    {
        final NamedList<Object> transactionsList = new SimpleOrderedMap<>();

        long txid = -1;
        while ((txid = transactions.nextSetBit(txid + 1)) != -1 && limit.decrementAndGet() >= 0)
        {
            transactionsList.add(String.valueOf(txid), nodesCounter.applyAsInt(txid));
            scheduler.accept(txid);
        }

        return transactionsList;
    }

    /**
     * Get detailed report for a core or for every core including information
     * related with handlers and trackers.
     *
     * Synchronous execution
     *
     * @param params Query Request with following parameters:
     * - core, optional: The name of the SOLR Core
     * - detail, optional, when true adds details to the report
     * - hist, optional, when true adds historic details to the report
     * - values, optional, when true adds values detail to the report
     * - reset, optional, when true stats are reset
     * @return report (Key, Value) list with the results of the report
     */
    private NamedList<Object> actionSUMMARY(SolrParams params)
    {
        NamedList<Object> report = new SimpleOrderedMap<>();

        String requestedCoreName = coreName(params);

        coreNames().stream()
                .filter(coreName -> requestedCoreName == null || coreName.equals(requestedCoreName))
                .forEach(coreName -> coreSummary(params, report, coreName));

        return report;
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

    DocRouter getDocRouter(String cname)
    {
        return ofNullable(trackerRegistry.getTrackerForCore(cname, MetadataTracker.class))
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
    CoreStatePublisher coreStatePublisher(String coreName)
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
    boolean isMasterOrStandalone(String coreName)
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

    Collection<String> coreNames()
    {
        return notNullOrEmpty(trackerRegistry.getCoreNames());
    }

    private void apply(SolrParams params, String parameterName, Consumer<Long> executeSideEffectAction)
    {
        ofNullable(params.get(parameterName))
                .map(Long::valueOf)
                .ifPresent(executeSideEffectAction);
    }

    /**
     * Returns the core name indicated in the request parameters.
     * A first attempt is done in order to check if a standard {@link CoreAdminParams#CORE} parameter is in the request.
     * If not, the alternative "coreName" parameter name is used.
     *
     * @param params the request parameters.
     * @return the core name specified in the request, null if the parameter is not found.
     */
    String coreName(SolrParams params)
    {
        return CORE_PARAMETER_NAMES.stream()
                .map(params::get)
                .filter(Objects::nonNull)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    int getMaxTransactionToSchedule(SolrParams params)
    {
        String requestedCoreName = coreName(params);
        return ofNullable(params.getInt(MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME))
                .orElseGet(() ->
                        ofNullable(coreContainer)
                            .map(container -> container.getCore(requestedCoreName))
                            .map(SolrCore::getResourceLoader)
                            .map(SolrResourceLoader::getCoreProperties)
                            .map(conf -> conf.getProperty(MAX_TRANSACTIONS_TO_SCHEDULE_CONF_PROPERTY_NAME))
                            .map(Integer::parseInt)
                            .orElse(Integer.MAX_VALUE)); // Last fallback if we don't have a request param and a value in configuration
    }
}