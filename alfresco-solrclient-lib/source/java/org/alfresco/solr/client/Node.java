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
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "Node [id=" + id + ", nodeRef=" + nodeRef + ", txnId=" + txnId + ", status=" + status + ", tenant=" + tenant + ", aclId=" + aclId + "]";
    }
   
}
