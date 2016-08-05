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

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joel Bernstein
 **/

public class CommitTracker extends AbstractTracker
{
    private List<Tracker> trackers;
    private long lastCommit;
    private long lastSearcherOpened;
    private long commitInterval;
    private long newSearcherInterval;
    private AtomicInteger rollbackCount = new AtomicInteger(0);

    protected final static Logger log = LoggerFactory.getLogger(CommitTracker.class);

    /**
     * Default constructor, for testing.
     **/
    CommitTracker()
    {
        super();
    }

    public CommitTracker(Properties p,
                         SOLRAPIClient client,
                         String coreName,
                         InformationServer informationServer,
                         List<Tracker> trackers)
    {
        super(p, client, coreName, informationServer);
        this.trackers = trackers;
        commitInterval = Long.parseLong(p.getProperty("alfresco.commitInterval", "60000")); // Default: commit once per minute
        newSearcherInterval = Integer.parseInt(p.getProperty("alfresco.newSearcherInterval", "120000")); // Default: Open searchers every two minutes
        lastSearcherOpened = lastCommit = System.currentTimeMillis();
    }

    public boolean hasMaintenance()
    {
        for(Tracker tracker : trackers)
        {
            if(tracker.hasMaintenance())
            {
                return true;
            }
        }

        return false;
    }

    public int getRollbackCount() {
        return rollbackCount.get();
    }

    public void maintenance() throws Exception
    {
        for(Tracker tracker : trackers)
        {
            tracker.maintenance();
        }
    }

    @Override
    protected void doTrack() throws Throwable
    {
        long currentTime = System.currentTimeMillis();
        boolean commitNeeded = false;
        boolean openSearcherNeeded = false;
        boolean hasMaintenance = hasMaintenance();
        //System.out.println("############# Commit Tracker doTrack()");


        if((currentTime - lastCommit) > commitInterval || hasMaintenance)
        {
            commitNeeded = true;
        }

        if(!commitNeeded)
        {
            return;
        }

        if((currentTime - lastSearcherOpened) > newSearcherInterval)
        {
           openSearcherNeeded = true;
        }

        //System.out.println("############# Commit Tracker commit needed");

        try
        {
            for(Tracker tracker : trackers)
            {
                tracker.getWriteLock().acquire();
            }

            //We've acquired all the tracker locks causing all indexing trackers to pause. Now we can do the work below in isolation.

            for(Tracker tracker : trackers) {
                if(tracker.getRollback()) {
                    doRollback();
                    return;
                }
            }

            if(hasMaintenance) {
                maintenance();
            }

            //Do the commit opening the searcher if needed. This will commit all the work done by indexing trackers.
            //This will return immediately and not wait for searchers to warm
            // TODO: put safegaurd in to avoid overlapping warming searchers.
            //System.out.println("################### Commit:"+openSearcherNeeded);
            infoSrv.commit(openSearcherNeeded);

            lastCommit = currentTime;
            if(openSearcherNeeded) {
                lastSearcherOpened = currentTime;
            }
        }
        finally
        {
            for(Tracker tracker : trackers)
            {
                assert(tracker.getWriteLock().availablePermits() == 0);
                tracker.getWriteLock().release();
            }
            //System.out.println("######## Commit Tracker Releasing Write Locks ########");

        }
    }

    protected void doRollback()
    {
        try
        {
            infoSrv.rollback();
        }
        catch (Exception e)
        {
            log.error("Rollback failed", e);
        }
        finally
        {
            //We did the rollback. Even it fails we set rollback to false so we don't continue to rollback over and over again.
            for(Tracker tracker : trackers)
            {
                tracker.setRollback(false);
                tracker.invalidateState();
            }
            rollbackCount.incrementAndGet();
        }
    }

}
