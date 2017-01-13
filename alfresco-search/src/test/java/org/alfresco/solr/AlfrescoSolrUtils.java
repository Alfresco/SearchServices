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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ACLID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ACLTXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ACLTXID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ANAME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_ANCESTOR;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_APATH;
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
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_TXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_TXID;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_TYPE;
import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_VERSION;
import static org.junit.Assert.assertEquals;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoSolrTests.SolrServletRequest;
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
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.SolrTestCaseJ4.XmlDoc;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.XML;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
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
        long txnCommitTime = System.currentTimeMillis();
        Transaction transaction = new Transaction();
        transaction.setCommitTimeMs(txnCommitTime);
        transaction.setId(generateId());
        transaction.setDeletes(deletes);
        transaction.setUpdates(updates);
        return transaction;
    }

    public static Transaction getTransaction(int deletes, int updates, long id)
    {
        long txnCommitTime = System.currentTimeMillis();
        Transaction transaction = new Transaction();
        transaction.setCommitTimeMs(txnCommitTime);
        transaction.setId(id);
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
        props.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.US, 5l, "UTF-8", "text/plain", null));
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
     *
     * @param aclChangeSet
     * @return
     */
    public static Acl getAcl(AclChangeSet aclChangeSet, long aclId)
    {
        Acl acl = new Acl(aclChangeSet.getId(), aclId);
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

    public static AclChangeSet getAclChangeSet(int aclCount, long id)
    {
        AclChangeSet aclChangeSet = new AclChangeSet(id, System.currentTimeMillis(), aclCount);
        return aclChangeSet;
    }

    private static AtomicLong id = new AtomicLong(System.currentTimeMillis());
    /**
     * Creates a unique id.
     * @return Long unique id
     */
    public static synchronized Long generateId()
    {
        return id.incrementAndGet();
    }
    /**
     * Generates an &lt;add&gt;&lt;doc&gt;... XML String with options
     * on the add.
     *
     * @param doc the Document to add
     * @param args 0th and Even numbered args are param names, Odds are param values.
     * @see #add
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
    /**
     * 
     * @param aclChangeSet
     * @param aclList
     * @param aclReadersList
     */
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
    }
    /**
     * Generate a collection from input.
     * @param strings
     * @return {@link List} made from the input
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
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
    /**
     * 
     * @param params
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map map(Object... params)
    {
        LinkedHashMap ret = new LinkedHashMap();
        for (int i=0; i<params.length; i+=2)
        {
            ret.put(params[i], params[i+1]);
        }
        return ret;
    }
    /**
     * 
     * @param core
     * @param dataModel
     * @param txid
     * @param dbid
     * @param aclid
     * @param type
     * @param aspects
     * @param properties
     * @param content
     * @param owner
     * @param parentAssocs
     * @param ancestors
     * @param paths
     * @param nodeRef
     * @param commit
     * @return
     * @throws IOException
     */
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
            
            StringBuilder builder = new StringBuilder();
            int i = 0;
    		for(NodeRef ancestor : ancestors)
    		{
    			builder.append('/').append(ancestor.getId());
    			doc.addField(FIELD_APATH, "" + i++ + builder.toString());
    		}
    		if(builder.length() > 0)
    		{
    			doc.addField(FIELD_APATH, "F" + builder.toString());
    		}
    		
    		builder = new StringBuilder();
    		for(int j = 0;  j < ancestors.length; j++)
    		{
    			NodeRef element = ancestors[ancestors.length - 1 - j];
    			builder.insert(0, element.getId());
    			builder.insert(0, '/');
    			doc.addField(FIELD_ANAME, "" + j +  builder.toString());
    		}
    		if(builder.length() > 0)
    		{
    			doc.addField(FIELD_ANAME, "F" +  builder.toString());
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
      /**
       * Add an acl.
       * @param core
       * @param dataModel
       * @param acltxid
       * @param aclId
       * @param maxReader
       * @param totalReader
       * @throws IOException
       */
      public static void addAcl(SolrCore core,
                                AlfrescoSolrDataModel dataModel, 
                                int acltxid, 
                                int aclId,
                                int maxReader,
                                int totalReader) throws IOException
      {
          SolrQueryRequest solrQueryRequest = new SolrServletRequest(core, null);
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
    /**
     * Add a store to root.  
     * @param core
     * @param dataModel
     * @param rootNodeRef
     * @param txid
     * @param dbid
     * @param acltxid
     * @param aclid
     * @throws IOException
     */
    public static void addStoreRoot(SolrCore core,
                                      AlfrescoSolrDataModel dataModel,
                                      NodeRef rootNodeRef,
                                      int txid,
                                      int dbid,
                                      int acltxid,
                                      int aclid) throws IOException
      {
          SolrServletRequest solrQueryRequest = null;
          try
          {
              solrQueryRequest = new SolrServletRequest(core, null);
              AddUpdateCommand addDocCmd = new AddUpdateCommand(solrQueryRequest);
              addDocCmd.overwrite = true;
              addDocCmd.solrDoc = createDocument(dataModel, new Long(txid), new Long(dbid), rootNodeRef,
                      ContentModel.TYPE_STOREROOT, new QName[]{ContentModel.ASPECT_ROOT}, null, null, new Long(aclid),
                      new String[]{"/"}, "system", null, null);
              core.getUpdateHandler().addDoc(addDocCmd);
              addAcl(solrQueryRequest, core, dataModel, acltxid, aclid, 0, 0);
              AddUpdateCommand txCmd = new AddUpdateCommand(solrQueryRequest);
              txCmd.overwrite = true;
              SolrInputDocument input = new SolrInputDocument();
              String id = AlfrescoSolrDataModel.getTransactionDocumentId(new Long(txid));
              input.addField(FIELD_SOLR4_ID, id);
              input.addField(FIELD_VERSION, "0");
              input.addField(FIELD_TXID, txid);
              input.addField(FIELD_INTXID, txid);
              input.addField(FIELD_TXCOMMITTIME, (new Date()).getTime());
              input.addField(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_TX);
              txCmd.solrDoc = input;
              core.getUpdateHandler().addDoc(txCmd);
              core.getUpdateHandler().commit(new CommitUpdateCommand(solrQueryRequest, false));
          }
              finally
          {
              solrQueryRequest.close();
          }
    }
    public static void addAcl(SolrQueryRequest solrQueryRequest, SolrCore core, AlfrescoSolrDataModel dataModel, int acltxid, int aclId, int maxReader,
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

    /**
     * Basic wrapper class to create some simple Acl changesets
     */
    public static class TestActChanges {
        private AclChangeSet aclChangeSet;
        private Acl acl;

        public AclChangeSet getChangeSet() {
            return aclChangeSet;
        }

        public Acl getFirstAcl() {
            return acl;
        }

        public TestActChanges createBasicTestData() {
            aclChangeSet = getAclChangeSet(1);

            acl = getAcl(aclChangeSet);
            Acl acl2 = getAcl(aclChangeSet);

            AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
            AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, list("jim"), list("phil"), null);

            indexAclChangeSet(aclChangeSet,
                    list(acl, acl2),
                    list(aclReaders, aclReaders2));
            return this;
        }
    }


    /**
     * Gets a SolrCore by name without incrementing the internal counter
     * @param coreContainer
     * @param coreName
     * @return SolrCore
     */
    public static SolrCore getCore(CoreContainer coreContainer, String coreName)
    {
        return coreContainer.getCores().stream()
                            .filter(aCore ->coreName.equals(aCore.getName()))
                            .findFirst().get();
    }

    /**
     * Creates a core using the specified template
     * @param coreContainer
     * @param coreAdminHandler
     * @param coreName
     * @param templateName
     * @param shards
     * @param nodes
     * @param extraParams Any number of additional parameters in name value pairs.
     * @return
     * @throws InterruptedException
     */
    public static SolrCore createCoreUsingTemplate(CoreContainer coreContainer, AlfrescoCoreAdminHandler coreAdminHandler,
                                                   String coreName, String templateName, int shards, int nodes,
                                                   String... extraParams) throws InterruptedException {
        SolrCore testingCore = null;
        ModifiableSolrParams coreParams = params(CoreAdminParams.ACTION, "newcore",
                "storeRef", "workspace://SpacesStore",
                "coreName", coreName,
                "numShards", String.valueOf(shards),
                "nodeInstance", String.valueOf(nodes),
                "template", templateName);
        coreParams.add(params(extraParams));
        SolrQueryRequest request = new LocalSolrQueryRequest(null,coreParams);
        SolrQueryResponse response = new SolrQueryResponse();
        coreAdminHandler.handleCustomAction(request, response);
        TimeUnit.SECONDS.sleep(1);
        if(shards > 1 )
        {
            NamedList vals = response.getValues();
            List<String> coreNames = vals.getAll("core");
            assertEquals(shards,coreNames.size());
            testingCore = getCore(coreContainer, coreNames.get(0));
        }
        else
        {
            assertEquals(coreName, response.getValues().get("core"));
            //Get a reference to the new core
            testingCore = getCore(coreContainer, coreName);
        }

        TimeUnit.SECONDS.sleep(4); //Wait a little for background threads to catchup
        assertNotNull(testingCore);
        return testingCore;
    }

    /**
     * Asserts the summary report has been returned for the correct core and that the number of searchers is 1 or more.
     * @param response
     * @param coreName
     */
    public static void assertSummaryCorrect(SolrQueryResponse response, String coreName) {
        NamedList<Object> summary = (NamedList<Object>) response.getValues().get("Summary");
        assertNotNull(summary);
        NamedList<Object> coreSummary = (NamedList<Object>) summary.get(coreName);
        assertNotNull(coreSummary);
        assertTrue("There must be a searcher for "+coreName, ((Integer)coreSummary.get("Number of Searchers")) > 0);
    }


}
