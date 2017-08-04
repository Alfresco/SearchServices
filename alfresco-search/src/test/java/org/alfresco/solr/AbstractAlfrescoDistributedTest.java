package org.alfresco.solr;

import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.stream.AlfrescoStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.embedded.SSLConfig;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.createCoreUsingTemplate;

/**
 * Clone of a helper base class for distributed search test cases
 *
 * By default, all tests in sub-classes will be executed with 1, 2, ...
 * DEFAULT_MAX_SHARD_COUNT number of shards set up repeatedly.
 *
 * In general, it's preferable to annotate the tests in sub-classes with a
 * {@literal @}ShardsFixed(num = N) or a {@literal @}ShardsRepeat(min = M, max =
 * N) to indicate whether the test should be called once, with a fixed number of
 * shards, or called repeatedly for number of shards = M to N.
 *
 * In some cases though, if the number of shards has to be fixed, but the number
 * itself is dynamic, or if it has to be set as a default for all sub-classes of
 * a sub-class, there's a fixShardCount(N) available, which is identical to
 * {@literal @}ShardsFixed(num = N) for all tests without annotations in that
 * class hierarchy. Ideally this function should be retired in favour of better
 * annotations..
 *
 * @since solr 1.5
 * @author Michael Suzuki 
 */
public abstract class AbstractAlfrescoDistributedTest extends SolrTestCaseJ4
{
    // TODO: this shouldn't be static. get the random when you need it to avoid
    // sharing.
    public static Random r;
    private AtomicInteger nodeCnt = new AtomicInteger(0);
    protected boolean useExplicitNodeNames;

    public static Properties DEFAULT_CORE_PROPS = new Properties();

    static {
        DEFAULT_CORE_PROPS.setProperty("alfresco.commitInterval", "1000");
        DEFAULT_CORE_PROPS.setProperty("alfresco.newSearcherInterval", "2000");
    }

    /**
     * Set's the value of the "hostContext" system property to a random path
     * like string (which may or may not contain sub-paths). This is used in the
     * default constructor for this test to help ensure no code paths have
     * hardcoded assumptions about the servlet context used to run solr.
     * <p>
     * Test configs may use the <code>${hostContext}</code> variable to access
     * this system property.
     * </p>
     *
     */
    @BeforeClass
    public static void setup()
    {
        System.setProperty("alfresco.test", "true");
        System.setProperty("solr.tests.maxIndexingThreads", "10");
        System.setProperty("solr.tests.ramBufferSizeMB", "1024");
        testDir = new File(System.getProperty("user.dir") + "/target/jettys");
        r = new Random(random().nextLong());

    }

    protected Map<String, JettySolrRunner> jettyContainers = new HashMap<>();
    protected Map<String, SolrClient> jettyClients = new HashMap<>();

    protected List<JettySolrRunner> jettyShards = new ArrayList<>();
    protected List<SolrClient> clientShards = new ArrayList<>();

    protected String[] deadServers;
    protected String shards;
    protected String[] shardsArr;
    protected static File testDir;

    // to stress with higher thread counts and requests, make sure the junit
    // xml formatter is not being used (all output will be buffered before
    // transformation to xml and cause an OOM exception).
    protected int stress = TEST_NIGHTLY ? 2 : 0;
    protected boolean verifyStress = true;
    protected int nThreads = 3;

    protected int clientConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    protected int clientSoTimeout = 90000;

    public static int ORDERED = 1;
    public static int SKIP = 2;
    public static int SKIPVAL = 4;
    public static int UNORDERED = 8;

    /**
     * When this flag is set, Double values will be allowed a difference ratio
     * of 1E-8 between the non-distributed and the distributed returned values
     */
    public static int FUZZY = 16;
    private static final double DOUBLE_RATIO_LIMIT = 1E-8;

    protected int flags;
    protected Map<String, Integer> handle = new HashMap<>();

    protected String id = "id";
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static RandVal rint = new RandVal()
    {
        @Override
        public Object val()
        {
            return r.nextInt();
        }
    };

    public static RandVal rlong = new RandVal()
    {
        @Override
        public Object val()
        {
            return r.nextLong();
        }
    };

    public static RandVal rfloat = new RandVal()
    {
        @Override
        public Object val()
        {
            return r.nextFloat();
        }
    };

    public static RandVal rdouble = new RandVal()
    {
        @Override
        public Object val()
        {
            return r.nextDouble();
        }
    };

    public static RandVal rdate = new RandDate();

    public static String[] fieldNames = new String[]
    { "n_ti1", "n_f1", "n_tf1", "n_d1", "n_td1", "n_l1", "n_tl1", "n_dt1", "n_tdt1" };
    public static RandVal[] randVals = new RandVal[]
    { rint, rfloat, rfloat, rdouble, rdouble, rlong, rlong, rdate, rdate };

    protected String[] getFieldNames()
    {
        return fieldNames;
    }

    protected RandVal[] getRandValues()
    {
        return randVals;
    }

    /**
     * Subclasses can override this to change a test's solr home (default is in
     * test-files)
     */
    public String getTestFilesHome()
    {
        return System.getProperty("user.dir") + "/target/test-classes/test-files";
    }
    public void distribSetUp(String serverName) throws Exception
    {
        SolrTestCaseJ4.resetExceptionIgnores(); // ignore anything with
                                                // ignore_exception in it
        System.setProperty("solr.test.sys.prop1", "propone");
        System.setProperty("solr.test.sys.prop2", "proptwo");
        System.setProperty("solr.directoryFactory", "org.apache.solr.core.MockDirectoryFactory");
        System.setProperty("solr.log.dir", testDir.toPath().resolve(serverName).toString());
    }

    public void distribTearDown() throws Exception
    {
        System.clearProperty("solr.directoryFactory");
        System.clearProperty("solr.log.dir");

        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
    }

    /**
     * Waits until all cores (including shards) reach a count.
     * @param query
     * @param count
     * @param waitMillis
     * @throws Exception
     */
    public void waitForDocCountAllCores(Query query, int count, long waitMillis) throws Exception
    {
        List<SolrCore> cores = getJettyCores(jettyContainers.values());
        cores.addAll(getJettyCores(jettyShards));

        long begin = System.currentTimeMillis();
        for (SolrCore core : cores) {
            waitForDocCountCore(core, query, count, waitMillis, begin);
        }
    }

    /**
     * Delele by query on all Clients
     * @param q
     * @throws Exception
     */
    public void deleteByQueryAllClients(String q) throws Exception {

        List<SolrClient> clients = getAllClients();

        for (SolrClient client : clients) {
            client.deleteByQuery(q);
        }
    }

    /**
     * Gets a list of all clients for that test
     * @return list of SolrClient
     */
    public List<SolrClient> getAllClients()
    {
        List<SolrClient> clients = new ArrayList();
        clients.addAll(jettyClients.values());
        clients.addAll(clientShards);
        return clients;
    }

    /**
     * Waits for the doc count on the first core available, then checks all the Shards match.
     * @param query
     * @param count
     * @param waitMillis
     * @throws Exception
     */
    public void waitForDocCount(Query query, int count, long waitMillis) throws Exception 
    {
        long begin = System.currentTimeMillis();
        List<SolrCore> cores = getJettyCores(jettyContainers.values());
        //TODO: Support multiple cores per jetty
        SolrCore controlCore = cores.get(0); //Get the first one
        waitForDocCountCore(controlCore, query, count, waitMillis, begin);
        waitForShardsCount(query, count, waitMillis, begin);
    }

    public void assertShardCount(int shardNumber, Query query, int count) throws Exception {
        List<SolrCore> cores = getJettyCores(jettyShards);
        SolrCore core = cores.get(shardNumber);
        RefCounted<SolrIndexSearcher> refCounted = null;
        try {
            refCounted = core.getSearcher();
            SolrIndexSearcher searcher = refCounted.get();
            TopDocs topDocs = searcher.search(query, 10);
            if(count != topDocs.totalHits) {
                throw new Exception("Expecting "+count+" docs on shard "+shardNumber+" , found "+topDocs.totalHits);
            }
        } finally {
            refCounted.decref();
        }
    }

    /**
     * Waits until all the shards reach the desired count, or errors.
     * @param query
     * @param count
     * @param waitMillis
     * @param start
     * @throws Exception
     */
    public void waitForShardsCount(Query query, int count, long waitMillis, long start) throws Exception
    {
        List<SolrCore> cores = getJettyCores(jettyShards);
        long timeOut = start+waitMillis;
        int totalCount = 0;
        while (System.currentTimeMillis() < timeOut) {
            totalCount = 0;
            for (SolrCore core : cores) {
                RefCounted<SolrIndexSearcher> refCounted = null;
                try {
                    refCounted = core.getSearcher();
                    SolrIndexSearcher searcher = refCounted.get();
                    TopDocs topDocs = searcher.search(query, 10);
                    totalCount += topDocs.totalHits;
                } finally {
                    refCounted.decref();
                }
            }
            if (totalCount == count) {
                return;
            }
        }
        throw new Exception("Cluster:Wait error expected "+count+" found "+totalCount+" : "+query.toString());
    }

    /**
     * Gets the cores for the jetty instances
     * @return
     */
    protected List<SolrCore> getJettyCores(Collection<JettySolrRunner> runners)
    {
        List<SolrCore> cores = new ArrayList();
        for (JettySolrRunner jettySolrRunner : runners) 
        {
            jettySolrRunner.getCoreContainer().getCores().forEach(aCore -> cores.add(aCore));
        }
        return cores;
    }

    /**
     * Gets the Default test client.
     * @return
     */
    protected SolrClient getDefaultTestClient()
    {
        return jettyClients.get(DEFAULT_TEST_CORENAME);
    }

    protected List<SolrClient> getClusterClients()
    {
        return clientShards;
    }


    public int assertNodesPerShardGreaterThan(int count) throws Exception
    {
        return assertNodesPerShardGreaterThan(count, false);
    }

    public int assertNodesPerShardGreaterThan(int count, boolean ignoreZero) throws Exception
    {
        int shardHit = 0;
        List<SolrCore> cores = getJettyCores(jettyShards);
        Query query = new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_NODE));
        StringBuilder error = new StringBuilder();
        for (SolrCore core : cores)
        {
            RefCounted<SolrIndexSearcher> refCounted = null;
            try
            {
                refCounted = core.getSearcher();
                SolrIndexSearcher searcher = refCounted.get();
                TopDocs topDocs = searcher.search(query, 10);
                if(topDocs.totalHits > 0)
                {
                    shardHit++;
                }
                if(topDocs.totalHits < count)
                {
                    if (ignoreZero && topDocs.totalHits == 0)
                    {
                        log.info(core.getName()+": have zero hits ");
                    }
                    else
                    {
                        error.append(" "+core.getName()+": ");
                        error.append("Expected nodes per shard greater than "+count+" found "+topDocs.totalHits+" : "+query.toString());
                    }
                }
                log.info(core.getName()+": Hits "+topDocs.totalHits);
            }
            finally
            {
                refCounted.decref();
            }
        }

        if (error.length() > 0)
        {
            throw new Exception(error.toString());
        }
        return shardHit;
    }

    public void assertCountAndColocation(Query query, int count) throws Exception
    {

        List<SolrCore> cores = getJettyCores(jettyShards);
        int shardHit = 0;
        int totalCount = 0;
        for (SolrCore core : cores)
        {
            RefCounted<SolrIndexSearcher> refCounted = null;
            try
            {
                refCounted = core.getSearcher();
                SolrIndexSearcher searcher = refCounted.get();
                TopDocs topDocs = searcher.search(query, 10);
                totalCount += topDocs.totalHits;
                if(topDocs.totalHits > 0)
                {
                    shardHit++;
                }
            }
            finally
            {
                refCounted.decref();
            }
        }

        if(totalCount != count) {
            throw new Exception(totalCount+" docs found for query: "+query.toString()+" expecting "+count);
        }

        if(shardHit > 1) {
            throw new Exception(shardHit+" shards found data for query: "+query.toString()+" expecting 1");
        }
    }

    public void assertShardSequence(int shard, Query query, int count) throws Exception
    {
        List<SolrCore> cores = getJettyCores(jettyShards);
        int totalCount = 0;
        SolrCore core = cores.get(shard);

        RefCounted<SolrIndexSearcher> refCounted = null;
        try
        {
            refCounted = core.getSearcher();
            SolrIndexSearcher searcher = refCounted.get();
            TopDocs topDocs = searcher.search(query, 10);
            totalCount = topDocs.totalHits;
        }
        finally
        {
            refCounted.decref();
        }

        if(totalCount != count) {
            throw new Exception(totalCount+" docs found for query: "+query.toString()+" expecting "+count);
        }
    }

    public void waitForDocCountCore(SolrCore core,
                                 Query query,
                                 long expectedNumFound,
                                 long waitMillis,
                                 long startMillis)
            throws Exception
    {
        long timeout = startMillis + waitMillis;
        int totalHits = 0;
        int increment = 1;
        while(new Date().getTime() < timeout)
        {
            RefCounted<SolrIndexSearcher> refCounted = null;
            try {
                refCounted = core.getSearcher();
                SolrIndexSearcher searcher = refCounted.get();
                TopDocs topDocs = searcher.search(query, 10);
                totalHits = topDocs.totalHits;
                if (topDocs.totalHits == expectedNumFound) {
                    return;
                } else {
                    Thread.sleep(500 * increment++);
                }
            } finally {
                refCounted.decref();
            }
        }
        throw new Exception("Core:Wait error expected "+expectedNumFound+" found "+totalHits+" : "+query.toString());
    }

    /**
     * Creates a JettySolrRunner (if one didn't exist already). DOES NOT START IT.
     * @return
     * @throws Exception
     */
    private JettySolrRunner createJetty(String jettyKey) throws Exception
    {
        if (jettyContainers.containsKey(jettyKey))
        {
            return jettyContainers.get(jettyKey);
        }
        else
        {
            Path jettySolrHome = testDir.toPath().resolve(jettyKey);
            seedSolrHome(jettySolrHome);
            JettySolrRunner jetty = createJetty(jettySolrHome.toFile(), null, null, false, getSchemaFile());
            return jetty;
        }
    }

    /**
     * Adds the core config information to the jetty file system.
     * Its best to call this before calling start() on Jetty
     * @param jettyKey
     * @param sourceConfigName
     * @param coreName
     * @param additionalProperties
     * @throws Exception
     */
    private void addCoreToJetty(String jettyKey, String sourceConfigName, String coreName, Properties additionalProperties) throws Exception
    {
        Path jettySolrHome = testDir.toPath().resolve(jettyKey);
        Path coreSourceConfig = new File(getTestFilesHome() + "/"+sourceConfigName).toPath();
        Path coreHome = jettySolrHome.resolve(coreName);
        seedCoreDir(coreName, coreSourceConfig, coreHome);
        updateSolrCoreProperties(coreHome, additionalProperties);
    }


    private void updateSolrCoreProperties(Path coreHome, Properties additionalProperties) throws IOException
    {
        if(additionalProperties != null)
        {
            InputStream in = null;
            OutputStream out = null;
            try
            {
                Properties properties = new Properties();
                String solrcoreProperties = coreHome.resolve("conf/solrcore.properties").toString();
                in = new FileInputStream(solrcoreProperties);
                properties.load(in);
                in.close();
                additionalProperties.entrySet().forEach(x-> properties.put(x.getKey(),x.getValue()));
                out = new FileOutputStream(solrcoreProperties);
                properties.store(out, null);
            }
            finally
            {
                out.close();
                in.close();
            }
            
        }
        
    }

    /**
     * Starts jetty if its not already running
     * @param jsr
     * @throws Exception
     */
    private void startJetty(JettySolrRunner jsr) throws Exception {
        if (!jsr.isRunning())
        {
            jsr.start();
        }
    }

    protected void createServers(String jettyKey, String[] coreNames, int numShards, Properties additionalProperties) throws Exception
    {
        JettySolrRunner jsr =  createJetty(jettyKey);
        jettyContainers.put(jettyKey, jsr);

        Properties properties = new Properties();

        if(additionalProperties != null && additionalProperties.size() > 0) {
            properties.putAll(additionalProperties);
            properties.remove("shard.method");
        }

        for (int i = 0; i < coreNames.length; i++) 
        {
            addCoreToJetty(jettyKey, coreNames[i], coreNames[i], properties);
        }

        //Now start jetty
        startJetty(jsr);

        int jettyPort = jsr.getLocalPort();
        for (int i = 0; i < coreNames.length; i++)
        {
            String url = buildUrl(jettyPort) + "/" + coreNames[i];
            log.info(url);
            jettyClients.put(coreNames[i], createNewSolrClient(url));
        }

        shardsArr = new String[numShards];
        StringBuilder sb = new StringBuilder();

        if (additionalProperties == null) {
            additionalProperties = new Properties();
        }


        String[] ranges = {"0-100", "100-200", "200-300", "300-400"};
        for (int i = 0; i < numShards; i++)
        {
            Properties props = new Properties();
            props.putAll(additionalProperties);
            if (sb.length() > 0) sb.append(',');
            final String shardname = "shard" + i;
            props.put("shard.instance", Integer.toString(i));
            props.put("shard.count", Integer.toString(numShards));

            if("DB_ID_RANGE".equalsIgnoreCase(props.getProperty("shard.method"))) {
                //Add
                props.put("shard.range", ranges[i]);
            }

            String shardKey = jettyKey+"_shard_"+i;
            JettySolrRunner j =  createJetty(shardKey);
            //use the first corename specified as the Share template
            addCoreToJetty(shardKey, coreNames[0], shardname, props);
            jettyShards.add(j);
            startJetty(j);
            String shardStr = buildUrl(j.getLocalPort()) + "/" + shardname;
            log.info(shardStr);
            SolrClient clientShard = createNewSolrClient(shardStr);
            clientShards.add(clientShard);
            shardsArr[i] = shardStr;
            sb.append(shardStr);
        }
        shards = sb.toString();
    }

    protected void setDistributedParams(ModifiableSolrParams params)
    {
        params.set("shards", getShardsString());
    }

    protected List<Tuple> getTuples(TupleStream tupleStream) throws IOException {
        List<Tuple> tuples = new ArrayList();
        tupleStream.open();
        try {
            while (true) {
                Tuple tuple = tupleStream.read();
                if (!tuple.EOF) {
                    tuples.add(tuple);
                } else {
                    break;
                }
            }
        }
        finally
        {
            try {
                tupleStream.close();
            } catch(Exception e2) {
                e2.printStackTrace();
            }
        }

        return tuples;
    }

    protected String getShardsString(List<SolrClient> clientList)
    {
        StringBuilder buf = new StringBuilder();
        for(int i=0; i<clientList.size(); ++i) {
            HttpSolrClient solrClient = (HttpSolrClient)clientList.get(i);

            if(buf.length() > 0) {
                buf.append(",");
            }

            buf.append(solrClient.getBaseURL());
        }
        return buf.toString();
    }

    protected String getShardsString()
    {
        if (deadServers == null)
            return shards;

        StringBuilder sb = new StringBuilder();
        for (String shard : shardsArr)
        {
            if (sb.length() > 0)
                sb.append(',');
            int nDeadServers = r.nextInt(deadServers.length + 1);
            if (nDeadServers > 0)
            {
                List<String> replicas = new ArrayList<>(Arrays.asList(deadServers));
                Collections.shuffle(replicas, r);
                replicas.add(r.nextInt(nDeadServers + 1), shard);
                for (int i = 0; i < nDeadServers + 1; i++)
                {
                    if (i != 0)
                        sb.append('|');
                    sb.append(replicas.get(i));
                }
            } else
            {
                sb.append(shard);
            }
        }

        return sb.toString();
    }

    protected void destroyServers() throws Exception
    {
        List<String> solrHomes = new ArrayList<String>();
        for (JettySolrRunner jetty : jettyContainers.values())
        {
            solrHomes.add(jetty.getSolrHome());
            jetty.stop();
        }
        for (SolrClient jClients : jettyClients.values())
        {
            jClients.close();
        }

        for (JettySolrRunner jetty : jettyShards)
        {
            solrHomes.add(jetty.getSolrHome());
            jetty.stop();
        }

        for (SolrClient client : clientShards)
        {
            client.close();
        }

        for(String home : solrHomes)
        {
            FileUtils.deleteDirectory(new File(home, "ContentStore"));
        }

        clientShards.clear();
        jettyShards.clear();
        jettyContainers.clear();
        jettyClients.clear();
    }

    public JettySolrRunner createJetty(File solrHome, String dataDir, String shardList, boolean sslEnabled, String schemaOverride) throws Exception
    {
        return createJetty(solrHome, dataDir, shardList, sslEnabled, schemaOverride, useExplicitNodeNames);
    }

    /**
     * Create a solr jetty server.
     * 
     * @param solrHome
     * @param dataDir
     * @param shardList
     * @param schemaOverride
     * @param explicitCoreNodeName
     * @return
     * @throws Exception
     */
    public JettySolrRunner createJetty(File solrHome, String dataDir, String shardList, boolean sslEnabled,
            String schemaOverride, boolean explicitCoreNodeName) throws Exception
    {
        Properties props = new Properties();
        if (schemaOverride != null)
            props.setProperty("schema", schemaOverride);
        if (shardList != null)
            props.setProperty("shards", shardList);
        if (dataDir != null)
        {
            props.setProperty("solr.data.dir", dataDir);
        }

        if (explicitCoreNodeName)
        {
            props.setProperty("coreNodeName", Integer.toString(nodeCnt.incrementAndGet()));
        }
        SSLConfig sslConfig = new SSLConfig(sslEnabled, false, null, null, null, null);
        JettyConfig config = JettyConfig.builder().setContext("/solr").stopAtShutdown(true).withSSLConfig(sslConfig)
                .build();
        JettySolrRunner jetty = new JettySolrRunner(solrHome.getAbsolutePath(), props, config);
        // .stopAtShutdown(true)
        // .withFilters(getExtraRequestFilters())
        // .withServlets(getExtraServlets())
        // .withSSLConfig(sslConfig)
        // .build());
        return jetty;
    }

    /**
     * Override this method to insert extra servlets into the JettySolrRunners
     * that are created using createJetty()
     */
    public SortedMap<ServletHolder, String> getExtraServlets()
    {
        return null;
    }

    /**
     * Override this method to insert extra filters into the JettySolrRunners
     * that are created using createJetty()
     */
    public SortedMap<Class<? extends Filter>, String> getExtraRequestFilters()
    {
        return null;
    }

    protected SolrClient createNewSolrClient(String url)
    {
        try
        {
            // setup the client...
            HttpSolrClient client = new HttpSolrClient(url);
            client.setConnectionTimeout(clientConnectionTimeout);
            client.setSoTimeout(clientSoTimeout);
            client.setDefaultMaxConnectionsPerHost(100);
            client.setMaxTotalConnections(100);
            return client;
        } catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    protected String buildUrl(int port)
    {
        return buildUrl(port, "/solr");
    }

    protected void addFields(SolrInputDocument doc, Object... fields)
    {
        for (int i = 0; i < fields.length; i += 2)
        {
            doc.addField((String) (fields[i]), fields[i + 1]);
        }
    }// add random fields to the documet before indexing

    protected void indexr(SolrClient client, boolean andShards, Object... fields) throws Exception
    {
        SolrInputDocument doc = new SolrInputDocument();
        addFields(doc, fields);
        addFields(doc, "rnd_b", true);
        addRandFields(doc);
        indexDoc(client, andShards, doc);
    }

    protected SolrInputDocument addRandFields(SolrInputDocument sdoc)
    {
        addFields(sdoc, getRandFields(getFieldNames(), getRandValues()));
        return sdoc;
    }

    protected void index(SolrClient client, boolean andShards, Object... fields) throws Exception
    {
        SolrInputDocument doc = new SolrInputDocument();
        addFields(doc, fields);
        indexDoc(client, andShards, doc);
    }

    /**
     * Indexes the document in both the client, and a randomly selected shard
     */
    protected void indexDoc(SolrClient client, boolean andShards, SolrInputDocument doc) throws IOException, SolrServerException
    {
        client.add(doc);
        if (andShards)
        {
            int which = (doc.getField(id).toString().hashCode() & 0x7fffffff) % clientShards.size();
            SolrClient clientShard = clientShards.get(which);
            clientShard.add(doc);
        }
    }

    /**
     * Indexes the document in 2 clients asserting that the respones are equivilent
     */
    protected UpdateResponse indexDoc(SolrClient client1, SolrClient client2, SolrParams params, SolrInputDocument... sdocs)
            throws IOException, SolrServerException
    {
        UpdateResponse controlRsp = add(client1, params, sdocs);
        UpdateResponse specificRsp = add(client2, params, sdocs);
        compareSolrResponses(specificRsp, controlRsp);
        return specificRsp;
    }

    protected UpdateResponse add(SolrClient client, SolrParams params, SolrInputDocument... sdocs)
            throws IOException, SolrServerException
    {
        UpdateRequest ureq = new UpdateRequest();
        ureq.setParams(new ModifiableSolrParams(params));
        for (SolrInputDocument sdoc : sdocs)
        {
            ureq.add(sdoc);
        }
        return ureq.process(client);
    }

    protected UpdateResponse del(SolrClient client, SolrParams params, Object... ids)
            throws IOException, SolrServerException
    {
        UpdateRequest ureq = new UpdateRequest();
        ureq.setParams(new ModifiableSolrParams(params));
        for (Object id : ids)
        {
            ureq.deleteById(id.toString());
        }
        return ureq.process(client);
    }

    protected UpdateResponse delQ(SolrClient client, SolrParams params, String... queries)
            throws IOException, SolrServerException
    {
        UpdateRequest ureq = new UpdateRequest();
        ureq.setParams(new ModifiableSolrParams(params));
        for (String q : queries)
        {
            ureq.deleteByQuery(q);
        }
        return ureq.process(client);
    }

    /**
     * Deletes from the specified client, and optionally all shards
     * @param q
     * @param client
     * @param andShards
     * @throws Exception
     */
    protected void del(String q, SolrClient client, boolean andShards) throws Exception
    {
        client.deleteByQuery(q);
        if (andShards)
        {
            for (SolrClient cshards : clientShards)
            {
                cshards.deleteByQuery(q);
            }
        }

    }// serial commit...

    /**
     * * Commits to the specified client, and optionally all shards
     * @param client
     * @param andShards
     * @throws Exception
     */
    protected void commit(SolrClient client, boolean andShards) throws Exception
    {
        client.commit();
        if (andShards)
        {
            for (SolrClient cshards : clientShards)
            {
                cshards.commit();
            }
        }

    }

    protected QueryResponse queryServer(ModifiableSolrParams params) throws SolrServerException, IOException
    {
        // query a random server
        int which = r.nextInt(clientShards.size());
        SolrClient client = clientShards.get(which);
        QueryResponse rsp = client.query(params);
        return rsp;
    }

    protected QueryResponse query(SolrClient solrClient, boolean andShards, String json, ModifiableSolrParams params) throws Exception
    {
        params.set("distrib", "false");
        QueryRequest request = getAlfrescoRequest(json, params);
        QueryResponse controlRsp = request.process(solrClient);
        validateResponse(controlRsp);
        if (andShards)
        {
            params.remove("distrib");
            setDistributedParams(params);
            QueryResponse rsp = queryServer(json, params);
            System.out.println("Cluster Response:"+rsp);
            System.out.println("Control Response:"+controlRsp);
            compareResponses(rsp, controlRsp);
            return rsp;
        }
        else
        {
            return controlRsp;
        }
    }

    protected QueryResponse queryServer(String json, SolrParams params) throws SolrServerException, IOException
    {
        // query a random server
        int which = r.nextInt(clientShards.size());
        SolrClient client = clientShards.get(which);
        QueryRequest request = getAlfrescoRequest(json, params);
        return request.process(client);
    }

    protected QueryRequest getAlfrescoRequest(String json, SolrParams params) {
        QueryRequest request = new AlfrescoStreamHandler.AlfrescoQueryRequest(json, params);
        request.setMethod(SolrRequest.METHOD.POST);
        return request;
    }

    /**
     * Returns the QueryResponse from {@link #queryServer}
     */
    protected QueryResponse query(SolrClient solrClient, boolean setDistribParams, SolrParams p) throws Exception
    {

        final ModifiableSolrParams params = new ModifiableSolrParams(p);

        // TODO: look into why passing true causes fails
        params.set("distrib", "false");
        final QueryResponse controlRsp = solrClient.query(params);
        validateResponse(controlRsp);

        params.remove("distrib");
        if (setDistribParams)
            setDistributedParams(params);

        QueryResponse rsp = queryServer(params);

        compareResponses(rsp, controlRsp);

        if (stress > 0)
        {
            log.info("starting stress...");
            Thread[] threads = new Thread[nThreads];
            for (int i = 0; i < threads.length; i++)
            {
                threads[i] = new Thread()
                {
                    @Override
                    public void run()
                    {
                        for (int j = 0; j < stress; j++)
                        {
                            int which = r.nextInt(clientShards.size());
                            SolrClient client = clientShards.get(which);
                            try
                            {
                                QueryResponse rsp = client.query(new ModifiableSolrParams(params));
                                if (verifyStress)
                                {
                                    compareResponses(rsp, controlRsp);
                                }
                            } catch (SolrServerException | IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };
                threads[i].start();
            }

            for (Thread thread : threads)
            {
                thread.join();
            }
        }
        return rsp;
    }

    public QueryResponse queryAndCompare(SolrParams params, SolrClient... clients)
            throws SolrServerException, IOException
    {
        return queryAndCompare(params, Arrays.<SolrClient> asList(clients));
    }

    public QueryResponse queryAndCompare(SolrParams params, Iterable<SolrClient> clients)
            throws SolrServerException, IOException
    {
        QueryResponse first = null;
        for (SolrClient client : clients)
        {
            QueryResponse rsp = client.query(new ModifiableSolrParams(params));
            if (first == null)
            {
                first = rsp;
            } else
            {
                compareResponses(first, rsp);
            }
        }

        return first;
    }

    public static boolean eq(String a, String b)
    {
        return a == b || (a != null && a.equals(b));
    }

    public static int flags(Map<String, Integer> handle, Object key)
    {
        if (handle == null)
            return 0;
        Integer f = handle.get(key);
        return f == null ? 0 : f;
    }

    public static String compare(NamedList a, NamedList b, int flags, Map<String, Integer> handle)
    {
        // System.out.println("resp a:" + a);
        // System.out.println("resp b:" + b);
        boolean ordered = (flags & UNORDERED) == 0;

        if (!ordered)
        {
            Map mapA = new HashMap(a.size());
            for (int i = 0; i < a.size(); i++)
            {
                Object prev = mapA.put(a.getName(i), a.getVal(i));
            }

            Map mapB = new HashMap(b.size());
            for (int i = 0; i < b.size(); i++)
            {
                Object prev = mapB.put(b.getName(i), b.getVal(i));
            }

            return compare(mapA, mapB, flags, handle);
        }

        int posa = 0, posb = 0;
        int aSkipped = 0, bSkipped = 0;

        for (;;)
        {
            if (posa >= a.size() && posb >= b.size())
            {
                break;
            }

            String namea = null, nameb = null;
            Object vala = null, valb = null;

            int flagsa = 0, flagsb = 0;
            while (posa < a.size())
            {
                namea = a.getName(posa);
                vala = a.getVal(posa);
                posa++;
                flagsa = flags(handle, namea);
                if ((flagsa & SKIP) != 0)
                {
                    namea = null;
                    vala = null;
                    aSkipped++;
                    continue;
                }
                break;
            }

            while (posb < b.size())
            {
                nameb = b.getName(posb);
                valb = b.getVal(posb);
                posb++;
                flagsb = flags(handle, nameb);
                if ((flagsb & SKIP) != 0)
                {
                    nameb = null;
                    valb = null;
                    bSkipped++;
                    continue;
                }
                if (eq(namea, nameb))
                {
                    break;
                }
                return "." + namea + "!=" + nameb + " (unordered or missing)";
                // if unordered, continue until we find the right field.
            }

            // ok, namea and nameb should be equal here already.
            if ((flagsa & SKIPVAL) != 0)
                continue; // keys matching is enough

            String cmp = compare(vala, valb, flagsa, handle);
            if (cmp != null)
                return "." + namea + cmp;
        }

        if (a.size() - aSkipped != b.size() - bSkipped)
        {
            return ".size()==" + a.size() + "," + b.size() + " skipped=" + aSkipped + "," + bSkipped;
        }

        return null;
    }

    public static String compare1(Map a, Map b, int flags, Map<String, Integer> handle)
    {
        String cmp;

        for (Object keya : a.keySet())
        {
            Object vala = a.get(keya);
            int flagsa = flags(handle, keya);
            if ((flagsa & SKIP) != 0)
                continue;
            if (!b.containsKey(keya))
            {
                return "[" + keya + "]==null";
            }
            if ((flagsa & SKIPVAL) != 0)
                continue;
            Object valb = b.get(keya);
            cmp = compare(vala, valb, flagsa, handle);
            if (cmp != null)
                return "[" + keya + "]" + cmp;
        }
        return null;
    }

    public static String compare(Map a, Map b, int flags, Map<String, Integer> handle)
    {
        String cmp;
        cmp = compare1(a, b, flags, handle);
        if (cmp != null)
            return cmp;
        return compare1(b, a, flags, handle);
    }

    public static String compare(SolrDocument a, SolrDocument b, int flags, Map<String, Integer> handle)
    {
        return compare(a.getFieldValuesMap(), b.getFieldValuesMap(), flags, handle);
    }

    public static String compare(SolrDocumentList a, SolrDocumentList b, int flags, Map<String, Integer> handle)
    {
        boolean ordered = (flags & UNORDERED) == 0;

        String cmp;
        int f = flags(handle, "maxScore");
        if (f == 0)
        {
            cmp = compare(a.getMaxScore(), b.getMaxScore(), 0, handle);
            if (cmp != null)
                return ".maxScore" + cmp;
        } else if ((f & SKIP) == 0)
        { // so we skip val but otherwise both should be present
            assert (f & SKIPVAL) != 0;
            if (b.getMaxScore() != null)
            {
                if (a.getMaxScore() == null)
                {
                    return ".maxScore missing";
                }
            }
        }

        cmp = compare(a.getNumFound(), b.getNumFound(), 0, handle);
        if (cmp != null)
            return ".numFound" + cmp;

        cmp = compare(a.getStart(), b.getStart(), 0, handle);
        if (cmp != null)
            return ".start" + cmp;

        cmp = compare(a.size(), b.size(), 0, handle);
        if (cmp != null)
            return ".size()" + cmp;

        // only for completely ordered results (ties might be in a different
        // order)
        if (ordered)
        {
            for (int i = 0; i < a.size(); i++)
            {
                cmp = compare(a.get(i), b.get(i), 0, handle);
                if (cmp != null)
                    return "[" + i + "]" + cmp;
            }
            return null;
        }

        // unordered case
        for (int i = 0; i < a.size(); i++)
        {
            SolrDocument doc = a.get(i);
            Object key = doc.getFirstValue("id");
            SolrDocument docb = null;
            if (key == null)
            {
                // no id field to correlate... must compare ordered
                docb = b.get(i);
            } else
            {
                for (int j = 0; j < b.size(); j++)
                {
                    docb = b.get(j);
                    if (key.equals(docb.getFirstValue("id")))
                        break;
                }
            }
            // if (docb == null) return "[id="+key+"]";
            cmp = compare(doc, docb, 0, handle);
            if (cmp != null)
                return "[id=" + key + "]" + cmp;
        }
        return null;
    }

    public static String compare(Object[] a, Object[] b, int flags, Map<String, Integer> handle)
    {
        if (a.length != b.length)
        {
            return ".length:" + a.length + "!=" + b.length;
        }
        for (int i = 0; i < a.length; i++)
        {
            String cmp = compare(a[i], b[i], flags, handle);
            if (cmp != null)
                return "[" + i + "]" + cmp;
        }
        return null;
    }

    public static String compare(Object a, Object b, int flags, Map<String, Integer> handle)
    {
        if (a == b)
            return null;
        if (a == null || b == null)
            return ":" + a + "!=" + b;

        if (a instanceof NamedList && b instanceof NamedList)
        {
            return compare((NamedList) a, (NamedList) b, flags, handle);
        }

        if (a instanceof SolrDocumentList && b instanceof SolrDocumentList)
        {
            return compare((SolrDocumentList) a, (SolrDocumentList) b, flags, handle);
        }

        if (a instanceof SolrDocument && b instanceof SolrDocument)
        {
            return compare((SolrDocument) a, (SolrDocument) b, flags, handle);
        }

        if (a instanceof Map && b instanceof Map)
        {
            return compare((Map) a, (Map) b, flags, handle);
        }

        if (a instanceof Object[] && b instanceof Object[])
        {
            return compare((Object[]) a, (Object[]) b, flags, handle);
        }

        if (a instanceof byte[] && b instanceof byte[])
        {
            if (!Arrays.equals((byte[]) a, (byte[]) b))
            {
                return ":" + a + "!=" + b;
            }
            return null;
        }

        if (a instanceof List && b instanceof List)
        {
            return compare(((List) a).toArray(), ((List) b).toArray(), flags, handle);

        }

        if ((flags & FUZZY) != 0)
        {
            if ((a instanceof Double && b instanceof Double))
            {
                double aaa = ((Double) a).doubleValue();
                double bbb = ((Double) b).doubleValue();
                if (aaa == bbb || ((Double) a).isNaN() && ((Double) b).isNaN())
                {
                    return null;
                }
                if ((aaa == 0.0) || (bbb == 0.0))
                {
                    return ":" + a + "!=" + b;
                }

                double diff = Math.abs(aaa - bbb);
                // When stats computations are done on multiple shards, there
                // may
                // be small differences in the results. Allow a small difference
                // between the result of the computations.

                double ratio = Math.max(Math.abs(diff / aaa), Math.abs(diff / bbb));
                if (ratio > DOUBLE_RATIO_LIMIT)
                {
                    return ":" + a + "!=" + b;
                } else
                {
                    return null;// close enough.
                }
            }
        }

        if (!(a.equals(b)))
        {
            return ":" + a + "!=" + b;
        }

        return null;
    }

    protected void compareSolrResponses(SolrResponse a, SolrResponse b)
    {
        // SOLR-3345: Checking QTime value can be skipped as there is no
        // guarantee that the numbers will match.
        handle.put("QTime", SKIPVAL);
        String cmp = compare(a.getResponse(), b.getResponse(), flags, handle);
        if (cmp != null)
        {
            log.error("Mismatched responses:\n" + a + "\n" + b);
            Assert.fail(cmp);
        }
    }

    protected void compareResponses(QueryResponse a, QueryResponse b)
    {
        if (System.getProperty("remove.version.field") != null)
        {
            // we don't care if one has a version and the other doesnt -
            // control vs distrib
            // TODO: this should prob be done by adding an ignore on _version_
            // rather than mutating the responses?
            if (a.getResults() != null)
            {
                for (SolrDocument doc : a.getResults())
                {
                    doc.removeFields("_version_");
                }
            }
            if (b.getResults() != null)
            {
                for (SolrDocument doc : b.getResults())
                {
                    doc.removeFields("_version_");
                }
            }
        }
        compareSolrResponses(a, b);
    }

    public static Object[] getRandFields(String[] fields, RandVal[] randVals)
    {
        Object[] o = new Object[fields.length * 2];
        for (int i = 0; i < fields.length; i++)
        {
            o[i * 2] = fields[i];
            o[i * 2 + 1] = randVals[i].uval();
        }
        return o;
    }

    /**
     * Validates that solr instance is running and that query was processed.
     * @param response
     */
    public void validateResponse(QueryResponse response)
    {
        switch (response.getStatus())
        {
        case 500:
            throw new RuntimeException("Solr instance internal server error 500");

        default:
            break;
        }
    }

    public static abstract class RandVal
    {
        public static Set uniqueValues = new HashSet();

        public abstract Object val();

        public Object uval()
        {
            for (;;)
            {
                Object v = val();
                if (uniqueValues.add(v))
                    return v;
            }
        }
    }

    public static class RandDate extends RandVal
    {
        @Override
        public Object val()
        {
            long v = r.nextLong();
            Date d = new Date(v);
            return d.toInstant().toString();
        }
    }

    protected String getSolrXml()
    {
        return "solr.xml";
    }

    /**
     * Given a directory that will be used as the SOLR_HOME for a jetty
     * instance, seeds that directory with the contents of {@link #getTestFilesHome}
     * and ensures that the proper {@link #getSolrXml} file is in place.
     */
    protected void seedSolrHome(Path jettyHome) throws IOException
    {
        String solrxml = getSolrXml();
        if (solrxml != null)
        {
            FileUtils.copyFile(new File(getTestFilesHome(), solrxml), jettyHome.resolve(getSolrXml()).toFile());
        }
        //Add solr home conf folder with alfresco based configuration.
        FileUtils.copyDirectory(new File(getTestFilesHome() + "/conf"), jettyHome.resolve("conf").toFile());
        // Add alfresco data model def
        FileUtils.copyDirectory(new File(getTestFilesHome() + "/alfrescoModels"), jettyHome.resolve("alfrescoModels").toFile());
        // Add templates
        FileUtils.copyDirectory(new File(getTestFilesHome() + "/templates"), jettyHome.resolve("templates").toFile());
        //add solr alfresco properties
        FileUtils.copyFile(new File(getTestFilesHome() + "/log4j-solr.properties"), jettyHome.resolve("log4j-solr.properties").toFile());
        
    }

    /**
     * Given a directory that will be used as the <code>coreRootDirectory</code>
     * for a jetty instance, Creates a core directory named
     * {@link #DEFAULT_TEST_CORENAME} using a trivial
     * <code>core.properties</code> if this file does not already exist.
     *
     * @see #writeCoreProperties(Path,String)
     * @see #CORE_PROPERTIES_FILENAME
     */
    private void seedCoreDir(String coreName, Path coreSourceConfig, Path coreDirectory) throws IOException
    {
        //Prepare alfresco solr core.
        Path confDir = coreDirectory.resolve("conf");
        confDir.toFile().mkdirs();
        if (Files.notExists(coreDirectory.resolve(CORE_PROPERTIES_FILENAME)))
        {
            Properties coreProperties = new Properties();
            coreProperties.setProperty("name", coreName);
            writeCoreProperties(coreDirectory, coreProperties, this.getTestName());
        } // else nothing to do, DEFAULT_TEST_CORENAME already exists
        //Add alfresco solr configurations
        FileUtils.copyDirectory(coreSourceConfig.resolve("conf").toFile(), confDir.toFile());
    }


    /**
     * Puts default values for handle
     */
    protected void putHandleDefaults() {
        handle.put("explain", SKIPVAL);
        handle.put("timestamp", SKIPVAL);
        handle.put("score", SKIPVAL);
        handle.put("wt", SKIP);
        handle.put("distrib", SKIP);
        handle.put("shards.qt", SKIP);
        handle.put("shards", SKIP);
        handle.put("q", SKIP);
        handle.put("maxScore", SKIPVAL);
        handle.put("_version_", SKIP);
        handle.put("_original_parameters_", SKIP);
    }


    protected void setupJettySolrHome(String coreName, Path jettyHome) throws IOException
    {
        seedSolrHome(jettyHome);

        Properties coreProperties = new Properties();
        coreProperties.setProperty("name", coreName);
        coreProperties.setProperty("shard", "${shard:}");
        coreProperties.setProperty("collection", "${collection:"+coreName+"}");
        coreProperties.setProperty("config", "${solrconfig:solrconfig.xml}");
        coreProperties.setProperty("schema", "${schema:schema.xml}");
        coreProperties.setProperty("coreNodeName", "${coreNodeName:}");

        writeCoreProperties(jettyHome.resolve("cores").resolve(coreName), coreProperties, coreName);
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

    /**
     * Calls the Admin handler with an action.
     * @param coreAdminHandler
     * @param testingCore
     * @param action
     * @return
     */
    protected SolrQueryResponse callHandler(AlfrescoCoreAdminHandler coreAdminHandler, SolrCore testingCore, String action) {
        SolrQueryRequest request = new LocalSolrQueryRequest(testingCore,
                params(CoreAdminParams.ACTION, action, CoreAdminParams.CORE, testingCore.getName()));
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        return response;
    }

    /**
     * A JUnit Rule to setup Jetty
     */
    public class JettyServerRule extends ExternalResource
    {

        final String serverName;
        final String[] coreNames;
        final int numShards;
        final Properties solrcoreProperties;

        /**
         * Creates the jetty servers
         * @param serverName The key to use for the Jetty server name
         * @param numShards Number of shards required
         * @param solrcoreProperties Additional properties to add to the solrcore.properties
         * @param coreNames names of the core config folders
         */
        public JettyServerRule(String serverName, int numShards, Properties solrcoreProperties, String ...coreNames)
        {
            this.serverName = serverName;
            this.coreNames = coreNames == null?new String[0]:coreNames;
            this.numShards = numShards;
            this.solrcoreProperties = solrcoreProperties;
        }

        /**
         * Creates the jetty servers with the specified number of shards and sensible defaults.
         * Please use the other constructor by passing in the class instance as well.
         * @param numShards
         */
        private JettyServerRule(int numShards)
        {
            this.serverName = DEFAULT_TEST_CORENAME;
            coreNames = new String[]{DEFAULT_TEST_CORENAME};
            this.numShards = numShards;
            this.solrcoreProperties = new Properties();
        }

        /**
         * Creates the jetty servers with the specified number of shards and sensible defaults.
         * @param numShards
         */
        public JettyServerRule(int numShards, AbstractAlfrescoDistributedTest testClass)
        {
            this.serverName = testClass.getClass().getSimpleName();
            coreNames = new String[]{DEFAULT_TEST_CORENAME};
            this.numShards = numShards;
            this.solrcoreProperties = new Properties();
        }
        /**
         * Creates the jetty servers with the specified number of shards and sensible defaults.
         * @param numShards
         */
        public JettyServerRule(int numShards, AbstractAlfrescoDistributedTest testClass, Properties solrcoreProperties)
        {
            this.serverName = testClass.getClass().getSimpleName();
            coreNames = new String[]{DEFAULT_TEST_CORENAME};
            this.numShards = numShards;
            this.solrcoreProperties = solrcoreProperties;
        }

        @Override
        protected void before() throws Throwable
        {
            distribSetUp(serverName);
            RandVal.uniqueValues = new HashSet(); // reset random values
            createServers(serverName, coreNames, numShards,solrcoreProperties);
        }

        @Override
        protected void after()
        {

            try
            {
                destroyServers();
                distribTearDown();

                boolean keepTests = Boolean.valueOf(System.getProperty("keep.tests"));
                if (!keepTests) FileUtils.deleteDirectory(testDir);
            }
            catch (Exception e)
            {
                log.error("Failed to shutdown test properly ", e);
            }
        }
    }

    /**
     * Creates a Jetty instance with the default "alfresco" core that uses the production rerank template.
     * There is only 1 shard.
     */
    public class DefaultAlrescoCoreRule extends JettyServerRule
    {
        SolrCore defaultCore;
        SolrClient defaultClient;
        Properties properties;

        public DefaultAlrescoCoreRule(String serverName, Properties properties) {
            super(serverName, 0, null, null);
            this.properties = properties;
        }

        @Override
        protected void before() throws Throwable {
            super.before();

            JettySolrRunner jsr = jettyContainers.get(serverName);
            CoreContainer coreContainer = jsr.getCoreContainer();
            AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
            assertNotNull(coreAdminHandler);
            String[] extras = null;
            if ((properties != null) && !properties.isEmpty())
            {
                int i = 0;
                extras = new String[properties.size()*2];
                for (Map.Entry<Object, Object> prop:properties.entrySet()) {
                    extras[i++] = "property."+prop.getKey();
                    extras[i++] = (String) prop.getValue();
                }

            }
            defaultCore = createCoreUsingTemplate(coreContainer, coreAdminHandler, "alfresco", "rerank", 1, 1, extras);
            assertNotNull(defaultCore);
            String url = buildUrl(jsr.getLocalPort()) + "/" + "alfresco";
            defaultClient = createNewSolrClient(url);
            assertNotNull(defaultClient);
            jettyClients.put("alfresco", defaultClient);

        }

        public SolrCore getDefaultCore() {
            return defaultCore;
        }

        public SolrClient getDefaultClient() {
            return defaultClient;
        }
    }
}
