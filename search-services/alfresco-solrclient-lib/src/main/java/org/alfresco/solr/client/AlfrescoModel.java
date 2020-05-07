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

import org.alfresco.repo.dictionary.M2Model;

/**
 * Represents an alfresco model and checksum.
 * 
 * @since 4.0
 */
public class AlfrescoModel
{
    private M2Model model;
    private Long checksum;
    
    public AlfrescoModel(M2Model model, Long checksum)
    {
        this.model = model;
        this.checksum = checksum;
    }

    public M2Model getModel()
    {
        return model;
    }

    public Long getChecksum()
    {
        return checksum;
    }
    
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if(!(other instanceof AlfrescoModel))
        {
            return false;
        }

        AlfrescoModel model = (AlfrescoModel)other;
        return (this.model.getName().equals(model.getModel().getName()) &&
        		checksum.equals(model.getChecksum()));
    }

    public int hashcode()
    {
    	int result = 17;
        result = 31 * result + model.hashCode();
        result = 31 * result + Long.valueOf(checksum).hashCode();
        return result;
    }
}
