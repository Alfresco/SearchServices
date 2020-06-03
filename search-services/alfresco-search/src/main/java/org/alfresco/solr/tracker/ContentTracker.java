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

import com.google.common.collect.Lists;
import org.alfresco.solr.AlfrescoSolrDataModel.TenantDbId;
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

import static org.alfresco.solr.utils.Utils.notNullOrEmpty;

/**
 * This tracker queries for docs with unclean content, and then updates them.
 * Similar to org.alfresco.repo.search.impl.lucene.ADMLuceneIndexerImpl
 * 
 * @author Ahmed Owian
 */
public class ContentTracker extends AbstractTracker implements Tracker
{
    protected final static Logger LOGGER = LoggerFactory.getLogger(ContentTracker.class);

    private static final int DEFAULT_CONTENT_TRACKER_MAX_PARALLELISM = 32;

    private int contentTrackerParallelism;
    private int contentUpdateBatchSize;
    
    // Share run and write locks across all ContentTracker threads
    private static final Map<String, Semaphore> RUN_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private static final Map<String, Semaphore> WRITE_LOCK_BY_CORE = new ConcurrentHashMap<>();
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

    public ContentTracker(Properties p, SOLRAPIClient client, String coreName, InformationServer informationServer)
    {
        super(p, client, coreName, informationServer, Tracker.Type.CONTENT);
        int DEFAULT_CONTENT_UPDATE_BATCH_SIZE = 2000;

        contentUpdateBatchSize = Integer.parseInt(p.getProperty("alfresco.contentUpdateBatchSize",
                String.valueOf(DEFAULT_CONTENT_UPDATE_BATCH_SIZE)));

        contentTrackerParallelism = Integer.parseInt(p.getProperty("alfresco.content.tracker.maxParallelism",
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
            long totalDocs = 0L;
            checkShutdown();
            while (true)
            {
                try
                {
                    getWriteLock().acquire();

                    List<TenantDbId> docs = notNullOrEmpty(this.infoSrv.getDocsWithUncleanContent());
                    if (docs.isEmpty())
                    {
                        LOGGER.trace("No unclean document has been detected in the current ContentTracker cycle.");
                        break;
                    }

                    List<List<TenantDbId>> docBatches = Lists.partition(docs, contentUpdateBatchSize);
                    for (List<TenantDbId> batch : docBatches)
                    {
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

                    }

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

    public boolean hasMaintenance()
    {
        return false;
    }

    public void maintenance()
    {
        // Nothing to be done here
    }

    public void invalidateState()
    {
        super.invalidateState();
        this.infoSrv.setCleanContentTxnFloor(-1);
    }

    class ContentIndexWorkerRunnable extends AbstractWorker
    {
        InformationServer infoServer;
        TenantDbId docRef;

        ContentIndexWorkerRunnable(TenantDbId doc, InformationServer infoServer)
        {
            this.docRef = doc;
            this.infoServer = infoServer;
        }

        @Override
        protected void doWork() throws Exception
        {
            checkShutdown();

            infoServer.updateContent(docRef);
        }
        
        @Override
        protected void onFail(Throwable failCausedBy)
        {
            // This will be redone in future tracking operations
            LOGGER.warn("Content tracker failed due to {}", failCausedBy.getMessage(), failCausedBy);
        }
    }
}
