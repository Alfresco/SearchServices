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
package org.alfresco.solr.tracker;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.repo.index.shard.ShardStateBuilder;
import org.alfresco.repo.search.impl.QueryParserUtils;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.BoundedDeque;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.NodeReport;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.client.GetNodesParameters;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Node.SolrApiNodeStatus;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.client.Transactions;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This tracks two things: transactions and metadata nodes
 * @author Ahmed Owianå
 */
public class MetadataTracker extends AbstractTracker implements Tracker
{

    protected final static Logger log = LoggerFactory.getLogger(MetadataTracker.class);
    private static final int DEFAULT_TRANSACTION_DOCS_BATCH_SIZE = 100;
    private static final int DEFAULT_NODE_BATCH_SIZE = 10;
    private int transactionDocsBatchSize = DEFAULT_TRANSACTION_DOCS_BATCH_SIZE;
    private int nodeBatchSize = DEFAULT_NODE_BATCH_SIZE;
    private int maxTransactionDocumentIdCacheSize = 700000;
    private ConcurrentLinkedQueue<Long> transactionsToReindex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> transactionsToIndex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> transactionsToPurge = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> nodesToReindex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> nodesToIndex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> nodesToPurge = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<String> queriesToReindex = new ConcurrentLinkedQueue<String>();
    private DocRouter docRouter;
    private QName shardProperty;

    public MetadataTracker(Properties p, SOLRAPIClient client, String coreName,
                InformationServer informationServer)
    {
        super(p, client, coreName, informationServer);
        //System.out.println("####### MetadatTracker() ########");
        transactionDocsBatchSize = Integer.parseInt(p.getProperty("alfresco.transactionDocsBatchSize", "100"));
        shardMethod = p.getProperty("shard.method", SHARD_METHOD_DBID);
        String shardKey = p.getProperty("shard.key");
        if(shardKey != null) {
            shardProperty = getShardProperty(shardKey);
        }

        docRouter = DocRouterFactory.getRouter(p, ShardMethodEnum.getShardMethod(shardMethod));
        nodeBatchSize = Integer.parseInt(p.getProperty("alfresco.nodeBatchSize", "10"));
        threadHandler = new ThreadHandler(p, coreName, "MetadataTracker");
    }
    
    MetadataTracker()
    {
        // Testing purposes only
    }

    @Override
    protected void doTrack() throws AuthenticationException, IOException, JSONException, EncoderException
    {
        // MetadataTracker must wait until ModelTracker has run
        ModelTracker modelTracker = this.infoSrv.getAdminHandler().getTrackerRegistry().getModelTracker();

        if (modelTracker != null && modelTracker.hasModels())
        {
            trackRepository();
        }
    }

    public void maintenance() throws Exception {
        purgeTransactions();
        purgeNodes();
        reindexTransactions();
        reindexNodes();
        reindexNodesByQuery();
        indexTransactions();
        indexNodes();
    }

    public boolean hasMaintenance() {
        return transactionsToReindex.size() > 0 ||
                transactionsToIndex.size() > 0 ||
                transactionsToPurge.size() > 0 ||
                nodesToReindex.size() > 0 ||
                nodesToIndex.size() > 0 ||
                nodesToPurge.size() > 0 ||
                queriesToReindex.size() > 0;
    }

    private void trackRepository() throws IOException, AuthenticationException, JSONException, EncoderException
    {
        //System.out.println("########################### MetadataTracker.trackRepository ##########");
        checkShutdown();

        if(!isMaster && isSlave)
        {
            // Dynamic registration
            
            ShardState shardstate = getShardState();
            client.getTransactions(0L, null, 0L, null, 0, shardstate);
            return;
        }

        // Check we are tracking the correct repository
        TrackerState state = super.getTrackerState();
        if(state.getTrackerCycles() == 0)
        {
            //We have a new tracker state so do the checks.
            checkRepoAndIndexConsistency(state);
        }

        checkShutdown();
        trackTransactions();
    }

    /**
     * @return
     */
    private ShardState getShardState()
    {
        TrackerState state = super.getTrackerState();
       
        ShardState shardstate =  ShardStateBuilder.shardState()
                .withMaster(isMaster)
                .withLastUpdated(System.currentTimeMillis())
                .withLastIndexedChangeSetCommitTime(state.getLastIndexedChangeSetCommitTime())
                .withLastIndexedChangeSetId(state.getLastIndexedChangeSetId())
                .withLastIndexedTxCommitTime(state.getLastIndexedTxCommitTime())
                .withLastIndexedTxId(state.getLastIndexedTxId())
                .withShardInstance()
                    .withBaseUrl(infoSrv.getBaseUrl())
                    .withPort(infoSrv.getPort())
                    .withHostName(infoSrv.getHostName())
                    .withShard()
                        .withInstance(shardInstance)
                        .withFloc()
                            .withNumberOfShards(shardCount)
                            .withAddedStoreRef(storeRef)
                            .withTemplate(shardTemplate)
                            .withHasContent(transformContent)
                            .withShardMethod(ShardMethodEnum.getShardMethod(shardMethod))
                            .endFloc()
                        .endShard()
                     .endShardInstance()
                .build();
        return shardstate;
    }

    /**
     * Checks the first and last TX time
     * @param state the state of this tracker
     * @throws AuthenticationException
     * @throws IOException
     * @throws JSONException
     */
    private void checkRepoAndIndexConsistency(TrackerState state) throws AuthenticationException, IOException, JSONException
    {
        Transactions firstTransactions = null;
        if (state.getLastGoodTxCommitTimeInIndex() == 0) 
        {
            state.setCheckedLastTransactionTime(true);
            state.setCheckedFirstTransactionTime(true);
            log.info("No transactions found - no verification required");

            firstTransactions = client.getTransactions(null, 0L, null, 2000L, 1);
            if (!firstTransactions.getTransactions().isEmpty())
            {
                Transaction firstTransaction = firstTransactions.getTransactions().get(0);
                long firstTransactionCommitTime = firstTransaction.getCommitTimeMs();
                state.setLastGoodTxCommitTimeInIndex(firstTransactionCommitTime);
                setLastTxCommitTimeAndTxIdInTrackerState(firstTransactions, state);
            }
        }
        
        if (!state.isCheckedFirstTransactionTime())
        {
            firstTransactions = client.getTransactions(null, 0L, null, 2000L, 1);
            if (!firstTransactions.getTransactions().isEmpty())
            {
                Transaction firstTransaction = firstTransactions.getTransactions().get(0);
                long firstTxId = firstTransaction.getId();
                long firstTransactionCommitTime = firstTransaction.getCommitTimeMs();
                //System.out.println("###### First txn:"+firstTxId+" : "+firstTransactionCommitTime);
                int setSize = this.infoSrv.getTxDocsSize(""+firstTxId, ""+firstTransactionCommitTime);
                
                if (setSize == 0)
                {
                    log.error("First transaction was not found with the correct timestamp.");
                    log.error("SOLR has successfully connected to your repository  however the SOLR indexes and repository database do not match."); 
                    log.error("If this is a new or rebuilt database your SOLR indexes also need to be re-built to match the database."); 
                    log.error("You can also check your SOLR connection details in solrcore.properties.");
                    throw new AlfrescoRuntimeException("Initial transaction not found with correct timestamp");
                }
                else if (setSize == 1)
                {
                    state.setCheckedFirstTransactionTime(true);
                    log.info("Verified first transaction and timestamp in index");
                }
                else
                {
                    log.warn("Duplicate initial transaction found with correct timestamp");
                }
            }
        }

        // Checks that the last TxId in solr is <= last TxId in repo
        if (!state.isCheckedLastTransactionTime())
        {
            if (firstTransactions == null)
            {
                firstTransactions = client.getTransactions(null, 0L, null, 2000L, 1);
            }
            
            setLastTxCommitTimeAndTxIdInTrackerState(firstTransactions, state);
            Long maxTxnCommitTimeInRepo = firstTransactions.getMaxTxnCommitTime();
            Long maxTxnIdInRepo = firstTransactions.getMaxTxnId();
            if (maxTxnCommitTimeInRepo != null && maxTxnIdInRepo != null)
            {
                Transaction maxTxInIndex = this.infoSrv.getMaxTransactionIdAndCommitTimeInIndex();
                if (maxTxInIndex.getCommitTimeMs() > maxTxnCommitTimeInRepo)
                {
                    log.error("Last transaction was found in index with timestamp later than that of repository.");
                    log.error("Max Tx In Index: " + maxTxInIndex.getId() + ", In Repo: " + maxTxnIdInRepo);
                    log.error("Max Tx Commit Time In Index: " + maxTxInIndex.getCommitTimeMs() + ", In Repo: "
                            + maxTxnCommitTimeInRepo);
                    log.error("SOLR has successfully connected to your repository  however the SOLR indexes and repository database do not match."); 
                    log.error("If this is a new or rebuilt database your SOLR indexes also need to be re-built to match the database.");
                    log.error("You can also check your SOLR connection details in solrcore.properties.");
                    throw new AlfrescoRuntimeException("Last transaction found in index with incorrect timestamp");
                }
                else
                {
                    state.setCheckedLastTransactionTime(true);
                    log.info("Verified last transaction timestamp in index less than or equal to that of repository.");
                }
            }
        }
    }
    
    private void indexTransactions() throws IOException, AuthenticationException, JSONException
    {
        long startElapsed = System.nanoTime();
        
        int docCount = 0;
        boolean requiresCommit = false;
        while (transactionsToIndex.peek() != null)
        {
            Long transactionId = transactionsToIndex.poll();
            if (transactionId != null)
            {
                Transactions transactions = client.getTransactions(null, transactionId, null, transactionId+1, 1);
                if ((transactions.getTransactions().size() > 0) && (transactionId.equals(transactions.getTransactions().get(0).getId())))
                {
                    Transaction info = transactions.getTransactions().get(0);

                    GetNodesParameters gnp = new GetNodesParameters();
                    ArrayList<Long> txs = new ArrayList<Long>();
                    txs.add(info.getId());
                    gnp.setTransactionIds(txs);
                    gnp.setStoreProtocol(storeRef.getProtocol());
                    gnp.setStoreIdentifier(storeRef.getIdentifier());
                    gnp.setShardProperty(shardProperty);

                    List<Node> nodes = client.getNodes(gnp, (int) info.getUpdates());
                    for (Node node : nodes)
                    {
                        docCount++;
                        if (log.isDebugEnabled())
                        {
                            log.debug(node.toString());
                        }
                        this.infoSrv.indexNode(node, false);
                        checkShutdown();
                    }

                    // Index the transaction doc after the node - if this is not found then a reindex will be done.
                    this.infoSrv.indexTransaction(info, false);
                    requiresCommit = true;

                    trackerStats.addTxDocs(nodes.size());
                }
            }

            if (docCount > batchCount)
            {
                if(this.infoSrv.getRegisteredSearcherCount() < getMaxLiveSearchers())
                {
                    checkShutdown();
                    //this.infoSrv.commit();
                    long endElapsed = System.nanoTime();
                    trackerStats.addElapsedNodeTime(docCount, endElapsed-startElapsed);
                    startElapsed = endElapsed;
                    docCount = 0;
                    requiresCommit = false;
                }
            }
        }
        if (requiresCommit || (docCount > 0))
        {
            checkShutdown();
            //this.infoSrv.commit();
            long endElapsed = System.nanoTime();
            trackerStats.addElapsedNodeTime(docCount, endElapsed - startElapsed);
        }
    }

    private void indexNodes() throws IOException, AuthenticationException, JSONException
    {
        boolean requiresCommit = false;
        while (nodesToIndex.peek() != null)
        {
            Long nodeId = nodesToIndex.poll();
            if (nodeId != null)
            {
                Node node = new Node();
                node.setId(nodeId);
                node.setStatus(SolrApiNodeStatus.UNKNOWN);
                node.setTxnId(Long.MAX_VALUE);

                this.infoSrv.indexNode(node, false);
                requiresCommit = true;
            }
            checkShutdown();
        }

        if(requiresCommit) {
            checkShutdown();
            //this.infoSrv.commit();
        }
    }

    private void reindexTransactions() throws IOException, AuthenticationException, JSONException
    {
        long startElapsed = System.nanoTime();
        int docCount = 0;
        while (transactionsToReindex.peek() != null)
        {
            Long transactionId = transactionsToReindex.poll();

            if (transactionId != null)
            {
                // make sure it is cleaned out so we do not miss deletes
                this.infoSrv.deleteByTransactionId(transactionId);

                Transactions transactions = client.getTransactions(null, transactionId, null, transactionId+1, 1);
                if ((transactions.getTransactions().size() > 0) && (transactionId.equals(transactions.getTransactions().get(0).getId())))
                {
                    Transaction info = transactions.getTransactions().get(0);
                    this.infoSrv.dirtyTransaction(info.getId());
                    GetNodesParameters gnp = new GetNodesParameters();
                    ArrayList<Long> txs = new ArrayList<Long>();
                    txs.add(info.getId());
                    gnp.setTransactionIds(txs);
                    gnp.setStoreProtocol(storeRef.getProtocol());
                    gnp.setStoreIdentifier(storeRef.getIdentifier());
                    List<Node> nodes = client.getNodes(gnp, (int) info.getUpdates());
                    for (Node node : nodes)
                    {
                        docCount++;
                        if (log.isDebugEnabled())
                        {
                            log.debug(node.toString());
                        }
                        this.infoSrv.indexNode(node, true);
                        checkShutdown();
                    }

                    // Index the transaction doc after the node - if this is not found then a reindex will be done.
                    this.infoSrv.indexTransaction(info, true);
                }
            }

            if (docCount > batchCount)
            {
                if(this.infoSrv.getRegisteredSearcherCount() < getMaxLiveSearchers())
                {
                    checkShutdown();
                    long endElapsed = System.nanoTime();
                    trackerStats.addElapsedNodeTime(docCount, endElapsed-startElapsed);
                    startElapsed = endElapsed;
                    docCount = 0;
                }
            }
        }
        if (docCount > 0)
        {
            checkShutdown();
            //this.infoSrv.commit();
            long endElapsed = System.nanoTime();
            trackerStats.addElapsedNodeTime(docCount, endElapsed - startElapsed);
        }
    }

    
    private void reindexNodes() throws IOException, AuthenticationException, JSONException
    {
        boolean requiresCommit = false;
        while (nodesToReindex.peek() != null)
        {
            Long nodeId = nodesToReindex.poll();
            if (nodeId != null)
            {
                // make sure it is cleaned out so we do not miss deletes
                this.infoSrv.deleteByNodeId(nodeId);

                Node node = new Node();
                node.setId(nodeId);
                node.setStatus(SolrApiNodeStatus.UNKNOWN);
                node.setTxnId(Long.MAX_VALUE);

                this.infoSrv.indexNode(node, true);
                requiresCommit = true;
            }
            checkShutdown();
        }

        if(requiresCommit)
        {
            checkShutdown();
            //this.infoSrv.commit();
        }
    }
    
    private void reindexNodesByQuery() throws IOException, AuthenticationException, JSONException
    {
        boolean requiresCommit = false;
        while (queriesToReindex.peek() != null)
        {
            String query = queriesToReindex.poll();
            if (query != null)
            {
                this.infoSrv.reindexNodeByQuery(query);
                requiresCommit = true;
            }
            checkShutdown();
        }

        if(requiresCommit)
        {
            checkShutdown();
            //this.infoSrv.commit();
        }
    }


    private void purgeTransactions() throws IOException, AuthenticationException, JSONException
    {
        boolean requiresCommit = false;
        while (transactionsToPurge.peek() != null)
        {
            Long transactionId = transactionsToPurge.poll();
            if (transactionId != null)
            {
                // make sure it is cleaned out so we do not miss deletes
                this.infoSrv.deleteByTransactionId(transactionId);
                requiresCommit = true;
            }
            checkShutdown();
        }
        
        if(requiresCommit)
        {
            checkShutdown();
            //this.infoSrv.commit();
        }
    }

    private void purgeNodes() throws IOException, AuthenticationException, JSONException
    {
        while (nodesToPurge.peek() != null)
        {
            Long nodeId = nodesToPurge.poll();
            if (nodeId != null)
            {
                // make sure it is cleaned out so we do not miss deletes
                this.infoSrv.deleteByNodeId(nodeId);
            }
            checkShutdown();
        }
    }

    protected Long getTxFromCommitTime(BoundedDeque<Transaction> txnsFound, long lastGoodTxCommitTimeInIndex) {
        if (txnsFound.size() > 0)
        {
            return txnsFound.getLast().getCommitTimeMs();
        }
        else
        {
            return lastGoodTxCommitTimeInIndex;
        }
    }

    private boolean alreadyFoundTransactions(BoundedDeque<Transaction> txnsFound, Transactions transactions)
    {
        if (txnsFound.size() == 0) { return false; }

        if (transactions.getTransactions().size() == 1)
        {
            return transactions.getTransactions().get(0).getId() == txnsFound.getLast().getId();
        }
        else
        {
            HashSet<Transaction> alreadyFound = new HashSet<Transaction>(txnsFound.getDeque());
            for (Transaction txn : transactions.getTransactions())
            {
                if (!alreadyFound.contains(txn)) { return false; }
            }
            return true;
        }
    }

    protected Transactions getSomeTransactions(BoundedDeque<Transaction> txnsFound, Long fromCommitTime, long timeStep,
                int maxResults, long endTime) throws AuthenticationException, IOException, JSONException, EncoderException
    {
        //System.out.println("###################### getSomeTransactions ####### "+fromCommitTime+":"+endTime);

        long actualTimeStep = timeStep;

        ShardState shardstate = getShardState();
        
        Transactions transactions;
        // step forward in time until we find something or hit the time bound
        // max id unbounded
        Long startTime = fromCommitTime == null ? Long.valueOf(0L) : fromCommitTime;
        do
        {
            transactions = client.getTransactions(startTime, null, startTime + actualTimeStep, null, maxResults, shardstate);
            startTime += actualTimeStep;

        } while (((transactions.getTransactions().size() == 0) && (startTime < endTime))
                    || ((transactions.getTransactions().size() > 0) && alreadyFoundTransactions(txnsFound, transactions)));

        //System.out.println("############ done with transactions ######### " + startTime + ":" + endTime + ":" + transactions.getTransactions().size());

        return transactions;
    }

    protected void trackTransactions() throws AuthenticationException, IOException, JSONException, EncoderException
    {
        long startElapsed = System.nanoTime();
        
        boolean upToDate = false;
        Transactions transactions;
        BoundedDeque<Transaction> txnsFound = new BoundedDeque<Transaction>(100);
        HashSet<Transaction> txsIndexed = new LinkedHashSet<>(); 
        long totalUpdatedDocs = 0;
        int docCount = 0;
        
        do
        {
            try
            {
                getWriteLock().acquire();
                //System.out.println("######## Metadata Tracket Acquiring Write Lock ########");

                TrackerState state = getTrackerState();

                //System.out.println("######## Do Track Transactions ########:"+docCount);


                Long fromCommitTime = getTxFromCommitTime(txnsFound, state.getLastGoodTxCommitTimeInIndex());
                transactions = getSomeTransactions(txnsFound, fromCommitTime, TIME_STEP_1_HR_IN_MS, 2000,
                                                   state.getTimeToStopIndexing());

                //System.out.println("######## get some! ########:"+transactions.getTransactions().size());

                setLastTxCommitTimeAndTxIdInTrackerState(transactions, state);

                log.info("Scanning transactions ...");
                if (transactions.getTransactions().size() > 0) {
                    log.info(".... from " + transactions.getTransactions().get(0));
                    log.info(".... to " + transactions.getTransactions().get(transactions.getTransactions().size() - 1));
                } else {
                    log.info(".... none found after lastTxCommitTime "
                            + ((txnsFound.size() > 0) ? txnsFound.getLast().getCommitTimeMs() : state
                            .getLastIndexedTxCommitTime()));
                }

                ArrayList<Transaction> txBatch = new ArrayList<>();
                for (Transaction info : transactions.getTransactions()) {
                    //System.out.println("############## Transaction:"+ info.getId());
                    boolean isInIndex = (infoSrv.txnInIndex(info.getId(), true) && info.getCommitTimeMs() <= state.getLastIndexedTxCommitTime());
                    if (isInIndex) {
                        //System.out.println("################## Transaction Found In Index:"+info.getId());
                        txnsFound.add(info);
                    } else {
                        //System.out.println("################## New Transaction:"+info.getId());
                        // Make sure we do not go ahead of where we started - we will check the holes here
                        // correctly next time
                        if (info.getCommitTimeMs() > state.getTimeToStopIndexing()) {
                            //System.out.println("################## We are upToDate:"+state.getTimeToStopIndexing());
                            upToDate = true;
                            break;
                        }

                        txBatch.add(info);
                        if (getUpdateAndDeleteCount(txBatch) > this.transactionDocsBatchSize) {

                            docCount += indexBatchOfTransactions(txBatch);
                            totalUpdatedDocs += docCount;

                            for (Transaction scheduledTx : txBatch) {
                                txnsFound.add(scheduledTx);
                                txsIndexed.add(scheduledTx);
                            }
                            txBatch.clear();
                        }
                    }

                    if (docCount > batchCount) {
                        indexTransactionsAfterAsynchronous(txsIndexed, state);
                        long endElapsed = System.nanoTime();
                        trackerStats.addElapsedNodeTime(docCount, endElapsed - startElapsed);
                        startElapsed = endElapsed;
                        docCount = 0;

                        //Release the write lock allowing the commit tracker to run.
                        this.getWriteLock().release();
                        //Re-acquire the write lock and keep indexing.
                        this.getWriteLock().acquire();
                    }
                    checkShutdown();
                }

                // Index any remaining transactions bringing the index to a consistent state so the CommitTracker can commit if need be.

                if (!txBatch.isEmpty()) {
                    if (this.getUpdateAndDeleteCount(txBatch) > 0) {
                        docCount += indexBatchOfTransactions(txBatch);
                        totalUpdatedDocs += docCount;
                    }

                    for (Transaction scheduledTx : txBatch) {
                        txnsFound.add(scheduledTx);
                        txsIndexed.add(scheduledTx);
                    }
                    txBatch.clear();
                }

                if (txsIndexed.size() > 0) {
                    indexTransactionsAfterAsynchronous(txsIndexed, state);
                    long endElapsed = System.nanoTime();
                    trackerStats.addElapsedNodeTime(docCount, endElapsed - startElapsed);
                    startElapsed = endElapsed;
                    docCount = 0;
                }
            }
            catch(Exception e)
            {
                throw new IOException(e);
            }
            finally
            {
                getWriteLock().release();
                //System.out.println("######## Metadata Tracket Releasing Write Lock ########:"+getWriteLock().availablePermits());
            }
        }
        while ((transactions.getTransactions().size() > 0) && (upToDate == false));

        log.info("total number of docs with metadata updated: " + totalUpdatedDocs);
    }

    private void setLastTxCommitTimeAndTxIdInTrackerState(Transactions transactions, TrackerState state)
    {
        Long maxTxnCommitTime = transactions.getMaxTxnCommitTime();
        if (maxTxnCommitTime != null)
        {
            state.setLastTxCommitTimeOnServer(maxTxnCommitTime);
        }

        Long maxTxnId = transactions.getMaxTxnId();
        if (maxTxnId != null)
        {
            state.setLastTxIdOnServer(maxTxnId);
        }
    }

    private void indexTransactionsAfterAsynchronous(HashSet<Transaction> txsIndexed, TrackerState state)
                throws IOException
    {
        waitForAsynchronous();
        for (Transaction tx : txsIndexed)
        {
            super.infoSrv.indexTransaction(tx, true);
            // Transactions are ordered by commit time and tie-broken by tx id
            if (tx.getCommitTimeMs() > state.getLastIndexedTxCommitTime()
                    || tx.getCommitTimeMs() == state.getLastIndexedTxCommitTime()
                    && tx.getId() > state.getLastIndexedTxId())
            {
                state.setLastIndexedTxCommitTime(tx.getCommitTimeMs());
                state.setLastIndexedTxId(tx.getId());
            }
            trackerStats.addTxDocs((int) (tx.getDeletes() + tx.getUpdates()));
        }
        txsIndexed.clear();
        //super.infoSrv.commit();
    }

    private long getUpdateAndDeleteCount(List<Transaction> txs)
    {
        long count = 0;
        for (Transaction tx : txs)
        {
            count += (tx.getUpdates() + tx.getDeletes());
        }
        return count;
    }

    private int indexBatchOfTransactions(List<Transaction> txBatch) throws AuthenticationException, IOException, JSONException
    {
        int nodeCount = 0;
        ArrayList<Transaction> nonEmptyTxs = new ArrayList<>(txBatch.size());
        GetNodesParameters gnp = new GetNodesParameters();
        ArrayList<Long> txIds = new ArrayList<Long>();
        for (Transaction tx : txBatch)
        {
            if (tx.getUpdates() > 0 || tx.getDeletes() > 0)
            {
                nonEmptyTxs.add(tx);
                txIds.add(tx.getId());
            }
        }

        gnp.setTransactionIds(txIds);
        gnp.setStoreProtocol(storeRef.getProtocol());
        gnp.setStoreIdentifier(storeRef.getIdentifier());
        gnp.setShardProperty(shardProperty);
        List<Node> nodes = client.getNodes(gnp, Integer.MAX_VALUE);
        
        ArrayList<Node> nodeBatch = new ArrayList<>();
        for (Node node : nodes)
        {
            if (log.isDebugEnabled())
            {
                log.debug(node.toString());
            }
            nodeBatch.add(node);
            if (nodeBatch.size() > nodeBatchSize)
            {
                nodeCount += nodeBatch.size();
                NodeIndexWorkerRunnable niwr = new NodeIndexWorkerRunnable(this.threadHandler, nodeBatch, this.infoSrv);
                this.threadHandler.scheduleTask(niwr);
                nodeBatch = new ArrayList<>();
            }
        }
        
        if (nodeBatch.size() > 0)
        {
            nodeCount += nodeBatch.size();
            NodeIndexWorkerRunnable niwr = new NodeIndexWorkerRunnable(this.threadHandler, nodeBatch, this.infoSrv);
            this.threadHandler.scheduleTask(niwr);
            nodeBatch = new ArrayList<>();
        }
        return nodeCount;
    }

    class NodeIndexWorkerRunnable extends AbstractWorkerRunnable
    {
        InformationServer infoServer;
        List<Node> nodes;

        NodeIndexWorkerRunnable(QueueHandler queueHandler, List<Node> nodes, InformationServer infoServer)
        {
            super(queueHandler);
            this.infoServer = infoServer;
            this.nodes = nodes;
        }

        @Override
        protected void doWork() throws IOException, AuthenticationException, JSONException
        { 
            List<Node> filteredNodes = filterNodes(nodes);
            if(filteredNodes.size() > 0)
            {
                this.infoServer.indexNodes(filteredNodes, true, false);
            }
        }
        
        @Override
        protected void onFail()
        {
        	setRollback(true);
        }
        
        private List<Node> filterNodes(List<Node> nodes)
        {
            ArrayList<Node> filteredList = new ArrayList<Node>(nodes.size());
            for(Node node : nodes)
            {

                if(docRouter.routeNode(shardCount, shardInstance, node))
                {
                    filteredList.add(node);
                }
                else
                {

                    if(node.getStatus() == SolrApiNodeStatus.UPDATED)
                    {
                        Node doCascade = new Node();
                        doCascade.setAclId(node.getAclId());
                        doCascade.setId(node.getId());
                        doCascade.setNodeRef(node.getNodeRef());
                        doCascade.setStatus(SolrApiNodeStatus.NON_SHARD_UPDATED);
                        doCascade.setTenant(node.getTenant());
                        doCascade.setTxnId(node.getTxnId());
                        filteredList.add(doCascade);
                    }
                    else // DELETED & UNKNOWN
                    {
                        // Make sure anything no longer relevant to this shard is deleted.
                        Node doDelete = new Node();
                        doDelete.setAclId(node.getAclId());
                        doDelete.setId(node.getId());
                        doDelete.setNodeRef(node.getNodeRef());
                        doDelete.setStatus(SolrApiNodeStatus.NON_SHARD_DELETED);
                        doDelete.setTenant(node.getTenant());
                        doDelete.setTxnId(node.getTxnId());
                        filteredList.add(doDelete);
                    }
                }
            }
            return filteredList;
        }
    }



    
    
    public NodeReport checkNode(Long dbid)
    {
        NodeReport nodeReport = new NodeReport();
        nodeReport.setDbid(dbid);

        // In DB

        GetNodesParameters parameters = new GetNodesParameters();
        parameters.setFromNodeId(dbid);
        parameters.setToNodeId(dbid);
        List<Node> dbnodes;
        try
        {
            dbnodes = client.getNodes(parameters, 1);
            if (dbnodes.size() == 1)
            {
                Node dbnode = dbnodes.get(0);
                nodeReport.setDbNodeStatus(dbnode.getStatus());
                nodeReport.setDbTx(dbnode.getTxnId());
            }
            else
            {
                nodeReport.setDbNodeStatus(SolrApiNodeStatus.UNKNOWN);
                nodeReport.setDbTx(-1l);
            }
        }
        catch (IOException e)
        {
            nodeReport.setDbNodeStatus(SolrApiNodeStatus.UNKNOWN);
            nodeReport.setDbTx(-2l);
        }
        catch (JSONException e)
        {
            nodeReport.setDbNodeStatus(SolrApiNodeStatus.UNKNOWN);
            nodeReport.setDbTx(-3l);
        }
        catch (AuthenticationException e1)
        {
            nodeReport.setDbNodeStatus(SolrApiNodeStatus.UNKNOWN);
            nodeReport.setDbTx(-4l);
        }
        
        this.infoSrv.addCommonNodeReportInfo(nodeReport);

        return nodeReport;
    }

    public NodeReport checkNode(Node node)
    {
        NodeReport nodeReport = new NodeReport();
        nodeReport.setDbid(node.getId());

        nodeReport.setDbNodeStatus(node.getStatus());
        nodeReport.setDbTx(node.getTxnId());

        this.infoSrv.addCommonNodeReportInfo(nodeReport);
        
        return nodeReport;
    }

    public List<Node> getFullNodesForDbTransaction(Long txid)
    {
        try
        {
            GetNodesParameters gnp = new GetNodesParameters();
            ArrayList<Long> txs = new ArrayList<Long>();
            txs.add(txid);
            gnp.setTransactionIds(txs);
            gnp.setStoreProtocol(storeRef.getProtocol());
            gnp.setStoreIdentifier(storeRef.getIdentifier());
            return client.getNodes(gnp, Integer.MAX_VALUE);
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("Failed to get nodes", e);
        }
        catch (JSONException e)
        {
            throw new AlfrescoRuntimeException("Failed to get nodes", e);
        }
        catch (AuthenticationException e)
        {
            throw new AlfrescoRuntimeException("Failed to get nodes", e);
        }
    }

    public IndexHealthReport checkIndex(Long toTx, Long toAclTx, Long fromTime, Long toTime)
                throws IOException, AuthenticationException, JSONException, EncoderException
    {
        // DB TX Count
        long firstTransactionCommitTime = 0;
        Transactions firstTransactions = client.getTransactions(null, 0L, null, 2000L, 1);
        if(firstTransactions.getTransactions().size() > 0)
        {
            Transaction firstTransaction = firstTransactions.getTransactions().get(0);
            firstTransactionCommitTime = firstTransaction.getCommitTimeMs();
        }

        IOpenBitSet txIdsInDb = infoSrv.getOpenBitSetInstance();
        Long lastTxCommitTime = Long.valueOf(firstTransactionCommitTime);
        if (fromTime != null)
        {
            lastTxCommitTime = fromTime;
        }
        long maxTxId = 0;
        Long minTxId = null;

        Transactions transactions;
        BoundedDeque<Transaction> txnsFound = new  BoundedDeque<Transaction>(100);
        long endTime = System.currentTimeMillis() + infoSrv.getHoleRetention();
        DO: do
        {
            transactions = getSomeTransactions(txnsFound, lastTxCommitTime, TIME_STEP_1_HR_IN_MS, 2000, endTime);
            for (Transaction info : transactions.getTransactions())
            {
                // include
                if (toTime != null)
                {
                    if (info.getCommitTimeMs() > toTime.longValue())
                    {
                        break DO;
                    }
                }
                if (toTx != null)
                {
                    if (info.getId() > toTx.longValue())
                    {
                        break DO;
                    }
                }

                // bounds for later loops
                if (minTxId == null)
                {
                    minTxId = info.getId();
                }
                if (maxTxId < info.getId())
                {
                    maxTxId = info.getId();
                }

                lastTxCommitTime = info.getCommitTimeMs();
                txIdsInDb.set(info.getId());
                txnsFound.add(info);
            }
        }
        while (transactions.getTransactions().size() > 0);

        return this.infoSrv.reportIndexTransactions(minTxId, txIdsInDb, maxTxId);
    }

    public void addTransactionToPurge(Long txId)
    {
        this.transactionsToPurge.offer(txId);
    }

    public void addNodeToPurge(Long nodeId)
    {
        this.nodesToPurge.offer(nodeId);
    }

    public void addTransactionToReindex(Long txId)
    {
        this.transactionsToReindex.offer(txId);
    }

    public void addNodeToReindex(Long nodeId)
    {
        nodesToReindex.offer(nodeId);
    }

    public void addTransactionToIndex(Long txId)
    {
        transactionsToIndex.offer(txId);
    }

    public void addNodeToIndex(Long nodeId)
    {
        this.nodesToIndex.offer(nodeId);
    }

    public void invalidateState() {
        super.invalidateState();
        infoSrv.clearProcessedTransactions();
    }

    /**
     * @param query
     */
    public void addQueryToReindex(String query)
    {
        this.queriesToReindex.offer(query);
    }

    private boolean transactionDocumentIdProcessed(Set<Long> transactionDocumentIdSet, Long id) throws IOException
    {
        if(transactionDocumentIdSet.contains(id))
        {
            return true;
        }
        else
        {
            if(infoSrv.isInIndex(Long.toString(id))) {
                return true;
            }
            else
            {
                transactionDocumentIdSet.add(id);
                return false;
            }
        }

    }

    public static QName getShardProperty(String field) {
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        NamespaceDAO namespaceDAO = dataModel.getNamespaceDAO();
        DictionaryService dictionaryService = dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT);
        PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition("http://www.alfresco.org/model/content/1.0",
                                                                                  namespaceDAO,
                                                                                  dictionaryService,
                                                                                  field);

        return propertyDef.getName();
    }




}
