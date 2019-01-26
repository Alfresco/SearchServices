package org.alfresco.solr;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import org.alfresco.solr.basics.RandomSupplier;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.apache.commons.io.FileUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.embedded.SSLConfig;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.BeforeClass;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;

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
@ThreadLeakLingering(linger = 5000)
public abstract class SolrTestInitializer extends SolrTestCaseJ4
{
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private static AtomicInteger nodeCnt;
    protected static boolean useExplicitNodeNames;

    public static Properties DEFAULT_CORE_PROPS = new Properties();

    protected static String currentTestName;
    protected static Map<String, JettySolrRunner> jettyContainers;
    protected static Map<String, SolrClient> solrCollectionNameToStandaloneClient;
    protected static List<JettySolrRunner> solrShards;
    protected static List<SolrClient> clientShards;
    protected static String shards;
    protected static String[] shardsArr;
    protected static File testDir;
    
    //Standalone Tests
    protected static SolrCore defaultCore;
 
    protected static final int clientConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;;
    protected static final int clientSoTimeout = 90000;;

    protected static final String id = "id";

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
        DEFAULT_CORE_PROPS.setProperty("alfresco.commitInterval", "1000");
        DEFAULT_CORE_PROPS.setProperty("alfresco.newSearcherInterval", "2000");
        
        System.setProperty("alfresco.test", "true");
        System.setProperty("solr.tests.maxIndexingThreads", "10");
        System.setProperty("solr.tests.ramBufferSizeMB", "1024");
        
        testDir = new File(System.getProperty("user.dir") + "/target/jettys");
    }
    
    public static void initSolrServers(int numShards, String testClassName, Properties solrcoreProperties) throws Throwable
    {
        clientShards = new ArrayList<>();
        solrShards = new ArrayList<>();
        solrCollectionNameToStandaloneClient = new HashMap<>();
        jettyContainers = new HashMap<>();
        
        nodeCnt = new AtomicInteger(0);
        String serverName = testClassName;
        currentTestName = serverName;
        String[] coreNames = new String[]{DEFAULT_TEST_CORENAME};
        
        distribSetUp(serverName);
        RandomSupplier.RandVal.uniqueValues = new HashSet(); // reset random values
        createServers(serverName, coreNames, numShards,solrcoreProperties);
    }

    public static void initSingleSolrServer(String testClassName, Properties solrcoreProperties) throws Throwable {
        initSolrServers(0,testClassName,solrcoreProperties);
        
        JettySolrRunner jsr = jettyContainers.get(testClassName);
        CoreContainer coreContainer = jsr.getCoreContainer();
        AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
        assertNotNull(coreAdminHandler);
        String[] extras = null;
        if ((solrcoreProperties != null) && !solrcoreProperties.isEmpty())
        {
            int i = 0;
            extras = new String[solrcoreProperties.size()*2];
            for (Map.Entry<Object, Object> prop:solrcoreProperties.entrySet()) {
                extras[i++] = "property."+prop.getKey();
                extras[i++] = (String) prop.getValue();
            }

        }
        defaultCore = createCoreUsingTemplate(coreContainer, coreAdminHandler, "alfresco", "rerank", 1, 1, extras);
        assertNotNull(defaultCore);
        String url = buildUrl(jsr.getLocalPort()) + "/" + "alfresco";
        SolrClient standaloneClient = createNewSolrClient(url);
        assertNotNull(standaloneClient);
        solrCollectionNameToStandaloneClient.put("alfresco", standaloneClient);
    }

    public static void dismissSolrServers()
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
    
    /**
     * Subclasses can override this to change a test's solr home (default is in
     * test-files)
     */
    public static String getTestFilesHome()
    {
        return System.getProperty("user.dir") + "/target/test-classes/test-files";
    }
    
    public static void distribSetUp(String serverName) throws Exception
    {
        SolrTestCaseJ4.resetExceptionIgnores(); // ignore anything with
                                                // ignore_exception in it
        System.setProperty("solr.test.sys.prop1", "propone");
        System.setProperty("solr.test.sys.prop2", "proptwo");
        System.setProperty("solr.directoryFactory", "org.apache.solr.core.MockDirectoryFactory");
        System.setProperty("solr.log.dir", testDir.toPath().resolve(serverName).toString());
    }

    public static void distribTearDown() throws Exception
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
     * Creates a JettySolrRunner (if one didn't exist already). DOES NOT START IT.
     * @return
     * @throws Exception
     */
    private static JettySolrRunner createJetty(String jettyKey, boolean basicAuth) throws Exception
    {
        if (jettyContainers.containsKey(jettyKey))
        {
            return jettyContainers.get(jettyKey);
        }
        else
        {
            Path jettySolrHome = testDir.toPath().resolve(jettyKey);
            seedSolrHome(jettySolrHome);
            JettySolrRunner jetty = createJetty(jettySolrHome.toFile(), null, null, false, getSchemaFile(), basicAuth);
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
    private static void addCoreToJetty(String jettyKey, String sourceConfigName, String coreName, Properties additionalProperties) throws Exception
    {
        Path jettySolrHome = testDir.toPath().resolve(jettyKey);
        Path coreSourceConfig = new File(getTestFilesHome() + "/"+sourceConfigName).toPath();
        Path coreHome = jettySolrHome.resolve(coreName);
        seedCoreDir(coreName, coreSourceConfig, coreHome);
        updateSolrCoreProperties(coreHome, additionalProperties);
    }


    private static void updateSolrCoreProperties(Path coreHome, Properties additionalProperties) throws IOException
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
    private static void startJetty(JettySolrRunner jsr) throws Exception {
        if (!jsr.isRunning())
        {
            jsr.start();
        }
    }

    protected static void createServers(String jettyKey, String[] coreNames, int numShards, Properties additionalProperties) throws Exception
    {
        boolean basicAuth = additionalProperties != null ? Boolean.parseBoolean(additionalProperties.getProperty("BasicAuth", "false")) : false;

        JettySolrRunner jsr =  createJetty(jettyKey, basicAuth);
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
            solrCollectionNameToStandaloneClient.put(coreNames[i], createNewSolrClient(url));
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

            if("DB_ID_RANGE".equalsIgnoreCase(props.getProperty("shard.method")))
            {
                props.put("shard.range", ranges[i]);
            }

            String shardKey = jettyKey+"_shard_"+i;
            JettySolrRunner j =  createJetty(shardKey, basicAuth);
            //use the first corename specified as the Share template
            addCoreToJetty(shardKey, coreNames[0], shardname, props);
            solrShards.add(j);
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

    protected static void destroyServers() throws Exception
    {
        List<String> solrHomes = new ArrayList<String>();
        for (JettySolrRunner jetty : jettyContainers.values())
        {
            solrHomes.add(jetty.getSolrHome());
            jetty.stop();
        }
        for (SolrClient jClients : solrCollectionNameToStandaloneClient.values())
        {
            jClients.close();
        }

        for (JettySolrRunner jetty : solrShards)
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
        solrShards.clear();
        jettyContainers.clear();
        solrCollectionNameToStandaloneClient.clear();
    }

    public static JettySolrRunner createJetty(File solrHome, String dataDir, String shardList, boolean sslEnabled, String schemaOverride, boolean basicAuth) throws Exception
    {
        return createJetty(solrHome, dataDir, shardList, sslEnabled, schemaOverride, useExplicitNodeNames, basicAuth);
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
    public static JettySolrRunner createJetty(File solrHome, String dataDir, String shardList, boolean sslEnabled,
            String schemaOverride, boolean explicitCoreNodeName, boolean basicAuth) throws Exception
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

        JettyConfig config = null;

        if(basicAuth) {
            System.out.println("###### adding basic auth ######");
            config = JettyConfig.builder().setContext("/solr").withFilter(BasicAuthFilter.class, "/sql/*").stopAtShutdown(true).withSSLConfig(sslConfig).build();
        } else {
            System.out.println("###### no basic auth ######");
            config = JettyConfig.builder().setContext("/solr").stopAtShutdown(true).withSSLConfig(sslConfig).build();
        }

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

    protected static SolrClient createNewSolrClient(String url)
    {
        try
        {
            // setUpSolrTestProperties the client...
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

    protected static String buildUrl(int port)
    {
        return buildUrl(port, "/solr");
    }

    protected static String getSolrXml()
    {
        return "solr.xml";
    }

    /**
     * Given a directory that will be used as the SOLR_HOME for a jetty
     * instance, seeds that directory with the contents of {@link #getTestFilesHome}
     * and ensures that the proper {@link #getSolrXml} file is in place.
     */
    protected static void seedSolrHome(Path jettyHome) throws IOException
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
    private static void seedCoreDir(String coreName, Path coreSourceConfig, Path coreDirectory) throws IOException
    {
        //Prepare alfresco solr core.
        Path confDir = coreDirectory.resolve("conf");
        confDir.toFile().mkdirs();
        if (Files.notExists(coreDirectory.resolve(CORE_PROPERTIES_FILENAME)))
        {
            Properties coreProperties = new Properties();
            coreProperties.setProperty("name", coreName);
            writeCoreProperties(coreDirectory, coreProperties, currentTestName);
        } // else nothing to do, DEFAULT_TEST_CORENAME already exists
        //Add alfresco solr configurations
        FileUtils.copyDirectory(coreSourceConfig.resolve("conf").toFile(), confDir.toFile());
    }

    protected void setupJettySolrHome(String coreName, Path jettyHome) throws IOException
    {
        seedSolrHome(jettyHome);

        Properties coreProperties = new Properties();
        coreProperties.setProperty("name", coreName);
        coreProperties.setProperty("shard", "${shard:}");
        coreProperties.setProperty("collection", "${collection:" + coreName + "}");
        coreProperties.setProperty("config", "${solrconfig:solrconfig.xml}");
        coreProperties.setProperty("schema", "${schema:schema.xml}");
        coreProperties.setProperty("coreNodeName", "${coreNodeName:}");

        writeCoreProperties(jettyHome.resolve("cores").resolve(coreName), coreProperties, coreName);
    }

    public static class BasicAuthFilter implements Filter {

        public BasicAuthFilter() {

        }

        public void init(FilterConfig config) {

        }

        public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException  {
            //Parse the basic auth filter
            String auth = ((HttpServletRequest)request).getHeader("Authorization");
            if(auth != null) {
                auth = auth.replace("Basic ", "");
                byte[] bytes = Base64.getDecoder().decode(auth);
                String decodedBytes = new String(bytes);
                String[] pair = decodedBytes.split(":");
                String user = pair[0];
                String password = pair[1];
                //Just look for the hard coded user and password.
                if (user.equals("test") && password.equals("pass")) {
                    filterChain.doFilter(request, response);
                } else {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            } else {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        }

        public void destroy() {

        }
    }
}
