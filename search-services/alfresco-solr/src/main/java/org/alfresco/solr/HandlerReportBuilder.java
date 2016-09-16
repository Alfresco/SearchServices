package org.alfresco.solr;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.service.cmr.repository.datatype.Duration;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.tracker.*;
import org.alfresco.util.CachingDateFormat;
import org.apache.commons.codec.EncoderException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by gethin on 13/09/16.
 */
public class HandlerReportBuilder {


    /**
     *
     * @param trackerRegistry
     * @param srv
     * @param coreName
     * @param tracker
     * @param acltxid
     * @return
     * @throws AuthenticationException
     * @throws IOException
     * @throws JSONException
     * @throws EncoderException
     */
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


    /**
     * @param cname
     * @param detail
     * @param hist
     * @param values
     * @param srv
     * @param report
     * @throws IOException
     */
    public static void addCoreSummary(TrackerRegistry trackerRegistry, String cname, boolean detail, boolean hist, boolean values,
                                InformationServer srv, NamedList<Object> report) throws IOException
    {
        NamedList<Object> coreSummary = new SimpleOrderedMap<Object>();
        coreSummary.addAll((SimpleOrderedMap<Object>) srv.getCoreStats());

        MetadataTracker metaTrkr = trackerRegistry.getTrackerForCore(cname, MetadataTracker.class);
        TrackerState metadataTrkrState = metaTrkr.getTrackerState();
        long lastIndexTxCommitTime = metadataTrkrState.getLastIndexedTxCommitTime();

        long lastIndexedTxId = metadataTrkrState.getLastIndexedTxId();
        long lastTxCommitTimeOnServer = metadataTrkrState.getLastTxCommitTimeOnServer();
        long lastTxIdOnServer = metadataTrkrState.getLastTxIdOnServer();
        Date lastIndexTxCommitDate = new Date(lastIndexTxCommitTime);
        Date lastTxOnServerDate = new Date(lastTxCommitTimeOnServer);
        long transactionsToDo = lastTxIdOnServer - lastIndexedTxId;
        if (transactionsToDo < 0)
        {
            transactionsToDo = 0;
        }

        AclTracker aclTrkr = trackerRegistry.getTrackerForCore(cname, AclTracker.class);
        TrackerState aclTrkrState = aclTrkr.getTrackerState();
        long lastIndexChangeSetCommitTime = aclTrkrState.getLastIndexedChangeSetCommitTime();
        long lastIndexedChangeSetId = aclTrkrState.getLastIndexedChangeSetId();
        long lastChangeSetCommitTimeOnServer = aclTrkrState.getLastChangeSetCommitTimeOnServer();
        long lastChangeSetIdOnServer = aclTrkrState.getLastChangeSetIdOnServer();
        Date lastIndexChangeSetCommitDate = new Date(lastIndexChangeSetCommitTime);
        Date lastChangeSetOnServerDate = new Date(lastChangeSetCommitTimeOnServer);
        long changeSetsToDo = lastChangeSetIdOnServer - lastIndexedChangeSetId;
        if (changeSetsToDo < 0)
        {
            changeSetsToDo = 0;
        }

        long nodesToDo = 0;
        long remainingTxTimeMillis = 0;
        if (transactionsToDo > 0)
        {
            // We now use the elapsed time as seen by the single thread farming out metadata indexing
            double meanDocsPerTx = srv.getTrackerStats().getMeanDocsPerTx();
            double meanNodeElaspedIndexTime = srv.getTrackerStats().getMeanNodeElapsedIndexTime();
            nodesToDo = (long)(transactionsToDo * meanDocsPerTx);
            remainingTxTimeMillis = (long) (nodesToDo * meanNodeElaspedIndexTime);
        }
        Date now = new Date();
        Date end = new Date(now.getTime() + remainingTxTimeMillis);
        Duration remainingTx = new Duration(now, end);

        long remainingChangeSetTimeMillis = 0;
        if (changeSetsToDo > 0)
        {
            // We now use the elapsed time as seen by the single thread farming out alc indexing
            double meanAclsPerChangeSet = srv.getTrackerStats().getMeanAclsPerChangeSet();
            double meanAclElapsedIndexTime = srv.getTrackerStats().getMeanAclElapsedIndexTime();
            remainingChangeSetTimeMillis = (long) (changeSetsToDo * meanAclsPerChangeSet * meanAclElapsedIndexTime);
        }
        now = new Date();
        end = new Date(now.getTime() + remainingChangeSetTimeMillis);
        Duration remainingChangeSet = new Duration(now, end);

        NamedList<Object> ftsSummary = new SimpleOrderedMap<Object>();
        long remainingContentTimeMillis = 0;
        srv.addFTSStatusCounts(ftsSummary);
        long cleanCount = ((Long)ftsSummary.get("Node count with FTSStatus Clean")).longValue();
        long dirtyCount = ((Long)ftsSummary.get("Node count with FTSStatus Dirty")).longValue();
        long newCount = ((Long)ftsSummary.get("Node count with FTSStatus New")).longValue();
        long nodesInIndex = ((Long)coreSummary.get("Alfresco Nodes in Index"));
        long contentYetToSee = nodesInIndex > 0 ? nodesToDo * (cleanCount + dirtyCount + newCount)/nodesInIndex  : 0;;
        if (dirtyCount + newCount + contentYetToSee > 0)
        {
            // We now use the elapsed time as seen by the single thread farming out alc indexing
            double meanContentElapsedIndexTime = srv.getTrackerStats().getMeanContentElapsedIndexTime();
            remainingContentTimeMillis = (long) ((dirtyCount + newCount + contentYetToSee) * meanContentElapsedIndexTime);
        }
        now = new Date();
        end = new Date(now.getTime() + remainingContentTimeMillis);
        Duration remainingContent = new Duration(now, end);
        coreSummary.add("FTS",ftsSummary);

        Duration txLag = new Duration(lastIndexTxCommitDate, lastTxOnServerDate);
        if (lastIndexTxCommitDate.compareTo(lastTxOnServerDate) > 0)
        {
            txLag = new Duration();
        }
        long txLagSeconds = (lastTxCommitTimeOnServer - lastIndexTxCommitTime) / 1000;
        if (txLagSeconds < 0)
        {
            txLagSeconds = 0;
        }

        Duration changeSetLag = new Duration(lastIndexChangeSetCommitDate, lastChangeSetOnServerDate);
        if (lastIndexChangeSetCommitDate.compareTo(lastChangeSetOnServerDate) > 0)
        {
            changeSetLag = new Duration();
        }
        long changeSetLagSeconds = (lastChangeSetCommitTimeOnServer - lastIndexChangeSetCommitTime) / 1000;
        if (txLagSeconds < 0)
        {
            txLagSeconds = 0;
        }

        ContentTracker contentTrkr = trackerRegistry.getTrackerForCore(cname, ContentTracker.class);
        TrackerState contentTrkrState = contentTrkr.getTrackerState();
        // Leave ModelTracker out of this check, because it is common
        boolean aTrackerIsRunning = aclTrkrState.isRunning() || metadataTrkrState.isRunning()
                || contentTrkrState.isRunning();
        coreSummary.add("Active", aTrackerIsRunning);

        ModelTracker modelTrkr = trackerRegistry.getModelTracker();
        TrackerState modelTrkrState = modelTrkr.getTrackerState();
        coreSummary.add("ModelTracker Active", modelTrkrState.isRunning());
        coreSummary.add("ContentTracker Active", contentTrkrState.isRunning());
        coreSummary.add("MetadataTracker Active", metadataTrkrState.isRunning());
        coreSummary.add("AclTracker Active", aclTrkrState.isRunning());

        // TX

        coreSummary.add("Last Index TX Commit Time", lastIndexTxCommitTime);
        coreSummary.add("Last Index TX Commit Date", lastIndexTxCommitDate);
        coreSummary.add("TX Lag", txLagSeconds + " s");
        coreSummary.add("TX Duration", txLag.toString());
        coreSummary.add("Timestamp for last TX on server", lastTxCommitTimeOnServer);
        coreSummary.add("Date for last TX on server", lastTxOnServerDate);
        coreSummary.add("Id for last TX on server", lastTxIdOnServer);
        coreSummary.add("Id for last TX in index", lastIndexedTxId);
        coreSummary.add("Approx transactions remaining", transactionsToDo);
        coreSummary.add("Approx transaction indexing time remaining", remainingTx.largestComponentformattedString());

        // Change set

        coreSummary.add("Last Index Change Set Commit Time", lastIndexChangeSetCommitTime);
        coreSummary.add("Last Index Change Set Commit Date", lastIndexChangeSetCommitDate);
        coreSummary.add("Change Set Lag", changeSetLagSeconds + " s");
        coreSummary.add("Change Set Duration", changeSetLag.toString());
        coreSummary.add("Timestamp for last Change Set on server", lastChangeSetCommitTimeOnServer);
        coreSummary.add("Date for last Change Set on server", lastChangeSetOnServerDate);
        coreSummary.add("Id for last Change Set on server", lastChangeSetIdOnServer);
        coreSummary.add("Id for last Change Set in index", lastIndexedChangeSetId);
        coreSummary.add("Approx change sets remaining", changeSetsToDo);
        coreSummary.add("Approx change set indexing time remaining",
                remainingChangeSet.largestComponentformattedString());

        coreSummary.add("Approx content indexing time remaining",
                remainingContent.largestComponentformattedString());

        // Stats

        coreSummary.add("Model sync times (ms)",
                srv.getTrackerStats().getModelTimes().getNamedList(detail, hist, values));
        coreSummary.add("Acl index time (ms)",
                srv.getTrackerStats().getAclTimes().getNamedList(detail, hist, values));
        coreSummary.add("Node index time (ms)",
                srv.getTrackerStats().getNodeTimes().getNamedList(detail, hist, values));
        coreSummary.add("Docs/Tx", srv.getTrackerStats().getTxDocs().getNamedList(detail, hist, values));
        coreSummary.add("Doc Transformation time (ms)", srv.getTrackerStats().getDocTransformationTimes()
                                                           .getNamedList(detail, hist, values));

        // Model

        Map<String, Set<String>> modelErrors = srv.getModelErrors();
        if (modelErrors.size() > 0)
        {
            NamedList<Object> errorList = new SimpleOrderedMap<Object>();
            for (Map.Entry<String, Set<String>> modelNameToErrors : modelErrors.entrySet())
            {
                errorList.add(modelNameToErrors.getKey(), modelNameToErrors.getValue());
            }
            coreSummary.add("Model changes are not compatible with the existing data model and have not been applied",
                    errorList);
        }

        report.add(cname, coreSummary);
    }
}
