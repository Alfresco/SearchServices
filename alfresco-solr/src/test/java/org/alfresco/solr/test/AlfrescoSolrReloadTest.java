package org.alfresco.solr.test;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.*;
import org.alfresco.solr.tracker.Tracker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.alfresco.solr.AlfrescoSolrUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoSolrReloadTest extends AbstractAlfrescoSolrTests {
    private static Log logger = LogFactory.getLog(org.alfresco.solr.tracker.AlfrescoSolrTrackerTest.class);
    private static long MAX_WAIT_TIME = 80000;

    static AlfrescoCoreAdminHandler admin;
    private static NodeRef rootNodeRef;
    private static NodeRef n01NodeRef;

    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
        admin = (AlfrescoCoreAdminHandler) h.getCoreContainer().getMultiCoreHandler();

        // Root
        SolrCore core = h.getCore();
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        dataModel.setCMDefaultUri();

        rootNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        addStoreRoot(core, dataModel, rootNodeRef, 1, 1, 1, 1);

        // 1
        n01NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n01QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "one");
        ChildAssociationRef n01CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n01QName,
                n01NodeRef, true, 0);

        Map<QName, PropertyValue> testProperties = new HashMap<QName, PropertyValue>();
        Date orderDate = new Date();
        testProperties.put(createdDate,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderDate)));
        testProperties.put(createdTime,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderDate)));
        testProperties.put(ContentModel.PROP_CONTENT, new StringPropertyValue("lettice and cabbage"));
        testProperties.put(ContentModel.PROP_NAME, new StringPropertyValue("reload"));

        addNode(core, dataModel, 1, 2, 1, testSuperType, null, testProperties, null, "andy",
                new ChildAssociationRef[]{n01CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n01QName.toString()}, n01NodeRef, true);

        testNodeRef = n01NodeRef;
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

        logger.info("######### Starting tracker reload test ###########");

        assertAQuery("PATH:\"/\"", 1);
        assertAQuery("TYPE:\"" + testSuperType + "\"", 1);

        Collection<Tracker> trackers = getTrackers();
        int numOfTrackers = trackers.size();
        int jobs = getJobsCount();

        addNodes(1, 10);
        assertAQuery("TYPE:\"" + testSuperType + "\"", 10);

        logger.info("######### reload called ###########");
        h.reload();
        logger.info("######### reload finished ###########");

        //Give it a little time to shutdown properly and recover.
        TimeUnit.SECONDS.sleep(1);
        logger.info("#################### After sleep ##############################");

        Collection<Tracker> reloadedTrackers = getTrackers();
        assertEquals("After a reload the number of trackers should be the same", numOfTrackers, getTrackers().size());
        assertEquals("After a reload the number of jobs should be the same", jobs, getJobsCount());

        trackers.forEach(tracker ->
        {
            assertFalse("The reloaded trackers should be different.", reloadedTrackers.contains(tracker));
        });

        addNodes(10, 21);
        assertAQuery("TYPE:\"" + testSuperType + "\"", 21);
    }

    private void addNodes(int startInclusive, int endExclusive) {
        IntStream.range(startInclusive, endExclusive).forEach(i ->
            {
                NodeRef newNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
                QName n2Name = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "loop"+i);
                ChildAssociationRef childAssoc = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n2Name, newNodeRef, true, 0);
                try {
                    addNode(h.getCore(), dataModel, 1+i, 2+i, 1+i, testSuperType, null, null, null, "andy",
                            new ChildAssociationRef[]{childAssoc}, new NodeRef[]{rootNodeRef}, new String[]{"/"+ n2Name.toString()},
                            newNodeRef, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        );
    }

    private int getJobsCount() throws SchedulerException {
        int count = admin.getScheduler().getJobsCount();
        logger.info("######### Number of jobs is "+count+" ###########");
        return count;
    }

    private Collection<Tracker> getTrackers() {
        Collection<Tracker> trackers = admin.getTrackerRegistry().getTrackersForCore(h.getCore().getName());
        logger.info("######### Number of trackers is "+trackers.size()+" ###########");
        return trackers;
    }

}
