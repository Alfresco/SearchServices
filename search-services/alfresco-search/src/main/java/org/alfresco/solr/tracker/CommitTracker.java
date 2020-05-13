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

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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
    private long lastCommit;
    private long lastSearcherOpened;
    private long commitInterval;
    private long newSearcherInterval;
    private MetadataTracker metadataTracker;
    private AclTracker aclTracker;
    private ContentTracker contentTracker;
    /** The cascade tracker. Note that this may be empty if cascade tracking is disabled. */
    private Optional<CascadeTracker> cascadeTracker = empty();
    private AtomicInteger rollbackCount = new AtomicInteger(0);

    protected final static Logger LOGGER = LoggerFactory.getLogger(CommitTracker.class);
    
    // Share run and write locks across all CommitTracker threads
    private static Map<String, Semaphore> RUN_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private static Map<String, Semaphore> WRITE_LOCK_BY_CORE = new ConcurrentHashMap<>();
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

    /**
     * Default constructor, for testing.
     **/
    CommitTracker()
    {
        super(Tracker.Type.COMMIT);
    }

    public CommitTracker(Properties p,
                         SOLRAPIClient client,
                         String coreName,
                         InformationServer informationServer,
                         List<Tracker> trackers)
    {
        super(p, client, coreName, informationServer, Tracker.Type.COMMIT);

        //Set the trackers
        for(Tracker tracker : trackers) {
            if(tracker instanceof MetadataTracker) {
                this.metadataTracker = (MetadataTracker) tracker;
            } else if(tracker instanceof AclTracker) {
                this.aclTracker = (AclTracker)tracker;
            } else if(tracker instanceof ContentTracker) {
                this.contentTracker = (ContentTracker)tracker;
            } else if(tracker instanceof CascadeTracker) {
                this.cascadeTracker = ofNullable((CascadeTracker) tracker);
            }
        }

        commitInterval = Long.parseLong(p.getProperty("alfresco.commitInterval", "60000")); // Default: commit once per minute
        newSearcherInterval = Integer.parseInt(p.getProperty("alfresco.newSearcherInterval", "120000")); // Default: Open searchers every two minutes
        lastSearcherOpened = lastCommit = System.currentTimeMillis();
        
        RUN_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
        WRITE_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
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
    protected void doTrack(String iterationId) throws Throwable
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

                /*
                * The metadataTracker and aclTracker will return true if an unhandled exception has occurred during indexing.
                *
                * The doRollback method rolls the index back to the state that it was in at the last commit. This will undo
                * all the work that has been done by other trackers after the last commit.
                *
                * The state of the other trackers is then set to null so the trackers will initialize their state from
                * the index, rather then the in-memory state. This keeps the trackers in-sync with index if their work is
                * rolled back.
                */

                doRollback();
                return;
            }

            if(hasMaintenance) {
                maintenance();
            }

            //Do the commit opening the searcher if needed. This will commit all the work done by indexing trackers.
            //This will return immediately and not wait for searchers to warm
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
        }
    }

    protected void doRollback()
    {
        try
        {
            //Acquire the locks for the content tracker and cascade tracker
            contentTracker.getWriteLock().acquire();
            assert(contentTracker.getWriteLock().availablePermits() == 0);

            if (cascadeTracker.isPresent())
            {
                cascadeTracker.get().getWriteLock().acquire();
                assert (cascadeTracker.get().getWriteLock().availablePermits() == 0);
            }

            infoSrv.rollback();
            
            // Log reasons why the rollback is performed
            if (aclTracker.getRollbackCausedBy() != null)
            {
                LOGGER.warn("Rollback performed due to ACL Tracker error", aclTracker.getRollbackCausedBy());                
            }
            if (metadataTracker.getRollbackCausedBy() != null)
            {
                LOGGER.warn("Rollback performed due to Metadata Tracker error", metadataTracker.getRollbackCausedBy());
            }
            
        }
        catch (Exception e)
        {
            LOGGER.error("Rollback failed", e);
        }
        finally
        {
            //Reset acl Tracker
            aclTracker.setRollback(false, null);
            aclTracker.invalidateState();

            //Reset metadataTracker
            metadataTracker.setRollback(false, null);
            metadataTracker.invalidateState();

            //Reset contentTracker
            contentTracker.setRollback(false, null);
            contentTracker.invalidateState();

            //Reset cascadeTracker
            cascadeTracker.ifPresent(c -> c.setRollback(false, null));
            cascadeTracker.ifPresent(c -> invalidateState());

            //Release the locks
            contentTracker.getWriteLock().release();
            cascadeTracker.ifPresent(c -> c.getWriteLock().release());

            rollbackCount.incrementAndGet();
        }
    }

}
