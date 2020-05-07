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

import static org.alfresco.solr.tracker.Tracker.Type.NODE_STATE_PUBLISHER;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.commons.codec.EncoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Despite belonging to the Tracker ecosystem, this component is actually a publisher, which periodically informs
 * Alfresco about the state of the hosting slave core.
 * As the name suggests, this worker is scheduled only when the owning core acts as a slave.
 * It allows Solr's master/slave setup to be used with dynamic shard registration.
 *
 * In this scenario the slave is polling a "tracking" Solr node. The tracker below calls
 * the repo to register the state of the node without pulling any real transactions from the repo.
 *
 * This allows the repo to register the replica so that it will be included in queries. But the slave Solr node
 * will pull its data from a "tracking" Solr node using Solr's master/slave replication, rather then tracking the repository.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
public class SlaveCoreStatePublisher extends CoreStatePublisher
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SlaveCoreStatePublisher.class);


    // Share run and write locks across all SlaveCoreStatePublisher threads
    private static final Map<String, Semaphore> RUN_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private static final Map<String, Semaphore> WRITE_LOCK_BY_CORE = new ConcurrentHashMap<>();

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

    public SlaveCoreStatePublisher(
            boolean isMaster,
            Properties coreProperties,
            SOLRAPIClient repositoryClient,
            String name,
            SolrInformationServer informationServer)
    {
        super(isMaster, coreProperties, repositoryClient, name, informationServer, NODE_STATE_PUBLISHER);

        RUN_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
        WRITE_LOCK_BY_CORE.put(coreName, new Semaphore(1, true));
    }

    @Override
    protected void doTrack(String iterationId)
    {
        try
        {
            ShardState shardstate = getShardState();
            client.getTransactions(0L, null, 0L, null, 0, shardstate);
        }
        catch (EncoderException | IOException | AuthenticationException exception )
        {
            LOGGER.error("Unable to publish this node state. " +
                    "A failure condition has been met during the outbound subscription message encoding process. " +
                    "See the stacktrace below for further details.", exception);
        }
    }

    @Override
    public void maintenance()
    {
        // Do nothing here
    }

    @Override
    public boolean isOnMasterOrStandalone()
    {
        return false;
    }

    @Override
    public boolean hasMaintenance()
    {
        return false;
    }

    /**
     * When running in a slave mode, we need to recreate the tracker state every time.
     * This because in that context we don't have any tracker updating the state (e.g. lastIndexedChangeSetCommitTime,
     * lastIndexedChangeSetId)
     *
     * @return a new, fresh and up to date instance of {@link TrackerState}.
     */
    @Override
    public TrackerState getTrackerState()
    {
        return infoSrv.getTrackerInitialState();
    }
}