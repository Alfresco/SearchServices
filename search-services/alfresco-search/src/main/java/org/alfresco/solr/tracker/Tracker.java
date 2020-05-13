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

import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.alfresco.solr.TrackerState;

public interface Tracker
{
    void track();

    void maintenance() throws Exception;

    boolean hasMaintenance() throws Exception;

    Semaphore getWriteLock();
    
    void setShutdown(boolean shutdown);

    boolean isAlreadyInShutDownMode();

    void shutdown();

    boolean getRollback();
    
    Throwable getRollbackCausedBy();
    
    Properties getProps();
    
    void setRollback(boolean rollback, Throwable rollbackCausedBy);

    void invalidateState();

    TrackerState getTrackerState();
    
    Type getType();
    
    enum Type
    {
        MODEL,
        CONTENT,
        ACL,
        CASCADE,
        COMMIT,
        METADATA,
        NODE_STATE_PUBLISHER
    }
}
