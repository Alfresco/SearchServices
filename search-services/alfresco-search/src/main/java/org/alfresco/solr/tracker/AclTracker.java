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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.AclReport;
import org.alfresco.solr.BoundedDeque;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclChangeSets;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.GetNodesParameters;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.SOLRAPIClient;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multithreaded ACL {@link Tracker} implementation.
 * 
 * @author Matt Ward
 **/

public class AclTracker extends AbstractTracker
{
    protected final static Logger log = LoggerFactory.getLogger(AclTracker.class);

    private static final int DEFAULT_CHANGE_SET_ACLS_BATCH_SIZE = 100;
    private static final int DEFAULT_ACL_BATCH_SIZE = 10;

    private int changeSetAclsBatchSize = DEFAULT_CHANGE_SET_ACLS_BATCH_SIZE;
    private int aclBatchSize = DEFAULT_ACL_BATCH_SIZE;

    private ConcurrentLinkedQueue<Long> aclChangeSetsToReindex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> aclChangeSetsToIndex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> aclChangeSetsToPurge = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> aclsToReindex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> aclsToIndex = new ConcurrentLinkedQueue<Long>();
    private ConcurrentLinkedQueue<Long> aclsToPurge = new ConcurrentLinkedQueue<Long>();
    private DocRouter docRouter;
    
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
        changeSetAclsBatchSize = Integer.parseInt(p.getProperty("alfresco.changeSetAclsBatchSize", "100"));
        aclBatchSize = Integer.parseInt(p.getProperty("alfresco.aclBatchSize", "10"));
        shardMethod = p.getProperty("shard.method", SHARD_METHOD_DBID);
        docRouter = DocRouterFactory.getRouter(p, ShardMethodEnum.getShardMethod(shardMethod));
        threadHandler = new ThreadHandler(p, coreName, "AclTracker");
    }

    @Override
    protected void doTrack() throws Throwable
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
                AclChangeSets aclChangeSets = client.getAclChangeSets(null, aclChangeSetId, null, aclChangeSetId+1, 1);
                if ((aclChangeSets.getAclChangeSets().size() > 0) && aclChangeSetId.equals(aclChangeSets.getAclChangeSets().get(0).getId()))
                {
                    AclChangeSet changeSet = aclChangeSets.getAclChangeSets().get(0);
                    List<Acl> acls = client.getAcls(Collections.singletonList(changeSet), null, Integer.MAX_VALUE);
                    for (Acl acl : acls)
                    {
                        List<AclReaders> readers = client.getAclReaders(Collections.singletonList(acl));
                        indexAcl(readers, false);
                    }
                    this.infoSrv.indexAclTransaction(changeSet, false);
                    requiresCommit = true;
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

                AclChangeSets aclChangeSets = client.getAclChangeSets(null, aclChangeSetId, null, aclChangeSetId+1, 1);
                if ((aclChangeSets.getAclChangeSets().size() > 0) && aclChangeSetId.equals(aclChangeSets.getAclChangeSets().get(0).getId()))
                {
                    AclChangeSet changeSet = aclChangeSets.getAclChangeSets().get(0);
                    List<Acl> acls = client.getAcls(Collections.singletonList(changeSet), null, Integer.MAX_VALUE);
                    for (Acl acl : acls)
                    {
                        List<AclReaders> readers = client.getAclReaders(Collections.singletonList(acl));
                        indexAcl(readers, true);
                    }

                    this.infoSrv.indexAclTransaction(changeSet, true);
                    requiresCommit = true;
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

    protected void purgeAclChangeSets() throws AuthenticationException, IOException, JSONException
    {       
        while (aclChangeSetsToPurge.peek() != null)
        {
            Long aclChangeSetId = aclChangeSetsToPurge.poll();
            if (aclChangeSetId != null)
            {
                this.infoSrv.deleteByAclChangeSetId(aclChangeSetId);
            }
            checkShutdown();
        }

    }
    
    protected void purgeAcls() throws AuthenticationException, IOException, JSONException
    {
        while (aclsToPurge.peek() != null)
        {
            Long aclId = aclsToPurge.poll();
            if (aclId != null)
            {
                this.infoSrv.deleteByAclId(aclId);
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

    protected void trackRepository() throws IOException, AuthenticationException, JSONException
    {
        checkShutdown();
        
        if(!isMaster && isSlave)
        {
            return;
        }

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
     * @param state the state of this tracker
     * @throws AuthenticationException
     * @throws IOException
     * @throws JSONException
     */
    private void checkRepoAndIndexConsistency(TrackerState state) throws AuthenticationException, IOException, JSONException
    {
        AclChangeSets firstChangeSets = null;
        if (state.getLastGoodChangeSetCommitTimeInIndex() == 0)
        {
            state.setCheckedLastAclTransactionTime(true);
            state.setCheckedFirstAclTransactionTime(true);
            log.info("No acl transactions found - no verification required");
            
            firstChangeSets = client.getAclChangeSets(null, 0L, null, 2000L, 1);
            if (!firstChangeSets.getAclChangeSets().isEmpty())
            {
                AclChangeSet firstChangeSet = firstChangeSets.getAclChangeSets().get(0);
                long firstChangeSetCommitTime = firstChangeSet.getCommitTimeMs();
                state.setLastGoodChangeSetCommitTimeInIndex(firstChangeSetCommitTime);
                setLastChangeSetIdAndCommitTimeInTrackerState(firstChangeSets, state);
            }
        }
        
        if (!state.isCheckedFirstAclTransactionTime())
        {
            firstChangeSets = client.getAclChangeSets(null, 0l, null, 2000L, 1);
            if (!firstChangeSets.getAclChangeSets().isEmpty())
            {
                AclChangeSet firstAclChangeSet= firstChangeSets.getAclChangeSets().get(0);
                long firstAclTxId = firstAclChangeSet.getId();
                long firstAclTxCommitTime = firstAclChangeSet.getCommitTimeMs();
                int setSize = this.infoSrv.getAclTxDocsSize(""+firstAclTxId, ""+firstAclTxCommitTime);
                
                if (setSize == 0)
                {
                    log.error("First acl transaction was not found with the correct timestamp.");
                    log.error("SOLR has successfully connected to your repository  however the SOLR indexes and repository database do not match."); 
                    log.error("If this is a new or rebuilt database your SOLR indexes also need to be re-built to match the database."); 
                    log.error("You can also check your SOLR connection details in solrcore.properties.");
                    throw new AlfrescoRuntimeException("Initial acl transaction not found with correct timestamp");
                }
                else if (setSize == 1)
                {
                    state.setCheckedFirstTransactionTime(true);
                    log.info("Verified first acl transaction and timestamp in index");
                }
                else
                {
                    log.warn("Duplicate initial acl transaction found with correct timestamp");
                }
            }
        }

        // Checks that the last aclTxId in solr is <= last aclTxId in repo
        if (!state.isCheckedLastAclTransactionTime())
        {
            if (firstChangeSets == null)
            {
                firstChangeSets = client.getAclChangeSets(null, 0l, null, 2000L, 1);
            }

            setLastChangeSetIdAndCommitTimeInTrackerState(firstChangeSets, state);
            Long maxChangeSetCommitTimeInRepo = firstChangeSets.getMaxChangeSetCommitTime();
            Long maxChangeSetIdInRepo = firstChangeSets.getMaxChangeSetId();
            if (maxChangeSetCommitTimeInRepo != null && maxChangeSetIdInRepo != null)
            {
                AclChangeSet maxAclTxInIndex = this.infoSrv.getMaxAclChangeSetIdAndCommitTimeInIndex();
                if (maxAclTxInIndex.getCommitTimeMs() > maxChangeSetCommitTimeInRepo)
                {
                    log.error("Last acl transaction was found in index with timestamp later than that of repository.");
                    log.error("Max Acl Tx In Index: " + maxAclTxInIndex.getId() + ", In Repo: " + maxChangeSetIdInRepo);
                    log.error("Max Acl Tx Commit Time In Index: " + maxAclTxInIndex.getCommitTimeMs() + ", In Repo: "
                            + maxChangeSetCommitTimeInRepo);
                    log.error("SOLR has successfully connected to your repository  however the SOLR indexes and repository database do not match."); 
                    log.error("If this is a new or rebuilt database your SOLR indexes also need to be re-built to match the database."); 
                    log.error("You can also check your SOLR connection details in solrcore.properties.");
                    throw new AlfrescoRuntimeException("Last acl transaction found in index with incorrect timestamp");
                }
                else
                {
                    state.setCheckedLastAclTransactionTime(true);
                    log.info("Verified last acl transaction timestamp in index less than or equal to that of repository.");
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

    protected AclChangeSets getSomeAclChangeSets(BoundedDeque<AclChangeSet> changeSetsFound, Long fromCommitTime, long timeStep, int maxResults, long endTime) throws AuthenticationException, IOException, JSONException
    {
        long actualTimeStep  = timeStep;

        AclChangeSets aclChangeSets;
        // step forward in time until we find something or hit the time bound
        // max id unbounded
        Long startTime = fromCommitTime == null ? Long.valueOf(0L) :fromCommitTime;
        do
        {
            aclChangeSets = client.getAclChangeSets(startTime, null, startTime + actualTimeStep, null, maxResults);
            startTime += actualTimeStep;
            actualTimeStep *= 2;
            if(actualTimeStep > TIME_STEP_32_DAYS_IN_MS)
            {
                actualTimeStep = TIME_STEP_32_DAYS_IN_MS;
            }
        }
        while( ((aclChangeSets.getAclChangeSets().size() == 0)  && (startTime < endTime)) || ((aclChangeSets.getAclChangeSets().size() > 0) && alreadyFoundChangeSets(changeSetsFound, aclChangeSets)));

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
            HashSet<AclChangeSet> alreadyFound = new HashSet<AclChangeSet>(changeSetsFound.getDeque());
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

    public IndexHealthReport checkIndex(Long toTx, Long toAclTx, Long fromTime, Long toTime) 
                throws AuthenticationException, IOException, JSONException
    {   
        // DB ACL TX Count
        long firstChangeSetCommitTimex = 0;
        AclChangeSets firstChangeSets = client.getAclChangeSets(null, 0L, null, 2000L, 1);
        if(firstChangeSets.getAclChangeSets().size() > 0)
        {
            AclChangeSet firstChangeSet = firstChangeSets.getAclChangeSets().get(0);
            firstChangeSetCommitTimex = firstChangeSet.getCommitTimeMs();
        }

        IOpenBitSet aclTxIdsInDb = infoSrv.getOpenBitSetInstance();
        Long lastAclTxCommitTime = Long.valueOf(firstChangeSetCommitTimex);
        if (fromTime != null)
        {
            lastAclTxCommitTime = fromTime;
        }
        
        long maxAclTxId = 0;
        Long minAclTxId = null;
        long endTime = System.currentTimeMillis() + infoSrv.getHoleRetention();
        AclChangeSets aclTransactions;
        BoundedDeque<AclChangeSet> changeSetsFound = new  BoundedDeque<AclChangeSet>(100);
        DO: do
        {
            aclTransactions = getSomeAclChangeSets(changeSetsFound, lastAclTxCommitTime, TIME_STEP_1_HR_IN_MS, 2000, 
                        endTime);
            for (AclChangeSet set : aclTransactions.getAclChangeSets())
            {
                // include
                if (toTime != null)
                {
                    if (set.getCommitTimeMs() > toTime.longValue())
                    {
                        break DO;
                    }
                }
                if (toAclTx != null)
                {
                    if (set.getId() > toAclTx.longValue())
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


    public List<Node> getFullNodesForDbTransaction(Long txid)
    {
        try
        {
            GetNodesParameters gnp = new GetNodesParameters();
            ArrayList<Long> txs = new ArrayList<Long>();
            txs.add(txid);
            gnp.setTransactionIds(txs);
            gnp.setStoreProtocol(storeRef.getProtocol());
            gnp.setStoreIdentifier(storeRef.getIdentifier());
            return client.getNodes(gnp, Integer.MAX_VALUE);
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("Failed to get nodes", e);
        }
        catch (JSONException e)
        {
            throw new AlfrescoRuntimeException("Failed to get nodes", e);
        }
        catch (AuthenticationException e)
        {
            throw new AlfrescoRuntimeException("Failed to get nodes", e);
        }
    }



    /**
     * @param acltxid Long
     * @return List<Long>
     **/
    public List<Long> getAclsForDbAclTransaction(Long acltxid)
    {
        try
        {
            ArrayList<Long> answer = new ArrayList<Long>();
            AclChangeSets changeSet = client.getAclChangeSets(null, acltxid, null, acltxid+1, 1);
            List<Acl> acls = client.getAcls(changeSet.getAclChangeSets(), null, Integer.MAX_VALUE);
            for (Acl acl : acls)
            {
                answer.add(acl.getId());
            }
            return answer;
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("Failed to get acls", e);
        }
        catch (JSONException e)
        {
            throw new AlfrescoRuntimeException("Failed to get acls", e);
        }
        catch (AuthenticationException e)
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
     * @throws AuthenticationException
     * @throws IOException
     * @throws JSONException
     */
    protected void trackAclChangeSets() throws AuthenticationException, IOException, JSONException
    {

        long startElapsed = System.nanoTime();
        
        boolean upToDate = false;
        AclChangeSets aclChangeSets;
        BoundedDeque<AclChangeSet> changeSetsFound = new BoundedDeque<AclChangeSet>(100);
        HashSet<AclChangeSet> changeSetsIndexed = new LinkedHashSet<AclChangeSet>();
        long totalAclCount = 0;
        int aclCount = 0;
        
        do
        {
            try
            {
                getWriteLock().acquire();
                //System.out.println("############# AclTracker acquire lock ################");

                TrackerState state = getTrackerState();

                Long fromCommitTime = getChangeSetFromCommitTime(changeSetsFound, state.getLastGoodChangeSetCommitTimeInIndex());
                aclChangeSets = getSomeAclChangeSets(changeSetsFound, fromCommitTime, TIME_STEP_1_HR_IN_MS, 2000,
                        state.getTimeToStopIndexing());

                //System.out.println("############# Changesets ################:"+aclChangeSets.getAclChangeSets().size());


                setLastChangeSetIdAndCommitTimeInTrackerState(aclChangeSets, state);

                log.info("Scanning Acl change sets ...");
                if (aclChangeSets.getAclChangeSets().size() > 0) {
                    log.info(".... from " + aclChangeSets.getAclChangeSets().get(0));
                    log.info(".... to " + aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1));
                } else {
                    log.info(".... none found after lastTxCommitTime " + fromCommitTime);
                }

                ArrayList<AclChangeSet> changeSetBatch = new ArrayList<AclChangeSet>();
                for (AclChangeSet changeSet : aclChangeSets.getAclChangeSets()) {
                    boolean isInIndex = (changeSet.getCommitTimeMs() <= state.getLastIndexedChangeSetCommitTime() &&
                                         infoSrv.aclChangeSetInIndex(changeSet.getId(), true));

                    if (isInIndex) {
                        changeSetsFound.add(changeSet);
                    } else {
                        // Make sure we do not go ahead of where we started - we will check the holes here
                        // correctly next time
                        if (changeSet.getCommitTimeMs() > state.getTimeToStopIndexing()) {
                            upToDate = true;
                            break;
                        }

                        changeSetBatch.add(changeSet);
                        if (getAclCount(changeSetBatch) > changeSetAclsBatchSize) {
                            aclCount += indexBatchOfChangeSets(changeSetBatch);
                            totalAclCount += aclCount;

                            for (AclChangeSet scheduled : changeSetBatch) {
                                changeSetsFound.add(scheduled);
                                changeSetsIndexed.add(scheduled);
                            }
                            changeSetBatch.clear();
                        }
                    }

                    if (aclCount > batchCount) {
                        if (super.infoSrv.getRegisteredSearcherCount() < getMaxLiveSearchers()) {
                            indexAclChangeSetAfterAsynchronous(changeSetsIndexed, state);
                            long endElapsed = System.nanoTime();
                            trackerStats.addElapsedAclTime(aclCount, endElapsed - startElapsed);
                            startElapsed = endElapsed;
                            aclCount = 0;
                        }
                    }
                    checkShutdown();
                }

                if (!changeSetBatch.isEmpty()) {
                    if (getAclCount(changeSetBatch) > 0) {
                        aclCount += indexBatchOfChangeSets(changeSetBatch);
                        totalAclCount += aclCount;
                    }

                    for (AclChangeSet scheduled : changeSetBatch) {
                        changeSetsFound.add(scheduled);
                        changeSetsIndexed.add(scheduled);
                    }
                    changeSetBatch.clear();
                }

                if(changeSetsIndexed.size() > 0)
                {
                    indexAclChangeSetAfterAsynchronous(changeSetsIndexed, state);
                    long endElapsed = System.nanoTime();
                    trackerStats.addElapsedAclTime(aclCount, endElapsed-startElapsed);
                    startElapsed = endElapsed;
                    aclCount = 0;
                }
            }
            catch(InterruptedException e)
            {
                throw new IOException(e);
            }
            finally
            {
                getWriteLock().release();
            }
        }
        while ((aclChangeSets.getAclChangeSets().size() > 0) && (upToDate == false));

        log.info("total number of acls updated: " + totalAclCount);
    }

    private void setLastChangeSetIdAndCommitTimeInTrackerState(AclChangeSets aclChangeSets, TrackerState state)
    {
        Long maxChangeSetCommitTime = aclChangeSets.getMaxChangeSetCommitTime();
        if(maxChangeSetCommitTime != null)
        {
            state.setLastChangeSetCommitTimeOnServer(maxChangeSetCommitTime);
        }

        Long maxChangeSetId = aclChangeSets.getMaxChangeSetId();
        if(maxChangeSetId != null)
        {
            state.setLastChangeSetIdOnServer(maxChangeSetId);
        }
    }

    private void indexAclChangeSetAfterAsynchronous(HashSet<AclChangeSet> changeSetsIndexed, TrackerState state)
                throws IOException
    {
        waitForAsynchronous();
        for (AclChangeSet set : changeSetsIndexed)
        {
            super.infoSrv.indexAclTransaction(set, true);
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
        changeSetsIndexed.clear();
        //super.infoSrv.commit();
    }

    private int getAclCount(List<AclChangeSet> changeSetBatch)
    {
        int count = 0;
        for (AclChangeSet set : changeSetBatch)
        {
            count += set.getAclCount();
        }
        return count;
    }

    private int indexBatchOfChangeSets(List<AclChangeSet> changeSetBatch) throws AuthenticationException, IOException, JSONException
    {
        int aclCount = 0;
        ArrayList<AclChangeSet> nonEmptyChangeSets = new ArrayList<AclChangeSet>(changeSetBatch.size());
        for (AclChangeSet set : changeSetBatch)
        {
            if (set.getAclCount() > 0)
            {
                nonEmptyChangeSets.add(set);
            }
        }

        ArrayList<Acl> aclBatch = new ArrayList<Acl>();
        List<Acl> acls = client.getAcls(nonEmptyChangeSets, null, Integer.MAX_VALUE);

        for (Acl acl : acls)
        {
            if (log.isDebugEnabled())
            {
                log.debug(acl.toString());
            }
            aclBatch.add(acl);
            if (aclBatch.size() > aclBatchSize)
            {
                aclCount += aclBatch.size();
                AclIndexWorkerRunnable aiwr = new AclIndexWorkerRunnable(this.threadHandler, aclBatch);
                this.threadHandler.scheduleTask(aiwr);
                aclBatch = new ArrayList<Acl>();
            }
        }
        if (aclBatch.size() > 0)
        {
            aclCount += aclBatch.size();
            AclIndexWorkerRunnable aiwr = new AclIndexWorkerRunnable(this.threadHandler, aclBatch);
            this.threadHandler.scheduleTask(aiwr);
            aclBatch = new ArrayList<Acl>();
        }
        return aclCount;
    }

    class AclIndexWorkerRunnable extends AbstractWorkerRunnable
    {
        List<Acl> acls;

        AclIndexWorkerRunnable(QueueHandler queueHandler, List<Acl> acls)
        {
            super(queueHandler);
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
        protected void onFail()
        {
        	setRollback(true);
        }
        
        private List<Acl> filterAcls(List<Acl> acls)
        {
            ArrayList<Acl> filteredList = new ArrayList(acls.size());
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
