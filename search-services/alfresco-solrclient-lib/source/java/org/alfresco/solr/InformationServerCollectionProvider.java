package org.alfresco.solr;

import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.adapters.ISimpleOrderedMap;

public interface InformationServerCollectionProvider 
{
	 IOpenBitSet getOpenBitSetInstance();

	 <T> ISimpleOrderedMap<T> getSimpleOrderedMapInstance();
}
