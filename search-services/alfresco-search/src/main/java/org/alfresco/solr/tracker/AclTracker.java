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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.solr.AclReport;
import org.alfresco.solr.BoundedDeque;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclChangeSets;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.SOLRAPIClient;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multithreaded ACL {@link Tracker} implementation.
 * 
 * @author Matt Ward
 **/

public class AclTracker extends ActivatableTracker
{
    protected final static Logger LOGGER = LoggerFactory.getLogger(AclTracker.class);

    private static final int ACL_CHANGE_SETS_FOUND_QUEUE_SIZE = 100;
    private static final int DEFAULT_CHANGE_SET_ACLS_BATCH_SIZE = 2000;
    private static final int DEFAULT_ACL_BATCH_SIZE = 100;
    private static final int DEFAULT_ACL_TRACKER_MAX_PARALLELISM = 32;
    private static final long DEFAULT_ACL_TRACKER_TIMESTEP = TIME_STEP_1_HR_IN_MS;

    protected static final long INITIAL_MAX_ACL_CHANGE_SET_ID = 2000L;
    private static final int MAX_NUMBER_OF_ACL_CHANGE_SETS = 2000;

    private static final long MAX_TIME_STEP = TIME_STEP_32_DAYS_IN_MS;
    
    // Repository Remote API doesn't accept more than 512 aclChangeSetIds by invocation
    private static final int MAX_ACL_CHANGE_SET_BATCH_SIZE = 512;

    private int aclTrackerParallelism;

    private int changeSetAclsBatchSize;
    private int aclBatchSize;
    private long timeStep;
    private int maxNumberOfAclChangeSets;

    private ConcurrentLinkedQueue<Long> aclChangeSetsToReindex = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> aclChangeSetsToIndex = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> aclChangeSetsToPurge = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> aclsToReindex = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> aclsToIndex = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> aclsToPurge = new ConcurrentLinkedQueue<>();
    private DocRouter docRouter;

    private ForkJoinPool forkJoinPool;

    // Share run and write locks across all AclTracker threads
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
     */
    AclTracker()
    {
        super(Tracker.Type.ACL);
    }
    
    public AclTracker(Properties p, SOLRAPIClient client,
                String coreName, InformationServer informationServer)
    {
        super(p, client, coreName, informationServer, Tracker.Type.ACL);
        changeSetAclsBatchSize = Integer.parseInt(p.getProperty("alfresco.changeSetAclsBatchSize",
                String.valueOf(DEFAULT_CHANGE_SET_ACLS_BATCH_SIZE)));
        if (changeSetAclsBatchSize > MAX_ACL_CHANGE_SET_BATCH_SIZE)
        {
            LOGGER.warn("Max value for 'alfresco.changeSetAclsBatchSize' is 512. "
                      + "This value is being taken instead of the one specified in 'solrcore.properties': " + changeSetAclsBatchSize);
            changeSetAclsBatchSize = MAX_ACL_CHANGE_SET_BATCH_SIZE;
        }
        aclBatchSize = Integer.parseInt(p.getProperty("alfresco.aclBatchSize",
                String.valueOf(DEFAULT_ACL_BATCH_SIZE)));
        docRouter = DocRouterFactory.getRouter(p, shardMethod);

        aclTrackerParallelism = Integer.parseInt(p.getProperty("alfresco.acl.tracker.maxParallelism",
                String.valueOf(DEFAULT_ACL_TRACKER_MAX_PARALLELISM)));
        forkJoinPool = new ForkJoinPool(aclTrackerParallelism);

        timeStep = Long.parseLong(p.getProperty("alfresco.acl.tracker.timestep",
                String.valueOf(DEFAULT_ACL_TRACKER_TIMESTEP)));

        maxNumberOfAclChangeSets = Integer.parseInt(p.getProperty("alfresco.acl.tracker.maxNumberOfAclChangeSets",
                String.valueOf(MAX_NUMBER_OF_ACL_CHANGE_SETS)));

        RUN_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
        WRITE_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
    }

    @Override
    protected void doTrack(String iterationId) throws Throwable
    {
        trackRepository();
    }

    public void maintenance() throws Exception
    {
        purgeAclChangeSets();
        purgeAcls();
        reindexAclChangeSets();
        reindexAcls();
        indexAclChangeSets();
        indexAcls();
    }


    public boolean hasMaintenance()
    {
        return  aclChangeSetsToReindex.size() > 0 ||
                aclChangeSetsToIndex.size() > 0 ||
                aclChangeSetsToPurge.size() > 0 ||
                aclsToReindex.size() > 0 ||
                aclsToIndex.size() > 0 ||
                aclsToPurge.size() > 0;
    }

    protected void indexAclChangeSets() throws AuthenticationException, IOException, JSONException
    {
        boolean requiresCommit = false;
        while (aclChangeSetsToIndex.peek() != null)
        {
            Long aclChangeSetId = aclChangeSetsToIndex.poll();
            if (aclChangeSetId != null)
            {
                AclChangeSets aclChangeSets = client.getAclChangeSets(null, aclChangeSetId,
                        null, aclChangeSetId+1, 1);
                if ((aclChangeSets.getAclChangeSets().size() > 0) &&
                        aclChangeSetId.equals(aclChangeSets.getAclChangeSets().get(0).getId()))
                {
                    AclChangeSet changeSet = aclChangeSets.getAclChangeSets().get(0);
                    List<Acl> acls = client.getAcls(Collections.singletonList(changeSet), null, Integer.MAX_VALUE);
                    for (Acl acl : acls)
                    {
                        List<AclReaders> readers = client.getAclReaders(Collections.singletonList(acl));
                        indexAcl(readers, false);
                    }
                    this.infoSrv.indexAclTransaction(changeSet, false);
                    LOGGER.info("[CORE {}] - INDEX ACTION - AclChangeSetId {} has been indexed", coreName, aclChangeSetId);
                    requiresCommit = true;
                }
                else
                {
                    LOGGER.info("[CORE {}] - INDEX ACTION - AclChangeSetId {} was not found in database, " +
                            "it has NOT been reindexed", coreName, aclChangeSetId);
                }
            }
            checkShutdown();
        }
        if(requiresCommit)
        {
            checkShutdown();
            //this.infoSrv.commit();
        }
    }
    
    protected void indexAcls() throws AuthenticationException, IOException, JSONException
    {
        while (aclsToIndex.peek() != null)
        {
            Long aclId = aclsToIndex.poll();
            if (aclId != null)
            {
                //System.out.println("############## Indexing ACL ID:"+aclId);
                Acl acl = new Acl(0, aclId);
                List<AclReaders> readers = client.getAclReaders(Collections.singletonList(acl));
                //AclReaders r = readers.get(0);
                //System.out.println("############## READERS ID:"+r.getId()+":"+r.getReaders());
                indexAcl(readers, false);
                LOGGER.info("[CORE {}] - INDEX ACTION - AclId {} has been indexed", coreName, aclId);
            }
            checkShutdown();
        }
    }

    protected void reindexAclChangeSets() throws AuthenticationException, IOException, JSONException
    {
        boolean requiresCommit = false;
        while (aclChangeSetsToReindex.peek() != null)
        {
            Long aclChangeSetId = aclChangeSetsToReindex.poll();
            if (aclChangeSetId != null)
            {
                this.infoSrv.deleteByAclChangeSetId(aclChangeSetId);

                AclChangeSets aclChangeSets = client.getAclChangeSets(null, aclChangeSetId,
                        null, aclChangeSetId+1, 1);
                if ((aclChangeSets.getAclChangeSets().size() > 0) &&
                        aclChangeSetId.equals(aclChangeSets.getAclChangeSets().get(0).getId()))
                {
                    AclChangeSet changeSet = aclChangeSets.getAclChangeSets().get(0);
                    List<Acl> acls = client.getAcls(Collections.singletonList(changeSet),
                            null, Integer.MAX_VALUE);
                    for (Acl acl : acls)
                    {
                        List<AclReaders> readers = client.getAclReaders(Collections.singletonList(acl));
                        indexAcl(readers, true);
                    }

                    this.infoSrv.indexAclTransaction(changeSet, true);
                    LOGGER.info("[CORE {}] - REINDEX ACTION - AclChangeSetId {} has been reindexed",
                            coreName, aclChangeSetId);
                    requiresCommit = true;
                }
                else
                {
                    LOGGER.info("[CORE {}] - REINDEX ACTION - AclChangeSetId {} was not found in database, " +
                                    "it has NOT been reindexed", coreName, aclChangeSetId);
                }
            }
            checkShutdown();
        }
        if(requiresCommit)
        {
            checkShutdown();
            //this.infoSrv.commit();
        }
    }

    protected void reindexAcls() throws AuthenticationException, IOException, JSONException
    {
        boolean requiresCommit = false;
        while (aclsToReindex.peek() != null)
        {
            Long aclId = aclsToReindex.poll();
            if (aclId != null)
            {
                this.infoSrv.deleteByAclId(aclId);

                Acl acl = new Acl(0, aclId);
                List<AclReaders> readers = client.getAclReaders(Collections.singletonList(acl));
                indexAcl(readers, true);
                LOGGER.info("[CORE {}] - REINDEX ACTION - aclId {} has been reindexed", coreName, aclId);
                requiresCommit = true;
            }
            checkShutdown();
        }
        if(requiresCommit)
        {
            checkShutdown();
            //this.infoSrv.commit();
        }
    }

    protected void purgeAclChangeSets() throws IOException, JSONException
    {       
        while (aclChangeSetsToPurge.peek() != null)
        {
            Long aclChangeSetId = aclChangeSetsToPurge.poll();
            if (aclChangeSetId != null)
            {
                this.infoSrv.deleteByAclChangeSetId(aclChangeSetId);
                LOGGER.info("[CORE {}] - PURGE ACTION - Purged aclChangeSetId {}", coreName, aclChangeSetId);
            }
            checkShutdown();
        }
    }
    
    protected void purgeAcls() throws IOException, JSONException
    {
        while (aclsToPurge.peek() != null)
        {
            Long aclId = aclsToPurge.poll();
            if (aclId != null)
            {
                this.infoSrv.deleteByAclId(aclId);
                LOGGER.info("[CORE {}] - PURGE ACTION - Purged aclId {}", coreName, aclId);                
            }
            checkShutdown();
        }
    }


    // ACL change sets

    public void addAclChangeSetToReindex(Long aclChangeSetToReindex)
    {
        aclChangeSetsToReindex.offer(aclChangeSetToReindex);
    }

    public void addAclChangeSetToIndex(Long aclChangeSetToIndex)
    {
        aclChangeSetsToIndex.offer(aclChangeSetToIndex);
    }

    public void addAclChangeSetToPurge(Long aclChangeSetToPurge)
    {
        aclChangeSetsToPurge.offer(aclChangeSetToPurge);
    }

    // ACLs

    public void addAclToReindex(Long aclToReindex)
    {
        aclsToReindex.offer(aclToReindex);
    }

    public void addAclToIndex(Long aclToIndex)
    {
        aclsToIndex.offer(aclToIndex);
    }

    public void addAclToPurge(Long aclToPurge)
    {
        aclsToPurge.offer(aclToPurge);
    }

    @Override
    protected void clearScheduledMaintenanceWork()
    {
        logAndClear(aclChangeSetsToIndex, "ACL ChangeSets to be indexed");
        logAndClear(aclsToIndex, "ACLs to be indexed");

        logAndClear(aclChangeSetsToReindex, "ACL ChangeSets to be re-indexed");
        logAndClear(aclsToReindex, "ACLs to be re-indexed");

        logAndClear(aclChangeSetsToPurge, "ACL ChangeSets to be purged");
        logAndClear(aclsToPurge, "ACLs to be purged");
    }

    protected void trackRepository() throws IOException, AuthenticationException, JSONException
    {
        checkShutdown();

        TrackerState state = super.getTrackerState();

        // Check we are tracking the correct repository
        if(state.getTrackerCycles() == 0)
        {
            //We have a new tracker state so do the checks.
            checkRepoAndIndexConsistency(state);
        }

        checkShutdown();
        trackAclChangeSets();
    }
    
    /**
     * Checks the first and last TX time
     */
    protected void checkRepoAndIndexConsistency(TrackerState state) throws AuthenticationException, IOException, JSONException
    {
        if (state.getLastGoodChangeSetCommitTimeInIndex() != 0 && state.isCheckedFirstAclTransactionTime() && state.isCheckedLastAclTransactionTime())
        {
            // Verification done previously.
            return;
        }

        // Load the first ACL change sets from the Repository.
        AclChangeSets firstChangeSets = client.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1);

        if (state.getLastGoodChangeSetCommitTimeInIndex() == 0)
        {
            state.setCheckedLastAclTransactionTime(true);
            state.setCheckedFirstAclTransactionTime(true);
            LOGGER.info("[CORE {}] - No acl transactions found - no verification required", coreName);

            if (!firstChangeSets.getAclChangeSets().isEmpty())
            {
                AclChangeSet firstChangeSet = firstChangeSets.getAclChangeSets().get(0);
                long firstChangeSetCommitTime = firstChangeSet.getCommitTimeMs();
                state.setLastGoodChangeSetCommitTimeInIndex(firstChangeSetCommitTime);
                setLastChangeSetIdAndCommitTimeInTrackerState(firstChangeSets.getAclChangeSets(), state);
            }
        }
        
        if (!state.isCheckedFirstAclTransactionTime() && !firstChangeSets.getAclChangeSets().isEmpty())
        {
            AclChangeSet firstAclChangeSet = firstChangeSets.getAclChangeSets().get(0);
            long firstAclTxId = firstAclChangeSet.getId();
            long firstAclTxCommitTime = firstAclChangeSet.getCommitTimeMs();
            int setSize = this.infoSrv.getAclTxDocsSize(Long.toString(firstAclTxId),
                    Long.toString(firstAclTxCommitTime));

            if (setSize == 0)
            {
                LOGGER.error("[CORE {}] First acl transaction was not found with the correct timestamp.", coreName);
                LOGGER.error("SOLR has successfully connected to your repository " +
                        "however the SOLR indexes and repository database do not match.");
                LOGGER.error("If this is a new or rebuilt database your SOLR indexes " +
                        "also need to be re-built to match the database.");
                LOGGER.error("Notice that SOLR will continue to track the repository, but the index may be corrupted.");
                LOGGER.error("You can also check your SOLR connection details in solrcore.properties.");
                throw new AlfrescoRuntimeException("Initial ACL transaction from DB with Id=" + firstAclTxId
                        + " and Timestamp=" + firstAclTxCommitTime + " was not found in Solr core.");
            }
            else if (setSize == 1)
            {
                state.setCheckedFirstAclTransactionTime(true);
                LOGGER.info("[CORE {}] Verified first acl transaction and timestamp in index", coreName);
            }
            else
            {
                LOGGER.warn("[CORE {}] Duplicate initial acl transaction found with correct timestamp", coreName);
            }
        }

        // Checks that the last aclTxId in solr is <= last aclTxId in repo
        if (!state.isCheckedLastAclTransactionTime())
        {
            Long maxChangeSetCommitTimeInRepo = firstChangeSets.getMaxChangeSetCommitTime();
            Long maxChangeSetIdInRepo = firstChangeSets.getMaxChangeSetId();

            if (maxChangeSetCommitTimeInRepo != null && maxChangeSetIdInRepo != null)
            {
                // We know the server has at least as many transactions as the index.
                state.setLastChangeSetCommitTimeOnServer(maxChangeSetCommitTimeInRepo);
                state.setLastChangeSetIdOnServer(maxChangeSetIdInRepo);

                AclChangeSet maxAclTxInIndex = this.infoSrv.getMaxAclChangeSetIdAndCommitTimeInIndex();
                if (maxAclTxInIndex.getCommitTimeMs() > maxChangeSetCommitTimeInRepo)
                {
                    LOGGER.error("[CORE {}] Last acl transaction was found in index with " +
                            "timestamp later than that of repository.", coreName);
                    LOGGER.error("Max Acl Tx In Index: " + maxAclTxInIndex.getId() + ", In Repo: " + maxChangeSetIdInRepo);
                    LOGGER.error("Max Acl Tx Commit Time In Index: " + maxAclTxInIndex.getCommitTimeMs() + ", In Repo: "
                            + maxChangeSetCommitTimeInRepo);
                    LOGGER.error("SOLR has successfully connected to your repository" +
                            " however the SOLR indexes and repository database do not match.");
                    LOGGER.error("If this is a new or rebuilt database your SOLR indexes " +
                            "also need to be re-built to match the database.");
                    LOGGER.error("You can also check your SOLR connection details in solrcore.properties.");
                    throw new AlfrescoRuntimeException("Last acl transaction found in index with incorrect timestamp");
                }
                else
                {
                    state.setCheckedLastAclTransactionTime(true);
                    LOGGER.info("[CORE {}] - Verified last acl transaction timestamp in index less" +
                            " than or equal to that of repository.", coreName);
                }
            }
        }
    }

    /**
     * @param changeSetsFound BoundedDeque<AclChangeSet>
     * @param lastGoodChangeSetCommitTimeInIndex long
     * @return Long
     */
    protected Long getChangeSetFromCommitTime(BoundedDeque<AclChangeSet> changeSetsFound, long lastGoodChangeSetCommitTimeInIndex)
    {
        if(changeSetsFound.size() > 0)
        {
            return changeSetsFound.getLast().getCommitTimeMs();
        }
        else
        {
            return lastGoodChangeSetCommitTimeInIndex;
        }
    }

    protected AclChangeSets getSomeAclChangeSets(BoundedDeque<AclChangeSet> changeSetsFound, Long fromCommitTime,
                                                 long timeStep, int maxResults, long endTime)
            throws AuthenticationException, IOException, JSONException
    {
        long actualTimeStep  = timeStep;

        AclChangeSets aclChangeSets;
        // step forward in time until we find something or hit the time bound
        // max id unbounded
        Long startTime = fromCommitTime == null ? Long.valueOf(0L) : fromCommitTime;
        do
        {
            aclChangeSets = client.getAclChangeSets(startTime, null,
                    startTime + actualTimeStep, null, maxResults);
            startTime += actualTimeStep;
            actualTimeStep *= 2;
            if(actualTimeStep > MAX_TIME_STEP)
            {
                actualTimeStep = MAX_TIME_STEP;
            }
        }
        while( ((aclChangeSets.getAclChangeSets().size() == 0) && (startTime < endTime)) ||
                ((aclChangeSets.getAclChangeSets().size() > 0) && alreadyFoundChangeSets(changeSetsFound, aclChangeSets)));

        return aclChangeSets;

    }

    private boolean alreadyFoundChangeSets(BoundedDeque<AclChangeSet> changeSetsFound, AclChangeSets aclChangeSets)
    {
        if(changeSetsFound.size() == 0)
        {
            return false;
        }

        if(aclChangeSets.getAclChangeSets().size() == 1)
        {
            return aclChangeSets.getAclChangeSets().get(0).getId() == changeSetsFound.getLast().getId();
        }
        else
        {
            HashSet<AclChangeSet> alreadyFound = new HashSet<>(changeSetsFound.getDeque());
            for(AclChangeSet aclChangeSet : aclChangeSets.getAclChangeSets())
            {
                if(!alreadyFound.contains(aclChangeSet))
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * @param aclReaderList List<AclReaders>
     * @param overwrite boolean
     */
    protected void indexAcl(List<AclReaders> aclReaderList, boolean overwrite) throws IOException
    {
        long time = this.infoSrv.indexAcl(aclReaderList, overwrite);
        trackerStats.addAclTime(time);
    }

    public IndexHealthReport checkIndex(Long toAclTx, Long fromTime, Long toTime)
                throws AuthenticationException, IOException, JSONException
    {   
        // DB ACL TX Count
        long firstChangeSetCommitTimex = 0;
        AclChangeSets firstChangeSets = client.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1);
        if(firstChangeSets.getAclChangeSets().size() > 0)
        {
            AclChangeSet firstChangeSet = firstChangeSets.getAclChangeSets().get(0);
            firstChangeSetCommitTimex = firstChangeSet.getCommitTimeMs();
        }

        IOpenBitSet aclTxIdsInDb = infoSrv.getOpenBitSetInstance();
        long lastAclTxCommitTime = firstChangeSetCommitTimex;
        if (fromTime != null)
        {
            lastAclTxCommitTime = fromTime;
        }
        
        long maxAclTxId = 0;
        Long minAclTxId = null;
        long endTime = System.currentTimeMillis() + infoSrv.getHoleRetention();
        AclChangeSets aclTransactions;
        BoundedDeque<AclChangeSet> changeSetsFound = new  BoundedDeque<>(ACL_CHANGE_SETS_FOUND_QUEUE_SIZE);
        DO: do
        {
            aclTransactions = getSomeAclChangeSets(changeSetsFound,
                    lastAclTxCommitTime, timeStep, maxNumberOfAclChangeSets, endTime);
            for (AclChangeSet set : aclTransactions.getAclChangeSets())
            {
                // include
                if (toTime != null)
                {
                    if (set.getCommitTimeMs() > toTime)
                    {
                        break DO;
                    }
                }
                if (toAclTx != null)
                {
                    if (set.getId() > toAclTx)
                    {
                        break DO;
                    }
                }

                // bounds for later loops
                if (minAclTxId == null)
                {
                    minAclTxId = set.getId();
                }
                if (maxAclTxId < set.getId())
                {
                    maxAclTxId = set.getId();
                }

                lastAclTxCommitTime = set.getCommitTimeMs();
                aclTxIdsInDb.set(set.getId());
                changeSetsFound.add(set);
            }
        }
        while (aclTransactions.getAclChangeSets().size() > 0);
        
        return this.infoSrv.reportAclTransactionsInIndex(minAclTxId, aclTxIdsInDb, maxAclTxId);
    }

    public List<Long> getAclsForDbAclTransaction(Long acltxid)
    {
        try
        {
            ArrayList<Long> answer = new ArrayList<>();
            AclChangeSets changeSet = client.getAclChangeSets(null, acltxid, null,
                    acltxid+1, 1);
            List<Acl> acls = client.getAcls(changeSet.getAclChangeSets(), null, Integer.MAX_VALUE);
            for (Acl acl : acls)
            {
                answer.add(acl.getId());
            }
            return answer;
        }
        catch (IOException | JSONException | AuthenticationException e)
        {
            throw new AlfrescoRuntimeException("Failed to get acls", e);
        }
    }

    public AclReport checkAcl(Long aclid)
    {
        AclReport aclReport = new AclReport();
        aclReport.setAclId(aclid);

        // In DB

        try
        {
            List<AclReaders> readers = client.getAclReaders(Collections.singletonList(new Acl(0, aclid)));
            aclReport.setExistsInDb(readers.size() == 1);
        }
        catch (IOException | JSONException | AuthenticationException e)
        {
            aclReport.setExistsInDb(false);
        }

        // In Index
        return this.infoSrv.checkAclInIndex(aclid, aclReport);
    }
    
    /**
     * Every ACL Change Set contains a list of ACLs to be indexed.
     * This method gets ACL Change Sets from Alfresco Repository to be indexed.
     * 
     * The indexing is performed in batches of ACL Change Sets and the Tracker Status
     * is updated in batched of ACLs.
     * 
     * Tracker Status contains the Commit Time from the latest ACL Change Set indexed,
     * so new operations can be retrieved from Repository starting with that time.
     */
    protected void trackAclChangeSets() throws AuthenticationException, IOException, JSONException
    {

        long startElapsed = System.nanoTime();

        AclChangeSets aclChangeSets;
        BoundedDeque<AclChangeSet> changeSetsFound = new BoundedDeque<>(ACL_CHANGE_SETS_FOUND_QUEUE_SIZE);
        long totalAclCount = 0;
        int aclCount;
        
        LOGGER.info("{}-[CORE {}] <init> Tracking ACLs", Thread.currentThread().getId(), coreName);
        
        do
        {
            try
            {
                getWriteLock().acquire();

                /*
                * We acquire the tracker state again here and set it globally. This is because the
                * tracker state could have been invalidated due to a rollback by the CommitTracker.
                * In this case the state will revert to the last transaction state record in the index.
                */

                this.state = getTrackerState();
                
                Long fromCommitTime = getChangeSetFromCommitTime(changeSetsFound, state.getLastGoodChangeSetCommitTimeInIndex());
                aclChangeSets = getSomeAclChangeSets(changeSetsFound, fromCommitTime, timeStep, maxNumberOfAclChangeSets,
                        state.getTimeToStopIndexing());
                
                if (aclChangeSets.getAclChangeSets().size() > 0) 
                {
                    LOGGER.info("{}-[CORE {}] Found {} ACL change sets after lastTxCommitTime {}, ACL Change Sets from {} to {}", 
                            Thread.currentThread().getId(),
                            coreName, 
                            aclChangeSets.getAclChangeSets().size(),
                            fromCommitTime,
                            aclChangeSets.getAclChangeSets().get(0),
                            aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1));
                } 
                else 
                {
                    LOGGER.info("{}-[CORE {}] No ACL change set found after lastTxCommitTime {}",
                            Thread.currentThread().getId(), coreName, fromCommitTime);
                }


                // Make sure we do not go ahead of where we started - we will check the holes here
                // correctly next time
                if (aclChangeSets.getAclChangeSets()
                        .stream()
                        .anyMatch(changeSet -> changeSet.getCommitTimeMs() > state.getTimeToStopIndexing()))
                {
                    break;
                }

                final AtomicInteger counter = new AtomicInteger();
                Collection<List<AclChangeSet>> changeSetBatches = aclChangeSets.getAclChangeSets().stream()
                        .peek(changeSetsFound::add)
                        .filter(this::isAclChangeSetAlreadyIndexed)
                        .collect(Collectors.groupingBy(it -> counter.getAndAdd(1) / changeSetAclsBatchSize))
                        .values();


                for (List<AclChangeSet> changeSetBatch : changeSetBatches)
                {
                    aclCount = indexBatchOfChangeSets(changeSetBatch);
                    // Update last committed transactions
                    indexAclChangeSetAfterWorker(changeSetBatch, state);

                    long endElapsed = System.nanoTime();
                    trackerStats.addElapsedAclTime(aclCount, endElapsed-startElapsed);
                    startElapsed = endElapsed;
                    totalAclCount += aclCount;
                }

                // Update state with server data on last transactions processed
                setLastChangeSetIdAndCommitTimeInTrackerState(aclChangeSets.getAclChangeSets(), state);

            }
            catch(InterruptedException | ExecutionException e)
            {
                throw new IOException(e);
            }
            finally
            {
                getWriteLock().release();
            }
            
        }
        while ((aclChangeSets.getAclChangeSets().size() > 0));
        
        LOGGER.info("{}-[CORE {}] <end> Tracked {} ACLs", Thread.currentThread().getId(), coreName, totalAclCount);
        
    }

    private boolean isAclChangeSetAlreadyIndexed(AclChangeSet changeSet)
    {

        try
        {
            boolean isInIndex = (changeSet.getCommitTimeMs() <= state.getLastIndexedChangeSetCommitTime() &&
                    infoSrv.aclChangeSetInIndex(changeSet.getId(), true));
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("{}-[CORE {}] Skipping change Set Id {} as it was already indexed",
                        Thread.currentThread().getId(), coreName, changeSet.getId());
            }
            return !isInIndex;
        }
        catch (IOException e)
        {
            LOGGER.warn(
                    "{}-[CORE {}] Error catched while checking if ACL Change Set {} was in index",
                    Thread.currentThread().getId(), coreName, changeSet.getId(), e);
            return true;
        }

    }

    private void setLastChangeSetIdAndCommitTimeInTrackerState(List<AclChangeSet> aclChangeSets, TrackerState state)
    {
        
        if (!aclChangeSets.isEmpty())
        {
            long maxChangeSetCommitTime =
                    aclChangeSets.stream().max(Comparator.comparing(AclChangeSet::getCommitTimeMs)).orElseThrow().getCommitTimeMs();
            state.setLastChangeSetCommitTimeOnServer(maxChangeSetCommitTime);

            long maxChangeSetId = aclChangeSets.stream().max(Comparator.comparing(AclChangeSet::getId)).orElseThrow().getId();
            state.setLastChangeSetIdOnServer(maxChangeSetId);
        }
        
    }

    /**
     * Index ACL Change Set transaction after ACLs has been indexed by the worker
     */
    private void indexAclChangeSetAfterWorker(Collection<AclChangeSet> changeSetsIndexed, TrackerState state)
                throws IOException
    {
        for (AclChangeSet set : changeSetsIndexed)
        {
            infoSrv.indexAclTransaction(set, true);
            // Acl change sets are ordered by commit time and tie-broken by id
            if (set.getCommitTimeMs() > state.getLastIndexedChangeSetCommitTime()
                    || set.getCommitTimeMs() == state.getLastIndexedChangeSetCommitTime()
                    && set.getId() > state.getLastIndexedChangeSetId())
            {
                state.setLastIndexedChangeSetCommitTime(set.getCommitTimeMs());
                state.setLastIndexedChangeSetId(set.getId());
            }
            trackerStats.addChangeSetAcls(set.getAclCount());
        }
    }


    /**
     * Index ACLs from ACL Change Sets contained in changeSetBatch
     * When total ACL indexed count is greater than the specified for a single execution
     * (maxAclsPerExecution), no more ACL Change Sets are processed.
     * 
     * @param changeSetBatch List of ACL Change Sets to be indexed
     * @return List of ACL Change Set indexed and Count of ACL indexed
     */
    private int indexBatchOfChangeSets(List<AclChangeSet> changeSetBatch)
            throws AuthenticationException, IOException, JSONException, ExecutionException, InterruptedException {
        // Exclude ACL Change Set with no ACLs inside
        List<AclChangeSet> nonEmptyChangeSets = changeSetBatch.stream()
                .filter(set -> set.getAclCount() > 0)
                .collect(Collectors.toList());

        List<Acl> acls = client.getAcls(nonEmptyChangeSets, null, Integer.MAX_VALUE);
        
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("{}-[CORE {}] Found {} Acls from Acl Change Sets: {}", Thread.currentThread().getId(),
                    coreName, acls.size(), nonEmptyChangeSets);
        }
        
        List<List<Acl>> aclBatches = Lists.partition(acls, aclBatchSize);

        return forkJoinPool.submit(() ->
                aclBatches.parallelStream().map(batch -> {
                    new AclIndexWorker(batch).run();
                    return batch.size();
                }).reduce(0, Integer::sum)
        ).get();
    }


    /**
     * ACL Indexer
     */
    class AclIndexWorker extends AbstractWorker
    {
        List<Acl> acls;

        AclIndexWorker(List<Acl> acls)
        {
            this.acls = acls;
        }

        @Override
        protected void doWork() throws IOException, AuthenticationException, JSONException
        {
            List<Acl> filteredAcls = filterAcls(acls);
            if(filteredAcls.size() > 0)
            {
                List<AclReaders> readers = client.getAclReaders(filteredAcls);
                indexAcl(readers, true);
            }
        }
        
        @Override
        protected void onFail(Throwable failCausedBy)
        {
        	setRollback(true, failCausedBy);
        }
        
        private List<Acl> filterAcls(List<Acl> acls)
        {
            ArrayList<Acl> filteredList = new ArrayList<>(acls.size());
            for(Acl acl : acls)
            {
                if(docRouter.routeAcl(shardCount, shardInstance, acl))
                {
                    filteredList.add(acl);
                }
            }
            return filteredList;
        }
    }


    public void invalidateState()
    {
        super.invalidateState();
        infoSrv.clearProcessedAclChangeSets();
    }
}
