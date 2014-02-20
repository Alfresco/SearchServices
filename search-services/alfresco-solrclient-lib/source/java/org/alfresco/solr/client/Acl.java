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
