/*
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 *
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
 */
package org.alfresco.solr;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.datatype.Duration;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.tracker.*;
import org.alfresco.util.CachingDateFormat;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * Methods taken from AlfrescoCoreAdminHandler that deal with building reports
 */
class HandlerReportHelper
{
    static NamedList<Object> buildAclReport(AclTracker tracker, Long aclid) throws JSONException
    {
        AclReport aclReport = tracker.checkAcl(aclid);

        NamedList<Object> nr = new SimpleOrderedMap<>();
        nr.add("Acl Id", aclReport.getAclId());
        nr.add("Acl doc in index", aclReport.getIndexAclDoc());
        if (aclReport.getIndexAclDoc() != null)
        {
            nr.add("Acl tx in Index", aclReport.getIndexAclTx());
        }

        return nr;
    }

    static NamedList<Object> buildTxReport(TrackerRegistry trackerRegistry, InformationServer srv, String coreName, MetadataTracker tracker, Long txid) throws JSONException
    {
        NamedList<Object> nr = new SimpleOrderedMap<>();
        nr.add("TXID", txid);
        nr.add("transaction", buildTrackerReport(trackerRegistry, srv, coreName, txid, txid, 0L, 0L, null, null));
        NamedList<Object> nodes = new SimpleOrderedMap<>();

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

    static NamedList<Object> buildAclTxReport(TrackerRegistry trackerRegistry, InformationServer srv, String coreName, AclTracker tracker, Long acltxid) throws JSONException
    {
        try {
            NamedList<Object> nr = new SimpleOrderedMap<>();
            nr.add("TXID", acltxid);
            nr.add("transaction", buildTrackerReport(trackerRegistry, srv, coreName, 0L, 0L, acltxid, acltxid, null, null));
            NamedList<Object> nodes = new SimpleOrderedMap<>();

            // add node reports ....
            List<Long> dbAclIds = tracker.getAclsForDbAclTransaction(acltxid);
            for (Long aclid : dbAclIds) {
                nodes.add("ACLID " + aclid, buildAclReport(tracker, aclid));
            }
            nr.add("aclTxDbAclCount", dbAclIds.size());
            nr.add("nodes", nodes);
            return nr;
        }
        catch (Exception exception)
        {
            throw new AlfrescoRuntimeException("", exception);
        }
    }

    static NamedList<Object> buildNodeReport(MetadataTracker tracker, Node node) throws JSONException
    {
        NodeReport nodeReport = tracker.checkNode(node);

        NamedList<Object> nr = new SimpleOrderedMap<>();
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

    static NamedList<Object> buildNodeReport(CoreStatePublisher publisher, Long dbid) throws JSONException
    {
        NodeReport nodeReport = publisher.checkNode(dbid);

        NamedList<Object> payload = new SimpleOrderedMap<>();
        payload.add("Node DBID", nodeReport.getDbid());

        if (publisher.isOnMasterOrStandalone())
        {
            ofNullable(nodeReport.getDbTx()).ifPresent(value -> payload.add("DB TX", value));
            ofNullable(nodeReport.getDbNodeStatus()).map(Object::toString).ifPresent(value -> payload.add("DB TX Status", value));
            ofNullable(nodeReport.getIndexLeafTx()).ifPresent(value -> payload.add("Leaf tx in Index", value));
            ofNullable(nodeReport.getIndexAuxDoc()).ifPresent(value -> payload.add("Aux tx in Index", value));
        }
        else
        {
            payload.add("WARNING", "This response comes from a slave core and it contains minimal information about the node. " +
                    "Please consider to re-submit the same request to the corresponding Master, in order to get more information.");
        }

        ofNullable(nodeReport.getIndexedNodeDocCount()).ifPresent(value -> payload.add("Indexed Node Doc Count", value));

        return payload;
    }

    /**
     * Builds Tracker report
     */
    static NamedList<Object> buildTrackerReport(TrackerRegistry trackerRegistry, InformationServer srv, String coreName, Long fromTx, Long toTx, Long fromAclTx, Long toAclTx,
                                                Long fromTime, Long toTime) throws JSONException
    {
        try
        {
            // ACL
            AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            IndexHealthReport aclReport = aclTracker.checkIndex(toTx, toAclTx, fromTime, toTime);
            NamedList<Object> ihr = new SimpleOrderedMap<>();
            ihr.add("Alfresco version", aclTracker.getAlfrescoVersion());
            ihr.add("DB acl transaction count", aclReport.getDbAclTransactionCount());
            ihr.add("Count of duplicated acl transactions in the index", aclReport.getDuplicatedAclTxInIndex()
                    .cardinality());
            if (aclReport.getDuplicatedAclTxInIndex().cardinality() > 0) {
                ihr.add("First duplicate acl tx", aclReport.getDuplicatedAclTxInIndex().nextSetBit(0L));
            }
            ihr.add("Count of acl transactions in the index but not the DB", aclReport.getAclTxInIndexButNotInDb()
                    .cardinality());
            if (aclReport.getAclTxInIndexButNotInDb().cardinality() > 0) {
                ihr.add("First acl transaction in the index but not the DB", aclReport.getAclTxInIndexButNotInDb()
                        .nextSetBit(0L));
            }
            ihr.add("Count of missing acl transactions from the Index", aclReport.getMissingAclTxFromIndex()
                    .cardinality());
            if (aclReport.getMissingAclTxFromIndex().cardinality() > 0) {
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
            if (metaReport.getDuplicatedTxInIndex().cardinality() > 0) {
                ihr.add("First duplicate", metaReport.getDuplicatedTxInIndex().nextSetBit(0L));
            }
            ihr.add("Count of transactions in the index but not the DB", metaReport.getTxInIndexButNotInDb()
                    .cardinality());
            if (metaReport.getTxInIndexButNotInDb().cardinality() > 0) {
                ihr.add("First transaction in the index but not the DB", metaReport.getTxInIndexButNotInDb()
                        .nextSetBit(0L));
            }
            ihr.add("Count of missing transactions from the Index", metaReport.getMissingTxFromIndex().cardinality());
            if (metaReport.getMissingTxFromIndex().cardinality() > 0) {
                ihr.add("First transaction missing from the Index", metaReport.getMissingTxFromIndex()
                        .nextSetBit(0L));
            }
            ihr.add("Index transaction count", metaReport.getTransactionDocsInIndex());
            ihr.add("Index unique transaction count", metaReport.getTransactionDocsInIndex());
            ihr.add("Index node count", metaReport.getLeafDocCountInIndex());
            ihr.add("Count of duplicate nodes in the index", metaReport.getDuplicatedLeafInIndex().cardinality());
            if (metaReport.getDuplicatedLeafInIndex().cardinality() > 0) {
                ihr.add("First duplicate node id in the index", metaReport.getDuplicatedLeafInIndex().nextSetBit(0L));
            }
            ihr.add("Index error count", metaReport.getErrorDocCountInIndex());
            ihr.add("Count of duplicate error docs in the index", metaReport.getDuplicatedErrorInIndex()
                    .cardinality());
            if (metaReport.getDuplicatedErrorInIndex().cardinality() > 0) {
                ihr.add("First duplicate error in the index", SolrInformationServer.PREFIX_ERROR
                        + metaReport.getDuplicatedErrorInIndex().nextSetBit(0L));
            }
            ihr.add("Index unindexed count", metaReport.getUnindexedDocCountInIndex());
            ihr.add("Count of duplicate unindexed docs in the index", metaReport.getDuplicatedUnindexedInIndex()
                    .cardinality());
            if (metaReport.getDuplicatedUnindexedInIndex().cardinality() > 0) {
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
        catch (Exception exception)
        {
            throw new AlfrescoRuntimeException("", exception);
        }
    }

    static void addSlaveCoreSummary(TrackerRegistry trackerRegistry, String cname, boolean detail, boolean hist, boolean values,
                                                 InformationServer srv, NamedList<Object> report) throws IOException
    {
        NamedList<Object> coreSummary = new SimpleOrderedMap<>();
        coreSummary.addAll((SimpleOrderedMap<Object>) srv.getCoreStats());

        SlaveCoreStatePublisher statePublisher = trackerRegistry.getTrackerForCore(cname, SlaveCoreStatePublisher.class);
        TrackerState trackerState = statePublisher.getTrackerState();
        long lastIndexTxCommitTime = trackerState.getLastIndexedTxCommitTime();

        long lastIndexedTxId = trackerState.getLastIndexedTxId();
        long lastTxCommitTimeOnServer = trackerState.getLastTxCommitTimeOnServer();
        long lastTxIdOnServer = trackerState.getLastTxIdOnServer();

        Date lastIndexTxCommitDate = new Date(lastIndexTxCommitTime);
        Date lastTxOnServerDate = new Date(lastTxCommitTimeOnServer);
        long transactionsToDo = lastTxIdOnServer - lastIndexedTxId;
        if (transactionsToDo < 0)
        {
            transactionsToDo = 0;
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

        now = new Date();
        end = new Date(now.getTime() + remainingChangeSetTimeMillis);
        Duration remainingChangeSet = new Duration(now, end);

        NamedList<Object> ftsSummary = new SimpleOrderedMap<>();
        long remainingContentTimeMillis = 0;
        srv.addFTSStatusCounts(ftsSummary);
        long cleanCount =
                ofNullable(ftsSummary.get("Node count with FTSStatus Clean"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);
        long dirtyCount =
                ofNullable(ftsSummary.get("Node count with FTSStatus Dirty"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);
        long newCount =
                ofNullable(ftsSummary.get("Node count with FTSStatus New"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);

        long nodesInIndex =
                ofNullable(coreSummary.get("Alfresco Nodes in Index"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);

        long contentYetToSee = nodesInIndex > 0 ? nodesToDo * (cleanCount + dirtyCount + newCount)/nodesInIndex  : 0;
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

        ModelTracker modelTrkr = trackerRegistry.getModelTracker();
        TrackerState modelTrkrState = modelTrkr.getTrackerState();
        coreSummary.add("ModelTracker Active", modelTrkrState.isRunning());
        coreSummary.add("NodeState Publisher Active", trackerState.isRunning());

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
        // Stats

        coreSummary.add("Model sync times (ms)", srv.getTrackerStats().getModelTimes().getNamedList(detail, hist, values));
        coreSummary.add("Docs/Tx", srv.getTrackerStats().getTxDocs().getNamedList(detail, hist, values));

        // Model

        Map<String, Set<String>> modelErrors = srv.getModelErrors();
        if (modelErrors.size() > 0)
        {
            NamedList<Object> errorList = new SimpleOrderedMap<>();
            for (Map.Entry<String, Set<String>> modelNameToErrors : modelErrors.entrySet())
            {
                errorList.add(modelNameToErrors.getKey(), modelNameToErrors.getValue());
            }
            coreSummary.add("Model changes are not compatible with the existing data model and have not been applied", errorList);
        }

        report.add(cname, coreSummary);
    }

    static void addMasterOrStandaloneCoreSummary(TrackerRegistry trackerRegistry, String cname, boolean detail, boolean hist, boolean values,
                                                 InformationServer srv, NamedList<Object> report) throws IOException
    {
        NamedList<Object> coreSummary = new SimpleOrderedMap<>();
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

        NamedList<Object> ftsSummary = new SimpleOrderedMap<>();
        long remainingContentTimeMillis = 0;
        srv.addFTSStatusCounts(ftsSummary);
        long cleanCount =
                ofNullable(ftsSummary.get("Node count with FTSStatus Clean"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);
        long dirtyCount =
                ofNullable(ftsSummary.get("Node count with FTSStatus Dirty"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);
        long newCount =
                ofNullable(ftsSummary.get("Node count with FTSStatus New"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);

        long nodesInIndex =
                ofNullable(coreSummary.get("Alfresco Nodes in Index"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);

        long contentYetToSee = nodesInIndex > 0 ? nodesToDo * (cleanCount + dirtyCount + newCount)/nodesInIndex  : 0;
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
            NamedList<Object> errorList = new SimpleOrderedMap<>();
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