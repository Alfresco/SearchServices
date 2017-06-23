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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tracker queries for docs with unclean content, and then updates them.
 * Similar to org.alfresco.repo.search.impl.lucene.ADMLuceneIndexerImpl
 * 
 * @author Ahmed Owian
 */
public class ContentTracker extends AbstractTracker implements Tracker
{

    protected final static Logger log = LoggerFactory.getLogger(ContentTracker.class);
    private int contentReadBatchSize;
    private int contentUpdateBatchSize;
    

    public ContentTracker(Properties p, SOLRAPIClient client, String coreName,
                InformationServer informationServer)
    {
        super(p, client, coreName, informationServer, Tracker.Type.Content);
        contentReadBatchSize = Integer.parseInt(p.getProperty("alfresco.contentReadBatchSize", "100"));
        contentUpdateBatchSize = Integer.parseInt(p.getProperty("alfresco.contentUpdateBatchSize", "1000"));
        threadHandler = new ThreadHandler(p, coreName, "ContentTracker");
    }
    
    ContentTracker()
    {
       super(Tracker.Type.Content);
    }
    
    @Override
    protected void doTrack() throws Exception
    {
        //System.out.println("############## Content Tracker doTrack()");
        try {
            long startElapsed = System.nanoTime();

            checkShutdown();
            final int ROWS = contentReadBatchSize;
            int start = 0;
            long totalDocs = 0l;
            checkShutdown();
            while (true) {
                try
                {

                    getWriteLock().acquire();

                    List<TenantAclIdDbId> docs = this.infoSrv.getDocsWithUncleanContent(start, ROWS);
                    //System.out.println("####################### Unclean content: "+docs.size()+" ##############################:"+totalDocs);
                    if (docs.size() == 0) {
                        break;
                    }

                    int docsUpdatedSinceLastCommit = 0;
                    for (TenantAclIdDbId doc : docs) {
                        ContentIndexWorkerRunnable ciwr = new ContentIndexWorkerRunnable(super.threadHandler, doc, infoSrv);
                        super.threadHandler.scheduleTask(ciwr);
                        docsUpdatedSinceLastCommit++;

                        if (docsUpdatedSinceLastCommit >= contentUpdateBatchSize) {
                            super.waitForAsynchronous();
                            checkShutdown();
                            //this.infoSrv.commit();
                            long endElapsed = System.nanoTime();
                            trackerStats.addElapsedContentTime(docsUpdatedSinceLastCommit, endElapsed - startElapsed);
                            startElapsed = endElapsed;
                            docsUpdatedSinceLastCommit = 0;
                        }
                    }

                    if (docsUpdatedSinceLastCommit > 0) {
                        super.waitForAsynchronous();
                        checkShutdown();
                        //this.infoSrv.commit();
                        long endElapsed = System.nanoTime();
                        trackerStats.addElapsedContentTime(docsUpdatedSinceLastCommit, endElapsed - startElapsed);
                    }
                    totalDocs += docs.size();
                    checkShutdown();
                }
                finally
                {
                    getWriteLock().release();
                }
            }

            log.info("total number of docs with content updated: " + totalDocs);
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

    class ContentIndexWorkerRunnable extends AbstractWorkerRunnable
    {
        InformationServer infoServer;
        TenantAclIdDbId doc;

        ContentIndexWorkerRunnable(QueueHandler queueHandler, TenantAclIdDbId doc, InformationServer infoServer)
        {
            super(queueHandler);
            this.doc = doc;
            this.infoServer = infoServer;
        }

        @Override
        protected void doWork() throws Exception
        {
            checkShutdown();
            //System.out.println("################ Update doc:"+doc.dbId);
            this.infoServer.updateContentToIndexAndCache(doc.dbId, doc.tenant);
        }
        
        @Override
        protected void onFail()
        {
        	// Will redo if not persisted
        }
    }
}
