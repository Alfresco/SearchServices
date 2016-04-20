package org.alfresco.solr.client;

import java.util.List;

/**
 * @author Andy
 *
 */
public class Transactions
{
    private List<Transaction> transactions;
    
    private Long maxTxnCommitTime;
    
    private Long maxTxnId;
    
    Transactions(List<Transaction> transactions, Long maxTxnCommitTime, Long maxTxnId)
    {
        this.transactions = transactions;
        this.maxTxnCommitTime = maxTxnCommitTime;
        this.maxTxnId = maxTxnId;
    }

    public List<Transaction> getTransactions()
    {
        return transactions;
    }

    public Long getMaxTxnCommitTime()
    {
        return maxTxnCommitTime;
    }
    
    public Long getMaxTxnId()
    {
        return maxTxnId;
    }
    
}
