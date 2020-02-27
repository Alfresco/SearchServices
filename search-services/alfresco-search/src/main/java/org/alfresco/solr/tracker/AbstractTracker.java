/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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

import static java.util.Optional.ofNullable;

import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.IndexTrackingShutdownException;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.SOLRAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that provides common {@link Tracker} behaviour.
 * 
 * @author Matt Ward
 */
public abstract class AbstractTracker implements Tracker
{
    static final long TIME_STEP_32_DAYS_IN_MS = 1000 * 60 * 60 * 24 * 32L;
    static final long TIME_STEP_1_HR_IN_MS = 60 * 60 * 1000L;
    static final String SHARD_METHOD_DBID = "DB_ID";

    protected final static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    protected Properties props;    
    protected SOLRAPIClient client;
    InformationServer infoSrv;
    protected String coreName;
    StoreRef storeRef;
    long batchCount;
    TrackerStats trackerStats;
    boolean runPostModelLoadInit = true;
    private int maxLiveSearchers;
    private volatile boolean shutdown = false;

    private Semaphore runLock = new Semaphore(1, true);
    private Semaphore writeLock = new Semaphore(1, true);

    protected volatile TrackerState state;
    protected int shardCount;
    protected int shardInstance;
    String shardMethod;
    protected boolean transformContent;
    String shardTemplate;
    protected volatile boolean rollback;
    /**
     * When rollback is set, original error is also gathered in order to provide detailed logging.
     */
    protected Throwable rollbackCausedBy;
    protected final Type type;

    /*
     * A thread handler can be used by subclasses, but they have to intentionally instantiate it.
     */
    ThreadHandler threadHandler;

    /**
     * Default constructor, strictly for testing.
     */
    protected AbstractTracker(Type type)
    {
        this.type = type;
    }
    
    protected AbstractTracker(Properties p, SOLRAPIClient client, String coreName, InformationServer informationServer,Type type)
    {
        this.props = p;
        this.client = client;
        this.coreName = coreName;
        this.infoSrv = informationServer;

        storeRef = new StoreRef(p.getProperty("alfresco.stores", "workspace://SpacesStore"));
        batchCount = Integer.parseInt(p.getProperty("alfresco.batch.count", "5000"));
        maxLiveSearchers =  Integer.parseInt(p.getProperty("alfresco.maxLiveSearchers", "2"));
        
        shardCount =  Integer.parseInt(p.getProperty("shard.count", "1"));
        shardInstance =  Integer.parseInt(p.getProperty("shard.instance", "0"));
        shardMethod = p.getProperty("shard.method", SHARD_METHOD_DBID);

        shardTemplate =  p.getProperty("alfresco.template", "");
        
        transformContent = Boolean.parseBoolean(p.getProperty("alfresco.index.transformContent", "true"));

        this.trackerStats = this.infoSrv.getTrackerStats();
        
        this.type = type;
    }

    
    /**
     * Subclasses must implement behaviour that completes the following steps, in order:
     *
     * <ol>
     *     <li>Purge</li>
     *     <li>Reindex</li>
     *     <li>Index</li>
     *     <li>Track repository</li>
     * </ol>
     */
    protected abstract void doTrack() throws Throwable;


    private boolean assertTrackerStateRemainsNull() {

        /*
        * This assertion is added to accommodate DistributedAlfrescoSolrTrackerRaceTest.
        * The sleep is needed to allow the test case to add a txn into the queue before
        * the tracker makes its call to pull transactions from the test repo client.
        */

        try
        {
            Thread.sleep(5000);
        }
        catch(Exception e)
        {
            // Ignore
        }


        /*
        *  This ensures that getTrackerState does not have the side effect of setting the
        *  state instance variable. This allows classes outside of the tracker framework
        *  to safely call getTrackerState without interfering with the trackers design.
        */

        getTrackerState();

        return state == null;

    }
    /**
     * Template method - subclasses must implement the {@link Tracker}-specific indexing
     * by implementing the abstract method {@link #doTrack()}.
     */
    @Override
    public void track()
    {
        if(runLock.availablePermits() == 0)
        {
            LOGGER.info("... {}  for core [{}] is already in use {}", this.getClass().getSimpleName(), coreName, getClass());
            return;
        }

        try
        {
            /*
            * The runLock ensures that for each tracker type (metadata, content, commit, cascade) only one tracker will
            * be running at a time.
            */
            runLock.acquire();

            if (state==null && Boolean.parseBoolean(System.getProperty("alfresco.test", "false")))
            {
                assert(assertTrackerStateRemainsNull());
            }

            LOGGER.info("[CORE {}] ... Running {}", coreName, this.getClass().getSimpleName());
            
            if(this.state == null)
            {
                this.state = getTrackerState();

                LOGGER.debug("[CORE {}] Global Tracker State set to: {}", coreName, this.state.toString());
                this.state.setRunning(true);
            }
            else
            {
                continueState();
                this.state.setRunning(true);
            }

            infoSrv.registerTrackerThread();

            try
            {
                doTrack();
            }
            catch(IndexTrackingShutdownException t)
            {
                setRollback(true, t);
                LOGGER.info("[CORE {}] Stopping index tracking for {}", coreName, getClass().getSimpleName());
            }
            catch(Throwable t)
            {
                setRollback(true, t);
                if (t instanceof SocketTimeoutException || t instanceof ConnectException)
                {
                    if (LOGGER.isDebugEnabled())
                    {
                        // DEBUG, so give the whole stack trace
                        LOGGER.warn("[CORE {}] Tracking communication timed out for {}", coreName, getClass().getSimpleName(), t);
                    }
                    else
                    {
                        // We don't need the stack trace.  It timed out.
                        LOGGER.warn("[CORE {}] Tracking communication timed out for {}", coreName, getClass().getSimpleName());
                    }
                }
                else
                {
                    LOGGER.error("[CORE {}] Tracking failed for {}", coreName, getClass().getSimpleName(), t);
                }
            }
        }
        catch (InterruptedException e)
        {
            LOGGER.error("[CORE {}] Semaphore interrupted for {}", coreName, getClass().getSimpleName(), e);
        }
        finally
        {
            infoSrv.unregisterTrackerThread();
            ofNullable(state).ifPresent(tstate -> {
                // During a rollback state is set to null.
                state.setRunning(false);
                state.setCheck(false);
            });
            runLock.release();
        }
    }

    public boolean getRollback()
    {
        return this.rollback;
    }
    
    public Throwable getRollbackCausedBy()
    {
        return this.rollbackCausedBy;
    }

    public void setRollback(boolean rollback, Throwable rollbackCausedBy)
    {
        this.rollback = rollback;
        this.rollbackCausedBy = rollbackCausedBy;
    }

    private void continueState()
    {
        infoSrv.continueState(state);
        state.incrementTrackerCycles();
    }

    public synchronized void invalidateState()
    {
        state = null;
    }
    
    @Override
    public synchronized TrackerState getTrackerState()
    {
        if(this.state != null)
        {
           return this.state;
        }
        else
        {
            return this.infoSrv.getTrackerInitialState();
        }
    }

    /**
     * Allows time for the scheduled asynchronous tasks to complete
     */
    synchronized void waitForAsynchronous()
    {
        AbstractWorkerRunnable currentRunnable = this.threadHandler.peekHeadReindexWorker();
        while (currentRunnable != null)
        {
            checkShutdown();
            synchronized (this)
            {
                try
                {
                    wait(100);
                }
                catch (InterruptedException e)
                {
                    // Nothing to be done here
                }
            }
            currentRunnable = this.threadHandler.peekHeadReindexWorker();
        }
    }

    int getMaxLiveSearchers()
    {
        return maxLiveSearchers;
    }

    void checkShutdown()
    {
        if(shutdown)
        {
            throw new IndexTrackingShutdownException();
        }
    }

    @Override
    public boolean isAlreadyInShutDownMode()
    {
        return shutdown;
    }

    @Override
    public void setShutdown(boolean shutdown)
    {
        this.shutdown = shutdown;
    }

    @Override
    public void shutdown()
    {
        setShutdown(true);
        if(this.threadHandler != null)
        {
            threadHandler.shutDownThreadPool();
        }
    }

    public Semaphore getWriteLock()
    {
        return this.writeLock;
    }

    Semaphore getRunLock()
    {
        return this.runLock;
    }

    public Properties getProps()
    {
        return props;
    }

    public Type getType()
    {
        return type;
    }
}