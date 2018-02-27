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
package org.alfresco.solr;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.apache.chemistry.opencmis.commons.impl.json.JSONArray;
import org.apache.chemistry.opencmis.commons.impl.json.JSONObject;
import org.apache.chemistry.opencmis.commons.impl.json.JSONValue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.SolrTestCaseJ4.XmlDoc;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.IndexSchemaFactory;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.BaseTestHarness;
import org.apache.solr.util.RefCounted;
import org.apache.solr.util.TestHarness;
import org.apache.solr.util.TestHarness.TestCoresLocator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.xml.sax.SAXException;

/**
 * Base class that provides the solr test harness.
 * This is used to manage the embedded solr used for unit and integration testing.
 * The abstract also provides helper method that interacts with the 
 * embedded solr.
 * 
 * @author Michael Suzuki
 *
 */
public abstract class  AbstractAlfrescoSolrTests implements SolrTestFiles, AlfrecsoSolrConstants
{
    private static Log log = LogFactory.getLog(AbstractAlfrescoSolrTests.class);

    protected static SolrConfig solrConfig;
    /**
     * Harness initialized by initTestHarness.
     * <p/>
     * <p>
     * For use in test methods as needed.
     * </p>
     */
    protected static TestHarness h;
    /**
     * LocalRequestFactory initialized by initTestHarness using sensible
     * defaults.
     * <p/>
     * <p>
     * For use in test methods as needed.
     * </p>
     */
    protected static TestHarness.LocalRequestFactory lrf;
    protected static AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
    protected static NodeRef testRootNodeRef;
    protected static NodeRef testNodeRef;
    protected static NodeRef testBaseFolderNodeRef;
    protected static NodeRef testFolder00NodeRef;
    protected static Date ftsTestDate;
    /**
     * Creates a Solr Alfresco test harness.
     * @param schema
     * @throws Exception
     */
    public static void initAlfrescoCore(String schema) throws Exception
    {
        log.info("##################################### init Alfresco core ##############");
        log.info("####initCore");
        System.setProperty("solr.solr.home", TEST_FILES_LOCATION);
        System.setProperty("solr.directoryFactory","solr.RAMDirectoryFactory");
        System.setProperty("solr.tests.maxBufferedDocs", "1000");
        System.setProperty("solr.tests.maxIndexingThreads", "10");
        System.setProperty("solr.tests.ramBufferSizeMB", "1024");
        // other  methods like starting a jetty instance need these too
        System.setProperty("solr.test.sys.prop1", "propone");
        System.setProperty("solr.test.sys.prop2", "proptwo");
        System.setProperty("alfresco.test", "true");
        System.setProperty("solr.tests.mergeScheduler", "org.apache.lucene.index.ConcurrentMergeScheduler");
        System.setProperty("solr.tests.mergePolicy", "org.apache.lucene.index.TieredMergePolicy");
        if (solrConfig == null) 
        {
            createAlfrescoCore(schema);
        }
        log.info("####initCore end");
    }

    public static void createAlfrescoCore(String schema) throws ParserConfigurationException, IOException, SAXException
    {
        Properties properties = new Properties();
        properties.put("solr.tests.maxBufferedDocs", "1000");
        properties.put("solr.tests.maxIndexingThreads", "10");
        properties.put("solr.tests.ramBufferSizeMB", "1024");
        properties.put("solr.tests.mergeScheduler", "org.apache.lucene.index.ConcurrentMergeScheduler");
        if("schema.xml".equalsIgnoreCase(schema))
        {
            String templateName = "rerank";
            String templateNameSystemProperty = System.getProperty("templateName");
            if (StringUtils.isNotBlank(templateNameSystemProperty))
            {
                templateName = templateNameSystemProperty;
            }
            FileUtils.copyFile(Paths.get(String.format(TEMPLATE_CONF, templateName) + schema).toFile(), Paths.get(TEST_SOLR_CONF + schema).toFile());
        }
        
        // The local test solrconfig with RAMDirectoryFactory and lockType of single.
        CoreContainer coreContainer = new CoreContainer(TEST_FILES_LOCATION);
        SolrResourceLoader resourceLoader = new SolrResourceLoader(Paths.get(TEST_SOLR_CONF), null, properties);
        solrConfig = new SolrConfig(resourceLoader, "solrconfig.xml", null);
        IndexSchema indexSchema = IndexSchemaFactory.buildIndexSchema(schema, solrConfig);
        log.info("################ Index schema:"+schema+":"+indexSchema.getResourceName());
        TestCoresLocator locator = new TestCoresLocator(SolrTestCaseJ4.DEFAULT_TEST_CORENAME,
                                                        "data", 
                                                        solrConfig.getResourceName(),
                                                        indexSchema.getResourceName());
        
        NodeConfig nodeConfig = new NodeConfig.NodeConfigBuilder("name", coreContainer.getResourceLoader())
                .setUseSchemaCache(false)
                .setCoreAdminHandlerClass("org.alfresco.solr.AlfrescoCoreAdminHandler")
                .build();
        coreContainer.shutdown();
        try
        {
            h = new TestHarness(nodeConfig, locator);
            h.coreName = SolrTestCaseJ4.DEFAULT_TEST_CORENAME;
            
        }
        catch(Exception e)
        {
            log.info("we hit an issue", e);
        }
        lrf = h.getRequestFactory
                ("standard",0,20, CommonParams.VERSION,"2.2");
    }
    @AfterClass
    public static void tearDown()
    {
        solrConfig = null;
        h.close();
    }
    /**
     * Generates a &lt;delete&gt;... XML string for an query
     *
     * @see TestHarness#deleteByQuery
     */
    public static String delQ(String q)
    {
        return TestHarness.deleteByQuery(q);
    }
    /**
     * Validates an update XML String is successful
     */
    public void assertU(String update) 
    {
        assertU(null, update);
    }

    /**
     * Validates an update XML String is successful
     */
    public void assertU(String message, String update)
    {
        checkUpdateU(message, update, true);
    }

    /**
     * Validates an update XML String failed
     */
    public void assertFailedU(String update) 
    {
        assertFailedU(null, update);
    }

    /**
     * Validates an update XML String failed
     */
    public void assertFailedU(String message, String update)
    {
        checkUpdateU(message, update, false);
    }

    /**
     * Checks the success or failure of an update message
     */
    private void checkUpdateU(String message, String update, boolean shouldSucceed)
    {
        try 
        {
            String m = (null == message) ? "" : message + " ";
            if (shouldSucceed) 
            {
                String res = h.validateUpdate(update);
                if (res != null) Assert.fail(m + "update was not successful: " + res);
            } 
            else
            {
                String res = h.validateErrorUpdate(update);
                if (res != null) Assert.fail(m + "update succeeded, but should have failed: " + res);
            }
        }
        catch (SAXException e) 
        {
            throw new RuntimeException("Invalid XML", e);
        }
    }
    /**
     * @see TestHarness#commit
     */
    public static String commit(String... args) 
    {
      return TestHarness.commit(args);
    }

    public static String adoc(String... fieldsAndValues)
    {
        XmlDoc d = doc(fieldsAndValues);
        return AlfrescoSolrUtils.add(d);
    }
    /**
     * Generates a simple &lt;doc&gt;... XML String with no options
     *
     * @param fieldsAndValues 0th and Even numbered args are fields names, Odds are field values.
     * @see TestHarness#makeSimpleDoc
     */
    public static XmlDoc doc(String... fieldsAndValues)
    {
        XmlDoc d = new XmlDoc();
        d.xml = TestHarness.makeSimpleDoc(fieldsAndValues);
        return d;
    }

    /**
     * Validates a query matches some XPath test expressions and closes the query
     * @param req
     * @param tests
     */
    public static void assertQ(SolrQueryRequest req, String... tests)
    {
        assertQ(null, req, tests);
    }
    
    /**
     * Validates a query matches some XPath test expressions and closes the query
     * @param message
     * @param req
     * @param tests
     */
    public static void assertQ(String message, SolrQueryRequest req, String... tests)
    {
        try
        {
            String response = h.query(req);
            if (req.getParams().getBool("facet", false)) 
            {
                // add a test to ensure that faceting did not throw an exception
                // internally, where it would be added to facet_counts/exception
                String[] allTests = new String[tests.length+1];
                System.arraycopy(tests,0,allTests,1,tests.length);
                allTests[0] = "*[count(//lst[@name='facet_counts']/*[@name='exception'])=0]";
                tests = allTests;
            }
            String results = BaseTestHarness.validateXPath(response, tests);

            if (null != results) 
            {
                String msg = "REQUEST FAILED: xpath=" + results
                        + "\n\txml response was: " + response
                        + "\n\trequest was:" + req.getParamString();
                log.error(msg);
                throw new RuntimeException(msg);
            }
        } catch (XPathExpressionException e1) 
        {
            throw new RuntimeException("XPath is invalid", e1);
        }
        catch (Exception e2)
        {
            throw new RuntimeException("Exception during query", e2);
        }
    }
    /**
     * Builds and asserts that query returns a collection in the correct order by
     * checking the dbid value of each document is in the correct order.
     * 
     * @author Michael Suzuki
     * @param query query used to search
     * @param dbids collection of dbids to compare
     * @throws Exception if error
     */
    public static void assertQueryCollection(String query, Integer[] dbids) throws Exception
    {
        SolrQueryRequest solrReq = req(params("rows", "20", "qt", "/cmis", "q",query,"wt","json"));
        try
        {
            String response = h.query(solrReq);
            JSONObject json = (JSONObject)JSONValue.parse(response);
            JSONObject res = (JSONObject) json.get("response");
            JSONArray docs = (JSONArray) res.get("docs");
            Assert.assertTrue(dbids.length == docs.size());
            int count = 0;
            for(Object doc : docs)
            {
                JSONObject item = (JSONObject) doc;
                BigInteger val = (BigInteger) item.get("DBID");
                Assert.assertEquals(dbids[count].intValue(), val.intValue());
                count++;
            }
        }
        finally
        {
            solrReq.close();
        }
    }
    /**
     * Creates a solr request.
     * @param params
     * @param json
     * @return
     */
    public SolrServletRequest areq(ModifiableSolrParams params, String json) 
    {
        if(params.get("wt" ) == null)
        {
            params.add("wt","xml");
        }
        SolrServletRequest req =  new SolrServletRequest(h.getCore(), null);
        req.setParams(params);
        if(json != null) 
        {
            ContentStream stream = new ContentStreamBase.StringStream(json);
            ArrayList<ContentStream> streams = new ArrayList<ContentStream>();
            streams.add(stream);
            req.setContentStreams(streams);
        }
        return req;
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
    /**
     * Makes a solr request.
     * @param q
     * @return
     */
    public static SolrQueryRequest req(String... q)
    {
        return lrf.makeRequest(q);
    }
    /**
     * 
     * @param aclId
     * @throws Exception
     */
    public void indexAclId(long aclId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "INDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "aclid", Long.toString(aclId)),
                resp);
    }
    /**
     * Maintenance method
     * @param aclId
     * @throws Exception
     */
    public void reindexAclId(long aclId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "REINDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "aclid", Long.toString(aclId)),
                resp);
    }
    /**
     * Maintenance method
     * @param txnId
     * @throws Exception
     */
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

    public void indexTransaction(Transaction transaction, List<Node> nodes, List<NodeMetaData> nodeMetaDatas, List<String> content)
    {
        //First map the nodes to a transaction.
        SOLRAPIQueueClient.nodeMap.put(transaction.getId(), nodes);

        //Next map a node to the NodeMetaData
        int i=0;
        for(NodeMetaData nodeMetaData : nodeMetaDatas)
        {
            SOLRAPIQueueClient.nodeMetaDataMap.put(nodeMetaData.getId(), nodeMetaData);
            SOLRAPIQueueClient.nodeContentMap.put(nodeMetaData.getId(), content.get(i++));
        }

        //Next add the transaction to the queue
        SOLRAPIQueueClient.transactionQueue.add(transaction);
    }


    public void purgeAclId(long aclId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "PURGE",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "aclid", Long.toString(aclId)),
                resp);
    }
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
    public void reindexNodeId(long nodeId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "REINDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "nodeid", Long.toString(nodeId)),
                resp);
    }
    public void reindexAclChangeSetId(long aclChangeSetId) throws Exception
    {
        CoreAdminHandler admin = h.getCoreContainer().getMultiCoreHandler();
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "REINDEX",
                        CoreAdminParams.NAME, h.getCore().getName(),
                        "acltxid", Long.toString(aclChangeSetId)),
                resp);
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
    /**
     * Clear the solr index.
     */
    public void clearIndex() 
    {
        assertU(delQ("*:*"));
    }
    protected void assertAQuery(String queryString,Integer count)throws IOException,ParseException 
    {
        assertAQuery(queryString, count, null, null, null);
    }
    protected void assertAQuery(String queryString,
            Integer count,
            Locale locale,
            String[] textAttributes,
            String[] allAttributes,
            String... name)throws IOException,ParseException
{
        SolrServletRequest solrQueryRequest = null;
        RefCounted<SolrIndexSearcher>refCounted = null;
        try
        {
            solrQueryRequest = new SolrServletRequest(h.getCore(), null);
            refCounted = h.getCore().getSearcher();
            SolrIndexSearcher solrIndexSearcher = refCounted.get();
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.setQuery(queryString);
            if (locale != null)
            {
                searchParameters.addLocale(locale);
            }
            
            if (textAttributes != null)
            {
                for (String textAttribute : textAttributes)
            {
                    searchParameters.addTextAttribute(textAttribute);
            }
        }
        if (allAttributes != null)
        {
            for (String allAttribute : allAttributes)
            {
                searchParameters.addAllAttribute(allAttribute);
            }
        }
        
        Query query = dataModel.getLuceneQueryParser(searchParameters, solrQueryRequest, FTSQueryParser.RerankPhase.SINGLE_PASS).parse(queryString);
        log.debug("####### Query ######:"+query);
        TopDocs docs = solrIndexSearcher.search(query, count * 2 + 10);

        if (count != null)
        {
            if (docs.totalHits != count)
            {
                throw new IOException("FAILED: " + fixQueryString(queryString, name)+" ; "+docs.totalHits);
            }
        }
    }
        finally
        {
            refCounted.decref();
            solrQueryRequest.close();
        }
    }
    protected String fixQueryString(String queryString, String... name)
    {
        if (name.length > 0)
        {
            return name[0].replace("\uFFFF", "<Unicode FFFF>");
        }
        else
        {
            return queryString.replace("\uFFFF", "<Unicode FFFF>");
        }
    }
    /**
     * Generates a SolrQueryRequest
     */
    public static SolrQueryRequest req(SolrParams params, String... moreParams)
    {
        ModifiableSolrParams mp = new ModifiableSolrParams(params);
        for (int i=0; i<moreParams.length; i+=2)
        {
            mp.add(moreParams[i], moreParams[i+1]);
        }
        return new LocalSolrQueryRequest(h.getCore(), mp);
    }
    
    public static ModifiableSolrParams params(String... params)
    {
        ModifiableSolrParams msp = new ModifiableSolrParams();
        for (int i=0; i<params.length; i+=2) 
        {
            msp.add(params[i], params[i+1]);
        }
        return msp;
    }
    public static class SolrServletRequest extends SolrQueryRequestBase
    {
        public SolrServletRequest(SolrCore core, HttpServletRequest req)
        {
            super(core, new MultiMapSolrParams(Collections.<String, String[]> emptyMap()));
        }
    }
}
