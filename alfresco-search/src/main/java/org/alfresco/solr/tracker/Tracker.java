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

import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.alfresco.solr.TrackerState;

public interface Tracker
{
    void track();

    void maintenance() throws Exception;

    boolean hasMaintenance();

    Semaphore getWriteLock();

    String getAlfrescoVersion();
    
    void setShutdown(boolean shutdown);
    
    void shutdown();

    boolean getRollback();
    
    Properties getProps();
    
    void setRollback(boolean rollback);

    void invalidateState();

    TrackerState getTrackerState();
    
    Type getType();
    
    enum Type{
        Model,
        Content,
        ACL,
        Cascade,
        Commit,
        MetaData
    }
}
