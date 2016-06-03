package org.alfresco.solr.client;

/**
 * SOLR-side representation of transaction information.
 * 
 * @since 4.0
 */
public class Transaction
{
    private long id;
    private long commitTimeMs;
    private long updates;
    private long deletes;

    public long getId()
    {
        return id;
    }
    public void setId(long id)
    {
        this.id = id;
    }
    public long getCommitTimeMs()
    {
        return commitTimeMs;
    }
    public void setCommitTimeMs(long commitTimeMs)
    {
        this.commitTimeMs = commitTimeMs;
    }
    public long getUpdates()
    {
        return updates;
    }
    public void setUpdates(long updates)
    {
        this.updates = updates;
    }
    public long getDeletes()
    {
        return deletes;
    }
    public void setDeletes(long deletes)
    {
        this.deletes = deletes;
    }
    @Override
    public String toString()
    {
        return "Transaction [id=" + id + ", commitTimeMs=" + commitTimeMs + ", updates=" + updates + ", deletes="
                + deletes + "]";
    }
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (commitTimeMs ^ (commitTimeMs >>> 32));
        result = prime * result + (int) (deletes ^ (deletes >>> 32));
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (int) (updates ^ (updates >>> 32));
        return result;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Transaction other = (Transaction) obj;
        if (commitTimeMs != other.commitTimeMs)
            return false;
        if (deletes != other.deletes)
            return false;
        if (id != other.id)
            return false;
        if (updates != other.updates)
            return false;
        return true;
    }
    
    
}
