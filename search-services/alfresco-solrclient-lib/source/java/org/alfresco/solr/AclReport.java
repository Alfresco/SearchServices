package org.alfresco.solr;

/**
 * @author Andy
 *
 */
public class AclReport
{

    private Long aclId;
    
    private boolean existsInDb;
    
    private Long indexAclDoc;
    
    private Long indexAclTx;
    private Long indexedAclDocCount;

    /**
     * @return the aclId
     */
    public Long getAclId()
    {
        return aclId;
    }

    /**
     * @param aclId the aclId to set
     */
    public void setAclId(Long aclId)
    {
        this.aclId = aclId;
    }

    /**
     * @return the existsInDb
     */
    public boolean isExistsInDb()
    {
        return existsInDb;
    }

    /**
     * @param existsInDb the existsInDb to set
     */
    public void setExistsInDb(boolean existsInDb)
    {
        this.existsInDb = existsInDb;
    }

    /**
     * @return the indexAclDoc
     */
    public Long getIndexAclDoc()
    {
        return indexAclDoc;
    }

    /**
     * @param indexAclDoc the indexAclDoc to set
     */
    public void setIndexAclDoc(Long indexAclDoc)
    {
        this.indexAclDoc = indexAclDoc;
    }

    /**
     * @return the indexAclTx
     */
    public Long getIndexAclTx()
    {
        return indexAclTx;
    }

    /**
     * @param indexAclTx the indexAclTx to set
     */
    public void setIndexAclTx(Long indexAclTx)
    {
        this.indexAclTx = indexAclTx;
    }

    public Long getIndexedAclDocCount()
    {
        return indexedAclDocCount;
    }

    public void setIndexedAclDocCount(Long indexedAclDocCount)
    {
        this.indexedAclDocCount = indexedAclDocCount;
    }
}
