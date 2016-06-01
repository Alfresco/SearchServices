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

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.util.FixedBitSet;

/**
*  The HybridBitSet is a random access (doesn't support iteration) BitSet implementation that uses a FixedBitSet for the first N bits
*  and a HashSet for higher bits. This is designed to provide a balance between the high performance of FixedBitSet and
*  the efficient sparse behavior of a HashSet.
**/

public class HybridBitSet
{
    private FixedBitSet bits;
    private Set<Long> set = new HashSet<Long>();
    private int maxBit;

    public HybridBitSet()
    {

    }

    HybridBitSet(int maxBit)
    {
        this.bits = new FixedBitSet(maxBit);
        this.maxBit = maxBit;
    }

    public void set(long bit)
    {
        if(bit < maxBit)
        {
            bits.set((int)bit);
        }
        else
        {
            set.add((long)bit);
        }
    }

    public boolean get(long bit)
    {
        if(bit < maxBit)
        {
            return bits.get((int)bit);
        }
        else
        {
            return set.contains((long)bit);
        }
    }
}
