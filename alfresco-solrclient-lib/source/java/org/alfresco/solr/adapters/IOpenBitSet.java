/*
 * Copyright (C) 2014 Alfresco Software Limited.
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