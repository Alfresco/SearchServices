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

import java.util.Comparator;

/**
 * Implementation of the Dual Pivot Quick Sort algorithm, based on the paper by Vladimir Yaroslavskiy,
 * available at <a href="http://iaroslavski.narod.ru/quicksort/DualPivotQuicksort.pdf">DualPivotQuicksort.pdf</a>.
 * 
 * This provides an in-place sorting mechanism, to allow the reuse of arrays containing a large number of objects, reducing
 * the number of temporary object created by the Alfresco Solr indexing service. 
 *
 * @author Alex Miller
 */
public class DualPivotQuickSort
{
    public static final int INSERTION_SORT_THRESHOLD = 17;
    public static final int DIST_SIZE = 13;

    /**
     * Sort elements using comparator, in place.
     */
    public static <T> void sort(T[] elements, Comparator<T> comparator)
    {
        sort(elements, 0, elements.length - 1, comparator);
    }
    

    /**
     * Sort elements between index left and index right, using comparator.
     */
    public static <T> void sort(T[] elements, int left, int right, Comparator<T> comparator)
    {
        int length = right - left + 1;

        // Use insertion sort on tiny arrays
        if (length < INSERTION_SORT_THRESHOLD) 
        {
            insertionSort(elements, left, right, comparator);
        }
        else 
        {
            quickSort(elements, left, right, comparator);
        }
    }

    /**
     * Actually do the dual pivot quick sort on elements from index left to index right, using comparator.
     */
    private static <T> void quickSort(T[] elements, int left, int right, Comparator<T> comparator)
    {
        T x;
        int length = right - left;
        
        // median indexes
        int sixth = length / 6;
        int m1 = left + sixth;
        int m2 = m1 + sixth;
        int m3 = m2 + sixth;
        int m4 = m3 + sixth;
        int m5 = m4 + sixth;
        
        // 5-element sorting network
        if (comparator.compare(elements[m1], elements[m2]) > 0) { x = elements[m1]; elements[m1] = elements[m2]; elements[m2] = x; }
        if (comparator.compare(elements[m4], elements[m5]) > 0) { x = elements[m4]; elements[m4] = elements[m5]; elements[m5] = x; }
        if (comparator.compare(elements[m1], elements[m3]) > 0) { x = elements[m1]; elements[m1] = elements[m3]; elements[m3] = x; }
        if (comparator.compare(elements[m2], elements[m3]) > 0) { x = elements[m2]; elements[m2] = elements[m3]; elements[m3] = x; }
        if (comparator.compare(elements[m1], elements[m4]) > 0) { x = elements[m1]; elements[m1] = elements[m4]; elements[m4] = x; }
        if (comparator.compare(elements[m3], elements[m4]) > 0) { x = elements[m3]; elements[m3] = elements[m4]; elements[m4] = x; }
        if (comparator.compare(elements[m2], elements[m5]) > 0) { x = elements[m2]; elements[m2] = elements[m5]; elements[m5] = x; }
        if (comparator.compare(elements[m2], elements[m3]) > 0) { x = elements[m2]; elements[m2] = elements[m3]; elements[m3] = x; }
        if (comparator.compare(elements[m4], elements[m5]) > 0) { x = elements[m4]; elements[m4] = elements[m5]; elements[m5] = x; }
        
        // pivots: [ < pivot1 | pivot1 <= && <= pivot2 | > pivot2 ]
        T pivot1 = elements[m2];
        T pivot2 = elements[m4];
        boolean diffPivots = comparator.compare(pivot1, pivot2) != 0;
        elements[m2] = elements[left];
        elements[m4] = elements[right];

        // center part pointers
        int less = left + 1;
        int great = right - 1;
        
        // sorting
        if (diffPivots) 
        {
            for (int k = less; k <= great; k++) 
            {
                x = elements[k];
                if (comparator.compare(x, pivot1) < 0) 
                {
                    elements[k] = elements[less];
                    elements[less++] = x;
                } 
                else if (comparator.compare(x, pivot2) > 0) 
                {
                    while ((comparator.compare(elements[great], pivot2) > 0) && (k < great)) 
                    {
                        great--;
                    }
                    elements[k] = elements[great];
                    elements[great--] = x;
                    x = elements[k];
                    if (comparator.compare(x, pivot1) < 0) 
                    {
                        elements[k] = elements[less];
                        elements[less++] = x;
                    }
                }
            }
        }
        else 
        {
            for (int k = less; k <= great; k++) 
            {
                x = elements[k];
                if (comparator.compare(x, pivot1) == 0) 
                {
                    continue;
                } 
                if (comparator.compare(x, pivot1) < 0) 
                {
                    elements[k] = elements[less];
                    elements[less++] = x;
                }
                else 
                {
                    while ((comparator.compare(elements[great], pivot2) > 0) && (k < great)) 
                    {
                        great--;
                    }
                    elements[k] = elements[great];
                    elements[great--] = x;
                    x = elements[k];
                    if (comparator.compare(x, pivot1) < 0) 
                    {
                        elements[k] = elements[less];
                        elements[less++] = x;
                    }
                }
            }
        }
        // swap
        elements[left] = elements[less - 1];
        elements[less - 1] = pivot1;
        elements[right] = elements[great + 1];
        elements[great + 1] = pivot2;

        // left and right parts
        sort(elements, left, less - 2, comparator);
        sort(elements, great + 2, right, comparator);
        
        // equal elements
        if (great - less > length - DIST_SIZE && diffPivots) 
        {
            for (int k = less; k <= great; k++) 
            {
                x = elements[k];
                if (comparator.compare(x, pivot1) == 0) 
                {
                    elements[k] = elements[less];
                    elements[less++] = x;
                }
                else if (comparator.compare(x, pivot2) == 0) 
                {
                    elements[k] = elements[great];
                    elements[great--] = x;
                    x = elements[k];
                    if (x == pivot1) 
                    {
                        elements[k] = elements[less];
                        elements[less++] = x;
                    }
                }
            }
        }
        // center part
        if (diffPivots) 
        {
            sort(elements, less, great, comparator);
        }
    }

    /**
     * DO an insertion sort for elements from index left, to index right, using comparator.
     * 
     * This is more efficient for small arrays.
     */
    private static <T> void insertionSort(T[] elements, int left, int right, Comparator<T> comparator)
    {
        for (int i = left + 1; i <= right; i++) 
        {
            for (int j = i ; j > left ; j --)
            {
                if (comparator.compare(elements[j], elements[j-1]) < 0)
                {
                    T x = elements[j -1];
                    elements[j-1] = elements[j];
                    elements[j] = x;
                }
            }
        }
    }
}
