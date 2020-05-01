/*
 * Copyright (C) 2014 Alfresco Software Limited.
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

import com.google.common.collect.Lists;
import org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;

/**
 * This tracker queries for docs with unclean content, and then updates them.
 * Similar to org.alfresco.repo.search.impl.lucene.ADMLuceneIndexerImpl
 * 
 * @author Ahmed Owian
 */
public class ContentTracker extends AbstractTracker implements Tracker
{

    protected final static Logger LOGGER = LoggerFactory.getLogger(ContentTracker.class);

    private static int DEFAULT_CONTENT_UPDATE_BATCH_SIZE = 2000;
    private static final int DEFAULT_CONTENT_TRACKER_MAX_PARALLELISM = 32;

    private int contentTrackerParallelism;
    private int contentUpdateBatchSize;
    
    // Share run and write locks across all ContentTracker threads
    private static Map<String, Semaphore> RUN_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private static Map<String, Semaphore> WRITE_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private ForkJoinPool forkJoinPool;

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

    public ContentTracker(Properties p, SOLRAPIClient client, String coreName,
                InformationServer informationServer)
    {
        super(p, client, coreName, informationServer, Tracker.Type.CONTENT);
        contentUpdateBatchSize = Integer.parseInt(p.getProperty("alfresco.contentUpdateBatchSize",
                String.valueOf(DEFAULT_CONTENT_UPDATE_BATCH_SIZE)));
        contentTrackerParallelism = Integer.parseInt(p.getProperty("alfresco.contentTrackerMaxParallelism",
                String.valueOf(DEFAULT_CONTENT_TRACKER_MAX_PARALLELISM)));
        forkJoinPool = new ForkJoinPool(contentTrackerParallelism);

        RUN_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
        WRITE_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
    }
    
    ContentTracker()
    {
       super(Tracker.Type.CONTENT);
    }
    
    @Override
    protected void doTrack(String iterationId) throws Exception
    {
        
        try 
        {
            long startElapsed = System.nanoTime();

            checkShutdown();
            long totalDocs = 0l;
            checkShutdown();
            while (true) {
                try
                {
                    getWriteLock().acquire();

                    List<TenantAclIdDbId> docs = this.infoSrv.getDocsWithUncleanContent();
                    if (docs.size() == 0) {
                        break;
                    }

                    List<List<TenantAclIdDbId>> docBatches = Lists.partition(docs, contentUpdateBatchSize);
                    for (List<TenantAclIdDbId> batch : docBatches) {



                        Integer processedDocuments = forkJoinPool.submit(() ->
                                // Parallel task here, for example
                                batch.parallelStream().map(doc -> {
                                    ContentIndexWorkerRunnable ciwr = new ContentIndexWorkerRunnable(doc, infoSrv);
                                    ciwr.run();
                                    return 1;
                                }).reduce(0, Integer::sum)
                        ).get();

                        long endElapsed = System.nanoTime();
                        trackerStats.addElapsedContentTime(processedDocuments, endElapsed - startElapsed);
                        startElapsed = endElapsed;

                    };

                    totalDocs += docs.size();
                    checkShutdown();
                }
                finally
                {
                    getWriteLock().release();
                }
            }

            LOGGER.info("{}-[CORE {}] Total number of docs with content updated: {} ", Thread.currentThread().getId(), coreName, totalDocs);
            
        }
        catch(Exception e)
        {
            throw new IOException(e);
        }
    }

    public boolean hasMaintenance() {
        return false;
    }

    public void maintenance() {
        return;
    }

    public void invalidateState() {
        super.invalidateState();
        this.infoSrv.setCleanContentTxnFloor(-1);
    }

    class ContentIndexWorkerRunnable extends AbstractWorker
    {
        InformationServer infoServer;
        TenantAclIdDbId doc;

        ContentIndexWorkerRunnable(TenantAclIdDbId doc, InformationServer infoServer)
        {
            this.doc = doc;
            this.infoServer = infoServer;
        }

        @Override
        protected void doWork() throws Exception
        {
            checkShutdown();
            this.infoServer.updateContentToIndexAndCache(doc.dbId, doc.tenant);
        }
        
        @Override
        protected void onFail(Throwable failCausedBy)
        {
            // This will be redone in future tracking operations
            LOGGER.warn("Content tracker failed due to {}", failCausedBy.getMessage(), failCausedBy);
        }
    }
}
