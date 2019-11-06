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

import static java.util.Optional.ofNullable;

import static org.alfresco.solr.HandlerOfResources.extractCustomProperties;
import static org.alfresco.solr.HandlerOfResources.getSafeBoolean;
import static org.alfresco.solr.HandlerOfResources.getSafeLong;
import static org.alfresco.solr.HandlerOfResources.openResource;
import static org.alfresco.solr.HandlerOfResources.updatePropertiesFile;
import static org.alfresco.solr.HandlerOfResources.updateSharedProperties;
import static org.alfresco.solr.HandlerReportBuilder.addMasterOrStandaloneCoreSummary;
import static org.alfresco.solr.HandlerReportBuilder.addSlaveCoreSummary;
import static org.alfresco.solr.HandlerReportBuilder.buildAclReport;
import static org.alfresco.solr.HandlerReportBuilder.buildAclTxReport;
import static org.alfresco.solr.HandlerReportBuilder.buildNodeReport;
import static org.alfresco.solr.HandlerReportBuilder.buildTrackerReport;
import static org.alfresco.solr.HandlerReportBuilder.buildTxReport;

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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.client.SOLRAPIClientFactory;
import org.alfresco.solr.config.ConfigUtil;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.DBIDRangeRouter;
import org.alfresco.solr.tracker.DocRouter;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.CoreStatePublisher;
import org.alfresco.solr.tracker.SlaveCoreStatePublisher;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.alfresco.util.shard.ExplicitShardingPolicy;
import org.apache.commons.codec.EncoderException;
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

/**
 * Alfresco Solr administration endpoints provider.
 * A customisation of the existing Solr {@link CoreAdminHandler} which offers additional administration endpoints.
 */
public class AlfrescoCoreAdminHandler extends CoreAdminHandler
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoCoreAdminHandler.class);
    
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
    private ConcurrentHashMap<String, InformationServer> informationServers = null;
    
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
    public void shutdown() 
    {
        super.shutdown();
        try 
        {
            LOGGER.info("Shutting down Alfresco core container services");
            AlfrescoSolrDataModel.getInstance().close();
            SOLRAPIClientFactory.close();
            MultiThreadedHttpConnectionManager.shutdownAll();

            //Remove any core trackers still hanging around
            trackerRegistry.getCoreNames().forEach(coreName -> trackerRegistry.removeTrackersForCore(coreName));

            //Remove any information servers
            informationServers.clear();

            //Shutdown the scheduler and model tracker.
            if (!scheduler.isShutdown())
            {
                scheduler.pauseAll();
                if (trackerRegistry.getModelTracker() != null) trackerRegistry.getModelTracker().shutdown();
                trackerRegistry.setModelTracker(null);
                scheduler.shutdown();
            }
        } 
        catch(Exception e) 
        {
            LOGGER.error("Problem shutting down", e);
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
        String cname = params.get(CoreAdminParams.CORE);
        String action = params.get(CoreAdminParams.ACTION);
        action = action==null?"":action.toUpperCase();
        try
        {
            switch (action)
            {
                case "NEWCORE":
                    newCore(req, rsp);
                    break;
                case "UPDATECORE":
                    updateCore(req);
                    break;
                case "UPDATESHARED":
                    updateShared(req);
                    break;
                case "REMOVECORE":
                    removeCore(req);
                    break;
                case "NEWDEFAULTINDEX":
                    newDefaultCore(req, rsp);
                    break;
                case "CHECK":
                    actionCHECK(cname);
                    break;
                case "NODEREPORT":
                    actionNODEREPORTS(rsp, params, cname);
                    break;
                case "ACLREPORT":
                    actionACLREPORT(rsp, params, cname);
                    break;
                case "TXREPORT":
                    actionTXREPORT(rsp, params, cname);
                    break;
                case "ACLTXREPORT":
                    actionACLTXREPORT(rsp, params, cname);
                    break;
                case "RANGECHECK":
                    rangeCheck(rsp, cname);
                    break;
                case "EXPAND":
                    expand(rsp, params, cname);
                    break;
                case "REPORT":
                    actionREPORT(rsp, params, cname);
                    break;
                case "PURGE":
                    if (cname != null)
                    {
                        actionPURGE(params, cname);
                    }
                    else
                    {
                        for (String coreName : getTrackerRegistry().getCoreNames())
                        {
                            actionPURGE(params, coreName);
                        }
                    }
                    break;
                case "REINDEX":
                    if (cname != null)
                    {
                        actionREINDEX(params, cname);
                    }
                    else
                    {
                        for (String coreName : getTrackerRegistry().getCoreNames())
                        {
                            actionREINDEX(params, coreName);
                        }
                    }
                    break;
                case "RETRY":
                    if (cname != null)
                    {
                        actionRETRY(rsp, cname);
                    }
                    else
                    {
                        for (String coreName : getTrackerRegistry().getCoreNames())
                        {
                            actionRETRY(rsp, coreName);
                        }
                    }
                    break;
                case "INDEX":
                    if (cname != null)
                    {
                        actionINDEX(params, cname);
                    }
                    else
                    {
                        for (String coreName : getTrackerRegistry().getCoreNames())
                        {
                            actionINDEX(params, coreName);
                        }
                    }
                    break;
                case "FIX":
                    if (cname != null)
                    {
                        actionFIX(cname);
                    }
                    else
                    {
                        for (String coreName : getTrackerRegistry().getCoreNames())
                        {
                            actionFIX(coreName);
                        }
                    }
                    break;
                case "SUMMARY":
                    if (cname != null)
                    {
                        NamedList<Object> report = new SimpleOrderedMap<>();
                        actionSUMMARY(params, report, cname);
                        rsp.add("Summary", report);
                    }
                    else
                    {
                        NamedList<Object> report = new SimpleOrderedMap<>();
                        for (String coreName : getTrackerRegistry().getCoreNames())
                        {
                            actionSUMMARY(params, report, coreName);
                        }
                        rsp.add("Summary", report);
                    }
                    break;
                case "LOG4J":
                    String resource = "log4j-solr.properties";
                    if (params.get("resource") != null)
                    {
                        resource = params.get("resource");
                    }
                    initResourceBasedLogging(resource);
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

    private boolean newCore(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        SolrParams params = req.getParams();
        req.getContext();

        // If numCore > 1 we are creating a collection of cores for a sole node in a cluster
        int numShards = params.getInt("numShards", 1);

        String store = params.get("storeRef");
        if (store == null || store.trim().length() == 0)
        {
            return false;
        }

        StoreRef storeRef = new StoreRef(store);

        String templateName = ofNullable(params.get("template")).orElse("vanilla");

        int replicationFactor =  params.getInt("replicationFactor", 1);
        int nodeInstance =  params.getInt("nodeInstance", -1);
        int numNodes =  params.getInt("numNodes", 1);

        String coreName = params.get("coreName");
        String shardIds = params.get("shardIds");

        Properties properties = extractCustomProperties(params);
        return newCore(coreName, numShards, storeRef, templateName, replicationFactor, nodeInstance, numNodes, shardIds, properties, rsp);
    }

    private boolean newDefaultCore(SolrQueryRequest req, SolrQueryResponse response)
    {
        SolrParams params = req.getParams();
        String coreName = params.get("coreName") != null?params.get("coreName"):"alfresco";
        String templateName = params.get("template") != null?params.get("template"): DEFAULT_TEMPLATE;

        Properties extraProperties = extractCustomProperties(params);

        return newDefaultCore(
                coreName,
                ofNullable(params.get("storeRef"))
                        .map(StoreRef::new)
                        .orElse(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE),
                templateName,
                extraProperties,
                response);
    }

    private boolean newDefaultCore(String coreName, StoreRef storeRef, String templateName, Properties extraProperties, SolrQueryResponse rsp)
    {
        return newCore(coreName, 1, storeRef, templateName, 1, 1, 1, null, extraProperties, rsp);
    }

    protected boolean newCore(String coreName, int numShards, StoreRef storeRef, String templateName, int replicationFactor, int nodeInstance, int numNodes, String shardIds, Properties extraProperties, SolrQueryResponse rsp)
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
                    return false;
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
                        return false;
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
                       
                return true;
            }
            else
            {
                if (coreName == null)
                {
                    coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
                }
                File newCore = new File(solrHome, coreName);
                createAndRegisterNewCore(rsp, extraProperties, storeRef, template, coreName, newCore, 0, 0, templateName);

                return true;
            }
          
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private List<Integer> extractShards(String shardIds, int numShards)
    {
        List<Integer> shards = new ArrayList<>();
        for(String shardId : shardIds.split(","))
        {
            try
            {
                int shard = Integer.parseInt(shardId);
                if(shard < numShards)
                {
                    shards.add(shard);
                }
            }
            catch(NumberFormatException nfe)
            {
                // ignore 
            }
        }
        return shards;
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

    private boolean hasAlfrescoCore(Collection<SolrCore> cores)
    {
        if (cores == null || cores.isEmpty()) return false;
        for (SolrCore core:cores)
        {
            if (trackerRegistry.hasTrackersForCore(core.getName())) return true;
        }
        return false;
    }

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
            String coreName = null;
            SolrParams params = req.getParams();

            if (params.get("coreName") != null)
            {
                coreName = params.get("coreName");
            }
            
            if ((coreName == null) || (coreName.length() == 0))
            {
                return;
            }

        try (SolrCore core = coreContainer.getCore(coreName))
        {

            if (core == null)
            {
                return;
            }

            String configLocaltion = core.getResourceLoader().getConfigDir();
            File config = new File(configLocaltion, "solrcore.properties");
            updatePropertiesFile(params, config, null);

            coreContainer.reload(coreName);
        }
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
        String coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
        if (params.get("coreName") != null)
        {
            coreName = params.get("coreName");
        }

        // remove core
        coreContainer.unload(coreName, true, true, true);
    }

    private void actionFIX(String coreName) throws AuthenticationException, IOException, JSONException, EncoderException
    {
        if (isMasterOrStandalone(coreName))
        {
            // Gets Metadata health and fixes any problems
            MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            IndexHealthReport indexHealthReport = metadataTracker.checkIndex(null, null, null, null);
            IOpenBitSet toReindex = indexHealthReport.getTxInIndexButNotInDb();
            toReindex.or(indexHealthReport.getDuplicatedTxInIndex());
            toReindex.or(indexHealthReport.getMissingTxFromIndex());
            long current = -1;
            // Goes through problems in the index
            while ((current = toReindex.nextSetBit(current + 1)) != -1)
            {
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
            while ((current = toReindex.nextSetBit(current + 1)) != -1)
            {
                aclTracker.addAclChangeSetToReindex(current);
            }
        }
    }

    private void actionCHECK(String cname)
    {
        trackerRegistry.getCoreNames()
                .stream()
                .filter(coreName -> cname == null || coreName.equals(cname))
                .map(trackerRegistry::getTrackersForCore)
                .flatMap(Collection::stream)
                .map(Tracker::getTrackerState)
                .forEach(state -> state.setCheck(true));
    }

    private void actionACLREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws JSONException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add("report", report);

        Long aclid =
                ofNullable(params.get(ARG_ACLID))
                        .map(Long::valueOf)
                        .orElseThrow(() -> new AlfrescoRuntimeException("No " + ARG_ACLID + " parameter set."));

        if (cname != null)
        {
            ofNullable(trackerRegistry.getTrackerForCore(cname, AclTracker.class))
                    .ifPresent(tracker -> report.add(cname, buildAclReport(tracker, aclid)));
        }
        else
        {
            trackerRegistry.getCoreNames()
                    .forEach(coreName ->
                            ofNullable(trackerRegistry.getTrackerForCore(coreName, AclTracker.class))
                                .ifPresent(tracker -> report.add(coreName, buildAclReport(tracker, aclid))));
        }

        if (report.size() == 0)
        {
            report.add("WARNING", "This response comes from a slave core. Please consider to ask the same request to its corresponding master core, in order to get more information about the requested Node");
        }
    }

    private void actionTXREPORT(SolrQueryResponse rsp, SolrParams params, String cname)
            throws AuthenticationException, IOException, JSONException, EncoderException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add("report", report);

        MetadataTracker tracker = trackerRegistry.getTrackerForCore(cname, MetadataTracker.class);
        if (tracker != null)
        {
            Long txid =
                    ofNullable(params.get(ARG_TXID))
                            .map(Long::valueOf)
                            .orElseThrow(() -> new AlfrescoRuntimeException("No " + ARG_TXID + " parameter set."));

            if (cname == null)
            {
                throw new AlfrescoRuntimeException("No cname parameter set");
            }

            report.add(cname, buildTxReport(getTrackerRegistry(), informationServers.get(cname), cname, tracker, txid));
        }
        else
        {
            report.add("WARNING", "This response comes from a slave core. Please consider to ask the same request to its corresponding master core, in order to get more information about the requested Node");
        }
    }

    private void actionACLTXREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws JSONException
    {
        if (params.get(ARG_ACLTXID) == null)
        {
            throw new AlfrescoRuntimeException("No acltxid parameter set");
        }

        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add("report", report);

        Long acltxid =
                ofNullable(params.get(ARG_ACLTXID))
                        .map(Long::valueOf)
                        .orElseThrow(() -> new AlfrescoRuntimeException("No " + ARG_ACLTXID + " parameter set."));

        if (cname != null)
        {
            ofNullable(trackerRegistry.getTrackerForCore(cname, AclTracker.class))
                    .ifPresent(tracker -> report.add(cname, buildAclTxReport(trackerRegistry, informationServers.get(cname), cname, tracker, acltxid)));
        }
        else
        {
            trackerRegistry.getCoreNames()
                    .forEach(coreName ->
                            ofNullable(trackerRegistry.getTrackerForCore(coreName, AclTracker.class))
                                    .ifPresent(tracker -> report.add(cname, buildAclTxReport(trackerRegistry, informationServers.get(cname), cname, tracker, acltxid))));
        }

        if (report.size() == 0)
        {
            report.add("WARNING", "This response comes from a slave core. Please consider to ask the same request to its corresponding master core, in order to get more information about the requested Node");
        }
    }

    private void actionREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws JSONException
    {
        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add("report", report);

        Long fromTime = getSafeLong(params, "fromTime");
        Long toTime = getSafeLong(params, "toTime");
        Long fromTx = getSafeLong(params, "fromTx");
        Long toTx = getSafeLong(params, "toTx");
        Long fromAclTx = getSafeLong(params, "fromAclTx");
        Long toAclTx = getSafeLong(params, "toAclTx");
        
        if (cname != null)
        {
            if (trackerRegistry.hasTrackersForCore(cname) && isMasterOrStandalone(cname))
            {
                report.add(cname, buildTrackerReport(trackerRegistry, informationServers.get(cname),cname, fromTx, toTx, fromAclTx, toAclTx, fromTime, toTime));
            }
        }
        else
        {
            trackerRegistry.getCoreNames().stream()
                    .filter(trackerRegistry::hasTrackersForCore)
                    .filter(this::isMasterOrStandalone)
                    .forEach(coreName -> report.add(coreName, buildTrackerReport(trackerRegistry, informationServers.get(coreName), coreName, fromTx, toTx, fromAclTx, toAclTx, fromTime, toTime)));
        }
    }

    private DocRouter getDocRouter(String cname)
    {
        Collection<Tracker> trackers = trackerRegistry.getTrackersForCore(cname);
        MetadataTracker metadataTracker = null;
        for(Tracker tracker : trackers)
        {
            if(tracker instanceof MetadataTracker)
            {
                metadataTracker = (MetadataTracker)tracker;
            }
        }

        return metadataTracker.getDocRouter();
    }


    private void rangeCheck(SolrQueryResponse rsp,String cname) throws IOException
    {
        InformationServer informationServer = informationServers.get(cname);

        DocRouter docRouter = getDocRouter(cname);

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

    private synchronized void expand(SolrQueryResponse rsp, SolrParams params, String cname) throws IOException
    {
        InformationServer informationServer = informationServers.get(cname);
        DocRouter docRouter = getDocRouter(cname);

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

    private void actionNODEREPORTS(SolrQueryResponse rsp, SolrParams params, String cname) throws JSONException
    {
        Long dbid =
                ofNullable(params.get(ARG_NODEID))
                    .map(Long::valueOf)
                    .orElseThrow(() -> new AlfrescoRuntimeException("No dbid parameter set."));

        NamedList<Object> report = new SimpleOrderedMap<>();
        rsp.add("report", report);

        if (cname != null)
        {
            report.add(cname, buildNodeReport(nodeStatePublisher(cname), dbid));
        }
        else
        {
            trackerRegistry.getCoreNames().forEach(coreName -> report.add(coreName, buildNodeReport(nodeStatePublisher(coreName), dbid)));
        }
    }

    private void actionSUMMARY(SolrParams params, NamedList<Object> report, String coreName) throws IOException
    {
        boolean detail = getSafeBoolean(params, "detail");
        boolean hist = getSafeBoolean(params, "hist");
        boolean values = getSafeBoolean(params, "values");
        boolean reset = getSafeBoolean(params, "reset");
        
        InformationServer srv = informationServers.get(coreName);
        if (srv != null)
        {
            if (isMasterOrStandalone(coreName))
            {
                addMasterOrStandaloneCoreSummary(trackerRegistry, coreName, detail, hist, values, srv, report);

                if (reset)
                {
                    srv.getTrackerStats().reset();
                }
            }
            else
            {
                addSlaveCoreSummary(trackerRegistry, coreName, detail, hist, values, srv, report);
            }
        }
        else
        {
            report.add(coreName, "Core unknown");
        }
    }

    private void actionINDEX(SolrParams params, String coreName)
    {
        if (isMasterOrStandalone(coreName))
        {
            if (params.get(ARG_TXID) != null)
            {
                Long txid = Long.valueOf(params.get(ARG_TXID));
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
                tracker.addTransactionToIndex(txid);
            }

            if (params.get(ARG_ACLTXID) != null)
            {
                Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                tracker.addAclChangeSetToIndex(acltxid);
            }

            if (params.get(ARG_NODEID) != null)
            {
                Long nodeid = Long.valueOf(params.get(ARG_NODEID));
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
                tracker.addNodeToIndex(nodeid);
            }

            if (params.get(ARG_ACLID) != null)
            {
                Long aclid = Long.valueOf(params.get(ARG_ACLID));
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                tracker.addAclToIndex(aclid);
            }
        }
    }

    private void actionRETRY(SolrQueryResponse rsp, String coreName) throws IOException
    {
        if (isMasterOrStandalone(coreName))
        {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            InformationServer srv = informationServers.get(coreName);

            Set<Long> errorDocIds = srv.getErrorDocIds();
            for (Long nodeid : errorDocIds)
            {
                tracker.addNodeToReindex(nodeid);
            }
            rsp.add(coreName, errorDocIds);
        }
    }

    private void actionREINDEX(SolrParams params, String coreName)
    {
        if (isMasterOrStandalone(coreName))
        {
            if (params.get(ARG_TXID) != null)
            {
                Long txid = Long.valueOf(params.get(ARG_TXID));
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
                tracker.addTransactionToReindex(txid);
            }

            if (params.get(ARG_ACLTXID) != null)
            {
                Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                tracker.addAclChangeSetToReindex(acltxid);
            }

            if (params.get(ARG_NODEID) != null)
            {
                Long nodeid = Long.valueOf(params.get(ARG_NODEID));
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
                tracker.addNodeToReindex(nodeid);
            }

            if (params.get(ARG_ACLID) != null)
            {
                Long aclid = Long.valueOf(params.get(ARG_ACLID));
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                tracker.addAclToReindex(aclid);
            }

            if (params.get(ARG_QUERY) != null)
            {
                String query = params.get(ARG_QUERY);
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
                tracker.addQueryToReindex(query);
            }
        }
    }

    private void actionPURGE(SolrParams params, String coreName)
    {
        if (isMasterOrStandalone(coreName))
        {
            if (params.get(ARG_TXID) != null)
            {
                Long txid = Long.valueOf(params.get(ARG_TXID));
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
                tracker.addTransactionToPurge(txid);
            }

            if (params.get(ARG_ACLTXID) != null)
            {
                Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                tracker.addAclChangeSetToPurge(acltxid);
            }

            if (params.get(ARG_NODEID) != null)
            {
                Long nodeid = Long.valueOf(params.get(ARG_NODEID));
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
                tracker.addNodeToPurge(nodeid);
            }

            if (params.get(ARG_ACLID) != null)
            {
                Long aclid = Long.valueOf(params.get(ARG_ACLID));
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                tracker.addAclToPurge(aclid);
            }
        }
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

    private boolean isMasterOrStandalone(String coreName)
    {
        return trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class) != null;
    }
}
