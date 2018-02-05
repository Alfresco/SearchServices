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

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.client.SOLRAPIClientFactory;
import org.alfresco.solr.config.ConfigUtil;
import org.alfresco.solr.tracker.*;
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

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.alfresco.solr.HandlerOfResources.*;
import static org.alfresco.solr.HandlerReportBuilder.*;

public class AlfrescoCoreAdminHandler extends CoreAdminHandler
{
    protected static final Logger log = LoggerFactory.getLogger(AlfrescoCoreAdminHandler.class);
    
    private static final String ARG_ACLTXID = "acltxid";
    private static final String ARG_TXID = "txid";
    private static final String ARG_ACLID = "aclid";
    private static final String ARG_NODEID = "nodeid";
    private static final String ARG_QUERY = "query";
    public static final String DATA_DIR_ROOT = "data.dir.root";
    public static final String ALFRESCO_DEFAULTS = "create.alfresco.defaults";
    public static final String DEFAULT_TEMPLATE = "rerank";

    private SolrTrackerScheduler scheduler = null;
    private TrackerRegistry trackerRegistry = null;
    private ConcurrentHashMap<String, InformationServer> informationServers = null;
    
    public AlfrescoCoreAdminHandler()
    {
        super();
    }

    /**
     * @param coreContainer
     */
    public AlfrescoCoreAdminHandler(CoreContainer coreContainer)
    {
        super(coreContainer);
        startup(coreContainer);
    }

    /**
     * Startup services that exist outside of the core.
     */
    public void startup(CoreContainer coreContainer)
    {
        log.info("Starting Alfresco core container services");

        trackerRegistry = new TrackerRegistry();
        informationServers = new ConcurrentHashMap<String, InformationServer>();
        this.scheduler = new SolrTrackerScheduler(this);

        String createDefaultCores = ConfigUtil.locateProperty(ALFRESCO_DEFAULTS, "");
        if (createDefaultCores != null && !createDefaultCores.isEmpty())
        {
            Runnable runnable = () ->
            {
                try
                {
                    TimeUnit.SECONDS.sleep(10); //Wait a little for the container to start up
                }
                catch (InterruptedException e)
                {
                    //Don't care
                }
                setupNewDefaultCores(createDefaultCores);
            };

            Thread thread = new Thread(runnable);
            thread.start();
        }
    }

    /**
     * Creates new default cores based on the "createDefaultCores" String passed in
     * @param createDefaultCores comma delimited list of corenames.
     */
    protected void setupNewDefaultCores(String createDefaultCores)
    {
        final String VERSION_STORE_PROTOCOL = "workspace";
        final String VERSION_STORE_ID = "version2Store";

        SolrQueryResponse response = new SolrQueryResponse();
        try
        {
            String[] coreNames = createDefaultCores.toLowerCase().split(",");
            for (String coreName:coreNames)
            {
                log.info("Attempting to create default alfresco core: "+coreName);
                switch (coreName)
                {
                    case "archive":
                        newDefaultCore("archive",  StoreRef.STORE_REF_ARCHIVE_SPACESSTORE,   DEFAULT_TEMPLATE, null, response);
                        break;
                    case "alfresco":
                        newDefaultCore("alfresco", StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, DEFAULT_TEMPLATE, null, response);
                        break;
                    case "version":
                        newDefaultCore("version", new StoreRef(VERSION_STORE_PROTOCOL, VERSION_STORE_ID), DEFAULT_TEMPLATE, null, response);
                        break;
                    default:
                        log.error("Invalid '"+ALFRESCO_DEFAULTS+"' permitted values are alfresco,archive,version");
                }
            }
        }
        catch (Exception e)
        {
            log.error("Failed to create default alfresco cores (workspace/archive stores)", e);
        }
    }

    /**
     * Shutsdown services that exist outside of the core.
     */
    public void shutdown() 
    {
        super.shutdown();
        try 
        {
            log.info("Shutting down Alfresco core container services");
            AlfrescoSolrDataModel.getInstance().close();
            SOLRAPIClientFactory.close();
            MultiThreadedHttpConnectionManager.shutdownAll();

            //Remove any core trackers still hanging around
            trackerRegistry.getCoreNames().forEach(coreName ->
            {
                trackerRegistry.removeTrackersForCore(coreName);
            });

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
            log.error("Problem shutting down", e);
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
            return;
        }
        catch (Exception e)
        {
            log.info("Failed to load " + resource, e);
        }
    }

    protected void handleCustomAction(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        log.info("######## Handle Custom Action ###########");
        SolrParams params = req.getParams();
        String cname = params.get(CoreAdminParams.CORE);
        String action = params.get(CoreAdminParams.ACTION);
        action = action==null?"":action.toUpperCase();
        try
        {
            switch (action) {
                case "NEWCORE":
                    newCore(req, rsp);
                    break;
                case "UPDATECORE":
                    updateCore(req, rsp);
                    break;
                case "UPDATESHARED":
                    updateShared(req, rsp);
                    break;
                case "REMOVECORE":
                    removeCore(req, rsp);
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
                    if (cname != null) {
                        actionPURGE(params, cname);
                    } else {
                        for (String coreName : getTrackerRegistry().getCoreNames()) {
                            actionPURGE(params, coreName);
                        }
                    }
                    break;
                case "REINDEX":
                    if (cname != null) {
                        actionREINDEX(params, cname);
                    } else {
                        for (String coreName : getTrackerRegistry().getCoreNames()) {
                            actionREINDEX(params, coreName);
                        }
                    }
                    break;
                case "RETRY":
                    if (cname != null) {
                        actionRETRY(rsp, cname);
                    } else {
                        for (String coreName : getTrackerRegistry().getCoreNames()) {
                            actionRETRY(rsp, coreName);
                        }
                    }
                    break;
                case "INDEX":
                    if (cname != null) {
                        actionINDEX(params, cname);
                    } else {
                        for (String coreName : getTrackerRegistry().getCoreNames()) {
                            actionINDEX(params, coreName);
                        }
                    }
                    break;
                case "FIX":
                    if (cname != null) {
                        actionFIX(cname);
                    } else {
                        for (String coreName : getTrackerRegistry().getCoreNames()) {
                            actionFIX(coreName);
                        }
                    }
                    break;
                case "SUMMARY":
                    if (cname != null) {
                        NamedList<Object> report = new SimpleOrderedMap<Object>();
                        actionSUMMARY(params, report, cname);
                        rsp.add("Summary", report);
                    } else {
                        NamedList<Object> report = new SimpleOrderedMap<Object>();
                        for (String coreName : getTrackerRegistry().getCoreNames()) {
                            actionSUMMARY(params, report, coreName);
                        }
                        rsp.add("Summary", report);
                    }
                    break;
                case "LOG4J":
                    String resource = "log4j-solr.properties";
                    if (params.get("resource") != null) {
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

    private boolean newCore(SolrQueryRequest req, SolrQueryResponse rsp) {
        SolrParams params = req.getParams();
        req.getContext();

        // If numCore > 1 we are createing a collection of cores for a sole node in a cluster
        int numShards = params.getInt("numShards", 1);

        String store = "";
        if (params.get("storeRef") != null) {
            store = params.get("storeRef");
        }
        if ((store == null) || (store.length() == 0)) {
            return false;
        }
        StoreRef storeRef = new StoreRef(store);

        String templateName = "vanilla";
        if (params.get("template") != null) {
            templateName = params.get("template");
        }

        int replicationFactor =  params.getInt("replicationFactor", 1);
        int nodeInstance =  params.getInt("nodeInstance", -1);
        int numNodes =  params.getInt("numNodes", 1);

        String coreName = params.get("coreName");
        String shardIds = params.get("shardIds");

        Properties properties = extractCustomProperties(params);
        return newCore(coreName, numShards, storeRef, templateName, replicationFactor, nodeInstance, numNodes, shardIds, properties, rsp);
    }

    private boolean newDefaultCore(SolrQueryRequest req, SolrQueryResponse rsp) {

        SolrParams params = req.getParams();
        String coreName = params.get("coreName") != null?params.get("coreName"):"alfresco";
        StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
        String templateName = params.get("template") != null?params.get("template"): DEFAULT_TEMPLATE;
        Properties extraProperties = extractCustomProperties(params);

        if (params.get("storeRef") != null) {
            String store = params.get("storeRef");
            storeRef = new StoreRef(store);
        }

        return newDefaultCore(coreName, storeRef, templateName, extraProperties, rsp);
    }

    protected boolean newDefaultCore(String coreName, StoreRef storeRef, String templateName, Properties extraProperties, SolrQueryResponse rsp)
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

            if(numShards > 1 )
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
                    coreName = coreBase+shard;
                    File newCore = new File(baseDirectory, coreName);
                    String solrCoreName = coreName;
                    if (coreName == null)
                    {
                        if(storeRef.equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE))
                        {
                            solrCoreName = "alfresco-"+shard;
                        }
                        else if(storeRef.equals(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE))
                        {
                            solrCoreName = "archive-"+shard;
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

    /**
     * @param shardIds
     * @return
     */
    private List<Integer> extractShards(String shardIds, int numShards)
    {
        ArrayList<Integer> shards = new ArrayList<Integer>();
        for(String shardId : shardIds.split(","))
        {
            try
            {
                Integer shard = Integer.valueOf(shardId);
                if(shard.intValue() < numShards)
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

    /**
     * @param rsp
     * @param storeRef
     * @param template
     * @param coreName
     * @param newCore
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void createAndRegisterNewCore(SolrQueryResponse rsp, Properties extraProperties, StoreRef storeRef, File template, String coreName, File newCore, int shardCount, int shardInstance, String templateName) throws IOException,
            FileNotFoundException
    {
        if (coreContainer.getCoreNames().contains(coreName))
        {
            //Core alfresco exists
            log.warn(coreName + " already exists, not creating again.");
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
        properties.load(new FileInputStream(config));

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

        properties.store(new FileOutputStream(config), null);
        
        SolrCore core = coreContainer.create(coreName, newCore.toPath(), new HashMap<String, String>(), false);
        rsp.add("core", core.getName());
    }

    /**
     * Tests to see if one of the cores is an Alfresco special core!
     * @param cores
     * @return
     */
    public boolean hasAlfrescoCore(Collection<SolrCore> cores)
    {
        if (cores == null || cores.isEmpty()) return false;
        for (SolrCore core:cores)
        {
            if (trackerRegistry.hasTrackersForCore(core.getName())) return true;
        }
        return false;
    }

    private boolean updateShared(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        SolrParams params = req.getParams();

        try {

            File config = new File(AlfrescoSolrDataModel.getResourceDirectory(), AlfrescoSolrDataModel.SHARED_PROPERTIES);
            updateSharedProperties(params, config, hasAlfrescoCore(coreContainer.getCores()));

            coreContainer.getCores().forEach(aCore -> coreContainer.reload(aCore.getName()));

            return true;
        } catch (IOException e)
        {
            log.error("Failed to update Shared properties ", e);
        }
        return false;
    }

    private boolean updateCore(SolrQueryRequest req, SolrQueryResponse rsp)
    {
            String coreName = null;
            SolrParams params = req.getParams();

            if (params.get("coreName") != null)
            {
                coreName = params.get("coreName");
            }
            
            if ((coreName == null) || (coreName.length() == 0)) { return false; }

            SolrCore core = null;
            try {
                core = coreContainer.getCore(coreName);

                if(core == null)
                {
                    return false;
                }

                String  configLocaltion = core.getResourceLoader().getConfigDir();
                File config = new File(configLocaltion, "solrcore.properties");
                updatePropertiesFile(params, config, null);

                coreContainer.reload(coreName);
                
                return true;
            }
            finally
            {
                //Decrement core open count
                if(core != null)
                {
                    core.close();
                }
            }
    }

    private boolean removeCore(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        String store = "";
        SolrParams params = req.getParams();
        if (params.get("storeRef") != null)
        {
            store = params.get("storeRef");
        }

        if ((store == null) || (store.length() == 0)) { return false; }

        StoreRef storeRef = new StoreRef(store);
        String coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
        if (params.get("coreName") != null)
        {
            coreName = params.get("coreName");
        }

        // remove core
        coreContainer.unload(coreName, true, true, true);

        return true;
    }



    private void actionFIX(String coreName) throws AuthenticationException, IOException, JSONException, EncoderException
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

    private void actionCHECK(String cname)
    {
        if (cname != null)
        {
            for (Tracker tracker : trackerRegistry.getTrackersForCore(cname))
            {
                tracker.getTrackerState().setCheck(true);
            }
        }
        else
        {
            for (String core : trackerRegistry.getCoreNames())
            {
                for (Tracker tracker : trackerRegistry.getTrackersForCore(core))
                {
                    tracker.getTrackerState().setCheck(true);
                }
            }
        }
    }

    private void actionACLREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws IOException,
                JSONException
    {
        if (params.get(ARG_ACLID) == null)
        {
            throw new AlfrescoRuntimeException("No aclid parameter set");
        }
        
        if (cname != null)
        {
            Long aclid = Long.valueOf(params.get(ARG_ACLID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            AclTracker tracker = trackerRegistry.getTrackerForCore(cname, AclTracker.class);
            report.add(cname, buildAclReport(tracker, aclid));
            rsp.add("report", report);
        }
        else
        {
            Long aclid = Long.valueOf(params.get(ARG_ACLID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                report.add(coreName, buildAclReport(tracker, aclid));
            }
            rsp.add("report", report);
        }
    }

    private void actionTXREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws AuthenticationException,
                IOException, JSONException, EncoderException
    {
        if (params.get(ARG_TXID) == null)
        {
            throw new AlfrescoRuntimeException("No txid parameter set");
        }

        if (cname != null)
        {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(cname, MetadataTracker.class);
            Long txid = Long.valueOf(params.get(ARG_TXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            report.add(cname, buildTxReport(getTrackerRegistry(), informationServers.get(cname), cname, tracker, txid));
            rsp.add("report", report);
        }
        else
        {
            Long txid = Long.valueOf(params.get(ARG_TXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(cname,
                            MetadataTracker.class);
                report.add(coreName, buildTxReport(getTrackerRegistry(), informationServers.get(coreName), coreName, tracker, txid));
            }
            rsp.add("report", report);
        }
    }

    private void actionACLTXREPORT(SolrQueryResponse rsp, SolrParams params, String cname)
                throws AuthenticationException, IOException, JSONException, EncoderException
    {
        if (params.get(ARG_ACLTXID) == null)
        {
            throw new AlfrescoRuntimeException("No acltxid parameter set");
        }
        
        if (cname != null)
        {
            AclTracker tracker = trackerRegistry.getTrackerForCore(cname, AclTracker.class);
            Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            report.add(cname, buildAclTxReport(getTrackerRegistry(), informationServers.get(cname), cname, tracker, acltxid));
            rsp.add("report", report);
        }
        else
        {
            Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                report.add(coreName, buildAclTxReport(getTrackerRegistry(), informationServers.get(coreName), coreName, tracker, acltxid));
            }
            rsp.add("report", report);
        }
    }

    private void actionREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws IOException,
                JSONException, AuthenticationException, EncoderException
    {
        Long fromTime = getSafeLong(params, "fromTime");
        Long toTime = getSafeLong(params, "toTime");
        Long fromTx = getSafeLong(params, "fromTx");
        Long toTx = getSafeLong(params, "toTx");
        Long fromAclTx = getSafeLong(params, "fromAclTx");
        Long toAclTx = getSafeLong(params, "toAclTx");
        
        if (cname != null)
        {
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            if (trackerRegistry.hasTrackersForCore(cname))
            {
                report.add(cname, buildTrackerReport(getTrackerRegistry(), informationServers.get(cname),cname, fromTx, toTx, fromAclTx, toAclTx, fromTime, toTime));
                rsp.add("report", report);
            }
            else 
            {
                report.add(cname, "Core unknown");
            }
        }
        else
        {
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                if (trackerRegistry.hasTrackersForCore(coreName))
                {
                    report.add(coreName, buildTrackerReport(getTrackerRegistry(), informationServers.get(coreName), coreName, fromTx, toTx, fromAclTx, toAclTx, fromTime, toTime));
                }
                else 
                {
                    report.add(coreName, "Core unknown");
                }
            }
            rsp.add("report", report);
        }
    }

    private void rangeCheck(SolrQueryResponse rsp,String cname) throws IOException
    {
        InformationServer informationServer = informationServers.get(cname);

        Collection<Tracker> trackers = trackerRegistry.getTrackersForCore(cname);
        MetadataTracker metadataTracker = null;
        for(Tracker tracker : trackers)
        {
            if(tracker instanceof MetadataTracker)
            {
                metadataTracker = (MetadataTracker)tracker;
            }
        }

        DocRouter docRouter = metadataTracker.getDocRouter();

        if(docRouter instanceof DBIDRangeRouter) {

            DBIDRangeRouter dbidRangeRouter = (DBIDRangeRouter) docRouter;
            long startRange = dbidRangeRouter.getStartRange();
            long endRange = dbidRangeRouter.getEndRange();

            long maxNodeId = informationServer.maxNodeId();
            long nodeCount = informationServer.nodeCount();

            long bestGuess = -1;  // -1 means expansion cannot be done. Either because expansion
                                 // has already happened or we're above safe range

            long range = endRange - startRange; // We want this many nodes on the server
            //System.out.println("##### Range:" + range);

            long midpoint = startRange + ((long) (range * .5));
            //System.out.println("##### Midpoint:"+midpoint);

            long safe = startRange + ((long) (range * .75));
            //System.out.println("##### Safe:"+safe);


            long offset = maxNodeId-startRange;
            //System.out.println("#####offset:"+offset);

            //System.out.println("#####max nodeid:"+maxNodeId);

            double density = 0;

            if(offset > 0) {
                density = ((double)nodeCount) / ((double)offset); // This is how dense we are so far.
            }

            //System.out.println("#####density:"+density);

            if (!dbidRangeRouter.getExpanded())
            {
                if(maxNodeId <= safe)
                {
                    if (maxNodeId >= midpoint)
                    {
                        if(density >= 1)
                        {
                            //This is fully dense shard. I'm not sure if it's possible to have more nodes on the shards
                            //then the offset, but if it does happen don't expand.
                            bestGuess=0;
                        }
                        else
                        {
                            double multiplier = range / (density*range); // Extrapolate across the entire range to find the multiplier
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
            rsp.add("maxDbid", maxNodeId);
            rsp.add("density", Math.abs(density));
            rsp.add("expand", bestGuess);
            rsp.add("expanded", dbidRangeRouter.getExpanded());

        } else {
            rsp.add("expand", -1);
            rsp.add("exception", "ERROR: Wrong document router type:"+docRouter.getClass().getSimpleName());
            return;
        }
    }

    private synchronized void expand(SolrQueryResponse rsp, SolrParams params, String cname)
        throws IOException
    {
        InformationServer informationServer = informationServers.get(cname);
        //Get the in-memory information from MetaDataTracker
        Collection<Tracker> trackers = trackerRegistry.getTrackersForCore(cname);
        MetadataTracker metadataTracker = null;
        for(Tracker tracker : trackers)
        {
            if(tracker instanceof MetadataTracker)
            {
                metadataTracker = (MetadataTracker)tracker;
            }
        }

        DocRouter docRouter = metadataTracker.getDocRouter();

        if(docRouter instanceof DBIDRangeRouter) {
            long expansion = Long.parseLong(params.get("add"));
            DBIDRangeRouter dbidRangeRouter = (DBIDRangeRouter)docRouter;

            if(dbidRangeRouter.getExpanded()) {
                rsp.add("expand", -1);
                rsp.add("exception", "ERROR: dbid range has already been expanded.");
                return;
            }

            long currentEndRange = dbidRangeRouter.getEndRange();
            long startRange = dbidRangeRouter.getStartRange();
            long maxNodeId = informationServer.maxNodeId();

            long range = currentEndRange-startRange;
            long safe = startRange + ((long) (range * .75));

            if(maxNodeId > safe) {
                rsp.add("expand", -1);
                rsp.add("exception", "ERROR: Expansion cannot occur if max DBID in the index is more then 75% of range.");
                return;
            }

            long newEndRange = expansion+dbidRangeRouter.getEndRange();
            informationServer.capIndex(newEndRange);
            informationServer.hardCommit();
            dbidRangeRouter.setEndRange(newEndRange);
            dbidRangeRouter.setExpanded(true);

            assert newEndRange == dbidRangeRouter.getEndRange();

            rsp.add("expand", dbidRangeRouter.getEndRange());
        } else {
            rsp.add("expand", -1);
            rsp.add("exception", "ERROR: Wrong document router type:"+docRouter.getClass().getSimpleName());
            return;
        }
    }

    private void actionNODEREPORTS(SolrQueryResponse rsp, SolrParams params, String cname) throws IOException,
                JSONException
    {
        if (cname != null)
        {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(cname,
                        MetadataTracker.class);
            Long dbid = null;
            if (params.get(ARG_NODEID) != null)
            {
                dbid = Long.valueOf(params.get(ARG_NODEID));
                NamedList<Object> report = new SimpleOrderedMap<Object>();
                report.add(cname, buildNodeReport(tracker, dbid));
                rsp.add("report", report);

            }
            else
            {
                throw new AlfrescoRuntimeException("No dbid parameter set");
            }
        }
        else
        {
            Long dbid = null;
            if (params.get(ARG_NODEID) != null)
            {
                dbid = Long.valueOf(params.get(ARG_NODEID));
                NamedList<Object> report = new SimpleOrderedMap<Object>();
                for (String coreName : trackerRegistry.getCoreNames())
                {
                    MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName,
                                MetadataTracker.class);
                    report.add(coreName, buildNodeReport(tracker, dbid));
                }
                rsp.add("report", report);
            }
            else
            {
                throw new AlfrescoRuntimeException("No dbid parameter set");
            }

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
            addCoreSummary(trackerRegistry, coreName, detail, hist, values, srv, report);

            if (reset)
            {
                srv.getTrackerStats().reset();
            }
        }
        else
        {
            report.add(coreName, "Core unknown");
        }
    }


    private void actionINDEX(SolrParams params, String coreName)
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

    private void actionRETRY(SolrQueryResponse rsp, String coreName) throws IOException
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

    private void actionREINDEX(SolrParams params, String coreName)
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

    private void actionPURGE(SolrParams params, String coreName)
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

    public ConcurrentHashMap<String, InformationServer> getInformationServers()
    {
        return this.informationServers;
    }

    public TrackerRegistry getTrackerRegistry()
    {
        return trackerRegistry;
    }

    public SolrTrackerScheduler getScheduler()
    {
        return scheduler;
    }
}
