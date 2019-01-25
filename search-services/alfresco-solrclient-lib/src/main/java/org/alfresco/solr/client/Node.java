/*
 * #%L
 * Alfresco Solr Client
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

public class Node
{
    public static enum SolrApiNodeStatus
    {
        UPDATED, DELETED, UNKNOWN, NON_SHARD_DELETED, NON_SHARD_UPDATED;
    };

    private long id;
    private String nodeRef;
    private long txnId;
    private SolrApiNodeStatus status;
    private String tenant;
    private long aclId;
    private String shardPropertyValue;
    
    public long getId()
    {
        return id;
    }
    public void setId(long id)
    {
        this.id = id;
    }
    public String getNodeRef()
    {
        return nodeRef;
    }
    public void setNodeRef(String nodeRef)
    {
        this.nodeRef = nodeRef;
    }
    public long getTxnId()
    {
        return txnId;
    }
    public void setTxnId(long txnId)
    {
        this.txnId = txnId;
    }
    public SolrApiNodeStatus getStatus()
    {
        return status;
    }
    public void setStatus(SolrApiNodeStatus status)
    {
        this.status = status;
    }
    /**
     * @return the tenant
     */
    public String getTenant()
    {
        return tenant;
    }
    /**
     * @param tenant the tenant to set
     */
    public void setTenant(String tenant)
    {
        this.tenant = tenant;
    }
    /**
     * @return the aclId
     */
    public long getAclId()
    {
        return aclId;
    }
    /**
     * @param aclId the aclId to set
     */
    public void setAclId(long aclId)
    {
        this.aclId = aclId;
    }
    
   
    /**
     * The property value to use for sharding - as requested
     *
     * @return null - if the node does not have the property, the standard "String" value of the property if it is present on the node.
     * For dates and datetime properties this will be the ISO formatted datetime.
     */
    public String getShardPropertyValue()
    {
        return this.shardPropertyValue;
    }
    
    public void setShardPropertyValue(String shardPropertyValue)
    {
        this.shardPropertyValue = shardPropertyValue;
    }
    @Override
    public String toString()
    {
        return "Node [id=" + this.id + ", nodeRef=" + this.nodeRef + ", txnId=" + this.txnId
                    + ", status=" + this.status + ", tenant=" + this.tenant + ", aclId="
                    + this.aclId + ", shardPropertyValue=" + this.shardPropertyValue + "]";
    }
    
    
   
}
