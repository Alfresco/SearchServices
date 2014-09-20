/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
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
package org.alfresco.solr.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AlfrescoHttpClient;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.httpclient.GetRequest;
import org.alfresco.httpclient.PostRequest;
import org.alfresco.httpclient.Response;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.Path.AttributeElement;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.cmr.repository.datatype.TypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConverter.Converter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.Pair;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.util.DateUtil;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO error handling, including dealing with a repository that is not responsive (ConnectException in sendRemoteRequest)
// TODO get text content transform status handling
/**
 * Http client to handle SOLR-Alfresco remote calls.
 * 
 * @since 4.0
 */
public class SOLRAPIClient
{
    protected final static Logger log = LoggerFactory.getLogger(SOLRAPIClient.class);
    private static final String GET_ACL_CHANGESETS_URL = "api/solr/aclchangesets";
    private static final String GET_ACLS = "api/solr/acls";
    private static final String GET_ACLS_READERS = "api/solr/aclsReaders";
    private static final String GET_TRANSACTIONS_URL = "api/solr/transactions";
    private static final String GET_METADATA_URL = "api/solr/metadata";
    private static final String GET_NODES_URL = "api/solr/nodes";
    private static final String GET_CONTENT = "api/solr/textContent";
    private static final String GET_MODEL = "api/solr/model";
    private static final String GET_MODELS_DIFF = "api/solr/modelsdiff";

    private static final String CHECKSUM_HEADER = "XAlfresco-modelChecksum";

    private AlfrescoHttpClient repositoryHttpClient;
    private SOLRDeserializer deserializer;
    private DictionaryService dictionaryService;
    private JsonFactory jsonFactory;
    private NamespaceDAO namespaceDAO;

    public SOLRAPIClient(AlfrescoHttpClient repositoryHttpClient,
            DictionaryService dictionaryService,
            NamespaceDAO namespaceDAO)
    {
    	this.repositoryHttpClient = repositoryHttpClient;
        this.dictionaryService = dictionaryService;
        this.namespaceDAO = namespaceDAO;
        this.deserializer = new SOLRDeserializer(namespaceDAO);
        this.jsonFactory = new JsonFactory();
    }
    
    /**
     * Get the ACL ChangeSets
     * 
     * @param fromCommitTime                the lowest commit time (optional)
     * @param minAclChangeSetId             the lowest ChangeSet ID (optional)
     * @param maxResults                    the maximum number of results (a reasonable value only)
     * @return                              the ACL ChangeSets in order of commit time and ID
     */
    public AclChangeSets getAclChangeSets(Long fromCommitTime, Long minAclChangeSetId, Long toCommitTime, Long maxAclChangeSetId, int maxResults)
             throws AuthenticationException, IOException, JSONException
    {
        StringBuilder url = new StringBuilder(GET_ACL_CHANGESETS_URL);
        StringBuilder args = new StringBuilder();
        if (fromCommitTime != null)
        {
            args.append("?").append("fromTime").append("=").append(fromCommitTime);            
        }
        if (minAclChangeSetId != null)
        {
            args.append(args.length() == 0 ? "?" : "&").append("fromId").append("=").append(minAclChangeSetId);            
        }
        if (toCommitTime != null)
        {
            args.append(args.length() == 0 ? "?" : "&").append("toTime").append("=").append(toCommitTime);            
        }
        if (maxAclChangeSetId != null)
        {
            args.append(args.length() == 0 ? "?" : "&").append("toId").append("=").append(maxAclChangeSetId);            
        }
        if (maxResults != 0 && maxResults != Integer.MAX_VALUE)
        {
            args.append(args.length() == 0 ? "?" : "&").append("maxResults").append("=").append(maxResults);
        }
        url.append(args);
        
        GetRequest req = new GetRequest(url.toString());
        Response response = null;
        JSONObject json = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);

	        if (response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException(GET_ACL_CHANGESETS_URL + " return status:" + response.getStatus());
	        }

	        Reader reader = new BufferedReader(new InputStreamReader(response.getContentAsStream(), "UTF-8"));
	        json = new JSONObject(new JSONTokener(reader));
        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }
        
        if (log.isDebugEnabled())
        {
            log.debug(json.toString(3));
        }
        
        JSONArray aclChangeSetsJSON = json.getJSONArray("aclChangeSets");
        List<AclChangeSet> aclChangeSets = new ArrayList<AclChangeSet>(aclChangeSetsJSON.length());
        for (int i = 0; i < aclChangeSetsJSON.length(); i++)
        {
            JSONObject aclChangeSetJSON = aclChangeSetsJSON.getJSONObject(i);
            long aclChangeSetId = aclChangeSetJSON.getLong("id");
            long commitTimeMs = aclChangeSetJSON.getLong("commitTimeMs");
            int aclCount = aclChangeSetJSON.getInt("aclCount");
            AclChangeSet aclChangeSet = new AclChangeSet(aclChangeSetId, commitTimeMs, aclCount);
            aclChangeSets.add(aclChangeSet);
        }
        
        
        Long maxChangeSetCommitTime = null;
        if(json.has("maxChangeSetCommitTime"))
        {
            maxChangeSetCommitTime = json.getLong("maxChangeSetCommitTime");
        }

        Long maxChangeSetIdOnServer = null;
        if(json.has("maxChangeSetId"))
        {
            maxChangeSetIdOnServer = json.getLong("maxChangeSetId");
        }
        
        // Done
        return new AclChangeSets(aclChangeSets, maxChangeSetCommitTime, maxChangeSetIdOnServer);
    }
    
    /**
     * Get the ACLs associated with a given list of ACL ChangeSets.  The ACLs may be truncated for
     * the last ACL ChangeSet in the return values - the ACL count from the
     * {@link #getAclChangeSets(Long, Long, int) ACL ChangeSets}.
     * 
     * @param aclChangeSets                 the ACL ChangeSets to include
     * @param minAclId                      the lowest ACL ID (may be <tt>null</tt>)
     * @param maxResults                    the maximum number of results to retrieve
     * @return                              the ACLs (includes ChangeSet ID)
     */
    public List<Acl> getAcls(List<AclChangeSet> aclChangeSets, Long minAclId, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        if (aclChangeSets.size() > 512)
        {
            throw new IllegalArgumentException("Cannot query for more than 512 ACL ChangeSets.");
        }

        StringBuilder url = new StringBuilder(GET_ACLS);
        StringBuilder args = new StringBuilder();
        if (minAclId != null)
        {
            args.append("?").append("fromId").append("=").append(minAclId);
        }
        
        if(maxResults >= 0)
        {
            args.append(args.length() == 0 ? "?" : "&").append("maxResults").append("=").append(maxResults);  
        }
        
        url.append(args);
        
        JSONObject jsonReq = new JSONObject();
        JSONArray aclChangeSetIdsJSON = new JSONArray();
        List<Long> aclChangeSetIds = new ArrayList<Long>();
        for (AclChangeSet aclChangeSet : aclChangeSets)
        {
            Long aclChangeSetId = aclChangeSet.getId();
            aclChangeSetIdsJSON.put(aclChangeSetId);
            aclChangeSetIds.add(aclChangeSetId);
        }
        jsonReq.put("aclChangeSetIds", aclChangeSetIdsJSON);

        PostRequest req = new PostRequest(url.toString(), jsonReq.toString(), "application/json");
        Response response = null;
        JSONObject json = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);

	        if (response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException(GET_ACL_CHANGESETS_URL + " return status:" + response.getStatus());
	        }
	        
	        Reader reader = new BufferedReader(new InputStreamReader(response.getContentAsStream(), "UTF-8"));
	        json = new JSONObject(new JSONTokener(reader));
        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }
        
        if (log.isDebugEnabled())
        {
            log.debug(json.toString(3));
        }

        JSONArray aclsJSON = json.getJSONArray("acls");
        List<Acl> acls = new ArrayList<Acl>(aclsJSON.length());
        for (int i = 0; i < aclsJSON.length(); i++)
        {
            JSONObject aclJSON = aclsJSON.getJSONObject(i);
            long aclChangeSetId = aclJSON.getLong("aclChangeSetId");
            long aclId = aclJSON.getLong("id");
            Acl acl = new Acl(aclChangeSetId, aclId);
            acls.add(acl);
        }
        // Done
        return acls;
    }
    
    /**
     * Get the ACL readers for a given list of ACLs
     * 
     * @param acls                          the ACLs
     * @return                              the readers for the ACLs
     */
    public List<AclReaders> getAclReaders(List<Acl> acls) throws AuthenticationException, IOException, JSONException
    {
        if (acls.size() > 512)
        {
            throw new IllegalArgumentException("Cannot query for more than 512 ACLs.");
        }

        StringBuilder url = new StringBuilder(GET_ACLS_READERS);
        
        JSONObject jsonReq = new JSONObject();
        JSONArray aclIdsJSON = new JSONArray();
        List<Long> aclIds = new ArrayList<Long>();
        for (Acl acl : acls)
        {
            Long aclId = acl.getId();
            aclIdsJSON.put(aclId);
            aclIds.add(aclId);
        }
        jsonReq.put("aclIds", aclIdsJSON);

        PostRequest req = new PostRequest(url.toString(), jsonReq.toString(), "application/json");
        Response response = null;
        JSONObject json = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);

	        if (response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException(GET_ACLS_READERS + " return status:" + response.getStatus());
	        }
        
	        Reader reader = new BufferedReader(new InputStreamReader(response.getContentAsStream(), "UTF-8"));
	        json = new JSONObject(new JSONTokener(reader));
        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }
        
        if (log.isDebugEnabled())
        {
            log.debug(json.toString(3));
        }

        JSONArray aclsReadersJSON = json.getJSONArray("aclsReaders");
        List<AclReaders> aclsReaders = new ArrayList<AclReaders>(aclsReadersJSON.length());
        for (int i = 0; i < aclsReadersJSON.length(); i++)
        {
            JSONObject aclReadersJSON = aclsReadersJSON.getJSONObject(i);
            long aclId = aclReadersJSON.getLong("aclId");
            JSONArray readersJSON = aclReadersJSON.getJSONArray("readers");
            List<String> readers = authorityListFromJSON(readersJSON);
            JSONArray deniedJSON = aclReadersJSON.getJSONArray("denied");
            List<String> denied = authorityListFromJSON(deniedJSON);
            long aclChangeSetId = aclReadersJSON.getLong("aclChangeSetId");
            
            String tenantDomain = aclReadersJSON.getString("tenantDomain");
            if (tenantDomain == null)
            {
                tenantDomain = TenantService.DEFAULT_DOMAIN;
            }
            
            AclReaders aclReaders = new AclReaders(aclId, readers, denied, aclChangeSetId, tenantDomain);
            aclsReaders.add(aclReaders);
        }
        // Done
        return aclsReaders;
    }
    
    /**
     * Convert a JSON array of authorities to a simple Java List&lt;String&gt;
     * 
     * @param jsonArray
     * @return List&lt;String&gt;
     * @throws JSONException
     */
    private List<String> authorityListFromJSON(JSONArray jsonArray) throws JSONException
    {
        List<String> authorities = new ArrayList<String>(jsonArray.length());
        for (int j = 0; j < jsonArray.length(); j++)
        {
            String authority = jsonArray.getString(j);
            authorities.add(authority);
        }
        return authorities;
    }
    
    public Transactions getTransactions(Long fromCommitTime, Long minTxnId, Long toCommitTime, Long maxTxnId, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        StringBuilder url = new StringBuilder(GET_TRANSACTIONS_URL);
        StringBuilder args = new StringBuilder();
        if (fromCommitTime != null)
        {
            args.append("?").append("fromCommitTime").append("=").append(fromCommitTime);
        }
        if (minTxnId != null)
        {
            args.append(args.length() == 0 ? "?" : "&").append("minTxnId").append("=").append(minTxnId);            
        }
        if (toCommitTime != null)
        {
            args.append(args.length() == 0 ? "?" : "&").append("toCommitTime").append("=").append(toCommitTime);            
        }
        if (maxTxnId != null)
        {
            args.append(args.length() == 0 ? "?" : "&").append("maxTxnId").append("=").append(maxTxnId);            
        }
        if (maxResults != 0 && maxResults != Integer.MAX_VALUE)
        {
            args.append(args.length() == 0 ? "?" : "&").append("maxResults").append("=").append(maxResults);            
        }
        
        url.append(args);
        
        GetRequest req = new GetRequest(url.toString());
        Response response = null;
        List<Transaction> transactions = new ArrayList<Transaction>();
        Long maxTxnCommitTime = null;
        Long maxTxnIdOnServer = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);
	        if(response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException("GetTransactions return status is " + response.getStatus());
	        }

            Reader reader = new BufferedReader(new InputStreamReader(response.getContentAsStream(), "UTF-8"));
	        JsonParser parser = jsonFactory.createJsonParser(reader);
	        
	        JsonToken token = parser.nextValue();
	        while (token != null) 
	        {
	            if ("transactions".equals(parser.getCurrentName()))
	            {
	                token = parser.nextToken(); //START_ARRAY
	                while (token == JsonToken.START_OBJECT)
	                {
	                    token = parser.nextValue();
	                    long id = parser.getLongValue();  
	                    
	                    token = parser.nextValue();
	                    long commitTime = parser.getLongValue();
	                    
	                    token = parser.nextValue();
	                    long updates = parser.getLongValue();
	                    
	                    token = parser.nextValue();
	                    long deletes = parser.getLongValue();

	                    Transaction txn = new Transaction();
	                    txn.setCommitTimeMs(commitTime);
	                    txn.setDeletes(deletes);
	                    txn.setId(id);
	                    txn.setUpdates(updates);
	                    
	                    transactions.add(txn);
	                    
	                    token = parser.nextToken(); //END_OBJECT
	                    token = parser.nextToken(); // START_OBJECT or END_ARRAY;
	                }
	            }
	            else if ("maxTxnCommitTime".equals(parser.getCurrentName()))
	            {
	                maxTxnCommitTime = parser.getLongValue();
	            }
	            else if ("maxTxnId".equals(parser.getCurrentName()))
	            {
	                maxTxnIdOnServer = parser.getLongValue();
	            }
	            token = parser.nextValue();
	        }
	        parser.close();
	        reader.close();

        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }

        return new Transactions(transactions, maxTxnCommitTime, maxTxnIdOnServer);
    }
  
    
    public List<Node> getNodes(GetNodesParameters parameters, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        StringBuilder url = new StringBuilder(GET_NODES_URL);

        JSONObject body = new JSONObject();
        
        if(parameters.getTransactionIds() != null)
        {
            JSONArray jsonTxnIds = new JSONArray();
            for(Long txnId : parameters.getTransactionIds())
            {
                jsonTxnIds.put(txnId);
            }
            body.put("txnIds", jsonTxnIds);
        }
    
        if(parameters.getFromNodeId() != null)
        {
            body.put("fromNodeId", parameters.getFromNodeId());
        }
        if(parameters.getToNodeId() != null)
        {
            body.put("toNodeId", parameters.getToNodeId());
        }
        if(parameters.getExcludeAspects() != null)
        {
            JSONArray jsonExcludeAspects = new JSONArray();
            for(QName excludeAspect : parameters.getExcludeAspects())
            {
                jsonExcludeAspects.put(excludeAspect.toString());
            }
            body.put("excludeAspects", jsonExcludeAspects);
        }
        if(parameters.getIncludeAspects() != null)
        {
            JSONArray jsonIncludeAspects = new JSONArray();
            for(QName includeAspect : parameters.getIncludeAspects())
            {
                jsonIncludeAspects.put(includeAspect.toString());
            }
            body.put("includeAspects", jsonIncludeAspects);
        }

        if(parameters.getStoreProtocol() != null)
        {
            body.put("storeProtocol", parameters.getStoreProtocol());
        }

        if(parameters.getStoreIdentifier() != null)
        {
            body.put("storeIdentifier", parameters.getStoreIdentifier());
        }
        
        body.put("maxResults", maxResults);

        PostRequest req = new PostRequest(url.toString(), body.toString(), "application/json");
 
        Response response = null;
        JSONObject json = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);
	        if(response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException("GetNodes return status is " + response.getStatus());
	        }

	        Reader reader = new BufferedReader(new InputStreamReader(response.getContentAsStream(), "UTF-8"));
	        json = new JSONObject(new JSONTokener(reader));
        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }
        
        if(log.isDebugEnabled())
        {
            log.debug(json.toString());
        }

        JSONArray jsonNodes = json.getJSONArray("nodes");
        List<Node> nodes = new ArrayList<Node>(jsonNodes.length());
        for(int i = 0; i < jsonNodes.length(); i++)
        {
            JSONObject jsonNodeInfo = jsonNodes.getJSONObject(i);
            Node nodeInfo = new Node();
            if(jsonNodeInfo.has("id"))
            {
                nodeInfo.setId(jsonNodeInfo.getLong("id"));
            }

            if(jsonNodeInfo.has("nodeRef"))
            {
                nodeInfo.setNodeRef(jsonNodeInfo.getString("nodeRef"));
            }
            
            if(jsonNodeInfo.has("txnId"))
            {
                nodeInfo.setTxnId(jsonNodeInfo.getLong("txnId"));
            }

            if(jsonNodeInfo.has("status"))
            {
                Node.SolrApiNodeStatus status;
                String statusStr = jsonNodeInfo.getString("status");
                if(statusStr.equals("u"))
                {
                    status = Node.SolrApiNodeStatus.UPDATED;
                }
                else if(statusStr.equals("d"))
                {
                    status = Node.SolrApiNodeStatus.DELETED;
                }
                else
                {
                    status = Node.SolrApiNodeStatus.UNKNOWN;
                }
                nodeInfo.setStatus(status);
            }
            
            nodes.add(nodeInfo);
        }

        return nodes;
    }
    
    private PropertyValue getSinglePropertyValue(DataTypeDefinition dataType, Object value) throws JSONException
    {
        PropertyValue ret = null;
        QName dataTypeName = dataType.getName();

        if(value == null || value == JSONObject.NULL)
        {
            ret = null;
        }
        else if(dataTypeName.equals(DataTypeDefinition.MLTEXT))
        {
            JSONArray a = (JSONArray)value;
            Map<Locale, String> mlValues = new HashMap<Locale, String>(a.length());

            for(int k = 0; k < a.length(); k++)
            {
                JSONObject pair = a.getJSONObject(k);
                Locale locale = deserializer.deserializeValue(Locale.class, pair.getString("locale"));
                String mlValue = pair.has("value") ? pair.getString("value") : null;
                mlValues.put(locale, mlValue);
            }

            ret = new MLTextPropertyValue(mlValues);
        }
        else if(dataTypeName.equals(DataTypeDefinition.CONTENT))
        {
            JSONObject o = (JSONObject)value;
            
            String localeStr = o.has("locale") ? o.getString("locale") : null;
            Locale locale = (o.has("locale") ? deserializer.deserializeValue(Locale.class, localeStr) : null);

            Long size = o.has("size") ? o.getLong("size") : null;

            String encoding = o.has("encoding") ? o.getString("encoding") : null;
            String mimetype = o.has("mimetype") ? o.getString("mimetype") : null;

            Long id = o.has("contentId") ? o.getLong("contentId") : null;
            
            ret = new ContentPropertyValue(locale, size, encoding, mimetype, id);
        }
        else
        {
            ret = new StringPropertyValue((String)value);
        }
        
        return ret;
    }

    private PropertyValue getPropertyValue(PropertyDefinition propertyDef, Object value) throws JSONException
    {
        PropertyValue ret = null;

        if(value == null || value == JSONObject.NULL)
        {
            ret = null;
        }
        else if(propertyDef == null)
        {
            // assume a string
            ret = new StringPropertyValue((String)value);
        }
        else
        {
            DataTypeDefinition dataType = propertyDef.getDataType();
            
            boolean isMulti = propertyDef.isMultiValued();
            if(isMulti)
            {
                if(!(value instanceof JSONArray))
                {
                    throw new IllegalArgumentException("Expected json array, got " + value.getClass().getName());
                }

                MultiPropertyValue multi = new MultiPropertyValue();
                JSONArray array = (JSONArray)value;
                for(int j = 0; j < array.length(); j++)
                {
                    multi.addValue(getSinglePropertyValue(dataType, array.get(j)));
                }
    
                ret = multi;
            }
            else
            {
                ret = getSinglePropertyValue(dataType, value);
            }
        }
        
        return ret;
    }
    
    public List<NodeMetaData> getNodesMetaData(NodeMetaDataParameters params, int maxResults) throws AuthenticationException, IOException, JSONException
    {
        List<Long> nodeIds = params.getNodeIds();
        
        StringBuilder url = new StringBuilder(GET_METADATA_URL);

        JSONObject body = new JSONObject();
        if(nodeIds != null && nodeIds.size() > 0)
        {
            JSONArray jsonNodeIds = new JSONArray();
            for(Long nodeId : nodeIds)
            {
                jsonNodeIds.put(nodeId);
            }
            body.put("nodeIds", jsonNodeIds);

        }
        if(params.getFromNodeId() != null)
        {
            body.put("fromNodeId", params.getFromNodeId());
        }
        if(params.getToNodeId() != null)
        {
            body.put("toNodeId", params.getToNodeId());
        }
        
        // only need to set in cases where we don't want them in the response
        // because they default to true
        if(!params.isIncludeAclId())
        {
            body.put("includeAclId", params.isIncludeAclId());
        }
        if(!params.isIncludeAspects())
        {
            body.put("includeAspects", params.isIncludeAspects());
        }
        if(!params.isIncludeProperties())
        {
            body.put("includeProperties", params.isIncludeProperties());
        }
        if(!params.isIncludeChildAssociations())
        {
            body.put("includeChildAssociations", params.isIncludeChildAssociations());
        }
        if(!params.isIncludeParentAssociations())
        {
            body.put("includeParentAssociations", params.isIncludeParentAssociations());
        }
        if(!params.isIncludeChildIds())
        {
            body.put("includeChildIds", params.isIncludeChildIds());
        }
        if(!params.isIncludePaths())
        {
            body.put("includePaths", params.isIncludePaths());
        }
        if(!params.isIncludeOwner())
        {
            body.put("includeOwner", params.isIncludeOwner());
        }
        if(!params.isIncludeNodeRef())
        {
            body.put("includeNodeRef", params.isIncludeNodeRef());
        }
        if(!params.isIncludeTxnId())
        {
            body.put("includeTxnId", params.isIncludeTxnId());
        }

        body.put("maxResults", maxResults);

        PostRequest req = new PostRequest(url.toString(), body.toString(), "application/json");
        Response response = null;
        JSONObject json = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);
	        if(response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException("GetNodeMetaData return status is " + response.getStatus());
	        }
        
	        Reader reader = new BufferedReader(new InputStreamReader(response.getContentAsStream(), "UTF-8"));
	        json = new JSONObject(new JSONTokener(reader));
        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }

        if (log.isDebugEnabled())
        {
            log.debug(json.toString(3));
        }
        
        JSONArray jsonNodes = json.getJSONArray("nodes");
        
        List<NodeMetaData> nodes = new ArrayList<NodeMetaData>(jsonNodes.length());
        for(int i = 0; i < jsonNodes.length(); i++)
        {
            JSONObject jsonNodeInfo = jsonNodes.getJSONObject(i);
            NodeMetaData metaData = new NodeMetaData();
            
            if(jsonNodeInfo.has("id"))
            {
                metaData.setId(jsonNodeInfo.getLong("id"));
            }
            
            if(jsonNodeInfo.has("tenantDomain"))
            {
                metaData.setTenantDomain(jsonNodeInfo.getString("tenantDomain"));
            }
            
            if(jsonNodeInfo.has("txnId"))
            {
                metaData.setTxnId(jsonNodeInfo.getLong("txnId"));
            }
            
            if(jsonNodeInfo.has("aclId"))
            {
                metaData.setAclId(jsonNodeInfo.getLong("aclId"));
            }

            if(jsonNodeInfo.has("nodeRef"))
            {
                metaData.setNodeRef(new NodeRef(jsonNodeInfo.getString("nodeRef")));
            }
            
            if(jsonNodeInfo.has("type"))
            {
                metaData.setType(deserializer.deserializeValue(QName.class, jsonNodeInfo.getString("type")));
            }
            
            if(jsonNodeInfo.has("aspects"))
            {
                JSONArray jsonAspects = jsonNodeInfo.getJSONArray("aspects");
                Set<QName> aspects = new HashSet<QName>(jsonAspects.length());
                for(int j = 0; j < jsonAspects.length(); j++)
                {
                    String jsonAspect = (String)jsonAspects.get(j);
                    aspects.add(deserializer.deserializeValue(QName.class, jsonAspect));
                }
                metaData.setAspects(aspects);
            }

            if(jsonNodeInfo.has("paths"))
            {
                JSONArray jsonPaths = jsonNodeInfo.getJSONArray("paths");
                List<Pair<String, QName>> paths = new ArrayList<Pair<String, QName>>(jsonPaths.length());
                for(int j = 0; j < jsonPaths.length(); j++)
                {
                    JSONObject path = new JSONObject(jsonPaths.getString(j));
                    String pathValue = path.getString("path");
                    QName qname = path.has("qname") ? deserializer.deserializeValue(QName.class, path.getString("qname")) : null;
                    paths.add(new Pair<String, QName>(pathValue, qname));
                }
                metaData.setPaths(paths);
            }
            
            if(jsonNodeInfo.has("namePaths"))
            {
                JSONArray jsonNamePaths = jsonNodeInfo.getJSONArray("namePaths");
                List<List<String>> namePaths = new ArrayList<List<String>>(jsonNamePaths.length());
                for(int j = 0; j < jsonNamePaths.length(); j++)
                {
                    JSONObject jsonNamePath = jsonNamePaths.getJSONObject(j);
                    JSONArray jsonNameElements = jsonNamePath.getJSONArray("namePath");
                    List<String> namePath = new ArrayList<String>(jsonNameElements.length());
                    for(int k = 0; k < jsonNameElements.length(); k++)
                    {
                        String namePathElement =  jsonNameElements.getString(k);
                        namePath.add(namePathElement);
                    }
                    namePaths.add(namePath);
                }
                metaData.setNamePaths(namePaths);
            }
            
            if(jsonNodeInfo.has("ancestors"))
            {
                JSONArray jsonAncestors = jsonNodeInfo.getJSONArray("ancestors");
                HashSet<NodeRef> ancestors = new HashSet<NodeRef>(jsonAncestors.length());
                for(int j = 0; j < jsonAncestors.length(); j++)
                {
                    String ancestorNodeRefString = jsonAncestors.getString(j);
                    NodeRef ancestorNodeRef = new NodeRef(ancestorNodeRefString);
                    ancestors.add(ancestorNodeRef);
                }
                metaData.setAncestors(ancestors);
            }

            if(jsonNodeInfo.has("properties"))
            {
                JSONObject jsonProperties = jsonNodeInfo.getJSONObject("properties");
                Map<QName, PropertyValue> properties = new HashMap<QName, PropertyValue>(jsonProperties.length());
                @SuppressWarnings("rawtypes")
                Iterator propKeysIterator = jsonProperties.keys();
                while(propKeysIterator.hasNext())
                {
                    String propName = (String)propKeysIterator.next();
                    QName propQName = deserializer.deserializeValue(QName.class, propName);
                    Object propValueObj = jsonProperties.opt(propName);

                    // check the expected property type to determine how to process the value
                    PropertyDefinition propertyDef = dictionaryService.getProperty(propQName);
//                    if(propertyDef == null)
//                    {
//                        // TODO which exception here?
//                        throw new IllegalArgumentException("Could not find property definition for property " + propName);
//                    }
                    
                    properties.put(propQName, getPropertyValue(propertyDef, propValueObj));
                }
                metaData.setProperties(properties);
            }
            
            if(jsonNodeInfo.has("parentAssocsCrc"))
            {
                metaData.setParentAssocsCrc(jsonNodeInfo.getLong("parentAssocsCrc"));
            }
            
            if(jsonNodeInfo.has("parentAssocs"))
            {
                JSONArray jsonParentAssocs = jsonNodeInfo.getJSONArray("parentAssocs");
                List<ChildAssociationRef> assocs = new ArrayList<ChildAssociationRef>(jsonParentAssocs.length());
                for(int j = 0; j < jsonParentAssocs.length(); j++)
                {
                    String childAssocRefStr = jsonParentAssocs.getString(j);
                    ChildAssociationRef childAssociationRef = new ChildAssociationRef(childAssocRefStr);
                    assocs.add(childAssociationRef);
                }
                metaData.setParentAssocs(assocs);
            }
            
            if(jsonNodeInfo.has("childAssocs"))
            {
                JSONArray jsonParentAssocs = jsonNodeInfo.getJSONArray("childAssocs");
                List<ChildAssociationRef> assocs = new ArrayList<ChildAssociationRef>(jsonParentAssocs.length());
                for(int j = 0; j < jsonParentAssocs.length(); j++)
                {
                    String childAssocRefStr = jsonParentAssocs.getString(j);
                    ChildAssociationRef childAssociationRef = new ChildAssociationRef(childAssocRefStr);
                    assocs.add(childAssociationRef);
                }
                metaData.setChildAssocs(assocs);
            }
            
            if(jsonNodeInfo.has("childIds"))
            {
                JSONArray jsonChildIds = jsonNodeInfo.getJSONArray("childIds");
                List<Long> childIds = new ArrayList<Long>(jsonChildIds.length());
                for(int j = 0; j < jsonChildIds.length(); j++)
                {
                    Long childId = jsonChildIds.getLong(j);
                    childIds.add(childId);
                }
                metaData.setChildIds(childIds);
            }
            
            if(jsonNodeInfo.has("owner"))
            {
                metaData.setOwner(jsonNodeInfo.getString("owner"));
            }
           
            
            nodes.add(metaData);
            
         
        }

        return nodes;
    }
    
    public GetTextContentResponse getTextContent(Long nodeId, QName propertyQName, Long modifiedSince) throws AuthenticationException, IOException
    {
        StringBuilder url = new StringBuilder(GET_CONTENT);
        
        StringBuilder args = new StringBuilder();
        if(nodeId != null)
        {
            args.append("?");
            args.append("nodeId");
            args.append("=");
            args.append(nodeId);            
        }
        else
        {
            throw new NullPointerException();
        }
        if(propertyQName != null)
        {
            if(args.length() == 0)
            {
                args.append("?");
            }
            else
            {
                args.append("&");
            }
            args.append("propertyQName");
            args.append("=");
            URLCodec encoder = new URLCodec();
            args.append(encoder.encode(propertyQName.toString(), "UTF-8"));
        }
       
        url.append(args);
        
        GetRequest req = new GetRequest(url.toString());
        Map<String, String> headers = new HashMap<String, String>(2);
        
        if(modifiedSince != null)
        {
            headers.put("If-Modified-Since", String.valueOf(DateUtil.formatDate(new Date(modifiedSince))));
        }

        req.setHeaders(headers);
        
        Response response = repositoryHttpClient.sendRequest(req);
        
        if(response.getStatus() != Status.STATUS_NOT_MODIFIED && response.getStatus() != Status.STATUS_NO_CONTENT && response.getStatus() != Status.STATUS_OK)
        {
            throw new AlfrescoRuntimeException("GetTextContentResponse return status is " + response.getStatus());
        }

        return new GetTextContentResponse(response);
    }
    
    public AlfrescoModel getModel(QName modelName) throws AuthenticationException, IOException, JSONException
    {
        // If the model is new to the SOLR side the prefix will be unknown so we can not generate prefixes for the request!
        // Always use the full QName with explicit URI
        StringBuilder url = new StringBuilder(GET_MODEL);

        URLCodec encoder = new URLCodec();
        // must send the long name as we may not have the prefix registered
        url.append("?modelQName=").append(encoder.encode(modelName.toString(), "UTF-8"));
        
        GetRequest req = new GetRequest(url.toString());

        Response response = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);
        	if(response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException("GetModel return status is " + response.getStatus());
	        }

	        return new AlfrescoModel(M2Model.createModel(response.getContentAsStream()),
	                Long.valueOf(response.getHeader(CHECKSUM_HEADER)));
        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }
    }
    
    public List<AlfrescoModelDiff> getModelsDiff(List<AlfrescoModel> currentModels) throws AuthenticationException, IOException, JSONException
    {
        StringBuilder url = new StringBuilder(GET_MODELS_DIFF);

        JSONObject body = new JSONObject();
        JSONArray jsonModels = new JSONArray();
        for(AlfrescoModel model : currentModels)
        {
            JSONObject jsonModel = new JSONObject();
            QName modelQName = QName.createQName( model.getModel().getName(), namespaceDAO);
            jsonModel.put("name", modelQName.toString());
            jsonModel.put("checksum", model.getChecksum());
            jsonModels.put(jsonModel);
        }
        body.put("models", jsonModels);

        PostRequest req = new PostRequest(url.toString(), body.toString(), "application/json");
        Response response = null;
        JSONObject json = null;
        try
        {
        	response = repositoryHttpClient.sendRequest(req);
	        if(response.getStatus() != HttpStatus.SC_OK)
	        {
	            throw new AlfrescoRuntimeException("GetModelsDiff return status is " + response.getStatus());
	        }
	
	        Reader reader = new BufferedReader(new InputStreamReader(response.getContentAsStream(), "UTF-8"));
	        json = new JSONObject(new JSONTokener(reader));
        }
        finally
        {
        	if(response != null)
        	{
        		response.release();
        	}
        }
        
        if(log.isDebugEnabled())
        {
            log.debug(json.toString());
        }
        JSONArray jsonDiffs = json.getJSONArray("diffs");
        if(jsonDiffs == null)
        {
            throw new AlfrescoRuntimeException("GetModelsDiff badly formatted response");
        }

        List<AlfrescoModelDiff> diffs = new ArrayList<AlfrescoModelDiff>(jsonDiffs.length());
        for(int i = 0; i < jsonDiffs.length(); i++)
        {
            JSONObject jsonDiff = jsonDiffs.getJSONObject(i);
            diffs.add(new AlfrescoModelDiff(
                    QName.createQName(jsonDiff.getString("name")),
                    AlfrescoModelDiff.TYPE.valueOf(jsonDiff.getString("type")),
                    (jsonDiff.isNull("oldChecksum") ? null : jsonDiff.getLong("oldChecksum")),
                    (jsonDiff.isNull("newChecksum") ? null : jsonDiff.getLong("newChecksum"))));
        }

        return diffs;
    }

    /*
     * type conversions from serialized JSON values to SOLR-consumable objects 
     */
    @SuppressWarnings("rawtypes")
    private class SOLRTypeConverter
    {
        /**
         * Default Type Converter
         */
        private TypeConverter instance = new TypeConverter();
        private NamespaceDAO namespaceDAO;
        
        @SuppressWarnings("unchecked")
        SOLRTypeConverter(NamespaceDAO namespaceDAO)
        {
            this.namespaceDAO = namespaceDAO;

            // add all default converters to this converter
            // TODO find a better way of doing this
            Map<Class<?>, Map<Class<?>, Converter<?,?>>> converters = DefaultTypeConverter.INSTANCE.getConverters();
            for(Class source : converters.keySet())
            {
                Map<Class<?>, Converter<?,?>> converters1 = converters.get(source);
                for(Class dest : converters1.keySet())
                {
                    Converter<?,?> converter = converters1.get(dest);
                    instance.addConverter(source, dest, converter);
                }
            }
            
            // dates
            instance.addConverter(String.class, Date.class, new TypeConverter.Converter<String, Date>()
            {
                public Date convert(String source)
                {
                    try
                    {
                        Date date = ISO8601DateFormat.parse(source);
                        return date;
                    }
                    catch (Exception e)
                    {
                        throw new TypeConversionException("Failed to convert date " + source + " to string", e);
                    }
                }
            });
                    
            // node refs        
            instance.addConverter(String.class, NodeRef.class, new TypeConverter.Converter<String, NodeRef>()
            {
                public NodeRef convert(String source)
                {
                    return new NodeRef(source);
                }
            });
            
            // paths
            instance.addConverter(String.class, AttributeElement.class, new TypeConverter.Converter<String, AttributeElement>()
            {
                public AttributeElement convert(String source)
                {
                    return new Path.AttributeElement(source);
                }
            });
            
            instance.addConverter(String.class, ChildAssocElement.class, new TypeConverter.Converter<String, ChildAssocElement>()
            {
                public ChildAssocElement convert(String source)
                {
                    return new Path.ChildAssocElement(instance.convert(ChildAssociationRef.class, source));
                }
            });
            
            instance.addConverter(String.class, Path.DescendentOrSelfElement.class, new TypeConverter.Converter<String, Path.DescendentOrSelfElement>()
            {
                public Path.DescendentOrSelfElement convert(String source)
                {
                    return new Path.DescendentOrSelfElement();
                }
            });
            
            instance.addConverter(String.class, Path.ParentElement.class, new TypeConverter.Converter<String, Path.ParentElement>()
            {
                public Path.ParentElement convert(String source)
                {
                    return new Path.ParentElement();
                }
            });
            
            instance.addConverter(String.class, Path.SelfElement.class, new TypeConverter.Converter<String, Path.SelfElement>()
            {
                public Path.SelfElement convert(String source)
                {
                    return new Path.SelfElement();
                }
            });
            
            // associations
            instance.addConverter(String.class, ChildAssociationRef.class, new TypeConverter.Converter<String, ChildAssociationRef>()
            {
                public ChildAssociationRef convert(String source)
                {
                    return new ChildAssociationRef(source);
                }
            });

            instance.addConverter(String.class, AssociationRef.class, new TypeConverter.Converter<String, AssociationRef>()
            {
                public AssociationRef convert(String source)
                {
                    return new AssociationRef(source);
                }
            });
            
            // qnames
            instance.addConverter(String.class, QName.class, new TypeConverter.Converter<String, QName>()
            {
                public QName convert(String source)
                {
                    return QName.createQName(source, SOLRTypeConverter.this.namespaceDAO);
                }
            });
            
            instance.addConverter(String.class, MLText.class, new TypeConverter.Converter<String, MLText>()
            {
                public MLText convert(String source)
                {
                    return new MLText(source);
                }
            });
        }
        
        public final <T> T convert(Class<T> c, Object value)
        {
            return instance.convert(c, value);
        }
    }
    
    /*
     * Deserializes JSON values from the remote API into objects consumable by SOLR
     */
    private class SOLRDeserializer
    {
        private SOLRTypeConverter typeConverter;

        public SOLRDeserializer(NamespaceDAO namespaceDAO)
        {
            typeConverter = new SOLRTypeConverter(namespaceDAO);
        }
        
        public <T> T deserializeValue(Class<T> targetClass, Object value) throws JSONException
        {
            return typeConverter.convert(targetClass, value);
        }
    }
    
    private static class SOLRResponse
    {
        protected Response response;
        
        public SOLRResponse(Response response)
        {
            super();
            this.response = response;
        }
        
        public Response getResponse()
        {
            return response;
        }
    }

    public static class GetTransactionsResponse extends SOLRResponse
    {
        private List<Transaction> txns;

        public GetTransactionsResponse(Response response, List<Transaction> txns)
        {
            super(response);
            this.txns = txns;
        }

        public List<Transaction> getTransaction()
        {
            return txns;
        }
    }
    
    public static class GetNodesResponse extends SOLRResponse
    {
        private List<Node> nodes;

        public GetNodesResponse(Response response, List<Node> nodes)
        {
            super(response);
            this.nodes = nodes;
        }

        public List<Node> getNodes()
        {
            return nodes;
        }
    }
    
    public static class GetNodesMetaDataResponse extends SOLRResponse
    {
        private List<NodeMetaData> nodes;

        public GetNodesMetaDataResponse(Response response, List<NodeMetaData> nodes)
        {
            super(response);
            this.nodes = nodes;
        }

        public List<NodeMetaData> getNodes()
        {
            return nodes;
        }
    }
    
    public static enum SolrApiContentStatus
    {
        NOT_MODIFIED, OK, NO_TRANSFORM, NO_CONTENT, UNKNOWN, TRANSFORM_FAILED, GENERAL_FAILURE;
        
        public static SolrApiContentStatus getStatus(String statusStr)
        {
            if(statusStr.equals("ok"))
            {
                return OK;
            }
            else if(statusStr.equals("transformFailed"))
            {
                return TRANSFORM_FAILED;
            }
            else if(statusStr.equals("noTransform"))
            {
                return NO_TRANSFORM;
            }
            else if(statusStr.equals("noContent"))
            {
                return NO_CONTENT;
            }
            else
            {
                return UNKNOWN;
            }
        }
    }
    
/*    public static enum TRANSFORM_STATUS
    {
        OK, FAILED, NO_TRANSFORM, UNKNOWN;
        
        public static TRANSFORM_STATUS getStatus(String statusStr)
        {
            if(statusStr.equals("ok"))
            {
                return OK;
            }
            else if(statusStr.equals("failed"))
            {
                return FAILED;
            }
            else if(statusStr.equals("noTransform"))
            {
                return NO_TRANSFORM;
            }
            else
            {
                return UNKNOWN;
            }
        }
    }*/

    // TODO register a stream close listener that release the response when the response has been read
    public static class GetTextContentResponse extends SOLRResponse
    {
        private InputStream content;
        private SolrApiContentStatus status;
        private String transformException;
        private String transformStatusStr;
        private Long transformDuration;

        public GetTextContentResponse(Response response) throws IOException
        {
            super(response);

//            try
//            {
	            this.content = response.getContentAsStream();
	            this.transformStatusStr = response.getHeader("X-Alfresco-transformStatus");
	            this.transformException = response.getHeader("X-Alfresco-transformException");
	            String tmp = response.getHeader("X-Alfresco-transformDuration");
	            this.transformDuration = (tmp != null ? Long.valueOf(tmp) : null);
//            }
//            finally
//            {
//            	response.release();
//            }
            setStatus();
        }

        public InputStream getContent()
        {
            return content;
        }

        public SolrApiContentStatus getStatus()
        {
            return status;
        }
        
        private void setStatus()
        {
            int status = response.getStatus();
            if(status == HttpStatus.SC_NOT_MODIFIED)
            {
                this.status = SolrApiContentStatus.NOT_MODIFIED;
            }
            else if(status == HttpStatus.SC_INTERNAL_SERVER_ERROR)
            {
                this.status = SolrApiContentStatus.GENERAL_FAILURE;
            }
            else if(status == HttpStatus.SC_OK)
            {
                this.status = SolrApiContentStatus.OK;
            }
            else if(status == HttpStatus.SC_NO_CONTENT)
            {
                if(transformStatusStr == null)
                {
                    this.status = SolrApiContentStatus.UNKNOWN;
                }
                else
                {
                    if(transformStatusStr.equals("noTransform"))
                    {
                        this.status = SolrApiContentStatus.NO_TRANSFORM;
                    }
                    else if(transformStatusStr.equals("transformFailed"))
                    {
                        this.status = SolrApiContentStatus.TRANSFORM_FAILED;
                    }
                    else
                    {
                        this.status = SolrApiContentStatus.UNKNOWN;
                    }
                }
            }
        }

        public String getTransformException()
        {
            return transformException;
        }
        
        public void release()
        {
        	response.release();
        }
        
        public Long getTransformDuration()
        {
        	return transformDuration;
        }
    }

    /**
     * 
     */
    public void close()
    {
       repositoryHttpClient.close();
        
    }
}
