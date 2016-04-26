package org.alfresco.solr.client;

/**
 * SOLR-side representation of basic ACL information.
 * 
 * @since 4.0
 */
public class Acl
{
    private final long aclChangeSetId;
    private final long id;

    public Acl(long aclChangeSetId, long id)
    {
        this.aclChangeSetId = aclChangeSetId;
        this.id = id;
    }

    @Override
    public String toString()
    {
        return "Acl [aclChangeSetId=" + aclChangeSetId + ", id=" + id + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (aclChangeSetId ^ (aclChangeSetId >>> 32));
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Acl other = (Acl) obj;
        if (aclChangeSetId != other.aclChangeSetId) return false;
        if (id != other.id) return false;
        return true;
    }

    public long getAclChangeSetId()
    {
        return aclChangeSetId;
    }

    public long getId()
    {
        return id;
    }
}
