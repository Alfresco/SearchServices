/*
 * #%L
 * Alfresco Solr Client
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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

package org.alfresco.solr;

/**
 * This class was moved from org.alfresco.solr.tracker.CoreTracker 
 * The data in this class is relevant for a particular Solr index.
 */
public class TrackerState
{
    private volatile long lastChangeSetIdOnServer;

    private volatile long lastChangeSetCommitTimeOnServer;

    private volatile long lastIndexedChangeSetId;

    private volatile long lastIndexedTxCommitTime = 0;

    private volatile long lastIndexedTxId = 0;

    private volatile long lastIndexedChangeSetCommitTime = 0;

    private volatile long lastTxCommitTimeOnServer = 0;

    private volatile long lastTxIdOnServer = 0;

    private volatile long lastIndexedTxIdBeforeHoles = -1;

    private volatile long lastIndexedChangeSetIdBeforeHoles = -1;

    private volatile boolean running = false;

    private volatile boolean checkedFirstTransactionTime = false;
    private volatile boolean checkedFirstAclTransactionTime = false;
    private volatile boolean checkedLastAclTransactionTime = false;
    private volatile boolean checkedLastTransactionTime = false;

    private volatile boolean check = false;

    private long timeToStopIndexing;

    private long lastGoodChangeSetCommitTimeInIndex;

    private long lastGoodTxCommitTimeInIndex;

    private long timeBeforeWhichThereCanBeNoHoles;

    public long getLastChangeSetIdOnServer()
    {
        return lastChangeSetIdOnServer;
    }

    public void setLastChangeSetIdOnServer(long lastChangeSetIdOnServer)
    {
        this.lastChangeSetIdOnServer = lastChangeSetIdOnServer;
    }

    public long getLastChangeSetCommitTimeOnServer()
    {
        return lastChangeSetCommitTimeOnServer;
    }

    public void setLastChangeSetCommitTimeOnServer(long lastChangeSetCommitTimeOnServer)
    {
        this.lastChangeSetCommitTimeOnServer = lastChangeSetCommitTimeOnServer;
    }

    public long getLastIndexedChangeSetId()
    {
        return lastIndexedChangeSetId;
    }

    public void setLastIndexedChangeSetId(long lastIndexedChangeSetId)
    {
        this.lastIndexedChangeSetId = lastIndexedChangeSetId;
    }

    public long getLastIndexedTxCommitTime()
    {
        return lastIndexedTxCommitTime;
    }

    public void setLastIndexedTxCommitTime(long lastIndexedTxCommitTime)
    {
        this.lastIndexedTxCommitTime = lastIndexedTxCommitTime;
    }

    public long getLastIndexedTxId()
    {
        return lastIndexedTxId;
    }

    public void setLastIndexedTxId(long lastIndexedTxId)
    {
        this.lastIndexedTxId = lastIndexedTxId;
    }

    public long getLastIndexedChangeSetCommitTime()
    {
        return lastIndexedChangeSetCommitTime;
    }

    public void setLastIndexedChangeSetCommitTime(long lastIndexedChangeSetCommitTime)
    {
        this.lastIndexedChangeSetCommitTime = lastIndexedChangeSetCommitTime;
    }

    public long getLastTxCommitTimeOnServer()
    {
        return lastTxCommitTimeOnServer;
    }

    public void setLastTxCommitTimeOnServer(long lastTxCommitTimeOnServer)
    {
        this.lastTxCommitTimeOnServer = lastTxCommitTimeOnServer;
    }

    public long getLastTxIdOnServer()
    {
        return lastTxIdOnServer;
    }

    public void setLastTxIdOnServer(long lastTxIdOnServer)
    {
        this.lastTxIdOnServer = lastTxIdOnServer;
    }

    public long getLastIndexedTxIdBeforeHoles()
    {
        return lastIndexedTxIdBeforeHoles;
    }

    public void setLastIndexedTxIdBeforeHoles(long lastIndexedTxIdBeforeHoles)
    {
        this.lastIndexedTxIdBeforeHoles = lastIndexedTxIdBeforeHoles;
    }

    public long getLastIndexedChangeSetIdBeforeHoles()
    {
        return lastIndexedChangeSetIdBeforeHoles;
    }

    public void setLastIndexedChangeSetIdBeforeHoles(long lastIndexedChangeSetIdBeforeHoles)
    {
        this.lastIndexedChangeSetIdBeforeHoles = lastIndexedChangeSetIdBeforeHoles;
    }

    public boolean isRunning()
    {
        return running;
    }

    public void setRunning(boolean running)
    {
        this.running = running;
    }

    public boolean isCheckedFirstTransactionTime()
    {
        return checkedFirstTransactionTime;
    }

    public void setCheckedFirstTransactionTime(boolean checkedFirstTransactionTime)
    {
        this.checkedFirstTransactionTime = checkedFirstTransactionTime;
    }

    public boolean isCheck()
    {
        return check;
    }

    public void setCheck(boolean check)
    {
        this.check = check;
    }

    public long getTimeToStopIndexing()
    {
        return timeToStopIndexing;
    }

    public void setTimeToStopIndexing(long timeToStopIndexing)
    {
        this.timeToStopIndexing = timeToStopIndexing;
    }

    public long getLastGoodChangeSetCommitTimeInIndex()
    {
        return lastGoodChangeSetCommitTimeInIndex;
    }

    public void setLastGoodChangeSetCommitTimeInIndex(long lastGoodChangeSetCommitTimeInIndex)
    {
        this.lastGoodChangeSetCommitTimeInIndex = lastGoodChangeSetCommitTimeInIndex;
    }

    public long getLastGoodTxCommitTimeInIndex()
    {
        return lastGoodTxCommitTimeInIndex;
    }

    public void setLastGoodTxCommitTimeInIndex(long lastGoodTxCommitTimeInIndex)
    {
        this.lastGoodTxCommitTimeInIndex = lastGoodTxCommitTimeInIndex;
    }

    public long getTimeBeforeWhichThereCanBeNoHoles()
    {
        return timeBeforeWhichThereCanBeNoHoles;
    }

    public void setTimeBeforeWhichThereCanBeNoHoles(long timeBeforeWhichThereCanBeNoHoles)
    {
        this.timeBeforeWhichThereCanBeNoHoles = timeBeforeWhichThereCanBeNoHoles;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "TrackerState [lastChangeSetIdOnServer=" + lastChangeSetIdOnServer
                    + ", lastChangeSetCommitTimeOnServer=" + lastChangeSetCommitTimeOnServer
                    + ", lastIndexedChangeSetId=" + lastIndexedChangeSetId 
                    + ", lastIndexedTxCommitTime=" + lastIndexedTxCommitTime 
                    + ", lastIndexedTxId=" + lastIndexedTxId
                    + ", lastIndexedChangeSetCommitTime=" + lastIndexedChangeSetCommitTime
                    + ", lastTxCommitTimeOnServer=" + lastTxCommitTimeOnServer 
                    + ", lastTxIdOnServer=" + lastTxIdOnServer 
                    + ", lastIndexedTxIdBeforeHoles=" + lastIndexedTxIdBeforeHoles
                    + ", lastIndexedChangeSetIdBeforeHoles=" + lastIndexedChangeSetIdBeforeHoles 
                    + ", running=" + running 
                    + ", checkedFirstTransactionTime=" + checkedFirstTransactionTime
                    + ", checkedFirstAclTransactionTime=" + this.checkedFirstAclTransactionTime
                    + ", checkedLastTransactionTime=" + this.checkedLastTransactionTime
                    + ", checkedLastAclTransactionTime=" + this.checkedLastAclTransactionTime 
                    + ", check=" + check
                    + ", timeToStopIndexing=" + timeToStopIndexing 
                    + ", lastGoodChangeSetCommitTimeInIndex=" + lastGoodChangeSetCommitTimeInIndex 
                    + ", lastGoodTxCommitTimeInIndex=" + lastGoodTxCommitTimeInIndex 
                    + ", timeBeforeWhichThereCanBeNoHoles=" + timeBeforeWhichThereCanBeNoHoles + "]";
    }

    public boolean isCheckedFirstAclTransactionTime()
    {
        return checkedFirstAclTransactionTime;
    }

    public void setCheckedFirstAclTransactionTime(boolean checkedFirstAclTransactionTime)
    {
        this.checkedFirstAclTransactionTime = checkedFirstAclTransactionTime;
    }

    public boolean isCheckedLastTransactionTime()
    {
        return checkedLastTransactionTime;
    }

    public void setCheckedLastTransactionTime(boolean checkedLastTransactionTime)
    {
        this.checkedLastTransactionTime = checkedLastTransactionTime;
    }

    public boolean isCheckedLastAclTransactionTime()
    {
        return checkedLastAclTransactionTime;
    }

    public void setCheckedLastAclTransactionTime(boolean checkedLastAclTransactionTime)
    {
        this.checkedLastAclTransactionTime = checkedLastAclTransactionTime;
    }

}
