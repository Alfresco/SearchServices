
package org.alfresco.solr.adapters;

public interface IOpenBitSet
{

    void set(long txid);

    void or(IOpenBitSet duplicatedTxInIndex);

    long nextSetBit(long l);

    long cardinality();

    boolean get(long i);
    
}