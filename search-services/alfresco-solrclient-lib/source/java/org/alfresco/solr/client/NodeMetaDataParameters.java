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

import java.util.List;

/**
 * Stores node meta data query parameters for use in SOLR remote api calls
 * 
 * @since 4.0
 */
public class NodeMetaDataParameters
{
    private List<Long> transactionIds;
    private Long fromTxnId;
    private Long toTxnId;

    // default is 'all' results
    private int maxResults = 0;

    private Long fromNodeId;
    private Long toNodeId;
    private List<Long> nodeIds;

    private boolean includeAclId = true;
    private boolean includeAspects = true;
    private boolean includeNodeRef = true;
    private boolean includeOwner = true;
    private boolean includeProperties = true;
    private boolean includePaths = true;
    private boolean includeType = true;
    private boolean includeChildAssociations = true;
    private boolean includeParentAssociations = true;
    private boolean includeChildIds = true;
    private boolean includeTxnId = true;

    public boolean isIncludeAclId()
    {
        return includeAclId;
    }

    public void setIncludeAclId(boolean includeAclId)
    {
        this.includeAclId = includeAclId;
    }

    public boolean isIncludeAspects()
    {
        return includeAspects;
    }

    public void setIncludeAspects(boolean includeAspects)
    {
        this.includeAspects = includeAspects;
    }

    public boolean isIncludeNodeRef()
    {
        return includeNodeRef;
    }

    public void setIncludeNodeRef(boolean includeNodeRef)
    {
        this.includeNodeRef = includeNodeRef;
    }

    public boolean isIncludeOwner()
    {
        return includeOwner;
    }

    public void setIncludeOwner(boolean includeOwner)
    {
        this.includeOwner = includeOwner;
    }

    public boolean isIncludeProperties()
    {
        return includeProperties;
    }

    public void setIncludeProperties(boolean includeProperties)
    {
        this.includeProperties = includeProperties;
    }

    public boolean isIncludePaths()
    {
        return includePaths;
    }

    public void setIncludePaths(boolean includePaths)
    {
        this.includePaths = includePaths;
    }

    public boolean isIncludeType()
    {
        return includeType;
    }

    public void setIncludeType(boolean includeType)
    {
        this.includeType = includeType;
    }

    public boolean isIncludeChildAssociations()
    {
        return includeChildAssociations;
    }

    public void setIncludeChildAssociations(boolean includeChildAssociations)
    {
        this.includeChildAssociations = includeChildAssociations;
    }
    
    public boolean isIncludeParentAssociations()
    {
        return includeParentAssociations;
    }

    public void setIncludeParentAssociations(boolean includeParentAssociations)
    {
        this.includeParentAssociations = includeParentAssociations;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }

    public List<Long> getNodeIds()
    {
        return nodeIds;
    }

    public void setNodeIds(List<Long> nodeIds)
    {
        this.nodeIds = nodeIds;
    }

    public void setTransactionIds(List<Long> txnIds)
    {
        this.transactionIds = txnIds;
    }

    public List<Long> getTransactionIds()
    {
        return transactionIds;
    }

    public Long getFromTxnId()
    {
        return fromTxnId;
    }

    public void setFromTxnId(Long fromTxnId)
    {
        this.fromTxnId = fromTxnId;
    }

    public Long getToTxnId()
    {
        return toTxnId;
    }

    public void setToTxnId(Long toTxnId)
    {
        this.toTxnId = toTxnId;
    }

    public Long getFromNodeId()
    {
        return fromNodeId;
    }

    public void setFromNodeId(Long fromNodeId)
    {
        this.fromNodeId = fromNodeId;
    }

    public Long getToNodeId()
    {
        return toNodeId;
    }

    public void setToNodeId(Long toNodeId)
    {
        this.toNodeId = toNodeId;
    }

    public boolean isIncludeChildIds()
    {
        return includeChildIds;
    }

    public void setIncludeChildIds(boolean includeChildIds)
    {
        this.includeChildIds = includeChildIds;
    }

    public boolean isIncludeTxnId()
    {
        return includeTxnId;
    }

    public void setIncludeTxnId(boolean includeTxnId)
    {
        this.includeTxnId = includeTxnId;
    }
    
}
