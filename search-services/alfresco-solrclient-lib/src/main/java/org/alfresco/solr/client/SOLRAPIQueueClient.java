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

package org.alfresco.solr.client;

import static java.util.Optional.ofNullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.alfresco.httpclient.Response;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.service.namespace.QName;
import org.apache.http.HttpStatus;
import org.json.JSONException;

// TODO error handling, including dealing with a repository that is not responsive (ConnectException in sendRemoteRequest)
// TODO get text content transform status handling
/**
 * A client that reads from an internal queue. This is used for test cases.
 */
public class SOLRAPIQueueClient extends SOLRAPIClient
{
    public final static List<AclChangeSet> ACL_CHANGE_SET_QUEUE = Collections.synchronizedList(new ArrayList<>());
    public final static Map<Long, List<Acl>> ACL_MAP = Collections.synchronizedMap(new HashMap<>());
    public final static Map<Long, AclReaders> ACL_READERS_MAP = Collections.synchronizedMap(new HashMap<>());

    public final static List<Transaction> TRANSACTION_QUEUE = Collections.synchronizedList(new ArrayList<>());
    public final static Map<Long, List<Node>> NODE_MAP = Collections.synchronizedMap(new HashMap<>());
    public final static Map<Long, NodeMetaData> NODE_META_DATA_MAP = Collections.synchronizedMap(new HashMap<>());
    public final static Map<Long, String> NODE_CONTENT_MAP =  Collections.synchronizedMap(new HashMap<>());

    private static boolean throwException;

    public SOLRAPIQueueClient(NamespaceDAO namespaceDAO)
    {
        super(null,null,namespaceDAO);
    }

    public static void setThrowException(boolean _throwException)
    {
        throwException = _throwException;
    }

    @Override
    public AclChangeSets getAclChangeSets(Long fromCommitTime, Long minAclChangeSetId, Long toCommitTime, Long maxAclChangeSetId, int maxResults) throws IOException, JSONException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        final AtomicLong maxTime = new AtomicLong();
        final AtomicLong maxId = new AtomicLong();

        if (fromCommitTime == null && toCommitTime == null)
        {
            return new AclChangeSets(
                    ACL_CHANGE_SET_QUEUE.stream()
                        .filter(aclChangeSet -> aclChangeSet.getId() >= minAclChangeSetId && aclChangeSet.getId() < maxAclChangeSetId)
                        .limit(maxResults)
                        .peek(aclChangeSet -> {
                            maxTime.set(Math.max(aclChangeSet.getCommitTimeMs(), maxTime.get()));
                            maxId.set(Math.max(aclChangeSet.getId(), maxId.get()));})
                        .collect(Collectors.toList()), maxTime.get(), maxId.get());
        }

        return new AclChangeSets(
                ACL_CHANGE_SET_QUEUE.stream()
                        .filter(aclChangeSet -> (fromCommitTime != null && aclChangeSet.getCommitTimeMs() >= fromCommitTime) && (toCommitTime != null && aclChangeSet.getCommitTimeMs() <= toCommitTime))
                        .limit(maxResults)
                        .peek(aclChangeSet -> {
                            maxTime.set(Math.max(aclChangeSet.getCommitTimeMs(), maxTime.get()));
                            maxId.set(Math.max(aclChangeSet.getId(), maxId.get()));})
                        .collect(Collectors.toList()), maxTime.get(), maxId.get());
    }

    /**
     * Get the ACLs associated with a given list of ACL ChangeSets.  The ACLs may be truncated for
     * the last ACL ChangeSet in the return values - the ACL count from the
     * {@link #getAclChangeSets(Long, Long, Long, Long, int) ACL ChangeSets}.
     *
     * @param aclChangeSets                 the ACL ChangeSets to include
     * @param minAclId                      the lowest ACL ID (may be <tt>null</tt>)
     * @param maxResults                    the maximum number of results to retrieve
     * @return                              the ACLs (includes ChangeSet ID)
     */
    public List<Acl> getAcls(List<AclChangeSet> aclChangeSets, Long minAclId, int maxResults) throws IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        return aclChangeSets.stream()
                    .map(AclChangeSet::getId)
                    .map(ACL_MAP::get)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
    }

    /**
     * Get the ACL readers for a given list of ACLs
     *
     * @param acls the ACLs
     * @return the readers for the ACLs
     */
    public List<AclReaders> getAclReaders(List<Acl> acls) throws IOException, JSONException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        return acls.stream()
                .map(Acl::getId)
                .map(ACL_READERS_MAP::get)
                .collect(Collectors.toList());
    }


    public List<AlfrescoModelDiff> getModelsDiff(String coreName, List<AlfrescoModel> currentModels) throws IOException, JSONException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }
        return Collections.emptyList();
    }


    public Transactions getTransactions(Long fromCommitTime, Long minTxnId, Long toCommitTime, Long maxTxnId, int maxResults) throws IOException, JSONException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        return getTransactions(fromCommitTime, minTxnId, toCommitTime, maxTxnId, maxResults, null);
    }

    public Transactions getTransactions(Long fromCommitTime, Long minTxnId, Long toCommitTime, Long maxTxnId, int maxResults, ShardState shardState) throws IOException, JSONException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        final AtomicLong maxTime = new AtomicLong();
        final AtomicLong maxId = new AtomicLong();

        if (fromCommitTime == null && toCommitTime == null)
        {
            return new Transactions(
                    TRANSACTION_QUEUE.stream()
                            .filter(txn -> txn.getId() >= minTxnId && txn.getId() < maxTxnId)
                            .limit(maxResults)
                            .peek(txn -> {
                                maxTime.set(Math.max(txn.getCommitTimeMs(), maxTime.get()));
                                maxId.set(Math.max(txn.getId(), maxId.get()));})
                            .collect(Collectors.toList()), maxTime.get(), maxId.get());
        }

        return new Transactions(
                TRANSACTION_QUEUE.stream()
                        .filter(txn -> (fromCommitTime != null && txn.getCommitTimeMs() >= fromCommitTime) && (toCommitTime != null && txn.getCommitTimeMs() <= toCommitTime))
                        .limit(maxResults)
                        .peek(txn -> {
                            maxTime.set(Math.max(txn.getCommitTimeMs(), maxTime.get()));
                            maxId.set(Math.max(txn.getId(), maxId.get()));})
                        .collect(Collectors.toList()), maxTime.get(), maxId.get());
    }

    public List<Node> getNodes(GetNodesParameters parameters, int maxResults) throws IOException, JSONException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        return parameters.getTransactionIds().stream()
                    .map(NODE_MAP::get)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
    }

    @Override
    public List<NodeMetaData> getNodesMetaData(NodeMetaDataParameters params) throws IOException, JSONException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        return ofNullable(params.getNodeIds())
                .map(identifiers ->
                        identifiers.stream()
                            .map(NODE_META_DATA_MAP::get)
                            .map(metadata -> getOnlyRequestedMetadata(metadata, params))
                            .collect(Collectors.toList()))
                .orElseGet(() ->
                        ofNullable(params.getFromNodeId())
                                .map(NODE_META_DATA_MAP::get)
                                .map(metadata -> getOnlyRequestedMetadata(metadata, params))
                                .map(Collections::singletonList)
                                .orElseGet(Collections::emptyList));
    }

    /**
     * This method is meant to use the input node metadata parameters to return only the appropriate metadata from the input node.
     * So if a metadata is not requested in the parameters, it will be removed from the input node metadata object.
     * This allow a behaviour of the test SOLRAPI closer to the real APIs where a metadata is returned only when asked.
     * 
     * @param nodeMetaData - node metadata to process
     * @param params - parameters that regulate the return of such metadata
     */
    private NodeMetaData getOnlyRequestedMetadata(NodeMetaData nodeMetaData, NodeMetaDataParameters params)
    {
        NodeMetaData paramFiltered = new NodeMetaData();
        paramFiltered.setId(nodeMetaData.getId());

        if (params.isIncludeType())
        {
            paramFiltered.setType(nodeMetaData.getType());
        }

        if (params.isIncludeAclId())
        {
            paramFiltered.setAclId(nodeMetaData.getAclId());
        }

        if (params.isIncludeAspects())
        {
            paramFiltered.setAspects(nodeMetaData.getAspects());
        }

        if (params.isIncludeProperties())
        {
            paramFiltered.setProperties(nodeMetaData.getProperties());
        }

        if (params.isIncludeChildAssociations())
        {
            paramFiltered.setChildAssocs(nodeMetaData.getChildAssocs());
        }

        if (params.isIncludeParentAssociations())
        {
            paramFiltered.setParentAssocs(nodeMetaData.getParentAssocs());
            paramFiltered.setParentAssocsCrc(nodeMetaData.getParentAssocsCrc());
        }

        if (params.isIncludeChildIds())
        {
            paramFiltered.setChildIds(nodeMetaData.getChildIds());
        }

        if (params.isIncludePaths())
        {
            paramFiltered.setPaths(nodeMetaData.getPaths());
            paramFiltered.setNamePaths(nodeMetaData.getNamePaths());
        }

        if (params.isIncludeOwner())
        {
            paramFiltered.setOwner(nodeMetaData.getOwner());
        }

        if (params.isIncludeNodeRef())
        {
            paramFiltered.setNodeRef(nodeMetaData.getNodeRef());
        }

        if (params.isIncludeTxnId())
        {
            paramFiltered.setTxnId(nodeMetaData.getTxnId());
        }
        
        /* Default metadata? they are not included in the parameters*/
        paramFiltered.setAncestors(nodeMetaData.getAncestors());
        paramFiltered.setAncestorPaths(nodeMetaData.getAncestorPaths());
        paramFiltered.setTenantDomain(nodeMetaData.getTenantDomain());
        
        return paramFiltered;
    }

    public GetTextContentResponse getTextContent(Long nodeId, QName propertyQName, Long modifiedSince) throws IOException
    {
        if(throwException)
        {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        if(NODE_CONTENT_MAP.containsKey(nodeId))
        {
            return new GetTextContentResponse(new DummyResponse(NODE_CONTENT_MAP.get(nodeId)));
        }

        return new GetTextContentResponse(new DummyResponse("Hello world " + nodeId));
    }

    private static class DummyResponse implements Response
    {
        private final String text;

        public DummyResponse(String text)
        {
            this.text = text;
        }

        @Override
        public InputStream getContentAsStream()
        {
            return new ByteArrayInputStream(text.getBytes());
        }

        @Override
        public int getStatus()
        {
            return HttpStatus.SC_OK;
        }

        @Override
        public void release()
        {

        }

        @Override
        public String getHeader(String key)
        {
            return null;
        }

        @Override
        public String getContentType()
        {
            return "text/html";
        }
    }

    @Override
    public void close()
    {
        // Nothing to be done here
    }
}
