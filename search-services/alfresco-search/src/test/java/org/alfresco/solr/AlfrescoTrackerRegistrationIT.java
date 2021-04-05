/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
public class AlfrescoTrackerRegistrationIT extends AbstractAlfrescoSolrIT
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
    }

    @After
    public void clearQueue()
    {
        SOLRAPIQueueClient.NODE_META_DATA_MAP.clear();
        SOLRAPIQueueClient.TRANSACTION_QUEUE.clear();
        SOLRAPIQueueClient.ACL_CHANGE_SET_QUEUE.clear();
        SOLRAPIQueueClient.ACL_READERS_MAP.clear();
        SOLRAPIQueueClient.ACL_MAP.clear();
        SOLRAPIQueueClient.NODE_MAP.clear();
    }

    @Test
    public void checkCronOnTrackers()
    {
        Collection<Tracker> trackers = getTrackers();
        Assert.assertNotNull(trackers);
        trackers.forEach((this::checkCronOnTracker));
    }

    private void checkCronOnTracker(Tracker tracker)
    {
        Properties props = tracker.getProps();
        Assert.assertEquals("0/10 * * * * ? *", props.get("alfresco.tracker.acl.cron"));
        Assert.assertEquals("0/10 * * * * ? *", props.get("alfresco.tracker.content.cron"));
        Assert.assertEquals("0/10 * * * * ? *", props.get("alfresco.tracker.metadata.cron"));
    }
}
