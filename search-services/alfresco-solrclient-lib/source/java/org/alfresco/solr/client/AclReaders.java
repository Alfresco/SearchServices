package org.alfresco.solr.client;

import java.util.List;

/**
 * SOLR-side representation of ACL Readers information.
 * 
 * @since 4.0
 */
public class AclReaders
{
    private final long id;

    private final List<String> readers;
    
    private final List<String> denied;

    private final long aclChangeSetId;
    
    private final String tenantDomain;

    public AclReaders(long id, List<String> readers, List<String> denied, long aclChangeSetId, String tenantDomain)
    {
        this.id = id;
        this.readers = readers;
        this.denied = denied;
        this.aclChangeSetId = aclChangeSetId;
        this.tenantDomain = tenantDomain;
    }

    @Override
    public String toString()
    {
        return "AclReaders [id=" + id + ", readers=" + readers + ", denied=" + denied + ", tenantDomain=" + tenantDomain + "]";
    }

    /**
     * ID should be enough for hashCode() and equals().
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    /**
     * ID should be enough for hashCode() and equals().
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AclReaders other = (AclReaders) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public long getId()
    {
        return id;
    }

    public List<String> getReaders()
    {
        return readers;
    }

    public List<String> getDenied()
    {
        return denied;
    }

    public long getAclChangeSetId()
    {
        return aclChangeSetId;
    }
    
    public String getTenantDomain()
    {
        return tenantDomain;
    }
}
