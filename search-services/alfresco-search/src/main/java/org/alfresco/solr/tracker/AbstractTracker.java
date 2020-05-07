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
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTracker.class);
    
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
    protected final String trackerId;

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

        if(getRunLock().availablePermits() == 0)
        {
            LOGGER.info("[{} / {} / {}] Tracker already registered.", coreName, trackerId, iterationId);
            return;
        }

        try
        {
            /*
            * The runLock ensures that for each tracker type (metadata, content, commit, cascade) only one tracker will
            * be running at a time.
            */
            getRunLock().acquire();

            if (state==null && Boolean.parseBoolean(System.getProperty("alfresco.test", "false")))
            {
                assert(assertTrackerStateRemainsNull());
            }

            if(this.state == null)
            {
                this.state = getTrackerState();

                LOGGER.debug("[{} / {} / {}]  Global Tracker State set to: {}", coreName, trackerId, iterationId, this.state.toString());
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
                setRollback(true, t);
                LOGGER.info("[{} / {} / {}] Tracking cycle stopped. See the stacktrace below for further details.", coreName, trackerId, iterationId, t);
            }
            catch(Throwable t)
            {
                setRollback(true, t);
                if (t instanceof SocketTimeoutException || t instanceof ConnectException)
                {
                    LOGGER.warn("[{} / {} / {}] Tracking communication timed out. See the stacktrace below for further details.", coreName, trackerId, iterationId);
                    LOGGER.debug("[{} / {} / {}] Stack trace", coreName, trackerId, iterationId, t);
                }
                else
                {
                    LOGGER.error("[{} / {} / {}] Tracking failure. See the stacktrace below for further details.", coreName, trackerId, iterationId, t);
                }
            }
        }
        catch (InterruptedException e)
        {
            LOGGER.error("[{} / {} / {}] Semaphore interruption. See the stacktrace below for further details.", coreName, trackerId, iterationId, e);
        }
        finally
        {
            infoSrv.unregisterTrackerThread();
            ofNullable(state).ifPresent(tstate -> {
                // During a rollback state is set to null.
                state.setRunning(false);
                state.setCheck(false);
            });
            getRunLock().release();
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
    }

    /**
     * Trackers implementing this method should decide if the Write Lock is applied
     * globally for every Tracker Thread (static) or locally for each running Thread
     */
    public abstract Semaphore getWriteLock();

    /**
     * Trackers implementing this method should decide if the Run Lock is applied
     * globally for every Tracker Thread (static) or locally for each running Thread
     */
    public abstract Semaphore getRunLock();

    public Properties getProps()
    {
        return props;
    }

    public Type getType()
    {
        return type;
    }
}