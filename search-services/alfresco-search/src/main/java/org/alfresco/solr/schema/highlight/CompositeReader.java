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

package org.alfresco.solr.schema.highlight;

import static java.util.Arrays.asList;

import org.alfresco.solr.utils.Utils;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link Reader} able to wrap multiple readers.
 * This class acts as a Facade if the wrapped readers, meaning with that its consumer will access to the underlying
 * content in a transparent way: the composite reader takes care about switching on the next wrapped reader when the
 * current one is exhausted.
 *
 * @author Andrea Gazzarini
 */
class CompositeReader extends Reader
{
    private final Iterator<Reader> iterator;
    private final List<Closeable> exhausted;
    private Reader current;

    public CompositeReader(final Reader ... readers)
    {
        if (readers == null || readers.length == 0)
        {
            throw new IllegalArgumentException("At least one reader instance is needed.");
        }
        this.exhausted = asList(readers);
        this.iterator = asList(readers).iterator();
        current = iterator.next();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        int read = current.read(cbuf, off, len);
        if (read == -1 && iterator.hasNext())
        {
            current = iterator.next();
            read = current.read(cbuf, off, len);
        }
        else if (read < len && iterator.hasNext())
        {
            current = iterator.next();
            read += current.read(cbuf, read, len - read);
        }

        return read;
    }

    @Override
    public void close()
    {
        exhausted.forEach(Utils::silentyClose);
    }
}
