/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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

    

}
