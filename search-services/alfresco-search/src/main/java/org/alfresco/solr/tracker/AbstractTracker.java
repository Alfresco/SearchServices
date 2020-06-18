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

import static java.util.Optional.ofNullable;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

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

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
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
    protected final Type type;
    protected final String trackerId;

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
        this.trackerId = type + "@" + hashCode();
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

        this.trackerId = type + "@" + hashCode();
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
     *
     * @param iterationId an identifier which is uniquely associated with a given iteration.
     */
    protected abstract void doTrack(String iterationId) throws Throwable;


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
     * by implementing the abstract method {@link #doTrack(String)}.
     */
    @Override
    public void track()
    {
        String iterationId = "IT #" + System.currentTimeMillis();

        if(runLock.availablePermits() == 0)
        {
            logger.info("[{} / {} / {}] Tracker already registered.", coreName, trackerId, iterationId);
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

            if(this.state == null)
            {
                this.state = getTrackerState();

                logger.debug("[{} / {} / {}]  Global Tracker State set to: {}", coreName, trackerId, iterationId, this.state.toString());
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
                doTrack(iterationId);
            }
            catch(IndexTrackingShutdownException t)
            {
                setRollback(true);
                logger.info("[{} / {} / {}] Tracking cycle stopped. See the stacktrace below for further details.", coreName, trackerId, iterationId, t);
            }
            catch(Throwable t)
            {
                setRollback(true);
                if (t instanceof SocketTimeoutException || t instanceof ConnectException)
                {
                    logger.warn("[{} / {} / {}] Tracking communication timed out. See the stacktrace below for further details.", coreName, trackerId, iterationId);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("[{} / {} / {}] Stack trace", coreName, trackerId, iterationId, t);
                    }
                }
                else
                {
                    logger.error("[{} / {} / {}] Tracking failure. See the stacktrace below for further details.", coreName, trackerId, iterationId, t);
                }
            }
        }
        catch (Exception exception)
        {
            logger.error("[{} / {} / {}] Some problem was detected while resetting the Tracker State. See the stracktrace below for further details."
                            , coreName, trackerId, iterationId, exception);
        }
        finally
        {
            infoSrv.unregisterTrackerThread();

            ofNullable(state).ifPresent(this::turnOff);

            runLock.release();
        }
    }

    /**
     * At the end of the tracking method, the {@link TrackerState} should be turned off.
     * However, during a rollback (that could be started by another tracker) the {@link TrackerState} instance
     * could be set to null, even after passing a null check we could get a NPE.
     * For that reason, this method turns off the tracker state and ignore any {@link NullPointerException} (actually
     * any exception) that could be thrown.
     */
    private void turnOff(TrackerState state) {
        try
        {
            state.setRunning(false);
            state.setCheck(false);
        } catch (Exception exception)
        {
            logger.error("Unable to properly turn off the TrackerState instance. See the stacktrace below for further details.", exception);
        }
    }

    public boolean getRollback()
    {
        return this.rollback;
    }

    public void setRollback(boolean rollback)
    {
        this.rollback = rollback;
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
