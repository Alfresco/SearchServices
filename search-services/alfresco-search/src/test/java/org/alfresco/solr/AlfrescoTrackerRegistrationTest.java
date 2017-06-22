/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
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
package org.alfresco.solr;

import java.util.Collection;
import java.util.Properties;

import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.tracker.Tracker;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class AlfrescoTrackerRegistrationTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("schema.xml");
    }

    @After
    public void clearQueue() throws Exception {
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
    }
    @Test
    public void checkCronOnTrackers()
    {
        Collection<Tracker> trackers = getTrackers();
        Assert.assertNotNull(trackers);
        trackers.forEach((tracker-> checkCronOnTracker(tracker)));
    }
    private void checkCronOnTracker(Tracker tracker)
    {
        Properties props = tracker.getProps();
        Assert.assertEquals("0/10 * * * * ? *", props.get("alfresco.tracker.acl.cron"));
        Assert.assertEquals("0/10 * * * * ? *", props.get("alfresco.tracker.content.cron"));
        Assert.assertEquals("0/10 * * * * ? *", props.get("alfresco.tracker.metadata.cron"));
    }
}
