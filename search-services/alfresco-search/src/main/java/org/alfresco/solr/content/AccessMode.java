/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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
package org.alfresco.solr.content;

import org.alfresco.solr.client.NodeMetaData;
import org.apache.solr.common.SolrInputDocument;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Behavioural interface which indicates the type of content store access mode for the owning node.
 * A replication interaction is composed at least by 2 roles: a master and a slave.
 *
 * Despite the same interface, the behaviour of the content store management changes depending on the node kind:
 *
 * <ul>
 *     <li>Master Node: READ + WRITE + Changes tracking. Writes and changes tracking are a direct consequence of the "indexing" nature of the master node.</li>
 *     <li>Slave Node: READ ONLY (i.e. never write: changes are applied on the master and replicated on slaves)</li>
 * </ul>
 *
 * Important: the owning entity *is not* the SolrCore instance: the Content store is shared across all cores of
 * A node can transit from Read to Write mode
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
interface AccessMode extends Closeable
{
    /**
     * Returns the last persisted content store version.
     *
     * @return the last persisted content store version, SolrContentStore#NO_VERSION_AVAILABLE in case the version isn't available.
     */
    long getLastCommittedVersion();

    /**
     * Persists the last committed version on the hosting node.
     * Note that this is tipically valid only on slave node, because the master already manages the content store version on
     * a persistent storage, so it doesn't need to call this method.
     *
     * @param version the last committed content store version.
     */
    void setLastCommittedVersion(long version);

    Map<String, List<Map<String, Object>>> getChanges(long version);

    /**
     * Stores a {@link SolrInputDocument} into Alfresco solr content store.
     *
     * @param tenant the owning tenant.
     * @param dbId   the document DBID
     * @param doc    the document itself.
     */
    void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc);

    /**
     * Stores a {@link SolrInputDocument} into Alfresco solr content store.
     *
     * @param nodeMetaData the node metadata.
     * @param doc          the document itself.
     */
    void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc);

    /**
     * Removes a node from the content store.
     *
     * @param nodeMetaData the node metadata.
     */
    void removeDocFromContentStore(NodeMetaData nodeMetaData);

    /**
     * Flushes pending changesets.
     *
     * @throws IOException in case of I/O failure.
     */
    void flushChangeSet() throws IOException;

    /**
     * Tries to transition from this mode to the readOnlyMode.
     */
    void switchOnReadOnlyMode();

    /**
     * Tries to transition from this mode to the read/write mode.
     */
    void switchOnReadWriteMode();
}