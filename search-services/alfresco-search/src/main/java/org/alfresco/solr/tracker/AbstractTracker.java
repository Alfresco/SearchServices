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
    public static final long TIME_STEP_32_DAYS_IN_MS = 1000 * 60 * 60 * 24 * 32L;
    public static final long TIME_STEP_1_HR_IN_MS = 60 * 60 * 1000L;
    public static final String SHARD_METHOD_ACLID = "ACL_ID";
    public static final String SHARD_METHOD_DBID = "DB_ID";
    protected final static Logger log = LoggerFactory.getLogger(AbstractTracker.class);
    
    protected Properties props;    
    protected SOLRAPIClient client;
    protected InformationServer infoSrv;
    protected String coreName;
    protected StoreRef storeRef;
    protected long batchCount;
    protected boolean isSlave = false;
    protected boolean isMaster = true;
    protected String alfrescoVersion;
    protected TrackerStats trackerStats;
    protected boolean runPostModelLoadInit = true;
    private int maxLiveSearchers;
    private volatile boolean shutdown = false;

    private Semaphore runLock = new Semaphore(1, true);
    private Semaphore writeLock = new Semaphore(1, true);

    protected volatile TrackerState state;
    protected int shardCount;
    protected int shardInstance;
    protected String shardMethod;
    protected boolean transformContent;
    protected String shardTemplate;
    protected volatile boolean rollback;
    protected final Type type;

    
    /*
     * A thread handler can be used by subclasses, but they have to intentionally instantiate it.
     */
    protected ThreadHandler threadHandler;
   
   
   

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
        isSlave =  Boolean.parseBoolean(p.getProperty("enable.slave", "false"));
        isMaster =  Boolean.parseBoolean(p.getProperty("enable.master", "true"));
        
        shardCount =  Integer.parseInt(p.getProperty("shard.count", "1"));
        shardInstance =  Integer.parseInt(p.getProperty("shard.instance", "0"));
        shardMethod = p.getProperty("shard.method", SHARD_METHOD_DBID);

        shardTemplate =  p.getProperty("alfresco.template", "");
        
        transformContent = Boolean.parseBoolean(p.getProperty("alfresco.index.transformContent", "true"));

        this.trackerStats = this.infoSrv.getTrackerStats();

        alfrescoVersion = p.getProperty("alfresco.version", "5.0.0");
        
        this.type = type;
        
        log.info("Solr built for Alfresco version: " + alfrescoVersion);
    }

    
    /**
     * Subclasses must implement behaviour that completes the following steps, in order:
     * <ol>
     *     <li>Purge</li>
     *     <li>Reindex</li>
     *     <li>Index</li>
     *     <li>Track repository</li>
     * </ol>
     * @throws Throwable
     */
    protected abstract void doTrack() throws Throwable;
    
    /**
     * Template method - subclasses must implement the {@link Tracker}-specific indexing
     * by implementing the abstract method {@link #doTrack()}.
     */
    @Override
    public void track()
    {
        if(runLock.availablePermits() == 0) {
            log.info("... " + this.getClass().getSimpleName() + " for core [" + coreName + "] is already in use "+ this.getClass());
            return;
        }

        try
        {
            runLock.acquire();
            log.info("... Running " + this.getClass().getSimpleName() + " for core [" + coreName + "].");

            if(state == null)
            {
                getTrackerState();
                state.setRunning(true);
            }
            else
            {
                continueState();
                state.setRunning(true);
            }

            infoSrv.registerTrackerThread();

            try
            {
                doTrack();
            }
            catch(IndexTrackingShutdownException t)
            {
                setRollback(true);
                log.info("Stopping index tracking for "+coreName);
            }
            catch(Throwable t)
            {
                setRollback(true);
                if (t instanceof SocketTimeoutException || t instanceof ConnectException)
                {
                    if (log.isDebugEnabled())
                    {
                        // DEBUG, so give the whole stack trace
                        log.warn("Tracking communication timed out.", t);
                    }
                    else
                    {
                        // We don't need the stack trace.  It timed out.
                        log.warn("Tracking communication timed out.");
                    }
                }
                else
                {
                    log.error("Tracking failed", t);
                }
            }
        }
        catch (InterruptedException e)
        {
            log.error("Semaphore interrupted", e);
        }
        finally
        {
            infoSrv.unregisterTrackerThread();
            if(state != null) {
                //During a rollback state is set to null.
                state.setRunning(false);
                state.setCheck(false);
            }
            runLock.release();
        }
    }

    public boolean getRollback() {
        return this.rollback;
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    private void continueState() {
        infoSrv.continueState(state);
        state.incrementTrackerCycles();
    }

    public void invalidateState()
    {
        state = null;
    }
    
    @Override
    public TrackerState getTrackerState()
    {
        if(state != null)
        {
           return state;
        }
        else
        {
            state = this.infoSrv.getTrackerInitialState();
            return state;
        }
    }
    
    

    /**
     * Allows time for the scheduled asynchronous tasks to complete
     */
    protected synchronized void waitForAsynchronous()
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
                }
            }
            currentRunnable = this.threadHandler.peekHeadReindexWorker();
        }
    }

    public int getMaxLiveSearchers()
    {
        return maxLiveSearchers;
    }

    protected void checkShutdown()
    {
        if(shutdown)
        {
            throw new IndexTrackingShutdownException();
        }
    }
    
    public void setShutdown(boolean shutdown)
    {
        this.shutdown = shutdown;
    }

    public void shutdown()
    {
        log.warn("Core "+ coreName+" shutdown called on tracker. " + getClass().getSimpleName() + " " + hashCode());
        setShutdown(true);
        if(this.threadHandler != null)
        {
            threadHandler.shutDownThreadPool();
        }
    }

    public Semaphore getWriteLock() {
        return this.writeLock;
    }

    public Semaphore getRunLock() {
        return this.runLock;
    }


    /**
     * @return Alfresco version Solr was built for
     */
    @Override
    public String getAlfrescoVersion()
    {
        return alfrescoVersion;
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
