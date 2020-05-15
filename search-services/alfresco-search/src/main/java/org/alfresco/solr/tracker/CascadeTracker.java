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

package org.alfresco.solr.tracker;

import static java.util.stream.Collectors.joining;

import static org.alfresco.solr.utils.Utils.notNullOrEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;

import com.google.common.collect.Lists;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.Transaction;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.joining;
import static org.alfresco.solr.utils.Utils.notNullOrEmpty;

/*
 * This tracks Cascading Updates
 * @author Joel Bernstein
 */
public class CascadeTracker extends AbstractTracker implements Tracker
{

    protected final static Logger LOGGER = LoggerFactory.getLogger(CascadeTracker.class);

    private static final int DEFAULT_CASCADE_TRACKER_MAX_PARALLELISM = 32;
    private static final int DEFAULT_CASCADE_NODE_BATCH_SIZE = 10;


    // Share run and write locks across all CascadeTracker threads
    private static Map<String, Semaphore> RUN_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private static Map<String, Semaphore> WRITE_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private int cascadeBatchSize;
    private ForkJoinPool forkJoinPool;
    private int cascadeTrackerParallelism;

    @Override
    public Semaphore getWriteLock()
    {
        return WRITE_LOCK_BY_CORE.get(coreName);
    }
    @Override
    public Semaphore getRunLock()
    {
        return RUN_LOCK_BY_CORE.get(coreName);
    }


    public CascadeTracker(Properties p, SOLRAPIClient client, String coreName,
                           InformationServer informationServer)
    {
        super(p, client, coreName, informationServer, Tracker.Type.CASCADE);

        cascadeTrackerParallelism = Integer.parseInt(p.getProperty("alfresco.cascadeTrackerMaxParallelism",
                String.valueOf(DEFAULT_CASCADE_TRACKER_MAX_PARALLELISM)));

        cascadeBatchSize = Integer.parseInt(p.getProperty("alfresco.cascadeNodeBatchSize",
                String.valueOf(DEFAULT_CASCADE_NODE_BATCH_SIZE)));;

        forkJoinPool = new ForkJoinPool(cascadeTrackerParallelism);
        RUN_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
        WRITE_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
    }

    CascadeTracker()
    {
       super(Tracker.Type.CASCADE);
    }

    @Override
    protected void doTrack(String iterationId) throws IOException, JSONException
    {
        // MetadataTracker must wait until ModelTracker has run
        ModelTracker modelTracker = this.infoSrv.getAdminHandler().getTrackerRegistry().getModelTracker();
        if (modelTracker != null && modelTracker.hasModels())
        {
            trackRepository(iterationId);
        }
    }

    public void maintenance()
    {
    }

    public boolean hasMaintenance() {
        return false;
    }

    private void trackRepository(String iterationId) throws IOException, JSONException
    {
        checkShutdown();
        processCascades(iterationId);
    }

    private void updateTransactionsAfterWorker(List<Transaction> txsIndexed)
            throws IOException
    {
        for (Transaction tx : txsIndexed)
        {
            super.infoSrv.updateTransaction(tx);
        }
    }

    class CascadeIndexWorker extends AbstractWorker
    {
        InformationServer infoServer;
        List<NodeMetaData> nodes;

        CascadeIndexWorker(List<NodeMetaData> nodes, InformationServer infoServer)
        {
            this.infoServer = infoServer;
            this.nodes = nodes;
        }

        @Override
        protected void doWork() throws IOException, AuthenticationException, JSONException
        {
            this.infoServer.cascadeNodes(nodes, true);
        }
        
        @Override
        protected void onFail(Throwable failCausedBy) 
        {
            setRollback(true, failCausedBy);
        }
    }

    public void invalidateState()
    {
        super.invalidateState();
        infoSrv.setCleanCascadeTxnFloor(-1);
    }

    private void processCascades(String iterationId) throws IOException
    {
        int num = 50;
        List<Transaction> txBatch = null;
        long totalUpdatedDocs = 0;

        do {
            try {
                getWriteLock().acquire();
                txBatch = infoSrv.getCascades(num);

                if (txBatch.size() > 0)
                {
                    LOGGER.info("{}-[CORE {}] Found {} transactions, transactions from {} to {}",
                            Thread.currentThread().getId(),
                            coreName,
                            txBatch.size(),
                            txBatch.get(0),
                            txBatch.get(txBatch.size() - 1));
                }
                else
                {
                    LOGGER.info("{}-[CORE {}] No transaction found",
                            Thread.currentThread().getId(), coreName);
                }

                if(txBatch.size() == 0) {
                    return;
                }

                ArrayList<Long> txIds = new ArrayList<>();
                Set<Long> txIdSet = new HashSet<>();
                for (Transaction tx : txBatch) {
                    txIds.add(tx.getId());
                    txIdSet.add(tx.getId());
                }

                List<NodeMetaData> nodeMetaDatas = infoSrv.getCascadeNodes(txIds);
                Integer processedCascades = 0;

                if(nodeMetaDatas.size() > 0) {
                    List<List<NodeMetaData>> nodeBatches = Lists.partition(nodeMetaDatas, cascadeBatchSize);

                    processedCascades = forkJoinPool.submit( () ->
                            nodeBatches.parallelStream().map( batch -> {

                                CascadeIndexWorker worker = new CascadeIndexWorker(batch, infoSrv);
                                worker.run();

                                if (LOGGER.isTraceEnabled())
                                {
                                    String nodes = notNullOrEmpty(batch).stream()
                                            .map(NodeMetaData::getId)
                                            .map(Object::toString)
                                            .collect(joining(","));
                                    LOGGER.trace("[{} / {} / {} / {}] Worker has been created for nodes {}", coreName, trackerId, iterationId, worker.hashCode(), nodes);
                                }
                                return batch.size();
                            }).reduce(0, Integer::sum)
                    ).get();


                }
                //Update the transaction records.
                updateTransactionsAfterWorker(txBatch);
                totalUpdatedDocs += processedCascades;
            }
            catch (AuthenticationException e)
            {
                throw new IOException(e);
            }
            catch (JSONException e)
            {
                throw new IOException(e);
            }
            catch(InterruptedException e)
            {
                throw new IOException(e);
            }
            catch (ExecutionException e)
            {
                e.printStackTrace();
            }
            finally
            {
                getWriteLock().release();
            }
        } while(txBatch.size() > 0);

        LOGGER.info("{}-[CORE {}] Updated {} DOCs", Thread.currentThread().getId(), coreName, totalUpdatedDocs);

    }
}
