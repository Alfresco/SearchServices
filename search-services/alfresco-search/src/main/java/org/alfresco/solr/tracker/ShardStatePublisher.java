/*
 * #%L
 * Alfresco Solr Client
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

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.repo.index.shard.ShardStateBuilder;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.commons.codec.EncoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static java.util.Optional.of;
import static org.alfresco.solr.tracker.Tracker.Type.NODE_STATE_PUBLISHER;

/**
 * Despite belonging to the Tracker ecosystem, this component is actually a publisher, which periodically informs
 * Alfresco about the state of the hosting core.
 *
 * This has been introduced in SEARCH-1752 for splitting the dual responsibility of the {@link org.alfresco.solr.tracker.MetadataTracker}.
 * As consequence of that, this class contains only the members needed for obtaining a valid
 * {@link org.alfresco.repo.index.shard.ShardState} that can be periodically communicated to Alfresco.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-1752">SEARCH-1752</a>
 */
public class ShardStatePublisher extends AbstractTracker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ShardStatePublisher.class);
    private static final Map<String, Semaphore> RUN_LOCK_BY_CORE = new ConcurrentHashMap<>();
    private static final Map<String, Semaphore> WRITE_LOCK_BY_CORE = new ConcurrentHashMap<>();

    private final boolean isMaster;

    public ShardStatePublisher(
            boolean isMaster,
            Properties p,
            SOLRAPIClient client,
            String coreName,
            InformationServer informationServer)
    {
        super(p, client, coreName, informationServer, NODE_STATE_PUBLISHER);

        this.isMaster = isMaster;

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
     * The {@link ShardState}, as the name suggests, encapsulates/stores the state of the shard which hosts this
     * {@link MetadataTracker} instance.
     *
     * @return the {@link ShardState} instance which stores the current state of the hosting shard.
     */
    ShardState getShardState()
    {
        TrackerState transactionsTrackerState = getTrackerState();
        TrackerState changeSetsTrackerState =
                of(infoSrv.getAdminHandler())
                        .map(AlfrescoCoreAdminHandler::getTrackerRegistry)
                        .map(registry -> registry.getTrackerForCore(coreName, AclTracker.class))
                        .map(Tracker::getTrackerState)
                        .orElse(transactionsTrackerState);

        HashMap<String, String> propertyBag = new HashMap<>();
        propertyBag.put("coreName", coreName);
        updateShardProperty();

        propertyBag.putAll(docRouter.getProperties(shardProperty));

        return ShardStateBuilder.shardState()
                .withMaster(isMaster)
                .withLastUpdated(System.currentTimeMillis())
                .withLastIndexedChangeSetCommitTime(changeSetsTrackerState.getLastIndexedChangeSetCommitTime())
                .withLastIndexedChangeSetId(changeSetsTrackerState.getLastIndexedChangeSetId())
                .withLastIndexedTxCommitTime(transactionsTrackerState.getLastIndexedTxCommitTime())
                .withLastIndexedTxId(transactionsTrackerState.getLastIndexedTxId())
                .withPropertyBag(propertyBag)
                    .withShardInstance()
                        .withBaseUrl(infoSrv.getBaseUrl())
                        .withPort(infoSrv.getPort())
                        .withHostName(infoSrv.getHostName())
                        .withShard()
                            .withInstance(shardInstance)
                            .withFloc()
                                .withNumberOfShards(shardCount)
                                .withAddedStoreRef(storeRef)
                                .withTemplate(shardTemplate)
                                .withHasContent(transformContent)
                                .withShardMethod(shardMethod)
                            .endFloc()
                        .endShard()
                    .endShardInstance()
                .build();

    }

    /**
     * Returns true if the hosting core is master or standalone.
     *
     * @return true if the hosting core is master or standalone.
     */
    public boolean isOnMasterOrStandalone()
    {
        return isMaster;
    }
}
