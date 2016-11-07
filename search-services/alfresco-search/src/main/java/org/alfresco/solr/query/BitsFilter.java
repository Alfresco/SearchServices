/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
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

package org.alfresco.solr.query;

import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.solr.search.BitsFilteredDocIdSet;
import org.apache.solr.search.Filter;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;


/*
* A segment level Lucene Filter where each segment is backed by a FixedBitSet.
*/

public class BitsFilter extends Filter {

    private final List<FixedBitSet> bitSets;

    public BitsFilter(List<FixedBitSet> bitSets)
    {
        if (bitSets == null) throw new IllegalStateException("bitSets cannot be null");
        this.bitSets = bitSets;
    }

    public void or(BitsFilter bitsFilter)
    {
        List<FixedBitSet> andSets = bitsFilter.bitSets;
        for(int i=0; i<bitSets.size(); i++)
        {
            FixedBitSet a = bitSets.get(i);
            FixedBitSet b = andSets.get(i);
            a.or(b);
        }
    }

    public void and(BitsFilter bitsFilter)
    {
        List<FixedBitSet> andSets = bitsFilter.bitSets;
        for(int i=0; i<bitSets.size(); i++)
        {
            FixedBitSet a = bitSets.get(i);
            FixedBitSet b = andSets.get(i);
            a.and(b);
        }
    }

    public List<FixedBitSet> getBitSets()
    {
        return this.bitSets;
    }

    public String toString(String s) {
        return s;
    }

	public DocIdSet getDocIdSet(LeafReaderContext context, Bits bits) {
		return BitsFilteredDocIdSet.wrap(new BitDocIdSet(bitSets.get(context.ord)), bits);
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BitsFilter)) return false;

        BitsFilter that = (BitsFilter) o;

        if (!bitSets.equals(that.bitSets)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return bitSets.hashCode();
    }
}
