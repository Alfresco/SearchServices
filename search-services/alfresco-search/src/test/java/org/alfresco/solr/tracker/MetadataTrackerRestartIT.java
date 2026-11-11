/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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

import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.list;
import static org.junit.Assert.assertTrue;

import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoSolrIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * MNT-25990: transactions committed while Search Services is stopped must be indexed after it restarts.
 */
@SolrTestCaseJ4.SuppressSSL
public class MetadataTrackerRestartIT extends AbstractAlfrescoSolrIT
{
    private static final long MAX_WAIT_TIME = 80000;
    private static final long ONE_HOUR = 3600000;

    private final Acl acl = getAcl(getAclChangeSet(1));

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
        SOLRAPIQueueClient.NODE_MAP.clear();
    }

    @Test
    public void transactionsCommittedWhileStoppedAreIndexedAfterRestart() throws Exception
    {
        MetadataTracker tracker = waitForRunningMetadataTracker();

        long stopTime = System.currentTimeMillis() - 3 * ONE_HOUR;
        Transaction lastIndexedBeforeStop = transaction(stopTime);
        Transaction committedWhileStopped = transaction(stopTime + ONE_HOUR / 2);

        // Before the stop: index a transaction older than the one-hour window that normal cycles scan.
        queueAndScanFrom(tracker, lastIndexedBeforeStop, stopTime);
        waitForLastIndexedTransaction(lastIndexedBeforeStop);

        // Restart: the first cycle scans [stopTime - 1h, stopTime], which holds only already-indexed transactions.
        queueAndScanFrom(tracker, committedWhileStopped, stopTime);
        waitForLastIndexedTransaction(committedWhileStopped);
    }

    // continueState() starts the next cycle at max(lastIndexedTxCommitTime, lastStartTime) - holeRetention.
    private void queueAndScanFrom(MetadataTracker tracker, Transaction transaction, long scanFromCommitTime) throws InterruptedException
    {
        Node node = getNode(transaction, acl, Node.SolrApiNodeStatus.UPDATED);
        indexTransaction(transaction, list(node), list(getNodeMetaData(node, transaction, acl, "mike", null, false)));

        // Holding the run lock keeps a starting cycle from overwriting lastStartTime.
        tracker.getRunLock().acquire();
        tracker.getTrackerState().setLastStartTime(scanFromCommitTime);
        tracker.getRunLock().release();
    }

    // Trackers register asynchronously, and getTrackerState() only returns the live state from the second cycle.
    private MetadataTracker waitForRunningMetadataTracker() throws InterruptedException
    {
        long timeout = System.currentTimeMillis() + MAX_WAIT_TIME;
        while (true)
        {
            MetadataTracker tracker = getTrackers().stream()
                    .filter(MetadataTracker.class::isInstance)
                    .map(MetadataTracker.class::cast)
                    .findFirst()
                    .orElse(null);
            if (tracker != null && tracker.getTrackerState().getTrackerCycles() > 0)
            {
                return tracker;
            }
            assertTrue("Metadata tracker did not start", System.currentTimeMillis() < timeout);
            Thread.sleep(1000);
        }
    }

    private static Transaction transaction(long commitTime)
    {
        Transaction transaction = getTransaction(0, 1);
        transaction.setCommitTimeMs(commitTime);
        return transaction;
    }

    private static void waitForLastIndexedTransaction(Transaction transaction) throws Exception
    {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST);
        query.add(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, transaction.getId(), transaction.getId() + 1, true, false),
                BooleanClause.Occur.MUST);
        waitForDocCount(query.build(), 1, MAX_WAIT_TIME);
    }
}
