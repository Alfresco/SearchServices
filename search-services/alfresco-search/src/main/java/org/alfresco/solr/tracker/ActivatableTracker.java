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

import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.solr.core.CoreDescriptorDecorator;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Supertype layer for trackers that can be enabled/disabled.
 */
public abstract class ActivatableTracker extends AbstractTracker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivatableTracker.class);

    protected static AtomicBoolean isEnabled = new AtomicBoolean(true);

    protected ActivatableTracker(Type type)
    {
        super(type);
    }

    protected ActivatableTracker(Properties properties, SOLRAPIClient client, String coreName, InformationServer informationServer, Type type)
    {
        super(properties, client, coreName, informationServer, type);

        if (isEnabled.get())
        {
            LOGGER.info("[{} / {} / {}] {} Tracker set to enabled at startup.", coreName, trackerId, state, type);
        }
        else
        {
            LOGGER.info("[{} / {} / {}] {} Tracker set to disabled at startup.", coreName, trackerId, state, type);
        }
    }

    /**
     * Disables this tracker instance.
     */
    public final void disable()
    {
        clearScheduledMaintenanceWork();

        if (isEnabled.compareAndSet(true, false))
        {
            if (state != null && state.isRunning())
            {
                LOGGER.info("[{} / {} / {}] {} Tracker has been disabled (the change will be effective at the next tracking cycle) and set in rollback mode because it is running.", coreName, trackerId, state, type);
                setRollback(true);
            }
            LOGGER.info("[{} / {} / {}] {} Tracker has been disabled. The change will be effective at the next tracking cycle.", coreName, trackerId, state, type);
        }
        else
        {
            LOGGER.warn("[{} / {} / {}] {} Tracker cannot be disabled because it is already in that state.", coreName, trackerId, state, type);
        }
    }

    /**
     * Enables this tracker instance.
     */
    public final void enable()
    {
        if (isEnabled.compareAndSet(false, true))
        {
            LOGGER.info("[{} / {} / {}] {} Tracker has been enabled. The change will be effective at the next tracking cycle.", coreName, trackerId, state, type);
        }
        else
        {
            LOGGER.warn("[{} / {} / {}] {} Tracker cannot be enabled because it is already in that state.", coreName, trackerId, state, type);
        }
    }

    @Override
    public void track()
    {
        if (isEnabled())
        {
            super.track();
        }
        else
        {
            LOGGER.trace("[{} / {} / {}] {} Tracker is disabled. That is absolutely ok, that means you disabled the tracking on this core.", coreName, trackerId, state, type);
        }
    }

    public boolean isEnabled()
    {
        return isEnabled.get();
    }

    public boolean isDisabled()
    {
        return !isEnabled();
    }

    /**
     * Cleans up the scheduled maintenance work collected by this tracker.
     */
    protected void clearScheduledMaintenanceWork()
    {
        // Default behaviour is: do nothing
    };

    /**
     * Logs out the content of the input collection.
     *
     * @param values the collection which (in case is not empty) contains the identifiers (e.g. txid, aclid) the system
     *               is going to clear.
     * @param kind the kind of identifier (e.g. Transaction, Node ID, ACL ID) in the input collection.
     */
    protected void logAndClear(Collection<Long> values, String kind)
    {
        if (values == null || values.size() == 0) {
            return;
        }

        final List<Long> tmp = new ArrayList<>(values);
        values.clear();

        LOGGER.info("[CORE {}] Scheduled work ({}) that will be cleaned: {}", coreName, kind, new ArrayList<>(tmp));
    }
}
