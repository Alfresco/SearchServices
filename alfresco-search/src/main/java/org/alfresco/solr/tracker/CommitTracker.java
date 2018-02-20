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
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joel Bernstein
 **/

public class CommitTracker extends AbstractTracker
{
    private long lastCommit;
    private long lastSearcherOpened;
    private long commitInterval;
    private long newSearcherInterval;
    private MetadataTracker metadataTracker;
    private AclTracker aclTracker;
    private ContentTracker contentTracker;
    private CascadeTracker cascadeTracker;
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

        //Set the trackers
        for(Tracker tracker : trackers) {
            if(tracker instanceof MetadataTracker) {
                this.metadataTracker = (MetadataTracker) tracker;
            } else if(tracker instanceof AclTracker) {
                this.aclTracker = (AclTracker)tracker;
            } else if(tracker instanceof ContentTracker) {
                this.contentTracker = (ContentTracker)tracker;
            } else if(tracker instanceof CascadeTracker) {
                this.cascadeTracker = (CascadeTracker)tracker;
            }
        }

        commitInterval = Long.parseLong(p.getProperty("alfresco.commitInterval", "60000")); // Default: commit once per minute
        newSearcherInterval = Integer.parseInt(p.getProperty("alfresco.newSearcherInterval", "120000")); // Default: Open searchers every two minutes
        lastSearcherOpened = lastCommit = System.currentTimeMillis();
    }

    public boolean hasMaintenance() throws Exception
    {
        return (metadataTracker.hasMaintenance() || aclTracker.hasMaintenance());
    }

    public int getRollbackCount() {
        return rollbackCount.get();
    }

    public void maintenance() throws Exception
    {
        metadataTracker.maintenance();
        aclTracker.maintenance();
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
            metadataTracker.getWriteLock().acquire();
            assert(metadataTracker.getWriteLock().availablePermits() == 0);

            aclTracker.getWriteLock().acquire();
            assert(aclTracker.getWriteLock().availablePermits() == 0);

            //See if we need a rollback
            if(metadataTracker.getRollback() || aclTracker.getRollback()) {
                doRollback();
                return;
            }

            if(hasMaintenance) {
                maintenance();
            }

            //Do the commit opening the searcher if needed. This will commit all the work done by indexing trackers.
            //This will return immediately and not wait for searchers to warm
            //System.out.println("################### Commit:"+openSearcherNeeded);
            boolean searcherOpened = infoSrv.commit(openSearcherNeeded);

            lastCommit = currentTime;
            if(searcherOpened) {
                lastSearcherOpened = currentTime;
            }
        }
        finally
        {
            //Release the lock on the metadata Tracker
            metadataTracker.getWriteLock().release();

            //Release the lock on the aclTracker
            aclTracker.getWriteLock().release();
            //System.out.println("######## Commit Tracker Releasing Write Locks ########");
        }
    }

    protected void doRollback()
    {
        try
        {
            //Acquire the locks for the content tracker and cascade tracker
            contentTracker.getWriteLock().acquire();
            assert(contentTracker.getWriteLock().availablePermits() == 0);

            cascadeTracker.getWriteLock().acquire();
            assert(cascadeTracker.getWriteLock().availablePermits() == 0);

            infoSrv.rollback();
        }
        catch (Exception e)
        {
            log.error("Rollback failed", e);
        }
        finally
        {
            //Reset acl Tracker
            aclTracker.setRollback(false);
            aclTracker.invalidateState();

            //Reset metadataTracker
            metadataTracker.setRollback(false);
            metadataTracker.invalidateState();

            //Reset contentTracker
            contentTracker.setRollback(false);
            contentTracker.invalidateState();

            //Reset cascadeTracker
            cascadeTracker.setRollback(false);
            cascadeTracker.invalidateState();

            //Release the locks
            contentTracker.getWriteLock().release();
            cascadeTracker.getWriteLock().release();

            rollbackCount.incrementAndGet();
        }
    }

}
