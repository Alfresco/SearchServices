/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.solr.tracker;

import org.alfresco.solr.InformationServerCollectionProvider;
import org.alfresco.solr.adapters.IOpenBitSet;


public class IndexHealthReport
{
    long dbTransactionCount;

    IOpenBitSet missingTxFromIndex;

    IOpenBitSet duplicatedTxInIndex;

    IOpenBitSet txInIndexButNotInDb;

    IOpenBitSet duplicatedLeafInIndex;
    
    IOpenBitSet duplicatedAuxInIndex;

    long transactionDocsInIndex;
    
    long uniqueTransactionDocsInIndex;
    
    long uniqueAclTransactionDocsInIndex;

    long aclTransactionDocsInIndex;

    long leafDocCountInIndex;
    
    long auxDocCountInIndex;

    long lastIndexedCommitTime;

    long lastIndexedIdBeforeHoles;

    long dbAclTransactionCount;

    IOpenBitSet missingAclTxFromIndex;

    IOpenBitSet duplicatedAclTxInIndex;

    IOpenBitSet aclTxInIndexButNotInDb;
    
    IOpenBitSet duplicatedErrorInIndex;
    
    IOpenBitSet duplicatedUnindexedInIndex;
    
    long errorDocCountInIndex;
    
    long unindexedDocCountInIndex;

    public IndexHealthReport(InformationServerCollectionProvider srv)
    {
        this.missingTxFromIndex = srv.getOpenBitSetInstance();
        this.duplicatedTxInIndex = srv.getOpenBitSetInstance();
        this.txInIndexButNotInDb = srv.getOpenBitSetInstance();
        this.duplicatedLeafInIndex = srv.getOpenBitSetInstance();
        this.duplicatedAuxInIndex = srv.getOpenBitSetInstance();
        
        this.missingAclTxFromIndex = srv.getOpenBitSetInstance();
        this.duplicatedAclTxInIndex = srv.getOpenBitSetInstance();
        this.aclTxInIndexButNotInDb = srv.getOpenBitSetInstance();
        this.duplicatedErrorInIndex = srv.getOpenBitSetInstance();
        this.duplicatedUnindexedInIndex = srv.getOpenBitSetInstance();
    }
    
    /**
     * @return the transactionDocsInIndex
     */
    public long getTransactionDocsInIndex()
    {
        return transactionDocsInIndex;
    }

    public long getAclTransactionDocsInIndex()
    {
        return aclTransactionDocsInIndex;
    }

    /**
     * @param leafDocCountInIndex long
     */
    public void setLeafDocCountInIndex(long leafDocCountInIndex)
    {
        this.leafDocCountInIndex = leafDocCountInIndex;
    }

    /**
     * @return the leafDocCountInIndex
     */
    public long getLeafDocCountInIndex()
    {
        return leafDocCountInIndex;
    }
    
    /**
     * @param auxDocCountInIndex long
     */
    public void setAuxDocCountInIndex(long auxDocCountInIndex)
    {
        this.auxDocCountInIndex = auxDocCountInIndex;
    }

    /**
     * @return the leafDocCountInIndex
     */
    public long getAuxDocCountInIndex()
    {
        return auxDocCountInIndex;
    }

    /**
     * @param txid long
     */
    public void setDuplicatedLeafInIndex(long txid)
    {
        duplicatedLeafInIndex.set(txid);

    }

    /**
     * @return the duplicatedLeafInIndex
     */
    public IOpenBitSet getDuplicatedLeafInIndex()
    {
        return duplicatedLeafInIndex;
    }
    
    /**
     * @param txid long
     */
    public void setDuplicatedAuxInIndex(long txid)
    {
        duplicatedAuxInIndex.set(txid);

    }

    /**
     * @return the duplicatedLeafInIndex
     */
    public IOpenBitSet getDuplicatedAuxInIndex()
    {
        return duplicatedAuxInIndex;
    }

    /**
     * @param transactionDocsInIndex
     *            the transactionDocsInIndex to set
     */
    public void setTransactionDocsInIndex(long transactionDocsInIndex)
    {
        this.transactionDocsInIndex = transactionDocsInIndex;
    }

    /**
     * @param aclTransactionDocsInIndex
     *            the transactionDocsInIndex to set
     */
    public void setAclTransactionDocsInIndex(long aclTransactionDocsInIndex)
    {
        this.aclTransactionDocsInIndex = aclTransactionDocsInIndex;
    }

    /**
     * @return the missingFromIndex
     */
    public IOpenBitSet getMissingTxFromIndex()
    {
        return missingTxFromIndex;
    }

    /**
     * @return the missingFromIndex
     */
    public IOpenBitSet getMissingAclTxFromIndex()
    {
        return missingAclTxFromIndex;
    }

    /**
     * @return the duplicatedInIndex
     */
    public IOpenBitSet getDuplicatedTxInIndex()
    {
        return duplicatedTxInIndex;
    }

    /**
     * @return the duplicatedInIndex
     */
    public IOpenBitSet getDuplicatedAclTxInIndex()
    {
        return duplicatedAclTxInIndex;
    }

    /**
     * @return the inIndexButNotInDb
     */
    public IOpenBitSet getTxInIndexButNotInDb()
    {
        return txInIndexButNotInDb;
    }

    /**
     * @return the inIndexButNotInDb
     */
    public IOpenBitSet getAclTxInIndexButNotInDb()
    {
        return aclTxInIndexButNotInDb;
    }

    /**
     * @return the dbTransactionCount
     */
    public long getDbTransactionCount()
    {
        return dbTransactionCount;
    }

    /**
     * @param dbTransactionCount
     *            the dbTransactionCount to set
     */
    public void setDbTransactionCount(long dbTransactionCount)
    {
        this.dbTransactionCount = dbTransactionCount;
    }

    public void setMissingTxFromIndex(long txid)
    {
        missingTxFromIndex.set(txid);
    }
    
    public void setMissingAclTxFromIndex(long txid)
    {
        missingAclTxFromIndex.set(txid);
    }

    public void setDuplicatedTxInIndex(long txid)
    {
        duplicatedTxInIndex.set(txid);
    }
    
    public void setDuplicatedAclTxInIndex(long txid)
    {
        duplicatedAclTxInIndex.set(txid);
    }

    public void setTxInIndexButNotInDb(long txid)
    {
        txInIndexButNotInDb.set(txid);
    }
    
    public void setAclTxInIndexButNotInDb(long txid)
    {
        aclTxInIndexButNotInDb.set(txid);
    }

    /**
     * @param lastIndexedCommitTime
     *            the lastIndexCommitTime to set
     */
    public void setLastIndexedCommitTime(long lastIndexedCommitTime)
    {
        this.lastIndexedCommitTime = lastIndexedCommitTime;
    }

    /**
     * @return the lastIndexedIdBeforeHoles
     */
    public long getLastIndexedIdBeforeHoles()
    {
        return lastIndexedIdBeforeHoles;
    }

    /**
     * @param lastIndexedIdBeforeHoles
     *            the lastIndexedIdBeforeHoles to set
     */
    public void setLastIndexedIdBeforeHoles(long lastIndexedIdBeforeHoles)
    {
        this.lastIndexedIdBeforeHoles = lastIndexedIdBeforeHoles;
    }

    /**
     * @param dbAclTransactionCount long
     */
    public void setDbAclTransactionCount(long dbAclTransactionCount)
    {
        this.dbAclTransactionCount = dbAclTransactionCount;
    }

    /**
     * @return the dbAclTransactionCount
     */
    public long getDbAclTransactionCount()
    {
        return dbAclTransactionCount;
    }

    /**
     * @return the uniqueTransactionDocsInIndex
     */
    public long getUniqueTransactionDocsInIndex()
    {
        return uniqueTransactionDocsInIndex;
    }

    /**
     * @param uniqueTransactionDocsInIndex the uniqueTransactionDocsInIndex to set
     */
    public void setUniqueTransactionDocsInIndex(long uniqueTransactionDocsInIndex)
    {
        this.uniqueTransactionDocsInIndex = uniqueTransactionDocsInIndex;
    }

    /**
     * @return the uniqueAclTransactionDocsInIndex
     */
    public long getUniqueAclTransactionDocsInIndex()
    {
        return uniqueAclTransactionDocsInIndex;
    }

    /**
     * @param uniqueAclTransactionDocsInIndex the uniqueAclTransactionDocsInIndex to set
     */
    public void setUniqueAclTransactionDocsInIndex(long uniqueAclTransactionDocsInIndex)
    {
        this.uniqueAclTransactionDocsInIndex = uniqueAclTransactionDocsInIndex;
    }

    /**
     * @return the lastIndexedCommitTime
     */
    public long getLastIndexedCommitTime()
    {
        return lastIndexedCommitTime;
    }

    /**
     * @param unindexedDocCountInIndex long
     */
    public void setUnindexedDocCountInIndex(long unindexedDocCountInIndex)
    {
        this.unindexedDocCountInIndex = unindexedDocCountInIndex;
    }

    /**
     * @param dbId long
     */
    public void setDuplicatedUnindexedInIndex(long dbId)
    {
        duplicatedUnindexedInIndex.set(dbId);
    }

    /**
     * @param errorDocCountInIndex long
     */
    public void setErrorDocCountInIndex(long errorDocCountInIndex)
    {
        this.errorDocCountInIndex = errorDocCountInIndex;
    }

    /**
     * @param dbId long
     */
    public void setDuplicatedErrorInIndex(long dbId)
    {
        duplicatedErrorInIndex.set(dbId);
        
    }

    /**
     * @return the duplicatedErrorInIndex
     */
    public IOpenBitSet getDuplicatedErrorInIndex()
    {
        return duplicatedErrorInIndex;
    }

    /**
     * @return the duplicatedUnindexedInIndex
     */
    public IOpenBitSet getDuplicatedUnindexedInIndex()
    {
        return duplicatedUnindexedInIndex;
    }

    /**
     * @return the errorDocCountInIndex
     */
    public long getErrorDocCountInIndex()
    {
        return errorDocCountInIndex;
    }

    /**
     * @return the unindexedDocCountInIndex
     */
    public long getUnindexedDocCountInIndex()
    {
        return unindexedDocCountInIndex;
    }
    
    

}