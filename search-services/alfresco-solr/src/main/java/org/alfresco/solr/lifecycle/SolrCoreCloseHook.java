/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr.lifecycle;

import org.alfresco.solr.tracker.*;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Called when a Solr core is closed.
 */
public class SolrCoreCloseHook extends CloseHook
{
    protected final static Logger log = LoggerFactory.getLogger(SolrCoreCloseHook.class);

    private SolrTrackerScheduler scheduler;
    private List<Tracker> trackers = new ArrayList<>();

    public SolrCoreCloseHook(SolrTrackerScheduler scheduler, CommitTracker commitTracker, Collection<Tracker> coreTrackers)
    {
        this.scheduler = scheduler;
        trackers.add(commitTracker);
        trackers.addAll(coreTrackers);
    }
    
    @Override
    public void postClose(SolrCore core)
    {
        // nothing
    }

    @Override
    public void preClose(SolrCore core)
    {
        log.info("Shutting down " + core.getName());
        SolrCoreLoadRegistration.shutdownTrackers(core.getName(), trackers, scheduler);
    }
}
