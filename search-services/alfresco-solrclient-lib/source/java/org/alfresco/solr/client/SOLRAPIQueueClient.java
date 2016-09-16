/*
 * #%L
 * Alfresco Data model classes
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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

import java.io.*;
import java.net.ConnectException;
import java.util.*;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.httpclient.Response;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONException;

// TODO error handling, including dealing with a repository that is not responsive (ConnectException in sendRemoteRequest)
// TODO get text content transform status handling
/**
 * A client that reads from an internal queue. This is used for test cases.
 */

public class SOLRAPIQueueClient extends SOLRAPIClient
{
    public static List<AclChangeSet> aclChangeSetQueue = Collections.synchronizedList(new ArrayList());
    public static Map<Long, List<Acl>> aclMap = Collections.synchronizedMap(new HashMap());
    public static Map<Long, AclReaders> aclReadersMap = Collections.synchronizedMap(new HashMap());

    public static List<Transaction> transactionQueue = Collections.synchronizedList(new ArrayList());
    public static Map<Long, List<Node>> nodeMap = Collections.synchronizedMap(new HashMap());
    public static Map<Long, NodeMetaData> nodeMetaDataMap = Collections.synchronizedMap(new HashMap());
    private static boolean throwException;

    public SOLRAPIQueueClient(NamespaceDAO namespaceDAO)
    {
        super(null,null,namespaceDAO);
    }

    public static void setThrowException(boolean _throwException) {
        throwException = _throwException;
    }

    public AclChangeSets getAclChangeSets(Long fromCommitTime, Long minAclChangeSetId, Long toCommitTime, Long maxAclChangeSetId, int maxResults)
        throws AuthenticationException, IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        int size = aclChangeSetQueue.size();
        long maxTime = 0L;
        long maxId = 0L;

        if(fromCommitTime == null && toCommitTime == null)
        {
            List<AclChangeSet> aclChangeSetList = new ArrayList();
            for(int i=0; i<size; i++)
            {
                AclChangeSet aclChangeSet = aclChangeSetQueue.get(i);
                if(aclChangeSet.getId() >= minAclChangeSetId && aclChangeSet.getId() < maxAclChangeSetId)
                {
                    aclChangeSetList.add(aclChangeSet);
                    maxTime = Math.max(aclChangeSet.getCommitTimeMs(), maxTime);
                    maxId = Math.max(aclChangeSet.getId(), maxId);
                }

                if(aclChangeSetList.size() == maxResults) {
                    break;
                }
            }

            return new AclChangeSets(aclChangeSetList, maxTime, maxId);
        }

        List<AclChangeSet> aclChangeSetList = new ArrayList();

        for(int i=0; i<size; i++)
        {
            AclChangeSet aclChangeSet = aclChangeSetQueue.get(i);

            if(aclChangeSet.getCommitTimeMs() < fromCommitTime)
            {
                //We have moved beyond this aclChangeSet
            }
            else if(aclChangeSet.getCommitTimeMs() > toCommitTime)
            {
                //We have not yet reached this alcChangeSet so break out of the loop
                break;
            }
            else
            {
                aclChangeSetList.add(aclChangeSet);
                maxTime = aclChangeSet.getCommitTimeMs();
                maxId = aclChangeSet.getId();

                if(aclChangeSetList.size() == maxResults)
                {
                    break;
                }
            }
        }

        return new AclChangeSets(aclChangeSetList, maxTime, maxId);
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
    public List<Acl> getAcls(List<AclChangeSet> aclChangeSets, Long minAclId, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        List<Acl> allAcls = new ArrayList();
        for(AclChangeSet aclChangeSet : aclChangeSets)
        {
            List aclList = aclMap.get(aclChangeSet.getId());
            allAcls.addAll(aclList);
        }
        return allAcls;
    }

    /**
     * Get the ACL readers for a given list of ACLs
     *
     * @param acls                          the ACLs
     * @return                              the readers for the ACLs
     */
    public List<AclReaders> getAclReaders(List<Acl> acls) throws AuthenticationException, IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        List<AclReaders> allAclReaders = new ArrayList();
        for(Acl acl : acls)
        {
            AclReaders aclReaders = aclReadersMap.get(acl.getId());
            allAclReaders.add(aclReaders);
        }
        return allAclReaders;
    }


    public List<AlfrescoModelDiff> getModelsDiff(List<AlfrescoModel> currentModels) throws AuthenticationException, IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }
        return new ArrayList();
    }


    public Transactions getTransactions(Long fromCommitTime, Long minTxnId, Long toCommitTime, Long maxTxnId, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }
        try
        {
            return getTransactions(fromCommitTime, minTxnId, toCommitTime, maxTxnId, maxResults, null);
        }
        catch(EncoderException e)
        {
            throw new IOException(e);
        }
    }




    public Transactions getTransactions(Long fromCommitTime, Long minTxnId, Long toCommitTime, Long maxTxnId, int maxResults, ShardState shardState) throws AuthenticationException, IOException, JSONException, EncoderException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        int size = transactionQueue.size();

        long maxTime = 0L;
        long maxId = 0L;

        if(fromCommitTime == null && toCommitTime == null)
        {
            List<Transaction> transactionList = new ArrayList();

            for(int i=0; i<size; i++)
            {
                Transaction txn = transactionQueue.get(i);
                if(txn.getId() >= minTxnId && txn.getId() < maxTxnId)
                {
                    transactionList.add(txn);
                    maxTime = Math.max(txn.getCommitTimeMs(), maxTime);
                    maxId = Math.max(txn.getId(), maxId);
                }

                if(transactionList.size() == maxResults) {
                    break;
                }
            }

            return new Transactions(transactionList, maxTime, maxId);
        }

        List<Transaction> transactionList = new ArrayList();

        for(int i=0; i<size; i++)
        {
            Transaction txn = transactionQueue.get(i);
            if(txn.getCommitTimeMs() < fromCommitTime)
            {
                //We have moved beyond this transaction.
            }
            else if(txn.getCommitTimeMs() > toCommitTime)
            {
                //We have not yet reached this transaction so break out of the loop
                break;
            }
            else
            {
                //We have a transaction to work with
                transactionList.add(txn);
                maxTime = txn.getCommitTimeMs();
                maxId = txn.getId();

                if(transactionList.size() == maxResults)
                {
                    break;
                }
            }
        }

        return new Transactions(transactionList, maxTime, maxId);
    }

    public List<Node> getNodes(GetNodesParameters parameters, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        List<Long> txnIds = parameters.getTransactionIds();
        List<Node> allNodes = new ArrayList();
        for(long txnId : txnIds)
        {
            List<Node> nodes = nodeMap.get(txnId);
            allNodes.addAll(nodes);
        }

        return allNodes;
    }

    public List<NodeMetaData> getNodesMetaData(NodeMetaDataParameters params, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }

        List<NodeMetaData> nodeMetaDatas = new ArrayList();
        List<Long> nodeIds = params.getNodeIds();
        if(nodeIds != null) {
            for (long nodeId : nodeIds) {
                NodeMetaData nodeMetaData = nodeMetaDataMap.get(nodeId);
                nodeMetaDatas.add(nodeMetaData);
            }
        } else {
            Long fromId = params.getFromNodeId();
            NodeMetaData nodeMetaData = nodeMetaDataMap.get(fromId);
            nodeMetaDatas.add(nodeMetaData);
        }

        return nodeMetaDatas;
    }

    public GetTextContentResponse getTextContent(Long nodeId, QName propertyQName, Long modifiedSince) throws AuthenticationException, IOException
    {
        if(throwException) {
            throw new ConnectException("THROWING EXCEPTION, better be ready!");
        }
        //Just put the nodeId innto the content so we query for this in tests.
        return new GetTextContentResponse(new DummyResponse("Hello world "+nodeId));
    }

    private class DummyResponse implements Response
    {
        private String text;

        public DummyResponse(String text)
        {
            this.text = text;
        }

        public InputStream getContentAsStream()
        {
            return new ByteArrayInputStream(text.getBytes());
        }

        public int getStatus() {
            return HttpStatus.SC_OK;
        }

        public void release()
        {

        }

        public String getHeader(String key) {
            return null;
        }

        public String getContentType() {
            return "text/html";
        }
    }

    public void close()
    {

    }
}
