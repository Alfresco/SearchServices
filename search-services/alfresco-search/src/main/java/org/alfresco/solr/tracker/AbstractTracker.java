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

import static org.alfresco.repo.index.shard.ShardMethodEnum.DB_ID;
import static org.alfresco.solr.tracker.DocRouterFactory.SHARD_KEY_KEY;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.repo.search.impl.QueryParserUtils;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.IndexTrackingShutdownException;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.NodeReport;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.commons.lang3.StringUtils;
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
    ShardMethodEnum shardMethod;
    protected boolean transformContent;
    String shardTemplate;
    protected volatile boolean rollback;
    /**
     * When rollback is set, original error is also gathered in order to provide detailed logging.
     */
    protected Throwable rollbackCausedBy;
    protected final Type type;
    protected final String trackerId;

    DocRouter docRouter;

    /**
     * The property to use for determining the shard.
     * Note that this property is not used by all trackers, it is actually managed by the {@link ShardStatePublisher} and
     * {@link MetadataTracker}. We put this property here because otherwise we should introduce another supertype layer
     * for those two trackers.
     */
    protected Optional<QName> shardProperty = Optional.empty();

    /**
     * The string representation of the shard key.
     * Note that this property is not used by all trackers, it is actually managed by the {@link ShardStatePublisher} and
     * {@link MetadataTracker}. We put this property here because otherwise we should introduce another supertype layer
     * for those two trackers.
     */
    protected Optional<String> shardKey;

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
        shardMethod = ShardMethodEnum.getShardMethod(p.getProperty("shard.method", DB_ID.name()));

        shardTemplate =  p.getProperty("alfresco.template", "");
        
        transformContent = Boolean.parseBoolean(p.getProperty("alfresco.index.transformContent", "true"));

        this.trackerStats = this.infoSrv.getTrackerStats();
        
        this.type = type;

        this.trackerId = type + "@" + hashCode();

        shardKey = ofNullable(p.getProperty(SHARD_KEY_KEY));

        firstUpdateShardProperty();

        docRouter = DocRouterFactory.getRouter(p, shardMethod);
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
        if (true) return true;
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

            updateTrackerState(iterationId);

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
        catch (Exception exception)
        {
            LOGGER.error("[{} / {} / {}] Some problem was detected while resetting the Tracker State. See the stracktrace below for further details."
                            , coreName, trackerId, iterationId, exception);
        }
        finally
        {
            infoSrv.unregisterTrackerThread();

            ofNullable(state).ifPresent(this::turnOff);

            getRunLock().release();
        }
    }

    private synchronized void updateTrackerState(String iterationId) {
        if(this.state == null)
        {
            this.state = getTrackerState();

            LOGGER.debug("[{} / {} / {}]  Global Tracker State set to: {}", coreName, trackerId, iterationId, this.state.toString());
        }
        else
        {
            continueState();
        }

        this.state.setRunning(true);
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
            LOGGER.error("Unable to properly turn off the TrackerState instance. See the stacktrace below for further details.", exception);
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

    /**
     * Set the shard property using the shard key.
     */
    void updateShardProperty()
    {
        shardKey.ifPresent(shardKeyName -> {
            Optional<QName> updatedShardProperty = getShardProperty(shardKeyName);
            if (!shardProperty.equals(updatedShardProperty))
            {
                if (updatedShardProperty.isEmpty())
                {
                    LOGGER.warn("The model defining {} property has been disabled", shardKeyName);
                }
                else
                {
                    LOGGER.info("New {} property found for {}", SHARD_KEY_KEY, shardKeyName);
                }
            }
            shardProperty = updatedShardProperty;
        });
    }

    /**
     * Given the field name, returns the name of the property definition.
     * If the property definition is not found, Empty optional is returned.
     *
     * @param field the field name.
     * @return the name of the associated property definition if present, Optional.Empty() otherwise
     */
    static Optional<QName> getShardProperty(String field)
    {
        if (StringUtils.isBlank(field))
        {
            throw new IllegalArgumentException("Sharding property " + SHARD_KEY_KEY + " has not been set.");
        }

        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        NamespaceDAO namespaceDAO = dataModel.getNamespaceDAO();
        DictionaryService dictionaryService = dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT);
        PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition("http://www.alfresco.org/model/content/1.0",
                namespaceDAO,
                dictionaryService,
                field);

        return ofNullable(propertyDef).map(PropertyDefinition::getName);
    }

    /**
     * Returns information about the {@link org.alfresco.solr.client.Node} associated with the given dbid.
     *
     * @param dbid the node identifier.
     * @return the {@link org.alfresco.solr.client.Node} associated with the given dbid.
     */
    public NodeReport checkNode(Long dbid)
    {
        NodeReport nodeReport = new NodeReport();
        nodeReport.setDbid(dbid);

        this.infoSrv.addCommonNodeReportInfo(nodeReport);

        return nodeReport;
    }

    /**
     * Returns the {@link DocRouter} instance in use on this node.
     *
     * @return the {@link DocRouter} instance in use on this node.
     */
    public DocRouter getDocRouter()
    {
        return this.docRouter;
    }

    private void firstUpdateShardProperty()
    {
        shardKey.ifPresent( shardKeyName -> {
            updateShardProperty();
            if (shardProperty.isEmpty())
            {
                LOGGER.warn("Sharding property {} was set to {}, but no such property was found.", SHARD_KEY_KEY, shardKeyName);
            }
        });
    }
}