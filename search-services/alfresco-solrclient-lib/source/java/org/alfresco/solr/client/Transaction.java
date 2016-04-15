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
