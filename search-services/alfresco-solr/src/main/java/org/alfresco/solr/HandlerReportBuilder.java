package org.alfresco.solr;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.alfresco.util.CachingDateFormat;
import org.apache.commons.codec.EncoderException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by gethin on 13/09/16.
 */
public class HandlerReportBuilder {


    public static NamedList<Object> buildAclTxReport(TrackerRegistry trackerRegistry, InformationServer srv, String coreName, AclTracker tracker, Long acltxid)
            throws AuthenticationException, IOException, JSONException, EncoderException
    {
        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("TXID", acltxid);
        nr.add("transaction", buildTrackerReport(trackerRegistry, srv, coreName, 0l, 0l, acltxid, acltxid, null, null));
        NamedList<Object> nodes = new SimpleOrderedMap<Object>();
        // add node reports ....
        List<Long> dbAclIds = tracker.getAclsForDbAclTransaction(acltxid);
        for (Long aclid : dbAclIds)
        {
            nodes.add("ACLID " + aclid, buildAclReport(tracker, aclid));
        }
        nr.add("aclTxDbAclCount", dbAclIds.size());
        nr.add("nodes", nodes);
        return nr;
    }


    public static NamedList<Object> buildAclReport(AclTracker tracker, Long aclid) throws IOException, JSONException
    {
        AclReport aclReport = tracker.checkAcl(aclid);

        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("Acl Id", aclReport.getAclId());
        nr.add("Acl doc in index", aclReport.getIndexAclDoc());
        if (aclReport.getIndexAclDoc() != null)
        {
            nr.add("Acl tx in Index", aclReport.getIndexAclTx());
        }

        return nr;
    }

    public static NamedList<Object> buildTxReport(TrackerRegistry trackerRegistry, InformationServer srv, String coreName, MetadataTracker tracker, Long txid)
            throws AuthenticationException, IOException, JSONException, EncoderException
    {
        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("TXID", txid);
        nr.add("transaction", buildTrackerReport(trackerRegistry, srv, coreName, txid, txid, 0l, 0l, null, null));
        NamedList<Object> nodes = new SimpleOrderedMap<Object>();
        // add node reports ....
        List<Node> dbNodes = tracker.getFullNodesForDbTransaction(txid);
        for (Node node : dbNodes)
        {
            nodes.add("DBID " + node.getId(), buildNodeReport(tracker, node));
        }

        nr.add("txDbNodeCount", dbNodes.size());
        nr.add("nodes", nodes);
        return nr;
    }

    public static NamedList<Object> buildNodeReport(MetadataTracker tracker, Node node) throws IOException, JSONException
    {
        NodeReport nodeReport = tracker.checkNode(node);

        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("Node DBID", nodeReport.getDbid());
        nr.add("DB TX", nodeReport.getDbTx());
        nr.add("DB TX status", nodeReport.getDbNodeStatus().toString());
        if (nodeReport.getIndexLeafDoc() != null)
        {
            nr.add("Leaf tx in Index", nodeReport.getIndexLeafTx());
        }
        if (nodeReport.getIndexAuxDoc() != null)
        {
            nr.add("Aux tx in Index", nodeReport.getIndexAuxTx());
        }
        nr.add("Indexed Node Doc Count", nodeReport.getIndexedNodeDocCount());
        return nr;
    }

    public static NamedList<Object> buildNodeReport(MetadataTracker tracker, Long dbid) throws IOException, JSONException
    {
        NodeReport nodeReport = tracker.checkNode(dbid);

        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("Node DBID", nodeReport.getDbid());
        nr.add("DB TX", nodeReport.getDbTx());
        nr.add("DB TX status", nodeReport.getDbNodeStatus().toString());
        if (nodeReport.getIndexLeafDoc() != null)
        {
            nr.add("Leaf tx in Index", nodeReport.getIndexLeafTx());
        }
        if (nodeReport.getIndexAuxDoc() != null)
        {
            nr.add("Aux tx in Index", nodeReport.getIndexAuxTx());
        }
        nr.add("Indexed Node Doc Count", nodeReport.getIndexedNodeDocCount());
        return nr;
    }


    public static NamedList<Object> buildTrackerReport(TrackerRegistry trackerRegistry, InformationServer srv, String coreName, Long fromTx, Long toTx, Long fromAclTx, Long toAclTx,
                                                 Long fromTime, Long toTime) throws IOException, JSONException, AuthenticationException, EncoderException
    {
        // ACL
        AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
        IndexHealthReport aclReport = aclTracker.checkIndex(toTx, toAclTx, fromTime, toTime);
        NamedList<Object> ihr = new SimpleOrderedMap<Object>();
        ihr.add("Alfresco version", aclTracker.getAlfrescoVersion());
        ihr.add("DB acl transaction count", aclReport.getDbAclTransactionCount());
        ihr.add("Count of duplicated acl transactions in the index", aclReport.getDuplicatedAclTxInIndex()
                                                                              .cardinality());
        if (aclReport.getDuplicatedAclTxInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate acl tx", aclReport.getDuplicatedAclTxInIndex().nextSetBit(0L));
        }
        ihr.add("Count of acl transactions in the index but not the DB", aclReport.getAclTxInIndexButNotInDb()
                                                                                  .cardinality());
        if (aclReport.getAclTxInIndexButNotInDb().cardinality() > 0)
        {
            ihr.add("First acl transaction in the index but not the DB", aclReport.getAclTxInIndexButNotInDb()
                                                                                  .nextSetBit(0L));
        }
        ihr.add("Count of missing acl transactions from the Index", aclReport.getMissingAclTxFromIndex()
                                                                             .cardinality());
        if (aclReport.getMissingAclTxFromIndex().cardinality() > 0)
        {
            ihr.add("First acl transaction missing from the Index", aclReport.getMissingAclTxFromIndex()
                                                                             .nextSetBit(0L));
        }
        ihr.add("Index acl transaction count", aclReport.getAclTransactionDocsInIndex());
        ihr.add("Index unique acl transaction count", aclReport.getAclTransactionDocsInIndex());
        TrackerState aclState = aclTracker.getTrackerState();
        ihr.add("Last indexed change set commit time", aclState.getLastIndexedChangeSetCommitTime());
        Date lastChangeSetDate = new Date(aclState.getLastIndexedChangeSetCommitTime());
        ihr.add("Last indexed change set commit date", CachingDateFormat.getDateFormat().format(lastChangeSetDate));
        ihr.add("Last changeset id before holes", aclState.getLastIndexedChangeSetIdBeforeHoles());

        // Metadata
        MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
        IndexHealthReport metaReport = metadataTracker.checkIndex(toTx, toAclTx, fromTime, toTime);
        ihr.add("DB transaction count", metaReport.getDbTransactionCount());
        ihr.add("Count of duplicated transactions in the index", metaReport.getDuplicatedTxInIndex()
                                                                           .cardinality());
        if (metaReport.getDuplicatedTxInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate", metaReport.getDuplicatedTxInIndex().nextSetBit(0L));
        }
        ihr.add("Count of transactions in the index but not the DB", metaReport.getTxInIndexButNotInDb()
                                                                               .cardinality());
        if (metaReport.getTxInIndexButNotInDb().cardinality() > 0)
        {
            ihr.add("First transaction in the index but not the DB", metaReport.getTxInIndexButNotInDb()
                                                                               .nextSetBit(0L));
        }
        ihr.add("Count of missing transactions from the Index", metaReport.getMissingTxFromIndex().cardinality());
        if (metaReport.getMissingTxFromIndex().cardinality() > 0)
        {
            ihr.add("First transaction missing from the Index", metaReport.getMissingTxFromIndex()
                                                                          .nextSetBit(0L));
        }
        ihr.add("Index transaction count", metaReport.getTransactionDocsInIndex());
        ihr.add("Index unique transaction count", metaReport.getTransactionDocsInIndex());
        ihr.add("Index node count", metaReport.getLeafDocCountInIndex());
        ihr.add("Count of duplicate nodes in the index", metaReport.getDuplicatedLeafInIndex().cardinality());
        if (metaReport.getDuplicatedLeafInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate node id in the index", metaReport.getDuplicatedLeafInIndex().nextSetBit(0L));
        }
        ihr.add("Index error count", metaReport.getErrorDocCountInIndex());
        ihr.add("Count of duplicate error docs in the index", metaReport.getDuplicatedErrorInIndex()
                                                                        .cardinality());
        if (metaReport.getDuplicatedErrorInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate error in the index", SolrInformationServer.PREFIX_ERROR
                    + metaReport.getDuplicatedErrorInIndex().nextSetBit(0L));
        }
        ihr.add("Index unindexed count", metaReport.getUnindexedDocCountInIndex());
        ihr.add("Count of duplicate unindexed docs in the index", metaReport.getDuplicatedUnindexedInIndex()
                                                                            .cardinality());
        if (metaReport.getDuplicatedUnindexedInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate unindexed in the index",
                    metaReport.getDuplicatedUnindexedInIndex().nextSetBit(0L));
        }
        TrackerState metaState = metadataTracker.getTrackerState();
        ihr.add("Last indexed transaction commit time", metaState.getLastIndexedTxCommitTime());
        Date lastTxDate = new Date(metaState.getLastIndexedTxCommitTime());
        ihr.add("Last indexed transaction commit date", CachingDateFormat.getDateFormat().format(lastTxDate));
        ihr.add("Last TX id before holes", metaState.getLastIndexedTxIdBeforeHoles());

        srv.addFTSStatusCounts(ihr);

        return ihr;
    }


}
