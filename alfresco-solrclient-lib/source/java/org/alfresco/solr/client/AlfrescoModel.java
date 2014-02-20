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
