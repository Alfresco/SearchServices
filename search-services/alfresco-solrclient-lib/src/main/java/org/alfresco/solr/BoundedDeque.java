/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

package org.alfresco.solr;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

public class BoundedDeque<T> implements Iterable<T>
{
    private LinkedBlockingDeque<T> deque;

    private int max = 10;

    public BoundedDeque(int max)
    {
        this.max = max;
        this.deque = new LinkedBlockingDeque<T>();
    }

    /**
     * @return int
     */
    public int size()
    {
        return deque.size();
    }

    public void add(T add)
    {
        while (deque.size() > (max - 1))
        {
            deque.removeLast();
        }
        deque.addFirst(add);
    }

    public T getLast()
    {
        return deque.getFirst();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator()
    {
        return deque.iterator();
    }

    /** Get a copy of the deque. */
    public LinkedBlockingDeque<T> getDeque()
    {
        return new LinkedBlockingDeque(deque);
    }

    public void setDeque(LinkedBlockingDeque<T> deque)
    {
        this.deque = new LinkedBlockingDeque(deque);
    }
}
