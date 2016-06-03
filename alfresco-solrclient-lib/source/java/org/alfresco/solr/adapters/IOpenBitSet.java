
package org.alfresco.solr.adapters;

/**
 * The reason we have this interface is so that lucene-free dependent classes can be dependent on IOpenBitSet instead of the
 * lucene-version-specific OpenBitSet.
 * @author Ahmed Owian
 */
public interface IOpenBitSet
{

    void set(long txid);

    void or(IOpenBitSet duplicatedTxInIndex);

    long nextSetBit(long l);

    long cardinality();

    boolean get(long i);
    
}