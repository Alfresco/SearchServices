/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
 * Behavioural interface for denoting a given role in a replication scenario.
 * A replication interaction is composed at least by 2 nodes: a master and one or more slaves.
 *
 * Despite the same interface, the behaviour of the content store management changes depending on the node kind:
 *
 * <ul>
 *     <li>Master Node: read, write and changes tracking</li>
 *     <li>Slave Node: read only</li>
 * </ul>
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
public interface ReplicationRole extends Closeable
{
    ReplicationRole NO_OP = new ReplicationRole()
    {
        @Override
        public long getLastCommittedVersion()
        {
            return 0;
        }

        @Override
        public void setLastCommittedVersion(long version)
        {

        }

        @Override
        public Map<String, List<Map<String, Object>>> getChanges(long version)
        {
            return null;
        }

        @Override
        public void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc)
        {

        }

        @Override
        public void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc)
        {

        }

        @Override
        public void removeDocFromContentStore(NodeMetaData nodeMetaData)
        {

        }

        @Override
        public void flushChangeSet()
        {

        }

        @Override
        public ReplicationRole enableMasterMode()
        {
            return null;
        }

        @Override
        public void close()
        {

        }
    };

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
     * @param dbId the document DBID
     * @param doc the document itself.
     */
    void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc);

    /**
     * Stores a {@link SolrInputDocument} into Alfresco solr content store.
     *
     * @param nodeMetaData the node metadata.
     * @param doc the document itself.
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
     * Enables the master mode.
     *
     * @return the master {@link ReplicationRole} instance.
     */
    ReplicationRole enableMasterMode();
}