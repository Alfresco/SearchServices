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

package org.alfresco.solr;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.tracker.ActivatableTracker;
import org.alfresco.solr.tracker.Tracker;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.SchedulerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SolrTestCaseJ4.SuppressSSL
public class AlfrescoIndexingStatePersistenceAcrossReloadsIT extends AbstractAlfrescoSolrIT
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
        admin = (AlfrescoCoreAdminHandler)getCore().getCoreContainer().getMultiCoreHandler();
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
    public void testIndexingStateAcrossReloads() throws Exception
    {
        long localId = 0L;

        AclChangeSet aclChangeSet = getAclChangeSet(1, ++localId);

        Acl acl = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                singletonList(acl),
                singletonList(aclReaders));

        int numNodes = 1;
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaDatas = new ArrayList<>();

        Transaction bigTxn = getTransaction(0, numNodes, ++localId);

        for(int i=0; i<numNodes; i++)
        {
            Node node = getNode(bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, acl, "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);

        Collection<Tracker> trackers = getTrackers();

        disableIndexing();

        // Make sure trackers have been disabled
        Collection<ActivatableTracker> activatableTrackers =
                getTrackers().stream()
                        .filter(tracker -> tracker instanceof ActivatableTracker)
                        .map(ActivatableTracker.class::cast)
                        .collect(Collectors.toList());

        assertFalse(activatableTrackers.isEmpty());
        activatableTrackers.forEach(tracker -> assertTrue(tracker.isDisabled()));

        // Reload the core
        reloadAndAssertCorrect(trackers, trackers.size(), getJobsCount());

        // Make sure indexing is disabled in the reloaded core
        Collection<ActivatableTracker> activatableTrackersBelongingToReloadedCore =
                                getTrackers().stream()
                                    .filter(tracker -> tracker instanceof ActivatableTracker)
                                    .map(ActivatableTracker.class::cast)
                                    .collect(Collectors.toList());

        assertFalse(activatableTrackersBelongingToReloadedCore.isEmpty());
        activatableTrackersBelongingToReloadedCore.forEach(tracker -> assertTrue(tracker.isDisabled()));

        // Re-enable indexing
        enableIndexing();

        // Make sure tracking has been enabled
        activatableTrackersBelongingToReloadedCore.forEach(tracker -> assertTrue(tracker.isEnabled()));

        Transaction bigTxn2 = getTransaction(0, numNodes, ++localId);
        for(int i=0; i<numNodes; i++)
        {
            Node node = getNode(bigTxn2, acl, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn2, acl, "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn2, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes * 2, 500000);
    }

    private void reloadAndAssertCorrect(Collection<Tracker> trackers, int numOfTrackers, int jobs) throws Exception
    {
        reload();
        //Give it a little time to shutdown properly and recover.
        TimeUnit.SECONDS.sleep(1);

        Collection<Tracker> reloadedTrackers = getTrackers();
        assertEquals("After a reload the number of trackers should be the same", numOfTrackers, getTrackers().size());
        assertEquals("After a reload the number of jobs should be the same", jobs, getJobsCount());

        trackers.forEach(tracker -> assertFalse("The reloaded trackers should be different.", reloadedTrackers.contains(tracker)));
    }

    private int getJobsCount() throws SchedulerException
    {
        return admin.getScheduler().getJobsCount();
    }
}