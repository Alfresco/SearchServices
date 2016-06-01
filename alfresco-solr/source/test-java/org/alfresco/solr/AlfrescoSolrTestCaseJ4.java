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


import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.IndexSchemaFactory;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.solr.util.TestHarness;
import org.apache.solr.util.TestHarness.TestCoresLocator;
import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


/**
 * @author Joel
 *
 */
public class AlfrescoSolrTestCaseJ4 extends SolrTestCaseJ4
{
    private long id = 0;
    private static final String TEST_NAMESPACE = "http://www.alfresco.org/test/solrtest";

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static File HOME() {
        return getFile("../source/test-files");
    }

    public static class SolrServletRequest extends SolrQueryRequestBase {
        public SolrServletRequest(SolrCore core, HttpServletRequest req)
        {
            super(core, new MultiMapSolrParams(Collections.<String, String[]> emptyMap()));
        }
    }

    public SolrServletRequest areq(ModifiableSolrParams params, String json) {
        if(params.get("wt" ) == null) params.add("wt","xml");
        SolrServletRequest req =  new SolrServletRequest(h.getCore(), null);
        req.setParams(params);
        if(json != null) {
            ContentStream stream = new ContentStreamBase.StringStream(json);
            ArrayList<ContentStream> streams = new ArrayList<ContentStream>();
            streams.add(stream);
            req.setContentStreams(streams);
        }
        return req;
    }

    /*
    @AfterClass
    public static void close() {
        System.out.println("################ Closing Core !!!! #########");
        h.close();
    }
    */

    public static void initAlfrescoCore(String config, String schema) throws Exception {
        System.out.println("##################################### init Alfresco core ##############");
        log.info("####initCore");

        configString = config;
        schemaString = schema;
        testSolrHome = HOME().toPath();
        ignoreException("ignore_exception");

        System.setProperty("solr.directoryFactory","solr.RAMDirectoryFactory");
        System.setProperty("solr.solr.home", getFile("../source/test-files").toString());

        System.setProperty("solr.tests.maxBufferedDocs", "1000");
        System.setProperty("solr.tests.maxIndexingThreads", "10");
        System.setProperty("solr.tests.ramBufferSizeMB", "1024");
        

        // other  methods like starting a jetty instance need these too
        System.setProperty("solr.test.sys.prop1", "propone");
        System.setProperty("solr.test.sys.prop2", "proptwo");
        System.setProperty("alfresco.test", "true");

        String configFile = getSolrConfigFile();
        if (configFile != null) {
            createAlfrescoCore(config, schema);
        }
        log.info("####initCore end");
    }

    public static void createAlfrescoCore(String config, String schema) throws ParserConfigurationException, IOException, SAXException {
        assertNotNull(testSolrHome);
        
        Properties properties = new Properties();
        properties.put("solr.tests.maxBufferedDocs", "1000");
        properties.put("solr.tests.maxIndexingThreads", "10");
        properties.put("solr.tests.ramBufferSizeMB", "1024");
        properties.put("solr.tests.mergeScheduler", "org.apache.lucene.index.ConcurrentMergeScheduler");
        properties.put("solr.tests.mergePolicy", "org.apache.lucene.index.TieredMergePolicy");
        
        CoreContainer coreContainer = new CoreContainer("../source/test-files");
        SolrResourceLoader resourceLoader = new SolrResourceLoader(Paths.get("../source/test-files/collection1/conf"), null, properties);
        SolrConfig solrConfig = new SolrConfig(resourceLoader, config, null);
        IndexSchema indexSchema = IndexSchemaFactory.buildIndexSchema(schema, solrConfig);
        //CoreDescriptor coreDescriptor = new CoreDescriptor(coreContainer, SolrTestCaseJ4.DEFAULT_TEST_CORENAME, SolrTestCaseJ4.DEFAULT_TEST_CORENAME);
        
        TestCoresLocator locator = new TestCoresLocator(SolrTestCaseJ4.DEFAULT_TEST_CORENAME, "data", solrConfig.getResourceName(), indexSchema.getResourceName());
        
        NodeConfig nodeConfig = new NodeConfig.NodeConfigBuilder("name", coreContainer.getResourceLoader())
        .setUseSchemaCache(false)
        .setCoreAdminHandlerClass("org.alfresco.solr.AlfrescoCoreAdminHandler")
        .build();

        h = new TestHarness(nodeConfig, locator);
        //h.getCoreContainer().create(coreDescriptor);
        h.coreName = SolrTestCaseJ4.DEFAULT_TEST_CORENAME;
        
        lrf = h.getRequestFactory
                ("standard",0,20, CommonParams.VERSION,"2.2");
        
        coreContainer.shutdown();
    }

    public synchronized Node getNode(Transaction txn, Acl acl, Node.SolrApiNodeStatus status)
    {
        ++id;
        Node node = new Node();
        node.setTxnId(txn.getId());
        node.setId(id);
        node.setAclId(acl.getId());
        node.setStatus(status);
        return node;
    }

    public NodeMetaData getNodeMetaData(Node node, Transaction txn, Acl acl, String owner, Set<NodeRef> ancestors, boolean createError)
    {
        NodeMetaData nodeMetaData = new NodeMetaData();
        nodeMetaData.setId(node.getId());
        nodeMetaData.setAclId(acl.getId());
        nodeMetaData.setTxnId(txn.getId());
        nodeMetaData.setOwner(owner);
        nodeMetaData.setAspects(new HashSet());
        nodeMetaData.setAncestors(ancestors);
        Map<QName, PropertyValue> props = new HashMap();
        props.put(ContentModel.PROP_IS_INDEXED, new StringPropertyValue("true"));
        props.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.US, 0l, "UTF-8", "text/plain", null));
        nodeMetaData.setProperties(props);
        //If create createError is true then we leave out the nodeRef which will cause an error
        if(!createError) {
            NodeRef nodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
            nodeMetaData.setNodeRef(nodeRef);
        }
        nodeMetaData.setType(QName.createQName(TEST_NAMESPACE, "testSuperType"));
        nodeMetaData.setAncestors(ancestors);
        nodeMetaData.setPaths(new ArrayList());
        nodeMetaData.setNamePaths(new ArrayList());
        return nodeMetaData;
    }


    public synchronized Acl getAcl(AclChangeSet aclChangeSet)
    {
        ++id;
        Acl acl = new Acl(aclChangeSet.getId(), id);
        return acl;
    }

    public synchronized AclChangeSet getAclChangeSet(int aclCount)
    {
        ++id;
        AclChangeSet aclChangeSet = new AclChangeSet(id, System.currentTimeMillis(), aclCount);
        return aclChangeSet;
    }

    public AclReaders getAclReaders(AclChangeSet aclChangeSet, Acl acl, List<String> readers, List<String> denied, String tenant)
    {
        if(tenant == null)
        {
            tenant = TenantService.DEFAULT_DOMAIN;
        }

        return new AclReaders(acl.getId(), readers, denied, aclChangeSet.getId(), tenant);
    }

    //Maintenance method
    public void reindexAclChangeSetId(long aclChangeSetId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "REINDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "acltxid", Long.toString(aclChangeSetId)),
                resp);
    }


    public void indexAclChangeSet(AclChangeSet aclChangeSet, List<Acl> aclList, List<AclReaders> aclReadersList)
    {
        //First map the nodes to a transaction.
        SOLRAPIQueueClient.aclMap.put(aclChangeSet.getId(), aclList);

        //Next map a node to the NodeMetaData
        for(AclReaders aclReaders : aclReadersList)
        {
            SOLRAPIQueueClient.aclReadersMap.put(aclReaders.getId(), aclReaders);
        }

        //Next add the transaction to the queue

        SOLRAPIQueueClient.aclChangeSetQueue.add(aclChangeSet);
        System.out.println("SOLRAPIQueueClient.aclChangeSetQueue.size():"+SOLRAPIQueueClient.aclChangeSetQueue.size());
    }


    //Maintenance method
    public void purgeAclChangeSetId(long aclChangeSetId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "PURGE",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "acltxid", Long.toString(aclChangeSetId)),
                resp);
    }

    //Maintenance method
    public void purgeNodeId(long nodeId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "PURGE",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "nodeid", Long.toString(nodeId)),
                resp);
    }

    //Maintenance method
    public void purgeTransactionId(long txnId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "PURGE",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "txid", Long.toString(txnId)),
                resp);
    }

    public List<String> list(String... strings)
    {
        List<String> list = new ArrayList();
        for(String s : strings)
        {
            list.add(s);
        }
        return list;
    }

    public List<Acl> list(Acl... acls)
    {
        List<Acl> list = new ArrayList();
        for(Acl acl : acls)
        {
            list.add(acl);
        }
        return list;

    }

    public List<Node> list(Node... nodes)
    {
        List<Node> list = new ArrayList();
        for(Node node : nodes)
        {
            list.add(node);
        }
        return list;

    }

    public List<AclReaders> list(AclReaders... aclReaders)
    {
        List<AclReaders> list = new ArrayList();
        for(AclReaders a : aclReaders)
        {
            list.add(a);
        }
        return list;

    }


    public List<NodeMetaData> list(NodeMetaData... nodeMetaDatas)
    {
        List<NodeMetaData> list = new ArrayList();
        for(NodeMetaData nodeMetaData : nodeMetaDatas)
        {
            list.add(nodeMetaData);
        }
        return list;

    }

    public Set<NodeRef> ancestors(NodeRef... refs) {
        Set set = new HashSet();
        for(NodeRef ref : refs) {
            set.add(ref);
        }
        return set;
    }

    //Maintenance method
    public void retry() throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "RETRY",
                        CoreAdminParams.NAME, h.getCore().getName()),
                resp);
    }

    //Maintenance method
    public void reindexAclId(long aclId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "REINDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "aclid", Long.toString(aclId)),
                resp);
    }

    //Maintenance method
    public void reindexTransactionId(long txnId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION,
                        "REINDEX",
                        CoreAdminParams.NAME,
                        h.getCore().getName(),
                        "txid", Long.toString(txnId)),
                resp);
    }

    //Maintenance method
    public void reindexNodeId(long nodeId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "REINDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "nodeid", Long.toString(nodeId)),
                resp);
    }

    //Maintenance method
    public void indexAclId(long aclId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "INDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "aclid", Long.toString(aclId)),
                resp);
    }

    public synchronized Transaction getTransaction(int deletes, int updates)
    {
        long txnId = id++;
        long txnCommitTime = System.currentTimeMillis();
        Transaction transaction = new Transaction();
        transaction.setCommitTimeMs(txnCommitTime);
        transaction.setId(txnId);
        transaction.setDeletes(deletes);
        transaction.setUpdates(updates);
        return transaction;
    }

    public void indexTransaction(Transaction transaction, List<Node> nodes, List<NodeMetaData> nodeMetaDatas)
    {
        //First map the nodes to a transaction.
        SOLRAPIQueueClient.nodeMap.put(transaction.getId(), nodes);

        //Next map a node to the NodeMetaData
        for(NodeMetaData nodeMetaData : nodeMetaDatas)
        {
            SOLRAPIQueueClient.nodeMetaDataMap.put(nodeMetaData.getId(), nodeMetaData);
        }

        //Next add the transaction to the queue
        SOLRAPIQueueClient.transactionQueue.add(transaction);
    }

    //Maintenance method
    public void purgeAclId(long aclId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "PURGE",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "aclid", Long.toString(aclId)),
                resp);
    }

    protected synchronized String createGUID()
    {
        long time;
        time = id++;
        return "00000000-0000-" + ((time / 1000000000000L) % 10000L) + "-" + ((time / 100000000L) % 10000L) + "-"
                + (time % 100000000L);
    }


    public void waitForDocCount(Query query, long expectedNumFound, long waitMillis)
            throws Exception
    {
        Date date = new Date();
        long timeout = (long)date.getTime() + waitMillis;

        RefCounted<SolrIndexSearcher> ref = null;
        int totalHits = 0;
        while(new Date().getTime() < timeout)
        {
            try
            {
                ref = h.getCore().getSearcher();
                SolrIndexSearcher searcher = ref.get();
                TopDocs topDocs = searcher.search(query, 10);
                totalHits = topDocs.totalHits;
                if (topDocs.totalHits == expectedNumFound)
                {
                    return;
                }
                else
                {
                    Thread.sleep(2000);
                }
            }
            finally
            {
                ref.decref();
            }
        }
        throw new Exception("Wait error expected "+expectedNumFound+" found "+totalHits+" : "+query.toString());
    }









}