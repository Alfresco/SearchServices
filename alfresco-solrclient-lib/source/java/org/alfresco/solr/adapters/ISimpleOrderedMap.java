package org.alfresco.solr.adapters;


/**
 * The reason we have this interface is so that lucene-free dependent classes can be dependent on ISimpleOrderedMap instead of the
 * lucene-version-specific SimpleOrderedMap.
 * @author Ahmed Owian
 */
public interface ISimpleOrderedMap<T>
{

    void add(String name, T val);

}
