/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ACLID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ACLTXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ACLTXID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ANCESTOR;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ASPECT;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ASSOCTYPEQNAME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DBID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DENIED;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_INACLTXID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_INTXID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ISNODE;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_LID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_OWNER;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_PARENT;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_PARENT_ASSOC_CRC;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_PATH;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_PRIMARYASSOCQNAME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_PRIMARYASSOCTYPEQNAME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_PRIMARYPARENT;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_QNAME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_READER;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_SOLR4_ID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_TENANT;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_TYPE;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_VERSION;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrTestCaseJ4.SolrServletRequest;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.SOLRAPIQueueClient;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.alfresco.util.ISO9075;
import org.alfresco.util.Pair;
import org.apache.solr.SolrTestCaseJ4.XmlDoc;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.XML;
import org.apache.solr.core.SolrCore;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
/**
 * Alfresco Solr Utility class which provide helper methods.
 * @author Michael Suzuki
 *
 */
public class AlfrescoSolrUtils
{
    public static final String TEST_NAMESPACE = "http://www.alfresco.org/test/solrtest";
    /**
     * Get transaction.
     * @param deletes
     * @param updates
     * @return {@link Transaction}
     */
    public static Transaction getTransaction(int deletes, int updates)
    {
        long txnCommitTime = generateId();
        Transaction transaction = new Transaction();
        transaction.setCommitTimeMs(txnCommitTime);
        transaction.setId(generateId());
        transaction.setDeletes(deletes);
        transaction.setUpdates(updates);
        return transaction;
    }
    /**
     * Get a node.
     * @param txn
     * @param acl
     * @param status
     * @return {@link Node}
     */
    public static Node getNode(Transaction txn, Acl acl, Node.SolrApiNodeStatus status)
    {
        Node node = new Node();
        node.setTxnId(txn.getId());
        node.setId(generateId());
        node.setAclId(acl.getId());
        node.setStatus(status);
        return node;
    }
    /**
     * Get a nodes meta data.
     * @param node
     * @param txn
     * @param acl
     * @param owner
     * @param ancestors
     * @param createError
     * @return {@link NodeMetaData}
     */
    public static NodeMetaData getNodeMetaData(Node node, Transaction txn, Acl acl, String owner, Set<NodeRef> ancestors, boolean createError)
    {
        NodeMetaData nodeMetaData = new NodeMetaData();
        nodeMetaData.setId(node.getId());
        nodeMetaData.setAclId(acl.getId());
        nodeMetaData.setTxnId(txn.getId());
        nodeMetaData.setOwner(owner);
        nodeMetaData.setAspects(new HashSet<QName>());
        nodeMetaData.setAncestors(ancestors);
        Map<QName, PropertyValue> props = new HashMap<QName, PropertyValue>();
        props.put(ContentModel.PROP_IS_INDEXED, new StringPropertyValue("true"));
        props.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.US, 0l, "UTF-8", "text/plain", null));
        nodeMetaData.setProperties(props);
        //If create createError is true then we leave out the nodeRef which will cause an error
        if(!createError) {
            NodeRef nodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
            nodeMetaData.setNodeRef(nodeRef);
        }
        nodeMetaData.setType(QName.createQName(TEST_NAMESPACE, "testSuperType"));
        nodeMetaData.setAncestors(ancestors);
        nodeMetaData.setPaths(new ArrayList<Pair<String, QName>>());
        nodeMetaData.setNamePaths(new ArrayList<List<String>>());
        return nodeMetaData;
    }
    /**
     * Create GUID
     * @return String guid
     */
    public static String createGUID()
    {
        long id = generateId();
        return "00000000-0000-" + ((id / 1000000000000L) % 10000L) + "-" + ((id / 100000000L) % 10000L) + "-"
                + (id % 100000000L);
    }
    /**
     * Creates a set of NodeRef from input
     * @param refs
     * @return
     */
    public static Set<NodeRef> ancestors(NodeRef... refs) 
    {
        Set<NodeRef> set = new HashSet<NodeRef>();
        for(NodeRef ref : refs) {
            set.add(ref);
        }
        return set;
    }
    /**
     * 
     * @param transaction
     * @param nodes
     * @param nodeMetaDatas
     */
    public void indexTransaction(Transaction transaction, List<Node> nodes, List<NodeMetaData> nodeMetaDatas)
    {
        //First map the nodes to a transaction.
        SOLRAPIQueueClient.nodeMap.put(transaction.getId(), nodes);

        //Next map a node to the NodeMetaData
        for(NodeMetaData nodeMetaData : nodeMetaDatas)
        {
            SOLRAPIQueueClient.nodeMetaDataMap.put(nodeMetaData.getId(), nodeMetaData);
        }

        //Next add the transaction to the queue
        SOLRAPIQueueClient.transactionQueue.add(transaction);
    }
    /**
     * 
     * @param aclChangeSet
     * @return
     */
    public static Acl getAcl(AclChangeSet aclChangeSet)
    {
        Acl acl = new Acl(aclChangeSet.getId(), generateId());
        return acl;
    }
    /**
     * Get an AclChangeSet
     * @param aclCount
     * @return {@link AclChangeSet}
     */
    public static AclChangeSet getAclChangeSet(int aclCount)
    {
        AclChangeSet aclChangeSet = new AclChangeSet(generateId(), System.currentTimeMillis(), aclCount);
        return aclChangeSet;
    }
    private static long id;
    /**
     * Creates a unique id.
     * @return Long unique id
     */
    private static synchronized Long generateId()
    {
        long newid = System.currentTimeMillis();
        if(newid != id)
        {
            id = newid;
            return id;
        }
        return generateId();
    }
    /**
     * Generates an &lt;add&gt;&lt;doc&gt;... XML String with options
     * on the add.
     *
     * @param doc the Document to add
     * @param args 0th and Even numbered args are param names, Odds are param values.
     * @see #add
     * @see #doc
     */
    public static String add(XmlDoc doc, String... args)
    {
        try {
        StringWriter r = new StringWriter();
        // this is annoying
        if (null == args || 0 == args.length)
        {
            r.write("<add>");
            r.write(doc.xml);
            r.write("</add>");
        } 
        else
        {
            XML.writeUnescapedXML(r, "add", doc.xml, (Object[])args);
        }
            return r.getBuffer().toString();
        } 
        catch (IOException e) 
        {
            throw new RuntimeException("this should never happen with a StringWriter", e);
        }
    }
    /**
     * Get an AclReader.
     * @param aclChangeSet
     * @param acl
     * @param readers
     * @param denied
     * @param tenant
     * @return
     */
    public static AclReaders getAclReaders(AclChangeSet aclChangeSet, Acl acl, List<String> readers, List<String> denied, String tenant)
    {
        if(tenant == null)
        {
            tenant = TenantService.DEFAULT_DOMAIN;
        }
        return new AclReaders(acl.getId(), readers, denied, aclChangeSet.getId(), tenant);
    }

    public static void indexAclChangeSet(AclChangeSet aclChangeSet, List<Acl> aclList, List<AclReaders> aclReadersList)
    {
        //First map the nodes to a transaction.
        SOLRAPIQueueClient.aclMap.put(aclChangeSet.getId(), aclList);

        //Next map a node to the NodeMetaData
        for(AclReaders aclReaders : aclReadersList)
        {
            SOLRAPIQueueClient.aclReadersMap.put(aclReaders.getId(), aclReaders);
        }

        //Next add the transaction to the queue

        SOLRAPIQueueClient.aclChangeSetQueue.add(aclChangeSet);
        System.out.println("SOLRAPIQueueClient.aclChangeSetQueue.size():" + SOLRAPIQueueClient.aclChangeSetQueue.size());
    }
    /**
     * Generate a collection from input.
     * @param strings
     * @return {@link List} made from the input
     */
    public static List list(Object... strings)
    {
        List list = new ArrayList();
        for(Object s : strings)
        {
            list.add(s);
        }
        return list;
    }
    /**
     * 
     * @param params
     * @return
     */
    public static ModifiableSolrParams params(String... params)
    {
        ModifiableSolrParams msp = new ModifiableSolrParams();
        for (int i=0; i<params.length; i+=2) {
          msp.add(params[i], params[i+1]);
        }
        return msp;
      }
    
    public static Map map(Object... params)
    {
        LinkedHashMap ret = new LinkedHashMap();
        for (int i=0; i<params.length; i+=2)
        {
            Object o = ret.put(params[i], params[i+1]);
        }
        return ret;
    }
      
    public static NodeRef addNode(SolrCore core, 
                                  AlfrescoSolrDataModel dataModel,
                                  int txid,
                                  int dbid,
                                  int aclid,
                                  QName type,
                                  QName[] aspects,
                                  Map<QName, PropertyValue> properties,
                                  Map<QName, String> content, 
                                  String owner,
                                  ChildAssociationRef[] parentAssocs, 
                                  NodeRef[] ancestors,
                                  String[] paths,
                                  NodeRef nodeRef,
                                  boolean commit) throws IOException
    {
        SolrServletRequest solrQueryRequest = null;
        try
        {
            solrQueryRequest = new SolrServletRequest(core, null);
            AddUpdateCommand addDocCmd = new AddUpdateCommand(solrQueryRequest);
            addDocCmd.overwrite = true;
            addDocCmd.solrDoc = createDocument(dataModel, new Long(txid), new Long(dbid), nodeRef, type, aspects,
                  properties, content, new Long(aclid), paths, owner, parentAssocs, ancestors);
            core.getUpdateHandler().addDoc(addDocCmd);
            if (commit)
            {
                core.getUpdateHandler().commit(new CommitUpdateCommand(solrQueryRequest, false));
            }
        }
        finally
        {
            solrQueryRequest.close();
        }
            return nodeRef;
        }
    /**
     * 
     * @param dataModel
     * @param txid
     * @param dbid
     * @param nodeRef
     * @param type
     * @param aspects
     * @param properties
     * @param content
     * @param aclId
     * @param paths
     * @param owner
     * @param parentAssocs
     * @param ancestors
     * @return
     * @throws IOException
     */
    public static SolrInputDocument createDocument(AlfrescoSolrDataModel dataModel,
                                                   Long txid,
                                                   Long dbid,
                                                   NodeRef nodeRef,
                                                   QName type,
                                                   QName[] aspects,
                                                   Map<QName, PropertyValue> properties,
                                                   Map<QName, String> content,
                                                   Long aclId, 
                                                   String[] paths,
                                                   String owner, 
                                                   ChildAssociationRef[] parentAssocs,
                                                   NodeRef[] ancestors)throws IOException
    {
        SolrInputDocument doc = new SolrInputDocument();
        String id = AlfrescoSolrDataModel.getNodeDocumentId(AlfrescoSolrDataModel.DEFAULT_TENANT, aclId, dbid);
        doc.addField(FIELD_SOLR4_ID, id);
        doc.addField(FIELD_VERSION, 0);
        doc.addField(FIELD_DBID, "" + dbid);
        doc.addField(FIELD_LID, nodeRef);
        doc.addField(FIELD_INTXID, "" + txid);
        doc.addField(FIELD_ACLID, "" + aclId);
        doc.addField(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_NODE);
        if (paths != null)
        {
            for (String path : paths)
            {
                doc.addField(FIELD_PATH, path);
            }
        }
        if (owner != null)
        {
            doc.addField(FIELD_OWNER, owner);
        }
        doc.addField(FIELD_PARENT_ASSOC_CRC, "0");
        StringBuilder qNameBuffer = new StringBuilder(64);
        StringBuilder assocTypeQNameBuffer = new StringBuilder(64);
        if (parentAssocs != null)
        {
            for (ChildAssociationRef childAssocRef : parentAssocs)
            {
                if (qNameBuffer.length() > 0)
                {
                    qNameBuffer.append(";/");
                    assocTypeQNameBuffer.append(";/");
                }
                qNameBuffer.append(ISO9075.getXPathName(childAssocRef.getQName()));
                assocTypeQNameBuffer.append(ISO9075.getXPathName(childAssocRef.getTypeQName()));
                doc.addField(FIELD_PARENT, childAssocRef.getParentRef());
                
                if (childAssocRef.isPrimary())
                {
                    doc.addField(FIELD_PRIMARYPARENT, childAssocRef.getParentRef());
                    doc.addField(FIELD_PRIMARYASSOCTYPEQNAME,
                            ISO9075.getXPathName(childAssocRef.getTypeQName()));
                    doc.addField(FIELD_PRIMARYASSOCQNAME, ISO9075.getXPathName(childAssocRef.getQName()));
                }
            }
            doc.addField(FIELD_ASSOCTYPEQNAME, assocTypeQNameBuffer.toString());
            doc.addField(FIELD_QNAME, qNameBuffer.toString());
        }
        
        if (ancestors != null)
        {
            for (NodeRef ancestor : ancestors)
            {
                doc.addField(FIELD_ANCESTOR, ancestor.toString());
            }
        }
        if (properties != null)
        {
            final boolean isContentIndexedForNode = true;
            final SolrInputDocument cachedDoc = null;
            final boolean transformContentFlag = true;
            SolrInformationServer.addPropertiesToDoc(properties, isContentIndexedForNode, doc, cachedDoc, transformContentFlag);
            addContentToDoc(doc, content);
        }
        
        doc.addField(FIELD_TYPE, type);
        if (aspects != null)
        {
            for (QName aspect : aspects)
            {
                doc.addField(FIELD_ASPECT, aspect);
            }
        }
        doc.addField(FIELD_ISNODE, "T");
        doc.addField(FIELD_TENANT, AlfrescoSolrDataModel.DEFAULT_TENANT);
        
        return doc;
    }
    private static void addContentToDoc(SolrInputDocument cachedDoc, Map<QName, String> content)
    {
        Collection<String> fieldNames = cachedDoc.deepCopy().getFieldNames();
        for (String fieldName : fieldNames)
        {
            if (fieldName.startsWith(AlfrescoSolrDataModel.CONTENT_S_LOCALE_PREFIX))
              {
                  String locale = String.valueOf(cachedDoc.getFieldValue(fieldName));
                  String qNamePart = fieldName.substring(AlfrescoSolrDataModel.CONTENT_S_LOCALE_PREFIX.length());
                  QName propertyQName = QName.createQName(qNamePart);
                  addContentPropertyToDoc(cachedDoc, propertyQName, locale, content);
              }
              // Could update multi content but it is broken ....
          }
      }
      private static void addContentPropertyToDoc(SolrInputDocument cachedDoc,
              QName propertyQName,
              String locale,
              Map<QName, String> content)
      {
          StringBuilder builder = new StringBuilder();
          builder.append("\u0000").append(locale).append("\u0000");
          builder.append(content.get(propertyQName));

          for (AlfrescoSolrDataModel.FieldInstance field : AlfrescoSolrDataModel.getInstance().getIndexedFieldNamesForProperty(propertyQName).getFields())
          {
              cachedDoc.removeField(field.getField());
              if(field.isLocalised())
              {
                  cachedDoc.addField(field.getField(), builder.toString());
              }
              else
              {
                  cachedDoc.addField(field.getField(), content.get(propertyQName));
              }
          }
      }
      public static void addAcl(SolrServletRequest solrQueryRequest,
                                SolrCore core,
                                AlfrescoSolrDataModel dataModel, 
                                int acltxid, 
                                int aclId,
                                int maxReader,
                                int totalReader) throws IOException
      {
          AddUpdateCommand aclTxCmd = new AddUpdateCommand(solrQueryRequest);
          aclTxCmd.overwrite = true;
          SolrInputDocument aclTxSol = new SolrInputDocument();
          String aclTxId = AlfrescoSolrDataModel.getAclChangeSetDocumentId(new Long(acltxid));
          aclTxSol.addField(FIELD_SOLR4_ID, aclTxId);
          aclTxSol.addField(FIELD_VERSION, "0");
          aclTxSol.addField(FIELD_ACLTXID, acltxid);
          aclTxSol.addField(FIELD_INACLTXID, acltxid);
          aclTxSol.addField(FIELD_ACLTXCOMMITTIME, (new Date()).getTime());
          aclTxSol.addField(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL_TX);
          aclTxCmd.solrDoc = aclTxSol;
          core.getUpdateHandler().addDoc(aclTxCmd);
          AddUpdateCommand aclCmd = new AddUpdateCommand(solrQueryRequest);
          aclCmd.overwrite = true;
          SolrInputDocument aclSol = new SolrInputDocument();
          String aclDocId = AlfrescoSolrDataModel.getAclDocumentId(AlfrescoSolrDataModel.DEFAULT_TENANT, new Long(aclId));
          aclSol.addField(FIELD_SOLR4_ID, aclDocId);
          aclSol.addField(FIELD_VERSION, "0");
          aclSol.addField(FIELD_ACLID, aclId);
          aclSol.addField(FIELD_INACLTXID, "" + acltxid);
          aclSol.addField(FIELD_READER, "GROUP_EVERYONE");
          aclSol.addField(FIELD_READER, "pig");
          for (int i = 0; i <= maxReader; i++)
          {
              aclSol.addField(FIELD_READER, "READER-" + (totalReader - i));
          }
          aclSol.addField(FIELD_DENIED, "something");
          aclSol.addField(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL);
          aclCmd.solrDoc = aclSol;
          core.getUpdateHandler().addDoc(aclCmd);
      }
      
}
