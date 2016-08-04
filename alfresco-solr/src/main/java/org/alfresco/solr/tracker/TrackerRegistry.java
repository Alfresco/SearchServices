/*
 * Copyright (C) 2014 Alfresco Software Limited.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of trackers for an AdminHandler
 * @author Ahmed Owian
 */
public class TrackerRegistry
{
    /*
     *  Keyed on core name and Tracker class.
     *  This facilitates getting a particular type of Tracker for a given core
     */
    private Map<String, ConcurrentHashMap<Class<? extends Tracker>, Tracker>> trackers = new ConcurrentHashMap<>();
    
    /*
     * There is one ModelTracker per Alfresco data schema/repository, and it has no dependency on cores.
     */
    private volatile ModelTracker modelTracker;
    
    public Set<String> getCoreNames()
    {
        return this.trackers.keySet();
    }
    
    public Collection<Tracker> getTrackersForCore(String coreName)
    {
        ConcurrentHashMap<Class<? extends Tracker>, Tracker> coreTrackers = this.trackers.get(coreName);
        return ((coreTrackers == null) ? Collections.emptyList() : coreTrackers.values());
    }
    
    public boolean hasTrackersForCore(String coreName)
    {
        return this.trackers.containsKey(coreName);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Tracker> T getTrackerForCore(String coreName, Class<T> trackerClass)
    {
        Map<Class<? extends Tracker>, Tracker> coreTrackers = this.trackers.get(coreName);
        return (T) coreTrackers.get(trackerClass);
    }
    
    public synchronized void register(String coreName, Tracker tracker)
    {
        ConcurrentHashMap<Class<? extends Tracker>, Tracker> coreTrackers = this.trackers.get(coreName);
        if (coreTrackers == null) 
        {
            coreTrackers = new ConcurrentHashMap<>();
            this.trackers.put(coreName, coreTrackers);
        }
        
        coreTrackers.put(tracker.getClass(), tracker);
    }

    /**
     * Removes the trackers for the specified core.
     * @param coreName 
     * @return <code>true</code> if there were trackers registered for the core
     */
    public boolean removeTrackersForCore(String coreName)
    {
        return this.trackers.remove(coreName) != null;
    }

    public ModelTracker getModelTracker()
    {
        return modelTracker;
    }

    public void setModelTracker(ModelTracker modelTracker)
    {
        this.modelTracker = modelTracker;
    }
}
