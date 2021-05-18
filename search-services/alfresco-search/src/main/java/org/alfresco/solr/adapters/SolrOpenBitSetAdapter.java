/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.solr.adapters;

import org.apache.lucene.util.LongBitSet;

/**
 * The reason we have this class is so that lucene-free dependent classes can be dependent on IOpenBitSet instead of the
 * lucene-version-specific OpenBitSet.
 * @author Ahmed Owian
 */
public class SolrOpenBitSetAdapter implements IOpenBitSet
{	
	LongBitSet delegate;
	
	public SolrOpenBitSetAdapter()
	{
		delegate = new LongBitSet(64);
	}

	@Override
	public void set(long index) 
	{
		delegate = LongBitSet.ensureCapacity(delegate, index);
		delegate.set(index);
	}

	@Override
	public void or(IOpenBitSet other) 
	{
		delegate.or( ((SolrOpenBitSetAdapter)other).delegate);
	}

	@Override
	public long nextSetBit(long index) 
	{
		return delegate.nextSetBit(index);
	}

	@Override
	public long cardinality() 
	{
		return delegate.cardinality();
	}

	@Override
	public boolean get(long index) 
	{
		return delegate.get(index);
	}


}
