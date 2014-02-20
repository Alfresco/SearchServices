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

/**
 * SOLR-side representation of ACL ChangeSet information.
 * 
 * @since 4.0
 */
public class AclChangeSet
{
    private final long id;
    private final long commitTimeMs;
    private final int aclCount;

    public AclChangeSet(long id, long commitTimeMs, int aclCount)
    {
        super();
        this.id = id;
        this.commitTimeMs = commitTimeMs;
        this.aclCount = aclCount;
    }

    @Override
    public String toString()
    {
        return "AclChangeSet [id=" + id + ", commitTimeMs=" + commitTimeMs + ", aclCount="
                + aclCount + "]";
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + aclCount;
        result = prime * result + (int) (commitTimeMs ^ (commitTimeMs >>> 32));
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AclChangeSet other = (AclChangeSet) obj;
        if (aclCount != other.aclCount) return false;
        if (commitTimeMs != other.commitTimeMs) return false;
        if (id != other.id) return false;
        return true;
    }

    public long getId()
    {
        return id;
    }
    public long getCommitTimeMs()
    {
        return commitTimeMs;
    }
    public int getAclCount()
    {
        return aclCount;
    }
}
