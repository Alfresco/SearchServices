
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
        setDeque(new LinkedBlockingDeque<T>());
    }

    /**
     * @return
     */
    public int size()
    {
        return getDeque().size();
    }

    public void add(T add)
    {
        while (getDeque().size() > (max - 1))
        {
            getDeque().removeLast();
        }
        getDeque().addFirst(add);
    }

    public T getLast()
    {
        return getDeque().getFirst();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator()
    {
        return getDeque().iterator();
    }

    public LinkedBlockingDeque<T> getDeque()
    {
        return deque;
    }

    public void setDeque(LinkedBlockingDeque<T> deque)
    {
        this.deque = deque;
    }

}