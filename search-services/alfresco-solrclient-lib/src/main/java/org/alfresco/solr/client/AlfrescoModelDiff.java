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

import org.alfresco.service.namespace.QName;

/**
 * Represents a diff between the set of current repository Alfresco models and the set maintained in SOLR.
 * The diff can represent a new, changed or removed Alfresco model. For a new model the newChecksum is
 * populated; for a changed model both checksums are populated; for a removed model neither checksum is populated.
 * 
 * @since 4.0
 */
public class AlfrescoModelDiff
{
    public static enum TYPE
    {
        NEW, CHANGED, REMOVED;
    };
    
    private QName modelName;
    private TYPE type;
    private Long oldChecksum;
    private Long newChecksum;

    public AlfrescoModelDiff(QName modelName, TYPE type, Long oldChecksum, Long newChecksum)
    {
        super();
        this.modelName = modelName;
        this.type = type;
        this.oldChecksum = oldChecksum;
        this.newChecksum = newChecksum;
    }

    public QName getModelName()
    {
        return modelName;
    }

    public TYPE getType()
    {
        return type;
    }

    public Long getOldChecksum()
    {
        return oldChecksum;
    }

    public Long getNewChecksum()
    {
        return newChecksum;
    }
    
    public boolean equals(Object other)
    {
        if(this == other)
    	{
        	return true;
    	}
        if(!(other instanceof AlfrescoModelDiff))
    	{
        	return false;
    	}
        AlfrescoModelDiff diff = (AlfrescoModelDiff)other;
        return(diff.getModelName().equals(getModelName()) &&
        		diff.getType().equals(getType()) &&
        		diff.getOldChecksum().equals(getOldChecksum()) &&
        		diff.getNewChecksum().equals(getNewChecksum()));
    }
    
    public int hashcode()
    {
    	int result = 17;
        result = 31 * result + getModelName().hashCode();
        result = 31 * result + getType().hashCode();
        result = 31 * result + (int)(getOldChecksum().longValue() ^ (getOldChecksum().longValue() >>> 32));
        return result;
    }
}
