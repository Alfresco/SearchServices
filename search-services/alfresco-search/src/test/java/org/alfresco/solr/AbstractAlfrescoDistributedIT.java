package org.alfresco.solr;

import static java.util.Arrays.asList;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;

import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.basics.RandomSupplier;
import org.alfresco.solr.basics.SolrResponsesComparator;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
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
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
 * @since solr 1.4.1
 * @author Michael Suzuki
 * @author Andrea Gazzarini
 */
public abstract class AbstractAlfrescoDistributedIT extends SolrITInitializer
{
    protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    protected String[] deadServers;
    protected static SolrResponsesComparator SOLR_RESPONSE_COMPARATOR = new SolrResponsesComparator();
    protected static RandomSupplier SOLR_RANDOM_SUPPLIER;

    // to stress with higher thread counts and requests, make sure the junit
    // xml formatter is not being used (all output will be buffered before
    // transformation to xml and cause an OOM exception).
    protected static int stress = TEST_NIGHTLY ? 2 : 0;
    protected static boolean verifyStress = true;
    protected static int nThreads = 3;
    protected static String id = "id";
    
    /**
     * Set's the value of the "hostContext" system property to a random path
     * like string (which may or may not contain sub-paths). This is used in the
     * default constructor for this test to help ensure no code paths have
     * hardcoded assumptions about the servlet context used to run solr.
     * <p>
     * Test configs may use the <code>${hostContext}</code> variable to access
     * this system property.
     * </p>
     */
    @BeforeClass
    public static void setUpSolrTestProperties()
    {
        SOLR_RANDOM_SUPPLIER = new RandomSupplier();
        System.setProperty("alfresco.test", "true");
        System.setProperty("solr.tests.maxIndexingThreads", "10");
        System.setProperty("solr.tests.ramBufferSizeMB", "1024");
    }

    protected static void putHandleDefaults()
    {
        SOLR_RESPONSE_COMPARATOR.putHandleDefaults();
    }

    /**
     * This method has the responsibility of checking that each shard has at least count documents
     * @param query - query to execute
     * @param count - min number of results each shard must satisfy
     * @param waitMillis - total ms to wait
     */
    public static boolean checkMinCountPerShard(Query query, int count, long waitMillis) throws SolrServerException,IOException
    {
        long begin = System.currentTimeMillis();
        List<SolrClient> shardedClients = getShardedClients();
        long timeout = begin + waitMillis;
        boolean allShardCompliant = false;

        for (SolrClient singleShard : shardedClients)
        {
            allShardCompliant = false;
            int totalHits;
            int cycles = 1;
            while ((new Date()).getTime() < timeout && (!allShardCompliant))
            {
                QueryResponse response = singleShard.query(luceneToSolrQuery(query));
                totalHits = (int) response.getResults().getNumFound();
                if ((long) totalHits >= count)
                {
                    allShardCompliant = true;
                }
                try
                {
                    Thread.sleep(500 * cycles++);
                }
                catch (InterruptedException e)
                {
                    // Ignore
                }
            }
        }
        return allShardCompliant;
    }

    public static void waitForShardsCount(ModifiableSolrParams query, int count, long waitMillis, long start) throws Exception
    {

        long timeOut = start+waitMillis;
        int totalCount = 0;
        query.set("shards", shards);
        SolrClient clientShard = clientShards.get(0);
        while (System.currentTimeMillis() < timeOut)
        {
            QueryResponse response = clientShard.query(query);
            totalCount = (int) response.getResults().getNumFound();
            if (totalCount == count)
            {
                return;
            }
        }
        throw new Exception("Cluster:Wait error expected "+count+" found "+totalCount+" : "+query.toString());
    }
    
    
    /**
     * Waits until all cores (including shards) reach a count.
     */
    public static void waitForDocCountAllCores(Query query, int count, long waitMillis) throws Exception
    {
        long begin = System.currentTimeMillis();

        for (SolrClient client : getStandaloneAndShardedClients()) {
            waitForDocCountCore(client, luceneToSolrQuery(query), count, waitMillis, begin);
        }
    }

    public void waitForDocCountAllShards(Query query, int count, long waitMillis) throws Exception
    {
        long begin = System.currentTimeMillis();
        for (SolrClient client : getShardedClients()) {
            waitForDocCountCore(client, luceneToSolrQuery(query), count, waitMillis, begin);
        }
    }
    
    /**
     * Delele by query on all Clients
     */
    public static void deleteByQueryAllClients(String q) throws Exception
    {
        List<SolrClient> clients = getStandaloneAndShardedClients();

        for (SolrClient client : clients) {
            client.deleteByQuery(q);
        }
    }

    public static void explicitCommitOnAllClients() throws Exception
    {
        List<SolrClient> clients = getStandaloneAndShardedClients();

        for (SolrClient client : clients) {
            client.commit();
        }
    }

    /**
     * Gets the Default test client.
     */
    protected static SolrClient getDefaultTestClient()
    {
        return solrCollectionNameToStandaloneClient.get(DEFAULT_TEST_CORENAME);
    }

    protected static List<SolrClient> getShardedClients()
    {
        return clientShards;
    }
    
    /**
     * Gets a list of all clients for that test
     *
     * @return list of SolrClient
     */
    public static List<SolrClient> getStandaloneAndShardedClients()
    {
        List<SolrClient> clients = new ArrayList<>();
        clients.addAll(solrCollectionNameToStandaloneClient.values());
        clients.addAll(clientShards);
        return clients;
    }


    public static List<SolrClient> getStandaloneClients()
    {
        return new ArrayList<>(solrCollectionNameToStandaloneClient.values());
    }

    /**
     * Waits for the doc count on the first core available, then checks all the Shards match.
     */
    public static void waitForDocCount(Query query, int count, long waitMillis) throws Exception
    {
        waitForDocCount(luceneToSolrQuery(query), count, waitMillis);
    }

    public static void waitForDocCount(ModifiableSolrParams query, int count, long waitMillis) throws Exception
    {
        long begin = System.currentTimeMillis();
        SolrClient standaloneClient = getStandaloneClients().get(0); //Get the first one
        waitForDocCountCore(standaloneClient, query, count, waitMillis, begin);
        waitForShardsCount(query, count, waitMillis, begin);
    }

    public static void assertShardCount(int shardNumber, Query query, int count) throws Exception
    {
        assertShardCount(shardNumber, luceneToSolrQuery(query), count);
    }

    public static void assertShardCount(int shardNumber, ModifiableSolrParams query, int count) throws Exception
    {
        List<SolrClient> clients = getShardedClients();
        SolrClient client = clients.get(shardNumber);

        QueryResponse response = client.query(query);
        int totalHits = (int) response.getResults().getNumFound();
        if (count != totalHits) {
            throw new Exception("Expecting " + count + " docs on shard " + shardNumber + " , found " + totalHits);
        }
    }

    private static String escapeQueryClause(String query)
    {
        int i = query.lastIndexOf(":");
        if (i == -1){
            return query + " ";
        }
        String field = query.substring(0, i);
        String value = query.substring(i+1);
        String escapedField = escapeQueryChars(field);

        return escapedField + ":" + value + " ";
    }

    protected static String escapeQueryChars(String query)
    {
        return query.replaceAll("\\:", "\\\\:")
            .replaceAll("\\{", "\\\\{")
            .replaceAll("\\}", "\\\\}");
    }
    
    public static SolrQuery luceneToSolrQuery(Query query)
    {
        String[] terms = query.toString().split(" ");
        String escapedQuery = "";
        for (String t : terms)
        {
            escapedQuery += escapeQueryClause(t);
        }
        
        return new SolrQuery("{!lucene}" + escapedQuery);
    }

    /**
     * Waits until all the shards reach the desired count, or errors.
     *
     * @param query the query that will be executed for getting the expected results.
     * @param count the expected results cardinality.
     * @param waitMillis how many msecs the test will wait before one try and another.
     * @param start the current timestamp, in msecs.
     *
     * @throws Exception in case the count check fails.
     */
    public static void waitForShardsCount(Query query, int count, long waitMillis, long start) throws Exception
    {
        SolrQuery solrQuery = luceneToSolrQuery(query);
        waitForShardsCount(solrQuery, count, waitMillis, start);
    }

    /**
     * Gets the cores for the jetty instances
     */
    protected static Collection<SolrCore> getCores(Collection<JettySolrRunner> runners)
    {
        return jettyContainers.values().iterator().next().getCoreContainer().getCores();
    }

    protected static List<AlfrescoCoreAdminHandler> getAdminHandlers(Collection<JettySolrRunner> runners)
    {
        List<AlfrescoCoreAdminHandler> coreAdminHandlers = new ArrayList<>();
        for (JettySolrRunner jettySolrRunner : runners)
        {
            CoreContainer coreContainer = jettySolrRunner.getCoreContainer();
            AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler)  coreContainer.getMultiCoreHandler();
            coreAdminHandlers.add(coreAdminHandler);
        }
        return coreAdminHandlers;
    }
    
    public static int assertNodesPerShardGreaterThan(int count) throws Exception
    {
        return assertNodesPerShardGreaterThan(count, false);
    }

    public static int assertNodesPerShardGreaterThan(int count, boolean ignoreZero) throws Exception
    {
        int shardHit = 0;
        List<SolrClient> clients = getShardedClients();
        SolrQuery query = luceneToSolrQuery(new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_NODE)));
        StringBuilder error = new StringBuilder();
        for (SolrClient client : clients)
        {
            QueryResponse response = client.query(query);
            int totalHits = (int) response.getResults().getNumFound();


            if (totalHits > 0)
            {
                shardHit++;
            }

            if (totalHits < count)
            {
                if (ignoreZero && totalHits == 0) {
                    log.info(client + ": have zero hits ");
                } else {
                    error.append(" " + client + ": ");
                    error.append("Expected nodes per shard greater than " + count + " found " + totalHits + " : " + query.toString());
                }
            }
            log.info(client + ": Hits " + totalHits);

        }

        if (error.length() > 0)
        {
            throw new Exception(error.toString());
        }
        return shardHit;
    }

    public static void assertCountAndColocation(Query query, int count) throws Exception
    {
        assertCountAndColocation(luceneToSolrQuery(query), count);
    }

    public static void assertCountAndColocation(SolrQuery query, int count) throws Exception
    {
        List<SolrClient> clients = getShardedClients();
        int shardHit = 0;
        int totalCount = 0;
        for (SolrClient client : clients)
        {
            QueryResponse response = client.query(query);
            int hits = (int) response.getResults().getNumFound();
            totalCount += hits;
            if (hits > 0) {
                shardHit++;
            }
        }

        if (totalCount != count) {
            throw new Exception(totalCount + " docs found for query: " + query.toString() + " expecting " + count);
        }

        if (shardHit > 1) {
            throw new Exception(shardHit + " shards found data for query: " + query.toString() + " expecting 1");
        }
    }

    public static void assertShardSequence(int shard, Query query, int count) throws Exception
    {
        assertShardSequence(shard, luceneToSolrQuery(query), count);
    }

    public static void assertShardSequence(int shard, SolrQuery query, int count) throws Exception
    {
        List<SolrClient> clients = getShardedClients();
        int totalCount;
        SolrClient client = clients.get(shard);

        QueryResponse response = client.query(query);
        totalCount = (int) response.getResults().getNumFound();

        if(totalCount != count) {
            throw new Exception(totalCount+" docs found for query: "+query.toString()+" expecting "+count);
        }
    }

    public static void waitForDocCountCore(SolrClient client,
                                 ModifiableSolrParams query,
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
            QueryResponse response = client.query(query);
            totalHits = (int) response.getResults().getNumFound();

            if (totalHits == expectedNumFound) {
                return;
            } else {
                Thread.sleep(500 * increment++);
            }

        }
        throw new Exception("Core:Wait error expected "+expectedNumFound+" found "+totalHits+" : "+query.toString());
    }
    

    protected void setDistributedParams(ModifiableSolrParams params)
    {
        params.set("shards", getShardsString());
    }

    protected static List<Tuple> getTuples(TupleStream tupleStream) throws IOException {
        List<Tuple> tuples = new ArrayList<>();
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

    protected String getShardsString()
    {
        Random r = SOLR_RANDOM_SUPPLIER.getRandomGenerator();
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
                List<String> replicas = new ArrayList<>(asList(deadServers));
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
    

    protected static void addFields(SolrInputDocument doc, Object... fields)
    {
        for (int i = 0; i < fields.length; i += 2)
        {
            doc.addField((String) (fields[i]), fields[i + 1]);
        }
    }// add random fields to the documet before indexing

    protected static void index(SolrClient client, int shardId, Object... fields) throws Exception
    {
        SolrInputDocument doc = new SolrInputDocument();
        addFields(doc, fields);
        indexDoc(client, shardId, doc);
    }

    /**
     * Indexes the document in both the client, and a selected shard
     */
    protected static void indexDoc(SolrClient client, int shardId, SolrInputDocument doc)
        throws IOException, SolrServerException
    {
        client.add(doc);
        SolrClient clientShard = clientShards.get(shardId);
        clientShard.add(doc);
    }
    
    protected static void index(SolrClient client, boolean andShards, Object... fields) throws Exception
    {
        SolrInputDocument doc = new SolrInputDocument();
        addFields(doc, fields);
        indexDoc(client, andShards, doc);
    }

    /**
     * Indexes the document in both the client, and a randomly selected shard
     */
    protected static void indexDoc(SolrClient client, boolean andShards, SolrInputDocument doc) throws IOException, SolrServerException
    {
        client.add(doc);
        if (andShards)
        {
            int which = (doc.getField(id).toString().hashCode() & 0x7fffffff) % clientShards.size();
            SolrClient clientShard = clientShards.get(which);
            clientShard.add(doc);
        }
    }

    protected static UpdateResponse add(SolrClient client, SolrParams params, SolrInputDocument... sdocs)
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

    /**
     * * Commits to the specified client, and optionally all shards
     */
    protected static void commit(SolrClient client, boolean andShards) throws Exception
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

    protected static QueryResponse queryRandomShard(ModifiableSolrParams params) throws SolrServerException, IOException
    {
        Random r = SOLR_RANDOM_SUPPLIER.getRandomGenerator();
        int which = r.nextInt(clientShards.size());
        SolrClient client = clientShards.get(which);
        return client.query(params);
    }

    protected QueryResponse query(SolrClient solrClient, boolean andShards, String json, ModifiableSolrParams params) throws Exception
    {
        params.set("distrib", "false");
        QueryRequest request = getAlfrescoRequest(json, params);
        QueryResponse controlRsp = request.process(solrClient);
        SOLR_RESPONSE_COMPARATOR.validateResponse(controlRsp);
        if (andShards)
        {
            params.remove("distrib");
            setDistributedParams(params);
            QueryResponse rsp = queryRandomShard(json, params);
            SOLR_RESPONSE_COMPARATOR.compareResponses(rsp, controlRsp);
            return rsp;
        }
        else
        {
            return controlRsp;
        }
    }

    protected static QueryResponse queryRandomShard(String json, SolrParams params) throws SolrServerException, IOException
    {
        Random r = SOLR_RANDOM_SUPPLIER.getRandomGenerator();
        int which = r.nextInt(clientShards.size());
        SolrClient client = clientShards.get(which);
        QueryRequest request = getAlfrescoRequest(json, params);
        return request.process(client);
    }

    protected static QueryRequest getAlfrescoRequest(String json, SolrParams params) {
        QueryRequest request = new AlfrescoJsonQueryRequest(json, params);
        request.setMethod(SolrRequest.METHOD.POST);
        return request;
    }
    

    /**
     * Returns the QueryResponse from {@link #queryRandomShard}
     */
    protected QueryResponse query(SolrClient solrClient, boolean setDistribParams, SolrParams p) throws Exception
    {
        Random r = SOLR_RANDOM_SUPPLIER.getRandomGenerator();
        final ModifiableSolrParams params = new ModifiableSolrParams(p);

        // TODO: look into why passing true causes fails
        params.set("distrib", "false");
        final QueryResponse controlRsp = solrClient.query(params);
        SOLR_RESPONSE_COMPARATOR.validateResponse(controlRsp);

        params.remove("distrib");
        if (setDistribParams)
            setDistributedParams(params);

        QueryResponse rsp = queryRandomShard(params);

        SOLR_RESPONSE_COMPARATOR.compareResponses(rsp, controlRsp);

        if (stress > 0)
        {
            log.info("starting stress...");
            Thread[] threads = new Thread[nThreads];
            for (int i = 0; i < threads.length; i++)
            {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < stress; j++)
                    {
                        int which = r.nextInt(clientShards.size());
                        SolrClient client = clientShards.get(which);
                        try
                        {
                            QueryResponse rsp1 = client.query(new ModifiableSolrParams(params));
                            if (verifyStress)
                            {
                                SOLR_RESPONSE_COMPARATOR.compareResponses(rsp1, controlRsp);
                            }
                        } catch (SolrServerException | IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads)
            {
                thread.join();
            }
        }
        return rsp;
    }

    public static QueryResponse queryAndCompare(SolrParams params, SolrClient... clients)
            throws SolrServerException, IOException
    {
        return queryAndCompare(params, asList(clients));
    }

    public static QueryResponse queryAndCompare(SolrParams params, Iterable<SolrClient> clients)
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
                SOLR_RESPONSE_COMPARATOR.compareResponses(first, rsp);
            }
        }

        return first;
    }

    public static void indexTransaction(Transaction transaction, List<Node> nodes, List<NodeMetaData> nodeMetaDatas)
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

    public static void indexTransaction(Transaction transaction, List<Node> nodes, List<NodeMetaData> nodeMetaDatas, List<String> content)
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
     */
    protected static SolrQueryResponse callHandler(AlfrescoCoreAdminHandler coreAdminHandler, SolrCore testingCore, String action)
    {
        SolrQueryRequest request = new LocalSolrQueryRequest(testingCore,
                params(CoreAdminParams.ACTION, action, CoreAdminParams.CORE, testingCore.getName()));
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        return response;
    }

    protected static SolrQueryResponse callExpand(AlfrescoCoreAdminHandler coreAdminHandler, SolrCore testingCore, int value)
    {
        SolrQueryRequest request = new LocalSolrQueryRequest(testingCore,
            params(CoreAdminParams.ACTION, "EXPAND",
                  CoreAdminParams.CORE, testingCore.getName(),
                  "add", Integer.toString(value)));
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        return response;
    }

    public static SolrQueryResponse rangeCheck(int shard) throws Exception
    {
        int maxAttemps = 10;
        for (int attemp=0; attemp < maxAttemps; ++attemp)
        {
            Collection<SolrCore> cores = getCores(solrShards);
            List<AlfrescoCoreAdminHandler> alfrescoCoreAdminHandlers = getAdminHandlers(solrShards);
            SolrCore core = cores.stream()
                    .filter(solrcore -> solrcore.getName().equals("shard" + shard)).findAny().orElseThrow(RuntimeException::new);
            AlfrescoCoreAdminHandler alfrescoCoreAdminHandler = alfrescoCoreAdminHandlers.get(shard);
            SolrQueryResponse response = callHandler(alfrescoCoreAdminHandler, core, "RANGECHECK");
            NamedList<?> values = response.getValues();

            boolean isReady = !Optional.ofNullable(values.get("report"))
                        .map(Object::toString)
                        .filter(r -> r.contains("WARNING=The requested endpoint is not available on the slave"))
                        .isPresent() &&
                    !Optional.ofNullable(values.get("exception"))
                        .map(Object::toString)
                        .filter(ex -> ex.contains("not initialized"))
                        .isPresent();

            if (!isReady)
            {
                Thread.sleep(1000);
            }
            else
            {
                return response;
            }
        }

        throw new Exception("impossible to perform rangeChack");
    }

    public static SolrQueryResponse expand(int shard, int value)
    {
        Collection<SolrCore> cores = getCores(solrShards);
        List<AlfrescoCoreAdminHandler> alfrescoCoreAdminHandlers = getAdminHandlers(solrShards);
        SolrCore core = cores.stream()
                .filter(solrcore -> solrcore.getName().equals("shard" + shard)).findAny().orElseThrow(RuntimeException::new);

//        SolrCore core = cores.get(shard);
        AlfrescoCoreAdminHandler alfrescoCoreAdminHandler = alfrescoCoreAdminHandlers.get(shard);
        return callExpand(alfrescoCoreAdminHandler, core, value);
    }

    public static class BasicAuthFilter implements Filter
    {
        @Override
        public void init(FilterConfig config)
        {
            // Nothing to be done here
        }

        public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException
        {
            //Parse the basic auth filter
            String auth = ((HttpServletRequest)request).getHeader("Authorization");
            if(auth != null)
            {
                auth = auth.replace("Basic ", "");
                byte[] bytes = Base64.getDecoder().decode(auth);
                String decodedBytes = new String(bytes);
                String[] pair = decodedBytes.split(":");
                String user = pair[0];
                String password = pair[1];
                //Just look for the hard coded user and password.
                if (user.equals("test") && password.equals("pass"))
                {
                    filterChain.doFilter(request, response);
                }
                else
                {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            }
            else
            {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        }

        public void destroy()
        {
            // Nothing to be done here
        }
    }
}