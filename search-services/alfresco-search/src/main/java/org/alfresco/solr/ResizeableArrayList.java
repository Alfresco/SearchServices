/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
package org.alfresco.solr;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

/**
 * A {@link List} implementation, backed by an array, taht supports resizing.
 * 
 * This class supports the reuse of arrays across cache instances, reducing the number of
 * temporary objects created by the Alfresco Solr indexing service.
 *
 * @author Alex Miller
 */
public class ResizeableArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable
{
    private static final long serialVersionUID = 1L;

    private Object[] values;
    private int size;
    boolean active = false;
    
    public ResizeableArrayList()
    {
        this(10);
    }
    
    public ResizeableArrayList(int initialSize)
    {
        values = new Object[initialSize];
        size = initialSize;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(int index)
    {
        checkSize(index);
        return (E)values[index];
    }

    private void checkSize(int index)
    {
        if (index >= size)
        {
            throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + values.length);
        }
    }

    /**
     * Resize the underlying to at least minSize.
     * 
     * @param minSize
     * @throws IllegalStateException if the instance is not active.
     */
    public void resize(int minSize)
    {
        isActive();
        int oldSize = values.length;
        if (minSize > oldSize) 
        {
            int newSize = (oldSize * 3)/2 + 1;
            if (newSize < minSize)
            {
                newSize = minSize;
            }
            // minCapacity is usually close to size, so this is a win:
            values = Arrays.copyOf(values, newSize);
        }
        size = minSize;
    }

    @Override
    public int size()
    {
        return size;
    }

    /**
     * Copy elements from from into this instance
     */
    public <T> void copyFrom(ResizeableArrayList<T> from)
    {
        isActive();
        values = Arrays.copyOf(from.values, from.values.length);
        size = from.size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E set(int index, E value)
    {
        checkSize(index);
        isActive();
        
        E oldValue = (E)values[index];
        values[index] = value;
        return oldValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        try {
            ResizeableArrayList<E> v = (ResizeableArrayList<E>) super.clone();
            v.values = Arrays.copyOf(values, values.length);
            v.size = size;
            return v;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
    
    private void isActive()
    {
        if (!active)
        {
            throw new IllegalStateException("Not active");
        }
    }
    
    void activate()
    {
        active = true;
    }
    
    void deactivate()
    {
        active = false;
        for (int i = 0 ; i < size ; i++)
        {
            values[i] = null;
        }
    }

    /**
     * Sort the elements, in-place, contained in this list using {@link Comparator#}
     * 
     * @param comparator The {@link Comparator} to sort with
     */
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> comparator)
    {
        DualPivotQuickSort.sort((E[])values, 0, size -1 , comparator);
    }
}
