package org.alfresco.solr;

import org.alfresco.solr.client.Node.SolrApiNodeStatus;

/**
 * @author Andy
 */
public class NodeReport
{
    private Long dbid;

    private Long dbTx;

    private SolrApiNodeStatus dbNodeStatus;

    private Long indexLeafDoc;

    private Long indexAuxDoc;

    private Long indexLeafTx;
    
    private Long indexAuxTx;
    private Long indexedNodeDocCount;
    

    /**
     * @return the dbid
     */
    public Long getDbid()
    {
        return dbid;
    }

    /**
     * @param dbid
     *            the dbid to set
     */
    public void setDbid(Long dbid)
    {
        this.dbid = dbid;
    }

    /**
     * @return the dbTx
     */
    public Long getDbTx()
    {
        return dbTx;
    }

    /**
     * @param dbTx
     *            the dbTx to set
     */
    public void setDbTx(Long dbTx)
    {
        this.dbTx = dbTx;
    }

    /**
     * @return the dbNodeStatus
     */
    public SolrApiNodeStatus getDbNodeStatus()
    {
        return dbNodeStatus;
    }

    /**
     * @param dbNodeStatus
     *            the dbNodeStatus to set
     */
    public void setDbNodeStatus(SolrApiNodeStatus dbNodeStatus)
    {
        this.dbNodeStatus = dbNodeStatus;
    }

    /**
     * @return the indexLeafDoc
     */
    public Long getIndexLeafDoc()
    {
        return indexLeafDoc;
    }

    /**
     * @param indexLeafDoc
     *            the indexLeafDoc to set
     */
    public void setIndexLeafDoc(Long indexLeafDoc)
    {
        this.indexLeafDoc = indexLeafDoc;
    }

    /**
     * @return the indexAuxDoc
     */
    public Long getIndexAuxDoc()
    {
        return indexAuxDoc;
    }

    /**
     * @param indexAuxDoc
     *            the indexAuxDoc to set
     */
    public void setIndexAuxDoc(Long indexAuxDoc)
    {
        this.indexAuxDoc = indexAuxDoc;
    }

    /**
     * @return the indexLeafTx
     */
    public Long getIndexLeafTx()
    {
        return indexLeafTx;
    }

    /**
     * @param indexLeafTx the indexLeafTx to set
     */
    public void setIndexLeafTx(Long indexLeafTx)
    {
        this.indexLeafTx = indexLeafTx;
    }

    /**
     * @return the indexAuxTx
     */
    public Long getIndexAuxTx()
    {
        return indexAuxTx;
    }

    /**
     * @param indexAuxTx the indexAuxTx to set
     */
    public void setIndexAuxTx(Long indexAuxTx)
    {
        this.indexAuxTx = indexAuxTx;
    }

    public Long getIndexedNodeDocCount()
    {
        return indexedNodeDocCount;
    }

    public void setIndexedNodeDocCount(Long indexedNodeDocCount)
    {
        this.indexedNodeDocCount = indexedNodeDocCount;
    }
}
