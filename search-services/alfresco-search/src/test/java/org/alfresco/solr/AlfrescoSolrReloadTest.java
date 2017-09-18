package org.alfresco.solr;

import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.tracker.Tracker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.SchedulerException;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoSolrReloadTest extends AbstractAlfrescoSolrTests {
    private static Log logger = LogFactory.getLog(org.alfresco.solr.tracker.AlfrescoSolrTrackerTest.class);

    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("schema.xml");
        admin = (AlfrescoCoreAdminHandler)h.getCore().getCoreContainer().getMultiCoreHandler();
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
    public void testReload() throws Exception {

        long localId = 0L;
        logger.info("######### Starting tracker reload test NODES ###########");

        AclChangeSet aclChangeSet = getAclChangeSet(1, ++localId);

        Acl acl = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);

        indexAclChangeSet(aclChangeSet,
                list(acl),
                list(aclReaders));

        int numNodes = 1000;
        List<Node> nodes = new ArrayList();
        List<NodeMetaData> nodeMetaDatas = new ArrayList();

        Transaction bigTxn = getTransaction(0, numNodes, ++localId);

        for(int i=0; i<numNodes; i++) {
            Node node = getNode(bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, acl, "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 1000, 100000);

        Collection<Tracker> trackers = getTrackers();
        

        int numOfTrackers = trackers.size();
        int jobs = getJobsCount();

        reloadAndAssertCorrect(trackers, numOfTrackers, jobs);

        Transaction bigTxn2 = getTransaction(0, numNodes, ++localId);

        for(int i=0; i<numNodes; i++) {
            Node node = getNode(bigTxn2, acl, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn2, acl, "mike", null, false);
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn2, nodes, nodeMetaDatas);

        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2000, 100000);
    }

    private void reloadAndAssertCorrect(Collection<Tracker> trackers, int numOfTrackers, int jobs) throws Exception {
        logger.info("######### reload called ###########");
        h.reload();
        //Give it a little time to shutdown properly and recover.
        TimeUnit.SECONDS.sleep(1);
        logger.info("######### reload finished ###########");

        Collection<Tracker> reloadedTrackers = getTrackers();
        assertEquals("After a reload the number of trackers should be the same", numOfTrackers, getTrackers().size());
        assertEquals("After a reload the number of jobs should be the same", jobs, getJobsCount());

        trackers.forEach(tracker ->
        {
            assertFalse("The reloaded trackers should be different.", reloadedTrackers.contains(tracker));
        });
    }

    private int getJobsCount() throws SchedulerException {
        int count = admin.getScheduler().getJobsCount();
        logger.info("######### Number of jobs is "+count+" ###########");
        return count;
    }
}
